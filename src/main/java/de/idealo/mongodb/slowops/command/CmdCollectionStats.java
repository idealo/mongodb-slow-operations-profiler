package de.idealo.mongodb.slowops.command;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import de.idealo.mongodb.slowops.collector.ProfilingReader;
import de.idealo.mongodb.slowops.dto.CommandResultDto;
import de.idealo.mongodb.slowops.dto.TableDto;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import de.idealo.mongodb.slowops.util.Util;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kay.agahd on 29.06.17.
 */
public class CmdCollectionStats implements ICommand {

    private static final Logger LOG = LoggerFactory.getLogger(CmdCollectionStats.class);


    private final CommandResultDto commandResultDto;

    public CmdCollectionStats() {
        commandResultDto = new CommandResultDto();
        commandResultDto.setTableHeader(Lists.newArrayList("dbs label",
                "host",
                "database",
                "collection",
                "sharded",
                "size",
                "count",
                "avgObjectSize",
                "storageSize",
                "capped",
                "#indexes",
                "totalIndexSize",
                "indexSizes"));

        commandResultDto.setJsonFormattedColumn(12);

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
        final TableDto table = new TableDto();

        try{
            final Document commandResultDoc = mongoDbAccessor.runCommand("admin", new BasicDBObject("hostInfo", 1));
            final Object system = commandResultDoc.get("system");
            String hostname = "";
            if (system instanceof Document) {
                final Document systemDoc = (Document) system;
                hostname = systemDoc.getString("hostname");
            }

            final ArrayList<String> collNames = getCollectionNames(mongoDbAccessor, profilingReader.getDatabase());
            final MongoDatabase db = mongoDbAccessor.getMongoDatabase(profilingReader.getDatabase());
            for(String collName : collNames){
                Document collStats = db.runCommand(new Document("collStats", collName));
                final List<Object> row = Lists.newArrayList();
                row.add(profilingReader.getProfiledServerDto().getLabel());
                row.add(hostname);
                row.add(profilingReader.getDatabase());
                row.add(collName);
                row.add(collStats.getBoolean("sharded"));
                row.add(Util.getNumber(collStats, "size",0));
                row.add(collStats.getInteger("count"));
                row.add(Util.getNumber(collStats, "avgObjSize",0));
                row.add(Util.getNumber(collStats, "storageSize",0));
                row.add(collStats.getBoolean("capped"));
                row.add(collStats.getInteger("nindexes"));
                row.add(Util.getNumber(collStats, "totalIndexSize",0));
                row.add(((Document)collStats.get("indexSizes")).toJson());
                table.addRow(row);
            }
    }catch (Exception e){
        LOG.warn("Exception while running command", e);
    }

        return table;
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
