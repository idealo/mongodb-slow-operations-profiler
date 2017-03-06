/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.collector;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mongodb.ServerAddress;
import de.idealo.mongodb.slowops.dto.ApplicationStatusDto;
import de.idealo.mongodb.slowops.dto.CollectorServerDto;
import de.idealo.mongodb.slowops.dto.CollectorStatusDto;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import de.idealo.mongodb.slowops.jmx.CollectorManagerMBean;
import de.idealo.mongodb.slowops.util.ConfigReader;
import de.idealo.mongodb.slowops.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * 
 * 
 * @author kay.agahd
 * @since 27.02.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
class CollectorManager extends Thread implements CollectorManagerMBean, ServerChecker {

    private static final Logger LOG = LoggerFactory.getLogger(CollectorManager.class);
    
    private final BlockingQueue<ProfilingEntry> jobQueue;
    private List<ProfilingReader> readers;
    private ProfilingWriter writer;
    private boolean stop;
    private long doneJobsOfRemovedReaders;
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();
    private final Lock readLock;
    private final Lock writeLock;
    private String logLine1 = null;
    private String logLine2 = null;

    
    
    private void startWriter() {
        writer = new ProfilingWriter(jobQueue);
        writer.start();
    }
    
    private void startReaders() {
        readers = new LinkedList<ProfilingReader>();

        List<ProfiledServerDto> profiledServers = ConfigReader.getProfiledServers();


        for (ProfiledServerDto dto : profiledServers) {


            //resolve all mongod addresses for this dto to be profiled
            HashSet<ServerAddress> allServerAddresses = new HashSet<ServerAddress>();
            for (ServerAddress serverAddress : dto.getHosts()) {
                List<ServerAddress> resolvedAddresses = Util.getMongodAddresses(dto.getAdminUser(), dto.getAdminPw(), serverAddress);
                allServerAddresses.addAll(resolvedAddresses);
            }

            HashMap<String, List<String>> collectionsPerDb = dto.getCollectionsPerDatabase();

            //create for each mongod and for each each database to profile one reader
            for (ServerAddress mongodAddress : allServerAddresses) {
                for (String db : collectionsPerDb.keySet()) {
                    if(!isReaderExists(mongodAddress, db)){//dont't create readers twice
                        createOneReader(0, mongodAddress, dto, db, collectionsPerDb.get(db), !dto.isEnabled(), dto.getSlowMs());
                    }else{
                        LOG.warn("Misconfigration detected because multiple profilers are defined for: {}/{}", mongodAddress, db );
                    }
               }
            }
        }


    }
    
    
    private void createOneReader(int id, ServerAddress address, ProfiledServerDto dto, String db, List<String> colls, boolean stop, long ms) {
        
            final Date lastTs = writer.getNewest(address, db);
            final ProfilingReader reader = new ProfilingReader(id, jobQueue, address, lastTs, dto, db, colls, stop, 0, ms, this);
            
            writeLock.lock();
            try{
                readers.add(reader);
            }finally{
                writeLock.unlock();
            }
            
            reader.start();//may be stopped if dto is not "enabled"
            
            LOG.info("New reader created for {}/{}", reader.getServerAddress(), reader.getDatabase());
    }
    
    
    @Override
    public synchronized void checkForNewOrRemovedMongods(ProfilingReader reader) {
        if(reader != null) {
            LOG.info(">>> checkForNewOrRemovedMongods: {} - {}/{}", new Object[]{reader.getLabel(), reader.getServerAddress(), reader.getDatabase()});
            ProfiledServerDto dto = reader.getProfiledServerDto();
            List<ServerAddress> currentServers = Util.getMongodAddresses(dto.getAdminUser(), dto.getAdminPw(), reader.getServerAddress());
            Set<ServerAddress> currentServersSet = Sets.newHashSet(currentServers);
            
            if(!currentServersSet.contains(reader.getServerAddress())) {
                //profiled server was removed from replSet or shard system, so stop reader and remove it
                if(removeReader(reader)) {
                    LOG.info("Reader removed for {}", reader.getServerAddress());
                }else {
                    LOG.error("Could not remove reader for {}", reader.getServerAddress());
                }
            }else {
                LOG.info("replSet or or shard system (still) contains {}", reader.getServerAddress());
            }
            
            //check if there are any new servers (thus new addresses) in replSet or Shard system and create readers for it
            List<ServerAddress> runningReaders = getExistingReaders();
            currentServers.removeAll(runningReaders);
            for (ServerAddress newAddress : currentServers) {
                createOneReader(reader.getIntanceId(), newAddress, dto, reader.getDatabase(), reader.getCollections(), reader.isStopped(), reader.getSlowMs());
            }
            
            //there may be readers for mongod's which are no more in DNS but as long as we can connect to them, profile them 
            LOG.info("<<< checkForNewOrRemovedMongods: " + reader.getLabel() + " - " + reader.getServerAddress() + "/" + reader.getDatabase());
        }
    }
    
    private List<ServerAddress> getExistingReaders() {
        List<ServerAddress> result = Lists.newLinkedList();
        readLock.lock();
        try{
            for (ProfilingReader reader : readers) {
                result.add(reader.getServerAddress());
            }
        }finally{
            readLock.unlock();
        }
        return result;
    }
    
    
    private boolean removeReader(ProfilingReader r) {
        
        writeLock.lock();
        try{
            r.terminate();
            for (ProfilingReader reader : readers) {
                if(reader.getIntanceId() == r.getIntanceId()) {
                    doneJobsOfRemovedReaders += r.getDoneJobs();
                    return readers.remove(reader);
                }
            }
        }finally{
            writeLock.unlock();
        }
        
        return false;
    }

    private boolean isReaderExists(ServerAddress adr, String db) {

        readLock.lock();
        try{
            for (ProfilingReader reader : readers) {
                if(reader.getServerAddress().equals(adr) && reader.getDatabase().equals(db)) {
                    return true;
                }
            }
        }finally{
            readLock.unlock();
        }

        return false;
    }
    
    
    
    private void registerMBean() {
        
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            final ObjectName name = new ObjectName(this.getClass().getPackage().getName() + ":type=" + this.getClass().getSimpleName());
            final StandardMBean mbean = new StandardMBean(this, CollectorManagerMBean.class, false);
            server.registerMBean(mbean, name);
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException | NotCompliantMBeanException | MBeanRegistrationException | NullPointerException e) {
            LOG.error("Error while registering MBean", e);
        }
    }
    
    private void unregisterMBean() {
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            final ObjectName name = new ObjectName(this.getClass().getPackage().getName() + ":type=" + this.getClass().getSimpleName());
            server.unregisterMBean(name);
        } catch (MalformedObjectNameException | MBeanRegistrationException | InstanceNotFoundException | IllegalStateException e) {
            LOG.error("Error while unregistering MBean", e);
        }
    }


    
    CollectorManager() {
        
        jobQueue = new LinkedBlockingQueue<ProfilingEntry>();
        doneJobsOfRemovedReaders = 0;
        readLock = globalLock.readLock();
        writeLock = globalLock.writeLock();

        registerMBean();
        addShutdownHook();
    }
    
    
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                terminate();
            }
        });
    }

    public void terminate() {
        
        try {
            readLock.lock();
            try{
            
                if(readers != null) {
                    for (ProfilingReader r : readers) {
                        try {
                            r.terminate();
                        } catch (Throwable e) {
                            LOG.error("Error while terminating reader ", e);
                        }
                    }
                }
            }finally{
                readLock.unlock();
            }
        } catch (Throwable e) {
            LOG.error("Error while terminating readers ", e);
        }
        
        try {
            if(writer != null) {
                writer.terminate();
            }
        } catch (Throwable e) {
            LOG.error("Error while terminating writers ", e);
        }
        
        try {
            unregisterMBean();
        } catch (Throwable e) {
            LOG.error("Error while unregistering MBeans ", e);
        }
        
        stop = true;
        
        interrupt();//need to interrupt when sleeping
    }
    
    public void startup() {
        this.start();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        stop = false;
        try {
        
            startWriter();
            
            startReaders();
        
        
            while(!stop) {
                monitor();
                
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    LOG.error("Exception while sleeping.", e);
                }
            }
        }finally {
            terminate();
        }
        LOG.info("Terminated");
    }

    @Override
    public long getNumberOfReadsOfRemovedReaders(){
        return doneJobsOfRemovedReaders;
    }
    
    @Override
    public long getNumberOfReads(){
        long result = 0;
        readLock.lock();
        try{
        
            if(readers != null) {
                for (ProfilingReader r : readers) {
                    result += r.getDoneJobs();
                }
            }
        }finally{
            readLock.unlock();
        }
        
        return result;
    }
    
        
    @Override
    public long getNumberOfWrites(){
        if(writer != null) {
            return writer.getDoneJobs();
        }
        return 0;
    }

    @Override
    public Date getRunningSince(){
        if(writer != null) {
            return writer.getRuningSince();
        }
        return new Date();
    }

    public ApplicationStatusDto getApplicationStatus() {
        LOG.debug(">>> getApplicationStatus");
      List<Integer> idList = Lists.newLinkedList();
        readLock.lock();
        try{
            for (ProfilingReader reader : readers) {
                idList.add(reader.getIntanceId());
            }
        }finally{
            readLock.unlock();
        }
        LOG.debug("<<< getApplicationStatus");
        return getApplicationStatus(idList);
    }

    public ApplicationStatusDto getApplicationStatus(List<Integer> idList) {
        LOG.debug(">>> getApplicationStatus listSize: {} ", idList.size());
        ApplicationStatusDto result = new ApplicationStatusDto();
        List<CollectorStatusDto> collectorStatuses = Lists.newLinkedList();
        HashSet<Integer> idSet = new HashSet<Integer>();
        idSet.addAll(idList);

        ExecutorService executor = Executors.newFixedThreadPool(idList.size());
        List<Future<CollectorStatusDto>> futureList = new ArrayList<>();

        readLock.lock();
        try{
            for (ProfilingReader reader : readers) {
                if(idSet.contains(reader.getIntanceId())) {
                    Future<CollectorStatusDto> future = executor.submit((Callable) reader);
                    futureList.add(future);
                }
            }
        }finally{
            readLock.unlock();
        }

        for(Future<CollectorStatusDto> future : futureList){
            try {
                CollectorStatusDto dto = future.get();
                collectorStatuses.add(dto);//wait until future gets a result

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();

        result.setCollectorStatuses(collectorStatuses);

        CollectorServerDto dto = getCollectorServerDto();
        if(dto != null){
            result.setCollectorServerDto(new CollectorServerDto(dto.getHosts(), dto.getDb(), dto.getCollection(), "", ""));//omit user/pw
        }

        result.setNumberOfReads(getNumberOfReads());

        result.setNumberOfWrites(getNumberOfWrites());

        result.setNumberOfReadsOfRemovedReaders(getNumberOfReadsOfRemovedReaders());

        result.setCollectorRunningSince(getRunningSince());

        LOG.debug("<<< getApplicationStatus");
        return result;
    }

    public void startStopProfilingReaders(List<Integer> idList, boolean stop){
        writeLock.lock();
        try{
            for(int id : idList){
                for (ProfilingReader reader : readers) {
                    if(id == reader.getIntanceId()){
                        if(stop){
                            reader.terminate();
                        }else if(reader.isStopped()){
                            ProfilingReader newReader = new ProfilingReader(
                                    reader.getIntanceId(),
                                    jobQueue,
                                    reader.getServerAddress(),
                                    reader.getLastTs(),
                                    reader.getProfiledServerDto(),
                                    reader.getDatabase(),
                                    reader.getCollections(),
                                    stop,
                                    reader.getDoneJobs(),
                                    reader.getSlowMs(),
                                    this
                            );
                            readers.add(newReader);
                            readers.remove(reader);

                            newReader.start();

                        }
                        break;
                    }
                }
            }

        }finally{
            writeLock.unlock();
        }
    }

    public void removeProfilingReaders(List<Integer> idList){
        writeLock.lock();
        try{
            for(int id : idList){
                for (ProfilingReader reader : readers) {
                    if(id == reader.getIntanceId()){
                        removeReader(reader);
                        break;
                    }
                }
            }

        }finally{
            writeLock.unlock();
        }
    }

    public void setSlowMs(List<Integer> idList, String ms){
        try {
            long slowMs = Long.parseLong(ms);
            writeLock.lock();
            try{
                for(int id : idList){
                    for (ProfilingReader reader : readers) {
                        if(id == reader.getIntanceId()){
                            reader.setSlowMs(1, slowMs);
                            break;
                        }
                    }
                }

            }finally{
                writeLock.unlock();
            }
        } catch (NumberFormatException e) {
            LOG.warn("slowMS must be numeric but was: {}", ms);
        }
    }


    public CollectorServerDto getCollectorServerDto(){
        if(writer != null){
            return writer.getCollectorServerDto();
        }
        return null;
    }
    
    private void monitor() {
        long allReadersDoneJobs = 0;
        final List<ProfilingReader> stoppedReaders = Lists.newArrayList();
        if(readers != null) {
            readLock.lock();
            try{ 
                StringBuffer sb = new StringBuffer();
                for (ProfilingReader r : readers) {
                    allReadersDoneJobs += r.getDoneJobs();
                    sb.append(r.getServerAddress()+"/"+r.getDatabase());
                    if(r.isStopped()) {
                        sb.append("(stopped)");
                        stoppedReaders.add(r);
                    }
                    sb.append("=");
                    sb.append(r.getDoneJobs());
                    sb.append(" ");
                }
                if(!sb.toString().equals(logLine1)) {
                    logLine1 = sb.toString(); 
                    LOG.info("Read {}", logLine1);
                }
                
            }finally{
                readLock.unlock();
            }
        }
        final String logLine = "Read all: " + (allReadersDoneJobs+ doneJobsOfRemovedReaders) + " Written: " + writer.getDoneJobs() + " Stopped: " + stoppedReaders.size();
        if(!logLine.equals(logLine2)) {
            logLine2 = logLine; 
            LOG.info(logLine);
        }
    }
    
    

    public static void main(String[] args) throws UnknownHostException {
        final CollectorManager result = new CollectorManager();
        result.startup();
    }



    

    
}
