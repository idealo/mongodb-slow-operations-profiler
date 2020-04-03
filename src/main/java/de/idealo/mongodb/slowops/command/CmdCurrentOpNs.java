package de.idealo.mongodb.slowops.command;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import de.idealo.mongodb.slowops.collector.ProfilingReader;
import de.idealo.mongodb.slowops.dto.CommandResultDto;
import de.idealo.mongodb.slowops.dto.TableDto;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by kay.agahd on 03.04.20.
 */
public class CmdCurrentOpNs extends CmdCurrentOp {

    private static final Logger LOG = LoggerFactory.getLogger(CmdCurrentOpNs.class);

    @Override
    public boolean isHostCommand(){
        return false;
    }

    @Override
    public DBObject getQuery(ProfilingReader profilingReader){
        return new BasicDBObject("currentOp", 1)
                .append("ns",
                        new BasicDBObject("$regex", "^"+Pattern.quote(profilingReader.getDatabase()+".")));
    };



}
