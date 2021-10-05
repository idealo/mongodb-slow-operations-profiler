package de.idealo.mongodb.slowops.command;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import de.idealo.mongodb.slowops.collector.ProfilingReader;
import de.idealo.mongodb.slowops.dto.CommandResultDto;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import de.idealo.mongodb.slowops.dto.TableDto;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import de.idealo.mongodb.slowops.util.Util;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by kay.agahd on 29.06.17.
 */
public class CmdHostInfo implements ICommand {

    private static final Logger LOG = LoggerFactory.getLogger(CmdHostInfo.class);


    private final CommandResultDto commandResultDto;

    public CmdHostInfo() {
        commandResultDto = new CommandResultDto();
        commandResultDto.setTableHeader(Lists.newArrayList("Dbs label",
                "Hostname",
                "Mongodb version",
                "CPU arch",
                "CPU cores",
                "CPU freq in MHz",
                "RAM in MB",
                "Numa enabled",
                "Page size",
                "Num pages",
                "Max open files",
                "OS",
                "Kernel version",
                "Libc version"));

    }

    @Override
    public boolean isHostCommand(){
        return true;
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
            final Document buildInfoDoc = mongoDbAccessor.runCommand("admin", new BasicDBObject("buildInfo", 1));

            if(commandResultDoc != null) {
                Object system = commandResultDoc.get("system");
                Object os = commandResultDoc.get("os");
                Object extra = commandResultDoc.get("extra");
                if (system instanceof Document && os instanceof Document && extra instanceof Document) {
                    final Document systemDoc = (Document) system;
                    final Document osDoc = (Document) os;
                    final Document extraDoc = (Document) extra;
                    final List<Object> row = Lists.newArrayList();
                    row.add(profilingReader.getProfiledServerDto().getLabel());
                    row.add(systemDoc.getString("hostname"));
                    if(buildInfoDoc != null) {
                        row.add(buildInfoDoc.getString("version"));
                    }else{
                        row.add("unknown");
                    }
                    row.add(systemDoc.getString("cpuArch"));
                    row.add(Util.getNumber(systemDoc, "numCores", 0));
                    row.add((Math.round(Double.parseDouble(extraDoc.getString("cpuFrequencyMHz")))));
                    row.add(Util.getNumber(systemDoc, "memSizeMB", 0));
                    row.add(systemDoc.getBoolean("numaEnabled").toString());
                    row.add(Util.getNumber(extraDoc, "pageSize", 0));
                    row.add(Util.getNumber(extraDoc, "numPages", 0));
                    row.add(Util.getNumber(extraDoc, "maxOpenFiles",0));
                    row.add(osDoc.getString("name"));
                    row.add(osDoc.getString("version"));
                    row.add(extraDoc.getString("libcVersion"));

                    table.addRow(row);
                }
            }
        }
        catch (Exception e){
            LOG.warn("Exception while running command", e);
        }

        return table;
    }


}
