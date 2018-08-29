/*
 * Copyright (c) 2013 Idealo Internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.monitor;

import com.google.common.collect.Lists;
import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.List;


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

    private final ServerAddress[] serverAddress;
    private final int socketTimeout;
    private final int connectTimeout;
    private final String user;
    private final String pw;
    private final boolean isSecondaryReadPreferred;
    private MongoClient mongo;

    
    
    private MongoDbAccessor(){
        this(-1, -1, false, null, null, null);
    };
    
    public MongoDbAccessor(String user, String pw, ServerAddress ... serverAddress){
        this(-1, -1, false, user, pw, serverAddress);
    }

    public MongoDbAccessor(int socketTimeout, int connectTimeout, String user, String pw, ServerAddress ... serverAddress){
        this(socketTimeout, connectTimeout, false, user, pw, serverAddress);
    }
    
    public MongoDbAccessor(int socketTimeout, int connectTimeout, boolean isSecondaryReadPreferred, String user, String pw, ServerAddress ... serverAddress){
        this.serverAddress = serverAddress;
        this.socketTimeout = socketTimeout<0? DEFAULT_SOCKET_TIMEOUT_MS :socketTimeout;
        this.connectTimeout = connectTimeout<0? DEFAULT_CONNECT_TIMEOUT_MS :connectTimeout;
        this.user = user;
        this.pw = pw;
        this.isSecondaryReadPreferred = isSecondaryReadPreferred;
        init();
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


    private void init() {
        LOG.info(">>> init {}", serverAddress);
        try {
            MongoClientOptions options = MongoClientOptions.builder().
            		socketTimeout(socketTimeout).
                    connectTimeout(connectTimeout).
            		//readPreference(isSecondaryReadPreferred?ReadPreference.secondaryPreferred():ReadPreference.primaryPreferred()).
                    readPreference(ReadPreference.primaryPreferred()).
                    writeConcern(WriteConcern.ACKNOWLEDGED).
                    socketKeepAlive(true).
            		build();

            if(user != null && !user.isEmpty() && pw!= null && !pw.isEmpty()) {
                MongoCredential mc = MongoCredential.createCredential(user, "admin", pw.toCharArray());
                if(serverAddress.length == 1) {
                    mongo = new MongoClient(serverAddress[0], Lists.newArrayList(mc), options);
                }else {
                    mongo = new MongoClient(Lists.newArrayList(serverAddress), Lists.newArrayList(mc), options);
                }
            }else{
                if(serverAddress.length == 1) {
                    mongo = new MongoClient(serverAddress[0], options);
                }else {
                    mongo = new MongoClient(Lists.newArrayList(serverAddress), options);
                }
            }


        } catch (MongoException e) {
            LOG.error("Error while initializing mongo at address {}", serverAddress, e);
            closeConnections();
        }
        
        LOG.info("<<< init");
    }

    
    public Document runCommand(String dbName, DBObject cmd) throws IllegalStateException {
        checkMongo();

        if(dbName != null && !dbName.isEmpty()) {
           long start = System.currentTimeMillis();
           Document result = new Document();
           try {
               result = getMongoDatabase(dbName).runCommand((Bson) cmd, isSecondaryReadPreferred ? ReadPreference.secondaryPreferred() : ReadPreference.primaryPreferred());
           }catch (Throwable e){
               LOG.warn("runCommand failed {} on {}/{}", new Object[]{cmd.toString(), mongo.getConnectPoint(), dbName});
               throw e;
           }
           long end = System.currentTimeMillis();
           LOG.info("runCommand {} execTime in ms: {} on {}/{}", new Object[]{cmd.toString(), (end-start), mongo.getConnectPoint(), dbName});
           return result;
        }
        throw new IllegalStateException("Database not initialized");
    }

    private void checkMongo() {
        if(mongo == null /*|| !mongo.getConnector().isOpen()*/) {
            init();
        }
    }
    
     
    public void closeConnections() {
        LOG.info(">>> closeConnections {}", serverAddress);
        
        try {
            if(mongo != null) {
                mongo.close();
                mongo = null;
            }
        } catch (Throwable e) {
            LOG.error("Error while closing mongo ", e);
        }
        
        LOG.info("<<< closeConnections {}", serverAddress);
    }

    //none of both methods is able to fetch all mongod addresses of the whole cluster when router addresses are used to initialize mongo
    private List<ServerAddress> getAllAddresses(){
        return mongo.getAllAddress();
        //return mongo.getServerAddressList();
    }
    
    
    public static void main(String[] args) throws UnknownHostException {
        //ServerAddress adr = new ServerAddress("localhost:27017");
        //mongo-offerlistservice01-03.db00.pro06.eu.idealo.com:27017
        //ServerAddress adr = new ServerAddress("mongo-microservices-01.db00.pro06.eu.idealo.com:27017");
        //ServerAddress adr2 = new ServerAddress("mongo-microservices-02.db00.pro05.eu.idealo.com:27017");
        ServerAddress adr = new ServerAddress("offerstore-de-mongo-n01.db00.pro00.eu.idealo.com:27017");
        ServerAddress adr2 = new ServerAddress("offerstore-de-mongo-n02.db00.pro00.eu.idealo.com:27017");


        MongoDbAccessor monitor = new MongoDbAccessor(null, null, adr, adr2);
        Document doc = monitor.runCommand("admin", new BasicDBObject("isMaster", "1"));
        LOG.info("doc: {}", doc);
        LOG.info("ismaster: {}",  doc.get("ismaster"));

        for(ServerAddress sa : monitor.getAllAddresses()){
            LOG.info("adr: {}", sa);
        }
        monitor.closeConnections();
        
    }

    

}
