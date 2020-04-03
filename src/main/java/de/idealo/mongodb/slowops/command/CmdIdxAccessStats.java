package de.idealo.mongodb.slowops.command;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.util.JSON;
import de.idealo.mongodb.slowops.collector.ProfilingReader;
import de.idealo.mongodb.slowops.dto.CommandResultDto;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import de.idealo.mongodb.slowops.dto.TableDto;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Created by kay.agahd on 29.06.17.
 */
public class CmdIdxAccessStats implements ICommand {

    private static final Logger LOG = LoggerFactory.getLogger(CmdIdxAccessStats.class);


    private final CommandResultDto commandResultDto;

    public CmdIdxAccessStats() {
        commandResultDto = new CommandResultDto();
        commandResultDto.setTableHeader(Lists.newArrayList("dbs label",
                "host",
                "database",
                "collection",
                "index",
                "key",
                "TTL",
                "#access",
                "since"));

        commandResultDto.setJsonFormattedColumn(5);

    }

    @Override
    public boolean isHostCommand(){
        return false;
    }

    @Override
    public CommandResultDto getCommandResultDto() {
        return commandResultDto;
    }

    @Override
    public TableDto runCommand(ProfilingReader profilingReader, MongoDbAccessor mongoDbAccessor) {
        final TableDto result = new TableDto();
        final MongoDatabase database = mongoDbAccessor.getMongoDatabase(profilingReader.getDatabase());
        final MongoIterable<String> collNames = database.listCollectionNames();

        if(collNames != null){
            for(String collName : collNames){
                if(!"system.profile".equals(collName)) {
                    final TableDto collStats = getIndexStats(database.getCollection(collName), profilingReader.getProfiledServerDto().getLabel());
                    result.addRows(collStats);
                }
            }
        }
        return result;
    }


    private TableDto getIndexStats(MongoCollection<Document> collection, String dbsLabel){
        final TableDto result = new TableDto();
        final MongoIterable<Document> stats = collection
                .aggregate(Arrays.asList(
                        new Document("$indexStats", new Document()),
                        new Document("$sort", new Document("accesses.ops", 1))
                ));
        final HashMap<String, Document> indexesProperties = getIndexesProperties(collection);

        for(Document doc : stats){
            LOG.info("doc: {}", JSON.serialize(doc));
            final ArrayList<Object> row = new ArrayList<Object>();
            row.add(dbsLabel);
            row.add(doc.getString("host"));
            row.add(collection.getNamespace().getDatabaseName());
            row.add(collection.getNamespace().getCollectionName());
            final String indexName = doc.getString("name");
            row.add(indexName);
            row.add(((Document)doc.get("key")).toJson());
            row.add(Boolean.toString(isTTL(indexesProperties, indexName)));
            final Object accesses = doc.get("accesses");
            if(accesses instanceof Document){
                final Document accDoc = (Document) accesses;
                row.add(accDoc.getLong("ops"));
                final Date date = accDoc.getDate("since");
                final LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                row.add(localDateTime.format(DATE_TIME_FORMATTER));
            }else{
                row.add(0L);
                row.add("");
            }

            result.addRow(row);

        }

        return result;
    }

    private HashMap<String, Document> getIndexesProperties(MongoCollection collection){
        final HashMap<String, Document> result = new HashMap<>();
        final ListIndexesIterable<Document> currentIndexes = collection.listIndexes();
        for(Document doc: currentIndexes){
            String indexName = doc.getString("name");
            if(indexName != null){
                result.put(indexName, doc);
            }
        }
        return result;
    }

    private boolean isTTL(HashMap<String, Document> indexesProperties, String indexName){
        final Document indexProps = indexesProperties.get(indexName);
        if(indexProps != null){
            return indexProps.get("expireAfterSeconds")!=null;
        }
        return false;
    }

}
