/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.collector;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.*;

import com.mongodb.*;

import de.idealo.mongodb.slowops.util.Util;

/**
 * 
 * 
 * @author kay.agahd
 * @since 25.02.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public class ProfilingWriter extends Thread{
    
    private static final Logger LOG = LoggerFactory.getLogger(ProfilingWriter.class);
    private static final int RETRY_AFTER_SECONDS = 10;
    
    private final BlockingQueue<ProfilingEntry> jobQueue;
    
    private boolean stop = false;
    private AtomicLong doneJobs = new AtomicLong(0);
    private DBCollection profileCollection;
    private Mongo mongo;
    private ProfilingEntry lastJob;
    
    
    public ProfilingWriter(BlockingQueue<ProfilingEntry> jobQueue) {
        this.jobQueue = jobQueue;
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                closeConnections();
            }
        });
    }
    
    private void init() {
        LOG.info(">>> init");
        final String seeds = Util.getProperty(Util.COLLECTOR_SERVER_ADDRESSES, null);
        final String dbName = Util.getProperty(Util.COLLECTOR_DATABASE, null);
        final String collName = Util.getProperty(Util.COLLECTOR_COLLECTION, null);
        
        if(seeds == null || dbName == null || collName == null) {
            throw new IllegalArgumentException("Config incomplete! All properties must be set: " + Util.COLLECTOR_SERVER_ADDRESSES + ", " + Util.COLLECTOR_DATABASE + ", " + Util.COLLECTOR_COLLECTION);
        }
        
        try {
            final List<ServerAddress> addresses = Util.getServerAddresses(seeds);
            mongo = new Mongo(addresses);
            
            final DB db = mongo.getDB(dbName);
            final String login = Util.getProperty(Util.COLLECTOR_LOGIN, null);
            final String pw = Util.getProperty(Util.COLLECTOR_PASSWORD, null);
            if(login!= null && pw != null) {
                boolean ok = db.authenticate(login, pw.toCharArray());
                LOG.info("auth on " + seeds + " ok: " + ok);
            }
            
            profileCollection =  db.getCollection(collName);
            if(profileCollection == null) {
                throw new IllegalArgumentException("Can't continue without profile collection!");
            }
        } catch (MongoException e) {
            LOG.error("Exception while connecting to: " + seeds, e);
        }    
        
        LOG.info("<<< init");
    }
    
    private void closeConnections() {
        LOG.info(">>> closeConnections");
        try {
            if(mongo != null) {
                mongo.close();
                mongo = null;
            }
        } catch (Throwable e) {
            LOG.error("Error while closing mongo ", e);
        }
        LOG.info("<<< closeConnections");
    }
    
    public void terminate() {
        stop = true;
        interrupt();//need to interrupt when sleeping or waiting on jobQueue
        closeConnections();
    }
    
    
    public long getDoneJobs() {
        return doneJobs.get();
    }
    
    public Date getNewest(ServerAddress adr) {
        try {
            if(mongo == null) {
                init();
            }
            if(adr != null) {
                final BasicDBObject query = new BasicDBObject();
                final BasicDBObject fields = new BasicDBObject();
                final BasicDBObject sort = new BasicDBObject();
                query.put("adr", adr.getHost() + ":" + adr.getPort());
                fields.put("_id", Integer.valueOf(0));
                fields.put("ts", Integer.valueOf(1));
                sort.put("ts", Integer.valueOf(-1));
                
                final DBCursor c = profileCollection.find(query, fields).sort(sort).limit(1);
                if(c.hasNext()) {
                    final DBObject obj = c.next();
                    final Object ts = obj.get("ts");
                    if(ts != null) {
                        return (Date)ts;
                    }
                }
            }
        }catch(Exception e) {
            LOG.error("Exception occurred, will shutdown to re-init next time.", e);
            closeConnections();
        }
        return null;
        
    }
    
    private void writeEntries() {
        
        if(mongo == null) {
            LOG.error("Can't write entries since mongo is not initialized.");
            return;
        }
        
        try {
            while(!stop) {
                if(lastJob == null) {
                    lastJob = jobQueue.take();
                }
                
                final DBObject obj = lastJob.getDBObject();
                final WriteResult wr = profileCollection.insert(obj, WriteConcern.SAFE);
                if(wr.getError() == null) {
                    doneJobs.incrementAndGet();
                    lastJob = null;
                }
                
            }
        }catch(Exception e) {
            LOG.error("Exception occurred, will return and try again.", e);
            return;
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        
        try {
            boolean isLog=true;
            while(!stop) {
                
                if(lastJob != null || jobQueue.size() > 0) {
                    isLog=true;
                    init();
                    writeEntries();
                    closeConnections();
                }
                
                if(!stop) {
                    try {
                        if(isLog) {
                            isLog = false;
                            LOG.info("sleeping...");
                        }
                        Thread.sleep(1000*RETRY_AFTER_SECONDS);
                        
                    } catch (InterruptedException e) {
                        LOG.error("InterruptedException while sleeping: ");
                    }
                }
            }
        }finally {
            terminate();
        }
        LOG.info("Run terminated.");
    }

}
