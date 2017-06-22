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


public class MongoResolver implements Callable<List<ServerAddress>> {

    private static final Logger LOG = LoggerFactory.getLogger(MongoResolver.class);

    private final String adminUser;
    private final String adminPassword;
    private final ServerAddress serverAddress;


    public MongoResolver(String adminUser, String adminPassword, ServerAddress serverAddress) {
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
        this.serverAddress = serverAddress;
    }

    /**
     * If the given serverAddress is a router (mongos) of a sharded system,
     * it returns a list of all mongod ServerAddresses of the sharded system.
     * If the given serverAddress is a mongod,
     * the returned List will contain the ServerAddresses of all replSet members
     * or, if not a replSet, the given ServerAddress.
     *
     * @return
     */
    public List<ServerAddress> getMongodAddresses() {

        final List<ServerAddress> result = Lists.newLinkedList();
        MongoDbAccessor mongo = null;

        try {
            mongo = new MongoDbAccessor(adminUser, adminPassword, serverAddress);

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
                    if(repl == null) result.add(serverAddress);
                }
            }

        } catch (MongoException e) {
            LOG.error("Couldn't start mongo node at address {}", serverAddress, e);
        } finally {
            if(mongo != null) {
                mongo.closeConnections();
            }
        }
        return result;

    }

    @Override
    public List<ServerAddress> call() throws Exception {
        return getMongodAddresses();
    }
}
