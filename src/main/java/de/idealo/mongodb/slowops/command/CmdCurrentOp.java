package de.idealo.mongodb.slowops.command;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
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
public class CmdCurrentOp implements ICommand {

    private static final Logger LOG = LoggerFactory.getLogger(CmdCurrentOp.class);


    private final CommandResultDto commandResultDto;

    public CmdCurrentOp() {
        commandResultDto = new CommandResultDto();
        commandResultDto.setTableHeader(Lists.newArrayList("dbs label",
        		"opid",
                "microsecs running",
                "secs running",
                "op",
                "ns",
                "command",
                "planSummary",
                "numYield",
                "active"));

        commandResultDto.setJsonFormattedColumn(6);

    }

    @Override
    public TableDto runCommand(ProfiledServerDto profiledServerDto, MongoDbAccessor mongoDbAccessor) {
        final TableDto table = new TableDto();

        try{
            final Document commandResultDoc = mongoDbAccessor.runCommand("admin", new BasicDBObject("currentOp", 1));

            if(commandResultDoc != null){
                Object inprog = commandResultDoc.get("inprog");
                if(inprog != null && inprog instanceof ArrayList) {
                    final List inprogList = (ArrayList) inprog;
                    for (Object entry : inprogList) {
                        if (entry instanceof Document) {
                            final Document entryDoc = (Document) entry;
                            final List<Object> row = Lists.newArrayList();
                            row.add(profiledServerDto.getLabel());
                            row.add("" + entryDoc.get("opid"));
                            row.add(entryDoc.getLong("microsecs_running"));
                            row.add(entryDoc.getLong("secs_running"));
                            row.add(entryDoc.getString("op"));
                            row.add(entryDoc.getString("ns"));
                            row.add(getJson(entryDoc, "command"));
                            row.add(entryDoc.getString("planSummary"));
                            row.add(entryDoc.getInteger("numYields", -1));
                            row.add(entryDoc.getBoolean("active"));
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



    private String getJson(Document entryDoc, String key){
        final Object obj = entryDoc.get(key);
        if(obj != null && obj instanceof Document){
            Document doc = (Document) obj;
            return doc.toJson();
        }
        return "";
    }

}
