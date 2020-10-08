package de.idealo.mongodb.slowops.collector;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.IndexOptions;
import de.idealo.mongodb.slowops.dto.CollectorServerDto;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import de.idealo.mongodb.slowops.util.ConfigReader;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ExampleSlowOpsCache {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleSlowOpsCache.class);

    public static ExampleSlowOpsCache INSTANCE = new ExampleSlowOpsCache();

    private volatile HashSet<String> cache;
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();
    private final Lock readLock;
    private final Lock writeLock;
    private final CollectorServerDto serverDto;
    private final MongoDbAccessor mongo;
    private ScheduledExecutorService cleanUpService =null;
    private ScheduledFuture<?> cleanUpFuture =null;

    private ExampleSlowOpsCache(){
        cache = new HashSet<>();
        serverDto = ConfigReader.getCollectorServer();
        readLock = globalLock.readLock();
        writeLock = globalLock.writeLock();

        mongo = new MongoDbAccessor(serverDto.getAdminUser(), serverDto.getAdminPw(), serverDto.getSsl(), serverDto.getHosts());
        try {
            final MongoCollection<Document> exampleCollection = getExampleCollection();

            final IndexOptions indexOptions = new IndexOptions();
            indexOptions.background(true);
            //index for retrieval by fingerprint (don't make it unique in case of collisions by the hashing algorithm)
            LOG.info("Create index {fp:1} in the background if it does not yet exists");
            exampleCollection.createIndex(new BasicDBObject("fp", 1), indexOptions);
            //index for removing old entries
            // e.g. when the slow ops collection is a capped collection
            // then entries older than the oldest slow op can be removed from the example collection.
            // The entry may be added automatically anew if the corresponding query is collected again.
            // However, if such query is not collected again, other still stored slow ops might have lost their example document if the have the same fingerprint.
            LOG.info("Create index {ts:1} in the background if it does not yet exists");
            exampleCollection.createIndex(new BasicDBObject("ts", 1), indexOptions);

            loadCache(exampleCollection);

            LOG.info("ExampleSlowOpsCache is ready at {}", serverDto.getHosts());

            final Runnable cleanupTask = getTaskToRemoveExpiredExamples();
            cleanUpService = Executors.newScheduledThreadPool(1);
            cleanUpFuture = cleanUpService.scheduleAtFixedRate(cleanupTask, 2, 1, TimeUnit.HOURS); // init Delay = 2, repeat the task every 1 hour


        } catch (MongoException e) {
            LOG.error("Exception while connecting to: {}", serverDto.getHosts(), e);
        }
    }

    private Runnable getTaskToRemoveExpiredExamples(){
        final Runnable result = () -> {
            final MongoCollection<Document> exampleCollection = getExampleCollection();
            final Date oldestSlowOp = getOldestSlowOp();
            final BasicDBObject query = new BasicDBObject("ts", new BasicDBObject("$lt", oldestSlowOp));
            final BasicDBObject fields = new BasicDBObject("_id", -1).append("fp", 1);
            final MongoCursor<Document> c = exampleCollection.find(query).projection(fields).iterator();
            final List toDeleteFromDB = Lists.newArrayList();
            final HashSet<String> toDeleteFromCache = new HashSet<>();
            try {
                while (c.hasNext()) {//since the cache is small enough to be stored in memory, we can read all fp's into memory first to delete them later at once as batch
                    final Document obj = c.next();
                    final String fp = obj.getString("fp");
                    toDeleteFromDB.add(new DeleteOneModel(new Document("fp", fp)));
                    toDeleteFromCache.add(fp);
                }
            } finally {
                c.close();
            }

            try {
                writeLock.lock();
                cache.removeAll(toDeleteFromCache);
                if(toDeleteFromDB.size() > 0) {
                    try {
                        exampleCollection.bulkWrite(toDeleteFromDB, new BulkWriteOptions().ordered(false));
                    } catch (Exception e) {
                        LOG.error("Error while deleting expired example documents from '{}', so reload the cache.", exampleCollection.getNamespace(), e);
                        loadCache(exampleCollection);
                    }
                }
            } finally {
                writeLock.unlock();
            }

        };
        return result;
    }

    private Date getOldestSlowOp(){
        final MongoCollection<Document> slowOpsCollection = getSlowOpsCollection("");
        final BasicDBObject fields = new BasicDBObject("_id", -1).append("ts", 1);
        final BasicDBObject sort = new BasicDBObject("ts", 1);
        final MongoCursor<Document> c = slowOpsCollection.find().projection(fields).sort(sort).limit(1).iterator();

        try {
            while(c.hasNext()) {
                final Document obj = c.next();
                return obj.getDate("ts");
            }
        }finally {
            c.close();
        }
        return null;
    }

    private MongoCollection<Document> getSlowOpsCollection(String suffix){
        final MongoDatabase db = mongo.getMongoDatabase(serverDto.getDb());
        final MongoCollection<Document> result =  db.getCollection(serverDto.getCollection() + suffix);

        if(result == null) {
            throw new IllegalArgumentException("Can't continue without collection '"+serverDto.getCollection() + suffix+"' for " + serverDto.getHosts());
        }
        return result;
    }


    private MongoCollection<Document> getExampleCollection(){
        return getSlowOpsCollection(".ex");
    }

    private void loadCache(MongoCollection<Document> exampleCollection) {
        try {
                final BasicDBObject fields = new BasicDBObject();

                fields.put("_id", Integer.valueOf(0));
                fields.put("fp", Integer.valueOf(1));

                final MongoCursor<Document> c = exampleCollection.find(new BasicDBObject()).projection(fields).iterator();
                try {
                    while(c.hasNext()) {
                        final Document obj = c.next();
                        cache.add(obj.getString("fp"));
                    }
                }finally {
                    c.close();
                }

        }catch(Exception e) {
            LOG.error("Couldn't load example cache from {}", new Object[]{exampleCollection.getNamespace(), e});
        }
    }


    public void addToCache(String fp, Document slowOperation) {
        LOG.debug(">>> addToCache fp:{}", fp);
        try{
            readLock.lock();
            if(cache.contains(fp)){
                LOG.debug("no need to add, fp exists:{}", fp);
                return;
            }
        }finally {
            readLock.unlock();
        }
        try{
            writeLock.lock();
            if(cache.add(fp)){
                final MongoCollection<Document> exampleCollection = getExampleCollection();
                final Document doc = new Document("fp", fp)
                        .append("ts", new Date())
                        .append("doc", replaceIllegalChars(slowOperation, new Document()));
                exampleCollection.insertOne(doc);
                LOG.debug("OK addToCache fp:{}", fp);
            }
        }finally {
            writeLock.unlock();
        }
        LOG.debug("<<< addToCache fp:{}", fp);
    }


    public List<Document> getSlowOp(String fp) {
        List<Document> result = Lists.newLinkedList();
        try{
            readLock.lock();
            if(cache.contains(fp)){
                final MongoCollection<Document> exampleCollection = getExampleCollection();
                final Document query = new Document("fp", fp);
                final Document fields = new Document("_id", -1).append("doc", 1);
                final MongoCursor<Document> c = exampleCollection.find(query).projection(fields).iterator();
                try {
                    while(c.hasNext()) {//it's usually only 1 doc but the hashing algorithm may have produced collisions, so return them all
                        final Document obj = c.next();
                        if(obj instanceof Document){
                            result.add((Document)obj.get("doc"));
                        }
                    }
                }finally {
                    c.close();
                }
            }
        }finally {
            readLock.unlock();
        }
        return result;
    }

    /**
     * replace illegal characters from field names such as $ and .
     * by their html entity &#36; respective &#46;
     * because the doc will be shown only in browsers
     * https://docs.mongodb.com/manual/reference/limits/#Restrictions-on-Field-Names
     *
     * Side effect: since all nodes of the document will be traversed in order to replace the illegal chars in their keys,
     * values that are shown in the mongo-shell as an Object will be cut down into their parts, e.g.:
     * e.g.:
     * ISODate("2020-02-29T20:42:23.979Z")
     * becomes:
     * {"$date": "2020-02-29T20:42:23.979Z"}
     * or:
     * {"id" : BinData(3,"zU1B4cnuCK9/ntQZkFU4gA==")}
     * becomes:
     *     {"id": {
     *         "$binary": {
     *           "base64": "zU1B4cnuCK9/ntQZkFU4gA==",
     *           "subType": "03"
     *          }
     *        }
     *     }
     *
     * @param
     */
    private Document replaceIllegalChars(Object input, Document output) {

        if(input instanceof Document) {
            Document dbObj = (Document)input;
            for (String key : dbObj.keySet()) {
                Object obj = dbObj.get(key);
                key = key.replace("$", "&#36;").replace(".", "&#46;");
                if (obj instanceof Collection) {
                    final List<Object> list = Lists.newLinkedList();
                    for (Object subObj : (Collection<Object>) obj) {
                        if (! (subObj instanceof Document || subObj instanceof Collection)) {
                            list.add(subObj); // add scalar value
                        }else{
                            list.add(replaceIllegalChars(subObj, new Document())); // add structured value
                        }
                    }
                    output.append(key, list);
                }else if(obj instanceof Document){
                        final Document cleanDoc = replaceIllegalChars(obj, new Document());
                        output.append(key, cleanDoc);
                }else{
                    output.append(key, obj);
                }
            }
        }
        return output;
    }

    public static void terminate() {
        if(INSTANCE != null) {
            if(INSTANCE.cleanUpFuture != null) INSTANCE.cleanUpFuture.cancel(true);
            if(INSTANCE.cleanUpService != null) INSTANCE.cleanUpService.shutdown();
        }
    }

    public static void main(String[] args) {
        /*
        $root": {
            "$number": 1,
            "$field": {
              "$date": "2020-04-08T13:07:27.456Z"
            },
            "$list": [
              {
                "$l1": "v1"
              },
              {
                "$l2": "v2"
              }
            ],
            "$array": [
              1,2
            ]
          },
         */

        List<Document> list = Lists.newArrayList();
        list.add(new Document("$l1", "v1"));
        list.add(new Document("$l2", "v2"));
        List<Integer> array = Lists.newArrayList();
        array.add(1);
        array.add(2);

        Document in = new Document("$root",
                new Document("$number", -1)
                        .append("$field", new Date())
                        .append("$list", list)
                        .append("$array", array)
        );

        Document out = ExampleSlowOpsCache.INSTANCE.replaceIllegalChars(in, new Document());

        JsonWriterSettings.Builder settingsBuilder = JsonWriterSettings.builder().indent(true);
        String json = out.toJson(settingsBuilder.build());
        LOG.info(json);

    }
}
