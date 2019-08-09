/*
 * Copyright (c) 2013 Idealo Internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.monitor;

import com.google.common.collect.Lists;
import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.util.JSON;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * 
 * 
 * @author kay.agahd
 * @since 30.04.2013
 * @version $Id: $
 * @copyright Idealo Internet GmbH
 */
public class MongoDbAccessor {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDbAccessor.class);

    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 2000;
    public static final int DEFAULT_SLOW_MS = 100;
    private static final int DEFAULT_SOCKET_TIMEOUT_MS = 10000;
    private static final ConcurrentHashMap<Integer, MongoDbAccessor> INSTANCES = new ConcurrentHashMap<Integer, MongoDbAccessor>();

    private final ServerAddress[] serverAddresses;
    private final int socketTimeout;
    private final int connectTimeout;
    private final String user;
    private final String pw;
    private final boolean ssl;
    private final boolean isSecondaryReadPreferred;
    private MongoClient mongo;
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();
    private final Lock writeLock;


    public static void terminate(){
        if(!INSTANCES.isEmpty()) {
            for (MongoDbAccessor i : INSTANCES.values()) {
                if (i.mongo != null) {
                    i.mongo.close();
                    i.mongo = null;
                }
            }
            INSTANCES.clear();
        }
    }
    
    private MongoDbAccessor(){
        this(-1, -1, false, null, null, false, null);
    };
    
    public MongoDbAccessor(String user, String pw, boolean ssl, ServerAddress ... serverAddresses){
        this(-1, -1, false, user, pw, ssl, serverAddresses);
    }


    public MongoDbAccessor(int socketTimeout, int connectTimeout, String user, String pw, boolean ssl, ServerAddress ... serverAddresses){
        this(socketTimeout, connectTimeout, false, user, pw, ssl, serverAddresses);
    }
    
    public MongoDbAccessor(int socketTimeout, int connectTimeout, boolean isSecondaryReadPreferred, String user, String pw, boolean ssl, ServerAddress ... serverAddresses){
        this.serverAddresses = serverAddresses;
        this.socketTimeout = socketTimeout<0? DEFAULT_SOCKET_TIMEOUT_MS :socketTimeout;
        this.connectTimeout = connectTimeout<0? DEFAULT_CONNECT_TIMEOUT_MS :connectTimeout;
        this.user = user;
        this.pw = pw;
        this.ssl = ssl;
        this.isSecondaryReadPreferred = isSecondaryReadPreferred;
        writeLock = globalLock.writeLock();
        init();

    }



    private void init() {
        LOG.info(">>> init connection to servers {} ", serverAddresses);
        try {
            writeLock.lock();
            final MongoDbAccessor instance = INSTANCES.get(this.hashCode());
            if(instance != null && instance.mongo != null){
                this.mongo = instance.mongo;
                LOG.debug("reuse connection to servers {} ", serverAddresses);
            }else {
                LOG.debug("create new connection to servers {} ", serverAddresses);
                INSTANCES.put(this.hashCode(), this);

                try {
                    MongoClientOptions options = MongoClientOptions.builder().
                        socketTimeout(socketTimeout).
                        connectTimeout(connectTimeout).
                        readPreference(isSecondaryReadPreferred?ReadPreference.secondaryPreferred():ReadPreference.primaryPreferred()).
                        writeConcern(WriteConcern.ACKNOWLEDGED).
                        sslEnabled(ssl).
                        sslInvalidHostNameAllowed(true).
                        build();

                    if (user != null && !user.isEmpty() && pw != null && !pw.isEmpty()) {
                        MongoCredential mc = MongoCredential.createCredential(user, "admin", pw.toCharArray());
                        if (serverAddresses.length == 1) {
                            mongo = new MongoClient(serverAddresses[0], mc, options);
                        } else {
                            mongo = new MongoClient(Lists.newArrayList(serverAddresses), mc, options);
                        }
                    } else {
                        if (serverAddresses.length == 1) {
                            mongo = new MongoClient(serverAddresses[0], options);
                        } else {
                            mongo = new MongoClient(Lists.newArrayList(serverAddresses), options);
                        }
                    }
                } catch (MongoException e) {
                    LOG.error("Error while initializing mongo at address {}", serverAddresses, e);
                }
            }
        }finally {
            writeLock.unlock();
        }
        LOG.info("<<< init");
    }

    public CodecRegistry getDefaultCodecRegistry(){
        return mongo.getDefaultCodecRegistry();
    }

    public MongoDatabase getMongoDatabase(String dbName) {
        return mongo.getDatabase(dbName);
    }

    public DB getMongoDB(String dbName) {
        return mongo.getDB(dbName);
    }



    public Document runCommand(String dbName, DBObject cmd) throws IllegalStateException {

        if(dbName != null && !dbName.isEmpty()) {
           long start = System.currentTimeMillis();
           Document result = new Document();
           try {
               result = getMongoDatabase(dbName).runCommand((Bson) cmd, isSecondaryReadPreferred ? ReadPreference.secondaryPreferred() : ReadPreference.primaryPreferred());
           }catch (Throwable e){
               LOG.warn("runCommand failed {} on {}/{}", new Object[]{cmd.toString(), serverAddresses, dbName});
               throw e;
           }
           long end = System.currentTimeMillis();
           LOG.info("runCommand {} execTime in ms: {} on {}/{}", new Object[]{cmd.toString(), (end-start), serverAddresses, dbName});
           return result;
        }
        throw new IllegalStateException("Database not initialized");
    }

    //none of both methods is able to fetch all mongod addresses of the whole cluster when router addresses are used to initialize mongo
    private List<ServerAddress> getAllAddresses(){
        return mongo.getAllAddress();
        //return mongo.getServerAddressList();
    }

    private void find(String dbName, String collName, int limit){

        final MongoIterable<Document> res = getMongoDatabase(dbName).getCollection(collName)
                .find()
                .limit(limit);

        if(res != null){
            for(Document doc : res){
                LOG.info("doc: {}", JSON.serialize(doc));
            }
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MongoDbAccessor that = (MongoDbAccessor) o;

        if (socketTimeout != that.socketTimeout) return false;
        if (connectTimeout != that.connectTimeout) return false;
        if (ssl != that.ssl) return false;
        if (isSecondaryReadPreferred != that.isSecondaryReadPreferred) return false;
        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        if (pw != null ? !pw.equals(that.pw) : that.pw != null) return false;

        int serverAddressesHashCode = 0;
        for (ServerAddress sa: serverAddresses) {
            serverAddressesHashCode += sa.hashCode();
        }
        int thatServerAddressesHashCode = 0;
        for (ServerAddress sa: that.serverAddresses) {
            thatServerAddressesHashCode += sa.hashCode();
        }

        return serverAddressesHashCode == thatServerAddressesHashCode;

    }

    @Override
    public int hashCode() {

        int result = 31 * socketTimeout;
        result = 31 * result + connectTimeout;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (pw != null ? pw.hashCode() : 0);
        result = 31 * result + (ssl ? 1 : 0);
        result = 31 * result + (isSecondaryReadPreferred ? 1 : 0);
        for (ServerAddress sa: serverAddresses) {
            result = /*31 * */result + sa.hashCode();//order of array elements does not matter, so don't multiply by 31
        }

        return result;
    }

    public static void main(String[] args) throws UnknownHostException {
        //ServerAddress adr = new ServerAddress("localhost:27017");
        ServerAddress adr = new ServerAddress("mongo-arbiter-01.db00.pro07.eu.idealo.com:27058");
        MongoClient mongo = new MongoClient(Lists.newArrayList(adr));
        Document doc = mongo.getDatabase("admin").runCommand(new BasicDBObject("replSetGetStatus", 1), ReadPreference.secondaryPreferred());
        LOG.info("doc: {}", doc);

        //previously todo: keytool -importcert -file /Users/kay.agahd/Downloads/ca-idealo-intern-2020.crt -keystore cacerts
        /*System.setProperty("javax.net.ssl.trustStore", "/Library/Java/JavaVirtualMachines/jdk-11.0.2.jdk/Contents/Home/lib/security/cacerts");
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        ServerAddress adr = new ServerAddress("mongo-charlie01-01.db00.tst05.eu.idealo.com:27017");

        MongoDbAccessor monitor = new MongoDbAccessor(1000, 1000, "admin", "<ADMINPW>", true, adr);
        Document doc = monitor.runCommand("admin", new BasicDBObject("isMaster", "1"));
        LOG.info("doc: {}", doc);
        LOG.info("ismaster: {}",  doc.get("ismaster"));

        for(ServerAddress sa : monitor.getAllAddresses()){
            LOG.info("adr: {}", sa);
        }
        */

    }

    

}
