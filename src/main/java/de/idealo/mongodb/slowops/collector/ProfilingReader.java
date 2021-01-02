/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.collector;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import de.idealo.mongodb.slowops.dto.ApplicationStatusDto;
import de.idealo.mongodb.slowops.dto.CollectorStatusDto;
import de.idealo.mongodb.slowops.dto.HostInfoDto;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import de.idealo.mongodb.slowops.util.Util;
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
    private static final long MIN_RETRY_TIMEOUT_MSEC = 1000;//1 second
    private static final long MAX_RETRY_TIMEOUT_MSEC = 60*60*1000;//1 hour
    private static final int MAX_LOG_LINE_LENGTH = 1000;
    private static AtomicInteger instances = new AtomicInteger(1);
    private static final String SYSTEM_PROFILE = "system.profile";
    private static final long ONE_MEGA_BYTE = 1024*1024;

    private final ServerAddress serverAddress;
    private final ProfiledServerDto profiledServerDto;
    private volatile HostInfoDto hostInfoDto;
    private final String database;
    private final List<String> collections;
    private final AtomicLong doneJobs;
    private long slowMs;
    private long systemProfileMaxSizeInBytes;
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
        REMOVED(10),
        //added to distinguish single nodes from replSets
        SINGLE(20);

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
                case 20:
                    return SINGLE;
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
        this.systemProfileMaxSizeInBytes = 0;
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
                LOG.error("InterruptedException while shutting down scheduler", e );
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
                //if at least one collection name is * then use regex to match all collections except SYSTEM_PROFILE
                query.append("ns", new BasicDBObject("$regex", "^" + database + ".")
                                             .append("$ne", database + "." + SYSTEM_PROFILE));
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
        if (!systemProfileExists(db)) {
            increaseSizeOfSystemProfileCollection();//it will create the SYSTEM_PROFILE collection
        }

        setSystemProfileMaxSizeInBytes(getSystemProfileMaxSizeInBytes(db));


        return db.getCollection(SYSTEM_PROFILE).find(query).sort(orderBy).cursorType(CursorType.TailableAwait).iterator();

    }

    private boolean systemProfileExists(MongoDatabase db){
        return db.listCollectionNames().into(new ArrayList<String>()).contains(SYSTEM_PROFILE);
    }

    private void readSystemProfile() {

        final MongoDbAccessor mongo = getMongoDbAccessor();
        final ExecutorService executorService = Executors.newFixedThreadPool(4);

        try {
            final MongoCursor<Document> profileCursor = getProfileCursor(mongo);//get tailable cursor from oldest to newest profile entry;

            try {
                if (profileCursor.hasNext()) {
                    while (!stop && profileCursor.hasNext()) {
                        final Document doc = profileCursor.next();
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                lastTs = (Date) doc.get("ts");
                                filterDoc(doc, executorService);
                                LOG.debug(getShrinkedLogLine(doc));
                            }
                        });
                    }
                } else {
                    throw new IllegalStateException("No newer profile entry found.");
                }
            } catch (Exception e) {
                if (! (e instanceof IllegalStateException)) { //don't log it as an error
                    LOG.error("Exception occurred on {}/{} , will return and try again.", new Object[]{serverAddress, database, e});
                }
                throw (e);
            } finally {
                LOG.info("profileCursor being closed for database {} at {}", database, serverAddress);
                if (profileCursor != null) {
                    profileCursor.close();
                }
            }
        }finally {
            executorService.shutdown();
            try {
                executorService.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOG.error("Error while awaiting termination of executorService for readSystemProfile", e);
            } finally {
                executorService.shutdownNow();
            }
        }
    }


    private void filterDoc(Document doc, ExecutorService executorService) {

        ProfilingEntry entry = profiledDocumentHandler.filterDoc(doc);
        entry.setLabel(profiledServerDto.getLabel());
        entry.setReplSet(replSet);
        try {

            jobQueue.put(entry);
            doneJobs.incrementAndGet();
            //update the cache concurrently
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    ExampleSlowOpsCache.INSTANCE.addToCache(entry.getFingerprint(), doc);
                }
            });

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
            if (ok instanceof Double && Double.valueOf(ok.toString()).doubleValue() == 1.0) {
                slowMs = ms;
                isProfiling = profile==0?false:true;
                LOG.info("slowMs successfully set to {} ms and isProfiling={} on {}/{}", new Object[]{slowMs, isProfiling, serverAddress, database});
            }else{
                LOG.error("setSlowMs failed on {}/{}", serverAddress, database);
            }
        } catch (MongoCommandException e) {
            LOG.error("Could not setSlowMs on {}/{}", new Object[]{serverAddress, database, e});
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
        replSetStatus = ReplicaStatus.UNKNOWN;
        try {
            try {
                Document doc = mongo.runCommand("admin", new BasicDBObject("replSetGetStatus", 1));
                Object rs = doc.get("set");
                if (rs instanceof String) {
                    replSet = rs.toString();
                }
                Object myState = doc.get("myState");
                if (myState instanceof Integer) {
                    replSetStatus = ReplicaStatus.getReplicaState((Integer)myState);
                }

            } catch (MongoCommandException e) {
                if(e.getErrorCode()==13){
                    try {
                        final Document doc = mongo.runCommand("admin", new BasicDBObject("ismaster", 1));
                        final Boolean isArbiter = doc.getBoolean("arbiterOnly");
                        if(isArbiter){
                            replSet = doc.getString("setName");
                            replSetStatus = ReplicaStatus.ARBITER;
                            LOG.info("Can't authenticate on arbiter {} to get replSet status due to bug https://jira.mongodb.org/browse/SERVER-5479 ", serverAddress);
                        }else{
                            LOG.warn("Not authorized to get replSet status for server {}", serverAddress, e);
                        }
                    } catch (Throwable e2) {
                        LOG.error("Could not execute ismaster command on server {} ", serverAddress, e2);
                    }

                }else {
                    LOG.info("This mongod seems not to be a replSet member {}", serverAddress);
                    replSetStatus = ReplicaStatus.SINGLE;
                }
            }

        } catch (MongoCommandException e) {
            LOG.info("Could not determine replSet status on {}", serverAddress);
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

                if (system instanceof Document && os instanceof Document && extra instanceof Document) {
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
            if (was instanceof Integer) {
                int wasInt = (Integer)was;
                LOG.debug("isProfiling wasInt: {}", wasInt);
                if(wasInt==0){
                    isProfiling = false;
                }else{//either 2 (profile all) or 1 (profile slow ops)
                    isProfiling = true;
                }
            }
            Object ms = doc.get("slowms");
            if (ms instanceof Integer) {
                slowMs = (long)(Integer)ms;
            }

        } catch (MongoCommandException e) {
            LOG.info("Could not determine profile status on database {} at {}", database, serverAddress);
        }

        setSystemProfileMaxSizeInBytes(getSystemProfileMaxSizeInBytes(mongo.getMongoDatabase(database)));

        LOG.debug("<<< updateProfileStatus");
    }


    @Override
    public long getDoneJobs() { return doneJobs.get(); }

    public long getSlowMs() { return slowMs; }

    public void setSystemProfileMaxSizeInBytes(long systemProfileMaxSizeInBytes) { this.systemProfileMaxSizeInBytes = systemProfileMaxSizeInBytes; }

    public long getSystemProfileMaxSizeInBytes() { return systemProfileMaxSizeInBytes; }


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
            double errorCount = 0;
            final double maxErrorCount = Math.log(MAX_RETRY_TIMEOUT_MSEC / MIN_RETRY_TIMEOUT_MSEC)/Math.log(2);
            while(!stop) {
                final Date d = getLastTs();
                Exception ex = null;
                try {
                    readSystemProfile();

                }catch (Exception e){
                    ex = e;
                } finally {
                    if(!stop){
                        if(d.before(getLastTs())){
                            errorCount = 0;  //reset errorCount because lastTS has increased, which means that the SYSTEM_PROFILE collection could be read at least one time
                        }else{
                            if(errorCount < maxErrorCount) errorCount++;//avoid overflow of sleepMs when calculating Math.pow(2, errorCount), so don't increment errorCount if sleepMs would be already greater than MAX_RETRY_TIMEOUT_MSEC
                        }
                        final long sleepMs = Math.min( (long)Math.pow(2, errorCount) * MIN_RETRY_TIMEOUT_MSEC, MAX_RETRY_TIMEOUT_MSEC); //double sleep time with each error up to MAX_RETRY_TIMEOUT_MSEC
                        if(ex != null) {
                            String msg = "ProfilingReader for '" + profiledServerDto.getLabel() + "' at " + serverAddress + "/" + database + " will be restarted in " + (sleepMs / 1000) + " seconds ";
                            if (ex instanceof MongoQueryException && (((MongoQueryException) ex).getCode() == 96 || ((MongoQueryException) ex).getCode() == 136)) {
                                msg += "because the number of profiled operations was too high or the " + SYSTEM_PROFILE + " collection too small to keep up reading. You may increase the slow operations threshold (slowMs), decrease the number of running operations and/or increase the size of the " + SYSTEM_PROFILE + " collection.";
                                ApplicationStatusDto.addWebLog(msg);
                                LOG.warn(msg, ex);
                                increaseSizeOfSystemProfileCollection();
                            }else if (ex instanceof IllegalStateException){ //this case it's rather an info than an error, so log it appropriately
                                msg += "because no new slow operations exist yet in " + SYSTEM_PROFILE + " collection.";
                                LOG.info(msg);
                            }else{//any other case it good to see in the web interface and also its stacktrace in the log file
                                msg += "with unspecified reason";
                                ApplicationStatusDto.addWebLog(msg);
                                LOG.warn(msg, ex);
                            }

                        }
                        sleepIfNotStopped(sleepMs);
                    }
                }
            }
        }finally {
            ApplicationStatusDto.addWebLog("ProfilingReader terminated for '"+profiledServerDto.getLabel()+"' at " + serverAddress + "/" + database);
            terminate();
        }
        LOG.info("Run terminated {}", serverAddress);
    }

    private long getSystemProfileMaxSizeInBytes(MongoDatabase db){
        if(systemProfileExists(db)) {
            final Document collStatsResults = db.runCommand(new Document("collStats", SYSTEM_PROFILE));
            return Util.getNumber(collStatsResults, "maxSize", 0);
        }
        return 0;
    }

    private void increaseSizeOfSystemProfileCollection(){
        final MongoDbAccessor mongo = getMongoDbAccessor();
        boolean needToPauseProfiling=false;

        try {

            final MongoDatabase db = mongo.getMongoDatabase(database);
            final Document isMaster = mongo.runCommand("admin", new BasicDBObject("isMaster", "1"));

            if(isMaster.getBoolean("ismaster")){
                final long currentMaxSize = getSystemProfileMaxSizeInBytes(db);
                if (currentMaxSize < ONE_MEGA_BYTE * profiledServerDto.getSystemProfileMaxSizeInMB()) {

                    if (isProfiling) {
                        needToPauseProfiling = true;
                        setSlowMs(0, slowMs * -1);//pause profiling
                    }
                    if(currentMaxSize > 0) {
                        db.getCollection(SYSTEM_PROFILE).drop();//drop only if it exists which is the case when size > 0
                        while(systemProfileExists(db)){
                            try {
                                LOG.warn("Collection {} could not be dropped on {}/{}, so try again in 1 sec", new Object[]{SYSTEM_PROFILE, serverAddress, database});
                                Thread.sleep(1000);
                                db.getCollection(SYSTEM_PROFILE).drop();//try to drop again
                            } catch (InterruptedException e) {
                                LOG.error("InterruptedException while sleeping.");
                            }
                        }
                    }

                    final long newMaxSize = currentMaxSize + ONE_MEGA_BYTE;
                    final CreateCollectionOptions opts = new CreateCollectionOptions().capped(true).sizeInBytes(newMaxSize);
                    db.createCollection(SYSTEM_PROFILE, opts);
                    setSystemProfileMaxSizeInBytes(newMaxSize);
                    final String msg = "Max size of collection '" + SYSTEM_PROFILE + "' successfully increased by 1 MB to " + Math.round(newMaxSize / ONE_MEGA_BYTE) + " MB on " + serverAddress + "/" + database;
                    LOG.info(msg);
                    ApplicationStatusDto.addWebLog(msg);
                } else {
                    final String msg = "Max size of collection '" + SYSTEM_PROFILE + "' on " + serverAddress + "/" + database + " will not be increased because its current maxSize of " + currentMaxSize + " Bytes is greater than the configured max size of " + ONE_MEGA_BYTE * profiledServerDto.getSystemProfileMaxSizeInMB() + " Bytes.";
                    ApplicationStatusDto.addWebLog(msg);
                    LOG.warn(msg);
                }
            }else{
                final String msg = "Can't increase max size of collection '" + SYSTEM_PROFILE + "' on " + serverAddress + "/" + database + " because it's not a Primary";
                ApplicationStatusDto.addWebLog(msg);
                LOG.warn(msg);
            }
        }catch (MongoCommandException e) {
            LOG.error("There was an error while trying to increase the size of collection {} on {}/{}", new Object[]{SYSTEM_PROFILE, serverAddress, database, e});
        }finally {
            if(needToPauseProfiling){
                setSlowMs(1, slowMs);//continue profiling if we had to pause it
            }
        }
    }

    private void sleepIfNotStopped(long ms){
        if (!stop) {
            try {
                LOG.info("{} sleeping...", serverAddress);
                Thread.sleep(ms);

            } catch (InterruptedException e) {
                LOG.error("InterruptedException while sleeping.");
                stop = true;
            }
        }
    }



    public static void main(String[] args) throws Exception {
        final ServerAddress address =  new ServerAddress("localhost",27017);

        BlockingQueue<ProfilingEntry> jobQueue = new LinkedBlockingQueue<ProfilingEntry>();
        ProfiledServerDto dto = new ProfiledServerDto(true, "some label", new ServerAddress[]{new ServerAddress("127.0.0.1:27017")}, new String[]{"offerStore.*"}, null, null, false, 0, 2000, Lists.newArrayList(), 1);
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
                getSystemProfileMaxSizeInBytes(),
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
