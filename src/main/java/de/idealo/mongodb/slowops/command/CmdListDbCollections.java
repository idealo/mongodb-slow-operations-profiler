package de.idealo.mongodb.slowops.command;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoIterable;
import de.idealo.mongodb.slowops.dto.CommandResultDto;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import de.idealo.mongodb.slowops.dto.TableDto;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kay.agahd on 29.06.17.
 */
public class CmdListDbCollections implements ICommand {

    private static final Logger LOG = LoggerFactory.getLogger(CmdListDbCollections.class);


    private final CommandResultDto commandResultDto;

    public CmdListDbCollections() {
        commandResultDto = new CommandResultDto();
        commandResultDto.setTableHeader(Lists.newArrayList("dbs label",
                "database name",
                "db size on disk",
                "collection(s)"));

        commandResultDto.setJsonFormattedColumn(3);

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
                            row.add(profiledServerDto.getLabel());
                            row.add(dbName);
                            row.add(entryDoc.get("sizeOnDisk"));
                            row.add((new Document(dbName, getCollectionNames(mongoDbAccessor, dbName))).toJson());
                            table.addRow(row);
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



    private ArrayList<String> getCollectionNames(MongoDbAccessor mongoDbAccessor, String dbName){
        final ArrayList<String> result = new ArrayList<String>();
        final MongoIterable<String> collNames = mongoDbAccessor.getMongoDatabase(dbName).listCollectionNames();

        if(collNames != null){
           for(String collName : collNames){
               result.add(collName);
           }
        }
        return result;
    }

}
