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
 * Created by kay.agahd on 31.03.20.
 */
public class CmdDatabaseStats implements ICommand {

    private static final Logger LOG = LoggerFactory.getLogger(CmdDatabaseStats.class);


    private final CommandResultDto commandResultDto;

    public CmdDatabaseStats() {
        commandResultDto = new CommandResultDto();
        commandResultDto.setTableHeader(Lists.newArrayList("dbs label",
                "host",
                "database",
                "objects",
                "avgObjectSize",
                "dataSize",
                "storageSize",
                "#indexes",
                "indexSize"));
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


            final MongoDatabase db = mongoDbAccessor.getMongoDatabase(profilingReader.getDatabase());
            final Document dbStats = db.runCommand(new Document("dbStats", 1));
            final List<Object> row = Lists.newArrayList();
            row.add(profilingReader.getProfiledServerDto().getLabel());
            row.add(hostname);
            row.add(profilingReader.getDatabase());
            row.add(Util.getNumber(dbStats, "objects", 0));
            row.add(Util.getNumber(dbStats, "avgObjSize", 0));
            row.add(Util.getNumber(dbStats, "dataSize",0));
            row.add(Util.getNumber(dbStats, "storageSize",0));
            row.add(Util.getNumber(dbStats, "indexes", 0));
            row.add(Util.getNumber(dbStats, "indexSize",0));
            table.addRow(row);

    }catch (Exception e){
        LOG.warn("Exception while running command", e);
    }

        return table;
    }


}
