/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.collector;

import com.google.common.collect.Lists;
import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import de.idealo.mongodb.slowops.dto.CollectorStatusDto;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 
 * 
 * @author kay.agahd
 * @since 25.02.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */

public class ProfilingReader extends Thread implements Callable{

    private static final Logger LOG = LoggerFactory.getLogger(ProfilingReader.class);
    private static final int RETRY_AFTER_SECONDS = 60*60;//1 hour
    private static final int MAX_LOG_LINE_LENGTH = 1000;

    private static AtomicInteger instances = new AtomicInteger(1);

    
    private final ServerAddress serverAddress;
    private final ProfiledServerDto profiledServerDto;
    private final String database;
    private final List<String> collections;
    private final AtomicLong doneJobs;
    private long slowMs;
    private boolean isProfiling;
    private ReplicaStatus replSetStatus;
    private boolean stop = false;

    private final BlockingQueue<ProfilingEntry> jobQueue;
    private MongoCollection<Document> profileCollection;
    private MongoDbAccessor mongo;
    private Date lastTs;
    private ServerChecker restarter;
    private MongoCursor<Document> profileCursor;
    private final ProfiledDocumentHandler profiledDocumentHandler;
    private String replSet;
    private final int instanceId;
    private final ScheduledExecutorService scheduler;
    private final LinkedList<Long> doneJobsHistory;
    private final int schedulePeriodInSeconds;
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();
    private final Lock readLock;
    private final Lock writeLock;


    //https://docs.mongodb.com/v2.6/reference/replica-states/
    public enum ReplicaStatus {
        STARTUP(0),
        PRIMARY(1),
        SECONDARY(2),
        RECOVERING(3),
        STARTUP2(5),
        UNKNOWN(6),
        ARBITER(7),
        DOWN(8),
        ROLLBACK(9),
        REMOVED(10);

        private int value;

        private ReplicaStatus(int value) {
            this.value = value;
        }

        static ReplicaStatus getReplicaState(int value){
            switch (value) {
                case 0:
                    return STARTUP;
                case 1:
                    return PRIMARY;
                case 2:
                    return SECONDARY;
                case 3:
                    return RECOVERING;
                case 5:
                    return STARTUP2;
                case 6:
                    return UNKNOWN;
                case 7:
                    return ARBITER;
                case 8:
                    return DOWN;
                case 9:
                    return ROLLBACK;
                case 10:
                    return REMOVED;
            }
            return UNKNOWN;
        }
    };




    public ProfilingReader(int id, BlockingQueue<ProfilingEntry> jobQueue, ServerAddress adr, Date lastTs, ProfiledServerDto profiledServerDto, String dbName, List<String> collNames, boolean stop, long doneJobs, long slowMs, ServerChecker restarter) {
        this.jobQueue = jobQueue;
        this.serverAddress = adr;
        this.profiledServerDto = profiledServerDto;
        this.restarter = restarter;
        if(lastTs == null) {
            this.lastTs = new Date(0);
        }else {
            this.lastTs = lastTs;
        }
        this.replSet = null;
        this.database = dbName;
        this.collections = collNames;
        this.doneJobs = new AtomicLong(doneJobs);
        this.slowMs = slowMs;
        this.profiledDocumentHandler = new ProfiledDocumentHandler(serverAddress);
        this.stop = stop;
        this.isProfiling = false;
        this.replSetStatus = ReplicaStatus.UNKNOWN;
        this.instanceId = id==0?instances.getAndIncrement():id;

        readLock = globalLock.readLock();
        writeLock = globalLock.writeLock();
        schedulePeriodInSeconds = 10;
        doneJobsHistory = new LinkedList<Long>(Collections.nCopies(24*60*60/schedulePeriodInSeconds, 0l));


        scheduler = Executors.newScheduledThreadPool( 1 );
        scheduler.scheduleAtFixedRate(
                new Runnable() {
                    long runs=0;
                    @Override public void run() {
                        writeLock.lock();
                        try{
                            doneJobsHistory.addFirst(getDoneJobs());
                            doneJobsHistory.removeLast();
                        }finally{
                            writeLock.unlock();
                        }
                    }
                },
                schedulePeriodInSeconds,
                schedulePeriodInSeconds,
                TimeUnit.SECONDS );

        init();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                closeConnections();
                shutdownScheduler();
            }
        });
    }
    
    private void init() {
        LOG.info(">>> init for {}", serverAddress);
        
        try {
            if(mongo == null) {
                mongo = new MongoDbAccessor(profiledServerDto.getAdminUser(), profiledServerDto.getAdminPw(), serverAddress);

                MongoDatabase db = mongo.getMongoDatabase(database);
                profileCollection = db.getCollection("system.profile");
                if (profileCollection == null) {
                    throw new IllegalArgumentException("Can't continue without profile collection for " + serverAddress);
                }
            }else{
                LOG.info("already inititialized for {}", serverAddress);
            }
        } catch (MongoException e) {
            LOG.error("Error while initializing mongo at address {}", serverAddress, e);
            closeConnections();
            if(restarter != null) {
                //commented out because might loop endlessly
                //restarter.checkForNewOrRemovedMongods(this);
            }
        }
        LOG.info("<<< init for {}", serverAddress);
    }
    
    private void closeConnections() {
        LOG.info(">>> closeConnections {}", serverAddress);
        
        try {
            if(profileCursor != null) {
                profileCursor.close();
            }
        } catch (Throwable e) {
            LOG.error("Error while closing profileCursor ", e);
        }
        
        try {
            if(mongo != null) {
                mongo.closeConnections();
                mongo = null;
            }
        } catch (Throwable e) {
            LOG.error("Error while closing mongo ", e);
        }
        
        LOG.info("<<< closeConnections {}", serverAddress);
    }

    private void shutdownScheduler(){
        if(scheduler!=null){
            scheduler.shutdown();
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(schedulePeriodInSeconds+1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOG.error("InterruptedException while shuting down scheduler", e );
            }
        }
    }

    private MongoCursor<Document> getProfileCursor() {
        LOG.info(">>> getProfileCursor for {} lastTs: {}", serverAddress, lastTs);
        final BasicDBObject query = new BasicDBObject();
        final BasicDBObject orderBy = new BasicDBObject();
        //search namespaces "database.collection" and also "database.$cmd" because count() is treated as command
        List<String> namesspaces = Lists.newArrayList(database + ".$cmd");
        for(String col : collections){
            namesspaces.add(database + "." + col);
        }

        query.put("ns", new BasicDBObject( "$in", namesspaces));
        query.put("ts",  new BasicDBObject( "$gt" , lastTs ));
        orderBy.put("$natural", Long.valueOf(1));//aufsteigend von alt zu neu
        
        return profileCollection.find(query).sort(orderBy).cursorType(CursorType.TailableAwait).iterator();

    }
    
    private void readSystemProfile() {
        
        if(mongo == null) {
            LOG.error("Can't read entries from {}/{} since mongo is not initialized.", serverAddress, database );
            return;
        }
        
        profileCursor = getProfileCursor();//get tailable cursor from oldest to newest profile entry
        
        try {
        
            if(profileCursor.hasNext()) {
                while(!stop && profileCursor.hasNext()) {
                    
                    final Document doc = profileCursor.next();
                    lastTs = (Date)doc.get("ts");
                    filterDoc(doc);
                    LOG.debug(getShrinkedLogLine(doc));
                }
            }else {
                LOG.info("No newer profile entry found for {}", serverAddress);
                return;
            }
        }catch(Exception e) {
            LOG.error("Exception occurred on {}/{} , will return and try again.", new Object[]{serverAddress, database}, e);
        }finally {
            LOG.info("profileCursor being closed for {}", serverAddress);
            if(profileCursor != null) {
                profileCursor.close();
            }
        }
    }


    private void filterDoc(Document doc) {

        ProfilingEntry entry = profiledDocumentHandler.filterDoc(doc);
        entry.setLabel(profiledServerDto.getLabel());
        entry.setReplSet(replSet);
        try {

            jobQueue.put(entry);
            doneJobs.incrementAndGet();

        } catch (InterruptedException e) {
            LOG.error("InterruptedException while waiting to put entry into jobQueue", e);
        }

    }


    private String getShrinkedLogLine(Document doc) {
        String result = ""+doc;
        final int lineLength = result.length(); 
        if(lineLength > MAX_LOG_LINE_LENGTH) {
            final int region = MAX_LOG_LINE_LENGTH/2;
            final int commas = result.substring(region, result.length()-region).split(",").length;
            final StringBuilder newLogLine = new StringBuilder(result.substring(0, region));
            newLogLine.append(" [original document is ").append(lineLength).append(" bytes big, so only first and last ")
            .append(region).append(" bytes are shown, ").append(commas).append(" comma separated values have been cut-out] ")
            .append(result.substring(result.length()-region));
            result = newLogLine.toString();
        }
        return result;
    }

    public void setSlowMs(int profile, long ms) {

        boolean wasNull = false;
        if (mongo == null) {
            wasNull = true;
            init();
        }

        try {
            if(ms < 0){
                profile = 0; //profiling off
                ms = Math.abs(ms);
            }
            Document doc = mongo.runCommand(database, new BasicDBObject("profile", profile).append("slowms", ms));
            Object ok = doc.get("ok");
            if (ok != null && ok instanceof Double && Double.valueOf(ok.toString()).doubleValue()==1.0) {
                slowMs = ms;
                LOG.info("setSlowMs successfully set to {} ms on {}/{}", new Object[]{ms, serverAddress, database});
            }else{
                LOG.error("setSlowMs failed on {}/{}", serverAddress, database);
            }
        } catch (MongoCommandException e) {
            LOG.error("Could not setSlowMs on {}/{}", new Object[]{serverAddress, database}, e);
        }

        if(wasNull){
            closeConnections();
        }
    }

    public boolean isProfiling(){
        return isProfiling;
    }


    public void updateServerStatusVariables(){
        LOG.debug(">>> updateServerStatusVariables");

        boolean wasNull = false;
        if (mongo == null) {
            wasNull = true;
            init();
        }

        try {
            Document doc = mongo.runCommand(database, new BasicDBObject("profile", -1));
            Object was = doc.get("was");
            LOG.debug("isProfiling was: {}", was);
            if (was != null && was instanceof Integer) {
                int wasInt = (Integer)was;
                LOG.debug("isProfiling wasInt: {}", wasInt);
                if(wasInt==0){
                    isProfiling = false;
                }else{//either 2 (profile all) or 1 (profile slow ops)
                    isProfiling = true;
                }
            }
            Object ms = doc.get("slowms");
            if (ms != null && ms instanceof Integer) {
                slowMs = (long)(Integer)ms;
            }

            try {
                doc = mongo.runCommand("admin", new BasicDBObject("replSetGetStatus", 1));
                Object rs = doc.get("set");
                if (rs != null && rs instanceof String) {
                    replSet = rs.toString();
                }
                Object myState = doc.get("myState");
                if (myState != null && myState instanceof Integer) {
                    replSetStatus = ReplicaStatus.getReplicaState((Integer)myState);
                }
            } catch (MongoCommandException e) {
                LOG.info("this mongod seems not to be a replSet member {}", serverAddress);
            }

        } catch (MongoCommandException e) {
            LOG.info("Could not determine status on {}", serverAddress);
        }

        if(wasNull){
            closeConnections();
        }
        LOG.debug("<<< updateServerStatusVariables");
    }


    public long getDoneJobs() { return doneJobs.get(); }

    public long getSlowMs() { return slowMs; }

    public ReplicaStatus getReplicaStatus() { return replSetStatus; }

    public ServerAddress getServerAddress() {
        return serverAddress;
    }

    public String getDatabase() {
        return database;
    }

    public List<String> getCollections() {
        return collections;
    }
    
    public String getLabel() { return profiledServerDto.getLabel(); }

    public ProfiledServerDto getProfiledServerDto() { return profiledServerDto; }

    public Date getLastTs() { return lastTs; }

    public String getReplSet() { return replSet; }

    public int getIntanceId() { return instanceId; }

    public ArrayList<Long> getDoneJobsHistory() {
        ArrayList<Long> result = Lists.newArrayList();

        readLock.lock();
        try{
            long newest = doneJobsHistory.get(0);
            result.add(getDoneJobs());//now
            result.add(newest - doneJobsHistory.get(1));//last 10 sec
            result.add(newest - doneJobsHistory.get(5));//last 1min
            result.add(newest - doneJobsHistory.get(59));//last 10min
            result.add(newest - doneJobsHistory.get(179));//last 30min
            result.add(newest - doneJobsHistory.get(359));//last 1hour
            result.add(newest - doneJobsHistory.get(4319));//last 12hours
            result.add(newest - doneJobsHistory.get(8639));//last 1day
        }finally{
            readLock.unlock();
        }
        return result;
    }

    public void terminate() {
        stop = true;
        interrupt(); //need to interrupt when sleeping or waiting on tailable cursor data
        closeConnections();
        shutdownScheduler();
    }
    
    public boolean isStopped() {
        return stop;
    }


    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        LOG.info("Run started {}", serverAddress);
        try {
            while(!stop) {
                init();
                readSystemProfile();
                closeConnections();
                if(!stop) {
                    try {
                        LOG.info("{} sleeping...", serverAddress);
                        Thread.sleep(1000*RETRY_AFTER_SECONDS);
                        
                    } catch (InterruptedException e) {
                        LOG.error("InterruptedException while sleeping.");
                    }
                }
            }
        }finally {
            terminate();
        }
        LOG.info("Run terminated {}", serverAddress);
    }



    public static void main(String[] args) throws Exception {
        final ServerAddress address =  new ServerAddress("localhost",27017);
        
        BlockingQueue<ProfilingEntry> jobQueue = new LinkedBlockingQueue<ProfilingEntry>();
        ProfiledServerDto dto = new ProfiledServerDto(true, "some label", new ServerAddress[]{new ServerAddress("127.0.0.1:27017")}, new String[]{"offerStore.offer"}, null, null, 100);
        ProfilingReader reader = new ProfilingReader(0, jobQueue, address, null, dto, "offerStore", Lists.newArrayList("offers"), false, 0, 100, null);
        reader.start();
        reader.setSlowMs(1, 3);
        reader.terminate();
        //Thread.sleep(5000);
        //reader.stop();
        
        LOG.info("main end");
        
        
    }


    @Override
    public CollectorStatusDto call() {
        updateServerStatusVariables();
        return new CollectorStatusDto(getIntanceId(),
                getLabel(),
                getReplSet(),
                getServerAddress(),
                getDatabase(),
                getCollections(),
                isStopped(),
                isProfiling(),
                getSlowMs(),
                getReplicaStatus().name(),
                getLastTs(),
                getDoneJobsHistory()
        );
    }
}
