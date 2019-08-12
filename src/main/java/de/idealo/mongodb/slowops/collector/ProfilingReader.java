/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.collector;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mongodb.BasicDBObject;
import com.mongodb.CursorType;
import com.mongodb.MongoCommandException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import de.idealo.mongodb.slowops.dto.ApplicationStatusDto;
import de.idealo.mongodb.slowops.dto.CollectorStatusDto;
import de.idealo.mongodb.slowops.dto.HostInfoDto;
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

public class ProfilingReader extends Thread implements Terminable{

    private static final Logger LOG = LoggerFactory.getLogger(ProfilingReader.class);
    private static final int RETRY_AFTER_SECONDS = 60*60;//1 hour
    private static final int MAX_LOG_LINE_LENGTH = 1000;
    private static AtomicInteger instances = new AtomicInteger(1);

    private final ServerAddress serverAddress;
    private final ProfiledServerDto profiledServerDto;
    private volatile HostInfoDto hostInfoDto;
    private final String database;
    private final List<String> collections;
    private final AtomicLong doneJobs;
    private long slowMs;
    private boolean isProfiling;
    private volatile String replSet;
    private volatile ReplicaStatus replSetStatus;
    private boolean stop = false;
    private final BlockingQueue<ProfilingEntry> jobQueue;
    private Date lastTs;
    private final ProfiledDocumentHandler profiledDocumentHandler;
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




    public ProfilingReader(int id, BlockingQueue<ProfilingEntry> jobQueue, ServerAddress adr, Date lastTs, ProfiledServerDto profiledServerDto, String dbName, List<String> collNames, boolean stop, long doneJobs, long slowMs) {
        this.jobQueue = jobQueue;
        this.serverAddress = adr;
        this.profiledServerDto = profiledServerDto;
        this.hostInfoDto = new HostInfoDto();
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

        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("JobHistoryScheduler-%d")
                .setDaemon(true)
                .build();
        scheduler = Executors.newScheduledThreadPool( 1, threadFactory );
        scheduler.scheduleAtFixedRate(
                new Runnable() {
                    long runs=0;
                    @Override public void run() {

                        try{
                            writeLock.lock();
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

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdownScheduler();
            }
        });
    }

    public MongoDbAccessor getMongoDbAccessor() {
        return new MongoDbAccessor(-1, profiledServerDto.getResponseTimeout(), true, profiledServerDto.getAdminUser(), profiledServerDto.getAdminPw(), profiledServerDto.getSsl(), serverAddress);
    }


    private void shutdownScheduler(){
        if(scheduler!=null){
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(schedulePeriodInSeconds+1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOG.error("InterruptedException while shuting down scheduler", e );
            }finally {
                scheduler.shutdownNow();
            }
        }
    }

    private MongoCursor<Document> getProfileCursor(MongoDbAccessor mongo) {
        LOG.info(">>> getProfileCursor for database {} at {}", database, serverAddress);
        final BasicDBObject query = new BasicDBObject();
        final BasicDBObject orderBy = new BasicDBObject();
        //search namespaces "database.collection" and also "database.$cmd" because count() is treated as command
        final List<String> namespaces = Lists.newArrayList(database + ".$cmd");
        for(String col : collections){
            if("*".equals(col)){
                LOG.info("found collection placeholder *");
                //if at least one collection name is * then use regex to match all collections except system.profile
                query.append("ns", new BasicDBObject("$regex", "^" + database + ".")
                                             .append("$ne", database + ".system.profile"));
                namespaces.clear();
                break;
            }else {
                namespaces.add(database + "." + col);
            }

        }
        if(!namespaces.isEmpty()){
            query.append("ns", new BasicDBObject("$in", namespaces));
        }
        query.append("ts",  new BasicDBObject( "$gt" , lastTs ));
        orderBy.append("$natural", Long.valueOf(1));//aufsteigend von alt zu neu


        final MongoDatabase db = mongo.getMongoDatabase(database);
        final MongoCollection<Document> profileCollection = db.getCollection("system.profile");
        if (profileCollection == null) {
            throw new IllegalArgumentException("Can't continue without profile collection for database " + database +  " at " + serverAddress);
        }

        return profileCollection.find(query).sort(orderBy).cursorType(CursorType.TailableAwait).iterator();

    }

    private void readSystemProfile() {

        final MongoDbAccessor mongo = getMongoDbAccessor();
        final MongoCursor<Document> profileCursor = getProfileCursor(mongo);//get tailable cursor from oldest to newest profile entry

        try {

            if(profileCursor.hasNext()) {
                while(!stop && profileCursor.hasNext()) {

                    final Document doc = profileCursor.next();
                    lastTs = (Date)doc.get("ts");
                    filterDoc(doc);
                    LOG.debug(getShrinkedLogLine(doc));
                }
            }else {
                LOG.info("No newer profile entry found for database {} at {}", database, serverAddress);
                return;
            }
        }catch(Exception e) {
            LOG.error("Exception occurred on {}/{} , will return and try again.", new Object[]{serverAddress, database}, e);
        }finally {
            LOG.info("profileCursor being closed for database {} at {}", database, serverAddress);
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

        final MongoDbAccessor mongo = getMongoDbAccessor();

        try {
            if(ms < 0){
                profile = 0; //profiling off
                ms = Math.abs(ms);
            }
            Document doc = mongo.runCommand(database, new BasicDBObject("profile", profile).append("slowms", ms));
            Object ok = doc.get("ok");
            if (ok != null && ok instanceof Double && Double.valueOf(ok.toString()).doubleValue()==1.0) {
                slowMs = ms;
                isProfiling = profile==0?false:true;
                LOG.info("setSlowMs successfully set to {} ms on {}/{}", new Object[]{ms, serverAddress, database});
            }else{
                LOG.error("setSlowMs failed on {}/{}", serverAddress, database);
            }
        } catch (MongoCommandException e) {
            LOG.error("Could not setSlowMs on {}/{}", new Object[]{serverAddress, database}, e);
        }
    }

    public boolean isProfiling(){
        return isProfiling;
    }

    protected synchronized void setReplSet(String rsName){
        replSet = rsName;
    }

    protected synchronized void setReplSetStatus(ReplicaStatus rStatus){
        replSetStatus = rStatus;
    }

    public synchronized void updateReplSetStatus(MongoDbAccessor mongo){
        LOG.debug(">>> updateReplSetStatus");

        try {
            try {
                Document doc = mongo.runCommand("admin", new BasicDBObject("replSetGetStatus", 1));
                Object rs = doc.get("set");
                if (rs != null && rs instanceof String) {
                    replSet = rs.toString();
                }
                Object myState = doc.get("myState");
                if (myState != null && myState instanceof Integer) {
                    replSetStatus = ReplicaStatus.getReplicaState((Integer)myState);
                }

            } catch (MongoCommandException e) {
                if(e.getErrorMessage().indexOf("error 13")!=-1){
                    LOG.info("Not authorized to get replSet status for server {} (if it's an arbiter we may have run into this bug: https://jira.mongodb.org/browse/SERVER-5479 ) ", serverAddress, e);
                }else {
                    LOG.info("This mongod seems not to be a replSet member {}", serverAddress, e);
                }
            }

        } catch (MongoCommandException e) {
            LOG.info("Could not determine replSet status on {}", serverAddress, e);
        }
        LOG.debug("<<< updateReplSetStatus");
    }

    protected synchronized HostInfoDto getHostInfo(){
        return hostInfoDto;
    }

    protected synchronized void setHostInfo(HostInfoDto hostInfoDto){
        this.hostInfoDto = hostInfoDto;
    }

    public synchronized void updateHostInfo(MongoDbAccessor mongo){
        LOG.debug(">>> updateHostInfo");

        try {

            final Document hostInfoDoc = mongo.runCommand("admin", new BasicDBObject("hostInfo", 1));
            final Document buildInfoDoc = mongo.runCommand("admin", new BasicDBObject("buildInfo", 1));

            if (hostInfoDoc != null) {

                Object system = hostInfoDoc.get("system");
                Object os = hostInfoDoc.get("os");
                Object extra = hostInfoDoc.get("extra");

                if (system != null && os != null && extra != null
                        && system instanceof Document && os instanceof Document && extra instanceof Document) {
                    final Document systemDoc = (Document) system;
                    final Document osDoc = (Document) os;
                    final Document extraDoc = (Document) extra;
                    hostInfoDto.setHostName(systemDoc.getString("hostname"));
                    hostInfoDto.setCpuArch(systemDoc.getString("cpuArch"));
                    hostInfoDto.setNumCores(systemDoc.getInteger("numCores"));
                    hostInfoDto.setCpuFreqMHz((Math.round(Double.parseDouble(extraDoc.getString("cpuFrequencyMHz")))));
                    hostInfoDto.setMemSizeMB(systemDoc.getInteger("memSizeMB"));
                    hostInfoDto.setNumaEnabled(systemDoc.getBoolean("numaEnabled"));
                    hostInfoDto.setPageSize(extraDoc.getLong("pageSize"));
                    hostInfoDto.setNumPages(extraDoc.getInteger("numPages"));
                    hostInfoDto.setMaxOpenFiles(extraDoc.getInteger("maxOpenFiles"));
                    hostInfoDto.setOsName(osDoc.getString("name"));
                    hostInfoDto.setOsVersion(osDoc.getString("version"));
                    hostInfoDto.setLibcVersion(extraDoc.getString("libcVersion"));
                }
            }
            if(buildInfoDoc != null) {
                hostInfoDto.setMongodbVersion(buildInfoDoc.getString("version"));
            }



        } catch (MongoCommandException e) {
            LOG.info("Could not determine host info on {}", serverAddress);
        }
        LOG.debug("<<< updateHostInfo");
    }

    public void updateProfileStatus(MongoDbAccessor mongo){
        LOG.debug(">>> updateProfileStatus");

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

        } catch (MongoCommandException e) {
            LOG.info("Could not determine profile status on database {} at {}", database, serverAddress);
        }

        LOG.debug("<<< updateProfileStatus");
    }


    @Override
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

    public int getInstanceId() { return instanceId; }

    public ArrayList<Long> getDoneJobsHistory() {
        ArrayList<Long> result = Lists.newArrayList();

        try{
            readLock.lock();
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


    @Override
    public void terminate() {
        stop = true;
        interrupt(); //need to interrupt when sleeping or waiting on tailable cursor data
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
                readSystemProfile();
                if(!stop) {
                    try {
                        LOG.info("{} sleeping...", serverAddress);
                        Thread.sleep(1000*RETRY_AFTER_SECONDS);

                    } catch (InterruptedException e) {
                        LOG.error("InterruptedException while sleeping.");
                        stop = true;
                    }
                }
            }
        }finally {
            ApplicationStatusDto.addWebLog("ProfilingReader terminated for '"+profiledServerDto.getLabel()+"' at " + serverAddress + "/" + database);
            terminate();
        }
        LOG.info("Run terminated {}", serverAddress);
    }



    public static void main(String[] args) throws Exception {
        final ServerAddress address =  new ServerAddress("localhost",27017);

        BlockingQueue<ProfilingEntry> jobQueue = new LinkedBlockingQueue<ProfilingEntry>();
        ProfiledServerDto dto = new ProfiledServerDto(true, "some label", new ServerAddress[]{new ServerAddress("127.0.0.1:27017")}, new String[]{"offerStore.*"}, null, null, false, 0, 2000);
        ProfilingReader reader = new ProfilingReader(0, jobQueue, address, null, dto, "offerStore", Lists.newArrayList("*"), false, 0, 0);
        reader.start();
        //reader.setSlowMs(1, 3);
        //reader.terminate();
        Thread.sleep(5000);
        //reader.terminate();
        //reader.stop();

        LOG.info("main end");


    }



    public CollectorStatusDto getCollectorStatusDto() {
        return new CollectorStatusDto(getInstanceId(),
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
                getDoneJobsHistory(),
                hostInfoDto.getCpuArch(),
                hostInfoDto.getNumCores(),
                hostInfoDto.getCpuFreqMHz(),
                hostInfoDto.getMemSizeMB(),
                hostInfoDto.getMongodbVersion());
    }
}
