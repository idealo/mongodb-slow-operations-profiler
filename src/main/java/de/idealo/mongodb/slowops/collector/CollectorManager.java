/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.collector;

import com.google.common.collect.Lists;
import com.mongodb.ServerAddress;
import de.idealo.mongodb.slowops.dto.ApplicationStatusDto;
import de.idealo.mongodb.slowops.dto.CollectorServerDto;
import de.idealo.mongodb.slowops.dto.CollectorStatusDto;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import de.idealo.mongodb.slowops.jmx.CollectorManagerMBean;
import de.idealo.mongodb.slowops.util.ConfigReader;
import de.idealo.mongodb.slowops.util.MongoResolver;
import de.idealo.mongodb.slowops.util.ProfilingReaderCreator;
import de.idealo.mongodb.slowops.util.Terminator;
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
public class CollectorManager extends Thread implements CollectorManagerMBean {

    private static final Logger LOG = LoggerFactory.getLogger(CollectorManager.class);
    
    private final BlockingQueue<ProfilingEntry> jobQueue;
    private List<ProfilingReader> readers;
    private ProfilingWriter writer;
    private boolean stop;
    private long doneJobsOfRemovedReaders;
    private long doneJobsOfRemovedWriters;
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();
    private final Lock readLock;
    private final Lock writeLock;
    private String logLine1 = null;
    private String logLine2 = null;


    public void reloadConfig(String cfg){
        if(ConfigReader.reloadConfig(cfg)){
            LOG.info("new config has been applied");

            terminateReadersAndWriters(false);
            startWriter();
            startReaders();
        }else{
            LOG.warn("new config was not applied");
        }
    }
    
    private void startWriter() {
        LOG.info(">>> start writer");
        boolean isSameWriter = writer != null && ConfigReader.getCollectorServer().equals(writer.getCollectorServerDto());
        if(!isSameWriter) {
            LOG.info("old and new writer are differeent, so start a new one");
            writer = new ProfilingWriter(jobQueue);
            writer.start();
        }
        LOG.info("<<< writer");
    }
    
    private void startReaders() {
        LOG.info(">>> start readers");
        if(readers == null) readers = new LinkedList<ProfilingReader>();

        List<ProfiledServerDto> profiledServers = ConfigReader.getProfiledServers();

        ThreadPoolExecutor hostExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(profiledServers.size() * 3);//each profiledServer has normally 3 defined access points

        //resolve in parallel all mongod addresses for all systems to be profiled
        for (ProfiledServerDto dto : profiledServers) {
            for (ServerAddress serverAddress : dto.getHosts()) {
                MongoResolver mongoResolver = new MongoResolver(dto.getAdminUser(), dto.getAdminPw(), serverAddress);
                Future<List<ServerAddress>> result = hostExecutor.submit(mongoResolver);
                dto.addFutureResolvedHostList(result);
            }
        }

        ThreadPoolExecutor profilingReaderExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(profiledServers.size());
        List<Future<ProfilingReader>> futureProfilingReaderList = new ArrayList<>();

        for (ProfiledServerDto dto : profiledServers) {
            HashSet<ServerAddress> systemServerAddresses = new HashSet<ServerAddress>();

            for(Future<List<ServerAddress>> futureAdressList : dto.getFutureResolvedHostLists()){
                try{
                    systemServerAddresses.addAll(futureAdressList.get());
                    break;//exit loop as soon as the first result returns since the other still pending results should contain the same resolved server addresses
                }
                catch (InterruptedException | ExecutionException e){
                    LOG.warn("Exception while adding resolved mongodb server addresses", e);
                }
            }

            HashMap<String, List<String>> collectionsPerDb = dto.getCollectionsPerDatabase();

            //create for each mongod and for each each database to profile one reader
            for (ServerAddress mongodAddress : systemServerAddresses) {
                for (String db : collectionsPerDb.keySet()) {
                    if(!isReaderExists(mongodAddress, db)){//dont't create readers twice
                        ProfilingReaderCreator profilingReaderCreator = new ProfilingReaderCreator(0, mongodAddress, dto, db, this);
                        Future<ProfilingReader> futureReader = profilingReaderExecutor.submit(profilingReaderCreator);
                        futureProfilingReaderList.add(futureReader);
                    }else{
                        LOG.info("Reader already exists for: {}/{}", mongodAddress, db );
                    }
               }
            }
        }
        hostExecutor.shutdown();

        for(Future<ProfilingReader> futureReader : futureProfilingReaderList){
            try{
                final ProfilingReader reader = futureReader.get();
                LOG.info("ProfilingReader for {}/{} is stopped: {} and profiling: {}", Lists.newArrayList(reader.getServerAddress(), reader.getDatabase(), reader.isStopped(), reader.isProfiling()));
            }
            catch (InterruptedException | ExecutionException e){
                LOG.warn("Exception while getting future profilingReader", e);
            }
        }
        profilingReaderExecutor.shutdown();

        LOG.info("<<< start readers");
    }

    public void addAndStartReader(ProfilingReader reader){
        if(!isReaderExists(reader.getServerAddress(), reader.getDatabase())) {//dont't start readers twice
            writeLock.lock();
            try {
                readers.add(reader);
                reader.start();//may be stopped if dto is not "enabled"
                LOG.info("New reader started for {}/{}", reader.getServerAddress(), reader.getDatabase());
            } finally {
                writeLock.unlock();
            }
        }else{
            LOG.info("No need to start reader because it already exists for: {}/{}", reader.getServerAddress(), reader.getDatabase());
        }
    }


    public ProfilingWriter getWriter(){
        return writer;
    }

    public BlockingQueue<ProfilingEntry> getJobQueue(){
        return jobQueue;
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

    public boolean isReaderExists(ServerAddress adr, String db) {

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
        doneJobsOfRemovedWriters = 0;
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


    private void terminateReadersAndWriters(boolean terminateAll) {
        LOG.info(">>> terminateReadersAndWriters {}", terminateAll );
        ThreadPoolExecutor terminatorExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1 + readers.size());
        List<Future<Long>> futureTerminatorList = new ArrayList<>();

        try {
            writeLock.lock();
            try{
                if(readers != null) {
                    final LinkedList<ProfilingReader> toBeRemoved = Lists.newLinkedList();
                    for (ProfilingReader r : readers) {
                        boolean isSameReader = ConfigReader.isProfiledServer(r.getProfiledServerDto());
                        if(terminateAll || !isSameReader) {
                            try {
                                Terminator terminator = new Terminator(r);
                                Future<Long> result = terminatorExecutor.submit(terminator);
                                futureTerminatorList.add(result);
                                LOG.info("will remove reader for {}/{}", r.getServerAddress(), r.getDatabase());
                                toBeRemoved.add(r);
                            } catch (Throwable e) {
                                LOG.error("Error while terminating reader ", e);
                            }
                        }
                    }
                    readers.removeAll(toBeRemoved);
                }
            }finally{
                writeLock.unlock();
            }
        } catch (Throwable e) {
            LOG.error("Error while terminating readers ", e);
        }


        boolean isSameWriter = ConfigReader.getCollectorServer().equals(writer.getCollectorServerDto());
        if(terminateAll || !isSameWriter) {
            try {
                if (writer != null) {
                    Terminator terminator = new Terminator(writer);
                    Future<Long> result = terminatorExecutor.submit(terminator);
                    futureTerminatorList.add(result);
                }
            } catch (Throwable e) {
                LOG.error("Error while terminating writers ", e);
            }
        }

        long doneJobs = 0;
        for(Future<Long> futureTerminator : futureTerminatorList){
            try{
                doneJobs += futureTerminator.get();
                LOG.debug("doneJobs {}", doneJobs);
            }
            catch (InterruptedException | ExecutionException e){
                LOG.warn("Exception while getting future terminator", e);
            }
        }
        terminatorExecutor.shutdown();

        if(terminateAll || !isSameWriter) {//take into account writerJobs if writer is terminated
            doneJobsOfRemovedReaders += (doneJobs - writer.getDoneJobs());
            doneJobsOfRemovedWriters += writer.getDoneJobs();
        }else{
            doneJobsOfRemovedReaders += doneJobs;
        }



        LOG.info("<<< terminateReadersAndWriters");

    }



    public void terminate() {

        terminateReadersAndWriters(true);
        
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
    public long getNumberOfWritesOfRemovedWriters(){
        return doneJobsOfRemovedWriters;
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

        ExecutorService executor = Executors.newFixedThreadPool(idList.size()+1);//+1 because idList may be empty
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

        result.setNumberOfWritesOfRemovedWriters(getNumberOfWritesOfRemovedWriters());

        result.setCollectorRunningSince(getRunningSince());

        result.setConfig(ConfigReader.getConfig());

        LOG.debug("<<< getApplicationStatus");
        return result;
    }

    public void startStopProfilingReaders(List<Integer> idList, boolean stop){
        writeLock.lock();
        try{
            final LinkedList<ProfilingReader> toBeAdded = Lists.newLinkedList();
            final LinkedList<ProfilingReader> toBeRemoved = Lists.newLinkedList();
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
                                    reader.getSlowMs()
                            );
                            LOG.info("will add reader for {}/{}", newReader.getServerAddress(), newReader.getDatabase());
                            toBeAdded.add(newReader);
                            LOG.info("will remove reader for {}/{}", reader.getServerAddress(), reader.getDatabase());
                            toBeRemoved.add(reader);

                            newReader.start();
                        }
                        break;
                    }
                }
            }
            readers.removeAll(toBeRemoved);
            readers.addAll(toBeAdded);

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
