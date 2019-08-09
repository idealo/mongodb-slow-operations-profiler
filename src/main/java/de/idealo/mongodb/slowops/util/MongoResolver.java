package de.idealo.mongodb.slowops.util;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;


public class MongoResolver implements Callable<MongoResolver> {

    private static final Logger LOG = LoggerFactory.getLogger(MongoResolver.class);

    private final String adminUser;
    private final String adminPassword;
    private final boolean ssl;
    private final ServerAddress[] serverAddress;
    private int socketTimeout;
    private int responseTimeout;
    private List<ServerAddress> resolvedHosts;
    private List<String> resolvedDatabases;


    public MongoResolver(String adminUser, String adminPassword, boolean ssl, ServerAddress serverAddress) {
        this(-1, -1, adminUser, adminPassword, ssl, serverAddress);
    }


    public MongoResolver(int socketTimeout, int responseTimeout, String adminUser, String adminPassword, boolean ssl, ServerAddress... serverAddress) {
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
        this.ssl = ssl;
        this.serverAddress = serverAddress;
        this.socketTimeout = socketTimeout;
        this.responseTimeout = responseTimeout;
        resolvedHosts = Lists.newLinkedList();
        resolvedDatabases = Lists.newLinkedList();
    }

    public List<ServerAddress> getResolvedHosts() {
        return resolvedHosts;
    }

    public List<String> getResolvedDatabases() {
        return resolvedDatabases;
    }

    /**
     * If the given serverAddresses point to routers (mongos) of a sharded system,
     * it returns a list of all mongod ServerAddresses of the sharded system.
     * If the given serverAddresses point to mongod's,
     * the returned List will contain the ServerAddresses of all replSet members
     * or, if not a replSet, the first given ServerAddress.
     *
     * @return
     */
    public List<ServerAddress> resolveMongodAddresses(MongoDbAccessor mongo) {

        final List<ServerAddress> result = Lists.newLinkedList();

        try {
            final Document doc = mongo.runCommand("admin", new BasicDBObject("listShards", 1));
            final Object shards = doc.get("shards");
            if(shards != null) {
                final ArrayList<Document> list = (ArrayList<Document>)shards;
                for (Object obj : list) {
                    final Document dbo = (Document)obj;
                    String hosts = dbo.getString("host");
                    int slashIndex = hosts.indexOf("/");
                    if(slashIndex > -1) {
                        hosts = hosts.substring(slashIndex+1);
                    }
                    //hosts = hosts.replace(',', ' ');

                    result.addAll(Util.getServerAddresses(hosts));
                }
                return result;
            }
        } catch (MongoCommandException e) {//replSets do not know the command listShards, thus throwing an Exception
            try {
                final Document doc = mongo.runCommand("admin", new BasicDBObject("replSetGetStatus", 1));
                final Object members = doc.get("members");
                if (members != null && members instanceof ArrayList) {
                    final ArrayList<Document> list = (ArrayList<Document>) members;
                    for (Object obj : list) {
                        final Document dbo = (Document) obj;
                        final String host = dbo.getString("name");
                        result.add(new ServerAddress(host));
                    }
                    return result;
                }
            }catch (MongoCommandException e2) {//single nodes do not know the command replSets, thus throwing an Exception
                final Document doc = mongo.runCommand("admin", new BasicDBObject("serverStatus", 1));
                final Object repl = doc.get("repl");//single nodes don't have serverStatus().repl
                if(repl == null) {
                    if(serverAddress.length > 0)
                    result.add(serverAddress[0]);
                }
            }
        }

        return result;

    }

    private List<String> resolveAllDbNames(MongoDbAccessor mongoDbAccessor){
        List<String> result = Lists.newArrayList();

        try{
            final Document commandResultDoc = mongoDbAccessor.runCommand("admin", new BasicDBObject("listDatabases", 1));

            if(commandResultDoc != null){
                Object databases = commandResultDoc.get("databases");
                if(databases != null && databases instanceof ArrayList) {
                    final List dbList = (ArrayList) databases;
                    for (Object entry : dbList) {
                        if (entry instanceof Document) {
                            final Document entryDoc = (Document) entry;
                            final String dbName = entryDoc.getString("name");
                            result.add(dbName);
                        }
                    }
                }
            }
        }
        catch (Exception e){
            LOG.warn("Exception while running command listDatabases", e);
        }
        return result;
    }

    @Override
    public MongoResolver call() throws Exception {
        final MongoDbAccessor mongo = new MongoDbAccessor(socketTimeout, responseTimeout, adminUser, adminPassword, ssl, serverAddress);

        try {
            resolvedHosts.addAll(resolveMongodAddresses(mongo));
            resolvedDatabases.addAll(resolveAllDbNames(mongo));

        } catch (MongoException e) {
            LOG.error("Couldn't start mongo node at address {}", serverAddress, e);
        }
        return this;
    }

}
