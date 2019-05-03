package de.idealo.mongodb.slowops.command;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoIterable;
import com.mongodb.util.JSON;
import de.idealo.mongodb.slowops.dto.CommandResultDto;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import de.idealo.mongodb.slowops.dto.TableDto;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by kay.agahd on 29.06.17.
 */
public class CmdIdxAccessStats implements ICommand {

    private static final Logger LOG = LoggerFactory.getLogger(CmdIdxAccessStats.class);


    private final CommandResultDto commandResultDto;

    public CmdIdxAccessStats() {
        commandResultDto = new CommandResultDto();
        commandResultDto.setTableHeader(Lists.newArrayList("dbs label",
                "database",
                "collection",
                "index",
                "#access",
                "since",
                "host"));

    }

    @Override
    public TableDto runCommand(ProfiledServerDto profiledServerDto, MongoDbAccessor mongoDbAccessor) {
        final TableDto table = new TableDto();

        try{
            final Document commandResultDoc = mongoDbAccessor.runCommand("admin", new BasicDBObject("listDatabases", 1));

            if(commandResultDoc != null){
                Object databases = commandResultDoc.get("databases");
                if(databases != null && databases instanceof ArrayList) {
                    final List dbList = (ArrayList) databases;
                    for (Object entry : dbList) {
                        if (entry instanceof Document) {
                            final Document entryDoc = (Document) entry;
                            final List<Object> row = Lists.newArrayList();
                            final String dbName = entryDoc.getString("name");
                            if(!"admin".equals(dbName) && !"config".equals(dbName) && !"local".equals(dbName)) {
                                final TableDto idxStats = getIndexStats(mongoDbAccessor, profiledServerDto.getLabel(), dbName);

                                table.addRows(idxStats);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e){
            LOG.warn("Exception while running command", e);
        }

        return table;
    }


    @Override
    public CommandResultDto getCommandResultDto() {
        return commandResultDto;
    }


    private TableDto getIndexStats(MongoDbAccessor mongoDbAccessor, String dbsLabel, String dbName){
        final TableDto result = new TableDto();
        final MongoIterable<String> collNames = mongoDbAccessor.getMongoDatabase(dbName).listCollectionNames();

        if(collNames != null){
            for(String collName : collNames){
                if(!"system.profile".equals(collName)) {
                    final TableDto collStats = getIndexStats(mongoDbAccessor, dbsLabel, dbName, collName);
                    result.addRows(collStats);
                }
            }
        }
        return result;
    }


    private TableDto getIndexStats(MongoDbAccessor mongoDbAccessor, String dbsLabel, String dbName, String collName){
        final TableDto result = new TableDto();
        final MongoIterable<Document> stats = mongoDbAccessor.getMongoDatabase(dbName).getCollection(collName)
                .aggregate(Arrays.asList(
                        new Document("$indexStats", new Document()),
                        new Document("$project", new Document("name", 1)
                                                    .append("accesses.ops", 1)
                                                    .append("accesses.since", 1)
                                                    .append("host", 1)
                        ),
                        new Document("$sort", new Document("accesses.ops", 1))
                ));

        if(stats != null){
            for(Document doc : stats){
                LOG.info("db: {} col: {}", dbName, collName);
                LOG.info("doc: {}", JSON.serialize(doc));
                final ArrayList<Object> row = new ArrayList<Object>();
                row.add(dbsLabel);
                row.add(dbName);
                row.add(collName);
                row.add(doc.getString("name"));
                final Object accesses = doc.get("accesses");
                if(accesses != null && accesses instanceof Document){
                    final Document accDoc = (Document) accesses;
                    row.add(accDoc.getLong("ops"));
                    final Date date = accDoc.getDate("since");
                    final LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    row.add(localDateTime.format(DATE_TIME_FORMATTER));
                }else{
                    row.add(0L);
                    row.add("");
                }
                row.add(doc.getString("host"));
                result.addRow(row);

            }
        }
        return result;
    }

}
