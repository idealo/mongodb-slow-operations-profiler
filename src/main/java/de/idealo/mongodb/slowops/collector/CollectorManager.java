/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.collector;

import java.lang.management.ManagementFactory;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import javax.management.*;

import org.slf4j.*;

import com.google.common.collect.*;
import com.mongodb.ServerAddress;

import de.idealo.mongodb.slowops.jmx.CollectorManagerMBean;
import de.idealo.mongodb.slowops.util.Util;


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
    private long deadReaderDoneJobs;
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
        
        final String[] countries = Util.getProperty(Util.PROFILED_MONGOD_NAMES, "").split(""+Util.PROPERTIES_SEPARATOR);
        
        for (String country : countries) {
            final List<ServerAddress> mongodList = Util.getMongodAddresses(country);
            for (ServerAddress mongodAddress : mongodList) {
                startOneReader(mongodAddress, country);
            }
        }
    }
    
    
    private void startOneReader(ServerAddress address, String country) {
        
            final Date lastTs = writer.getNewest(address);
            final ProfilingReader reader = new ProfilingReader(jobQueue, address, lastTs, country, this);
            
            writeLock.lock();
            try{
                readers.add(reader);
            }finally{
                writeLock.unlock();
            }
            
            reader.start();
            
            LOG.info("New reader started for " + reader.getServerAddress());
    }
    
    
    @Override
    public synchronized void checkForNewOrRemovedMongods(ProfilingReader reader) {
        if(reader != null) {
            LOG.info(">>> checkForNewOrRemovedMongods: " + reader.getCountry() + " - " + reader.getServerAddress());
            final String country = reader.getCountry();
            final String seeds = Util.getProperty(Util.PROFILED_SEED_ADDRESSES_PREFIX + country, "");
            final List<ServerAddress> currentServers = Util.getServerAddresses(seeds);
            final Set<ServerAddress> currentServersSet = Sets.newHashSet(currentServers);
            
            if(!currentServersSet.contains(reader.getServerAddress())) {
                //profiled server was removed from config or from shard system, so stop reader and remove it
                if(removeReader(reader)) {
                    deadReaderDoneJobs += reader.getDoneJobs();
                    LOG.info("Reader removed for " + reader.getServerAddress());
                    reader.terminate();
                }else {
                    LOG.error("Could not remove reader for " + reader.getServerAddress());
                }
            }else {
                LOG.info("Config or shard system (still) contains " + reader.getServerAddress());
            }
            
            //check if there are any new servers in config or Shard system and start readers for it
            final List<ServerAddress> runningReaders = getRunningReaders();
            currentServers.removeAll(runningReaders);
            for (ServerAddress newAddress : currentServers) {
                startOneReader(newAddress, country);
            }
            
            //there may be readers for mongod's which are no more in config but as long as we can connect to them, profile them 
            LOG.info("<<< checkForNewOrRemovedMongods: " + reader.getCountry() + " - " + reader.getServerAddress());
        }
    }
    
    private List<ServerAddress> getRunningReaders() {
        final List<ServerAddress> result = Lists.newLinkedList();
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
            for (ProfilingReader reader : readers) {
                if(reader.getServerAddress().equals(r.getServerAddress())) {
                    return readers.remove(reader);
                }
            }
        }finally{
            writeLock.unlock();
        }
        
        return false;
    }
    
    
    
    private void registerMBean() {
        
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            final ObjectName name = new ObjectName(this.getClass().getPackage().getName() + ":type=" + this.getClass().getSimpleName());
            final StandardMBean mbean = new StandardMBean(this, CollectorManagerMBean.class, false);
            server.registerMBean(mbean, name);
        } catch (MalformedObjectNameException e) {
            LOG.error("Error while registering MBean", e);
        } catch (InstanceAlreadyExistsException e) {
            LOG.error("Error while registering MBean", e);
        } catch (MBeanRegistrationException e) {
            LOG.error("Error while registering MBean", e);
        } catch (NotCompliantMBeanException e) {
            LOG.error("Error while registering MBean", e);
        } catch (NullPointerException e) {
            LOG.error("Error while registering MBean", e);
        }
    }
    
    private void unregisterMBean() {
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            final ObjectName name = new ObjectName(this.getClass().getPackage().getName() + ":type=" + this.getClass().getSimpleName());
            server.unregisterMBean(name);
        } catch (MalformedObjectNameException e) {
            LOG.error("Error while unregistering MBean", e);
        } catch (MBeanRegistrationException e) {
            LOG.error("Error while unregistering MBean", e);
        } catch (InstanceNotFoundException e) {
            LOG.error("Error while unregistering MBean", e);
        } 
    }


    
    CollectorManager() {
        
        jobQueue = new LinkedBlockingQueue<ProfilingEntry>();
        deadReaderDoneJobs = 0;
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
    public Map<String, Long> getNumberOfReads(){
        final Map<String, Long> result = new TreeMap<String, Long>();
        long allReadersDoneJobs = 0;
        readLock.lock();
        try{
        
            if(readers != null) {
                for (ProfilingReader r : readers) {
                    allReadersDoneJobs += r.getDoneJobs();
                    String label = r.getCountry() + " - " + r.getServerAddress();
                    if(r.isStopped()) {
                        label += " (stopped)";
                    }
                    result.put(label, Long.valueOf(r.getDoneJobs()));
                }
            }
        }finally{
            readLock.unlock();
        }    
        result.put("all done jobs of dead readers", Long.valueOf(deadReaderDoneJobs));
        result.put("all", Long.valueOf(allReadersDoneJobs + deadReaderDoneJobs));
        
        return result;
    }
    
        
    @Override
    public long getNumberOfWrites(){
        if(writer != null) {
            return writer.getDoneJobs();
        }
        return 0;
    }
    
    private void monitor() {
        long allReadersDoneJobs = 0;
        if(readers != null) {
            readLock.lock();
            try{ 
                StringBuffer sb = new StringBuffer();
                for (ProfilingReader r : readers) {
                    allReadersDoneJobs += r.getDoneJobs();
                    sb.append(r.getServerAddress());
                    sb.append("=");
                    sb.append(r.getDoneJobs());
                    sb.append(" ");
                }
                if(!sb.toString().equals(logLine1)) {
                    logLine1 = sb.toString(); 
                    LOG.info("Read " + logLine1);
                }
                
            }finally{
                readLock.unlock();
            }
        }
        final String logLine = "Read all: " + (allReadersDoneJobs+deadReaderDoneJobs) + " Written: " + writer.getDoneJobs();
        if(logLine.equals(logLine2)) {
            logLine2 = logLine; 
            LOG.info(logLine);
        }
    }
    
    

    public static void main(String[] args) throws UnknownHostException {
        final CollectorManager result = new CollectorManager();
        result.startup();
    }



    

    
}
