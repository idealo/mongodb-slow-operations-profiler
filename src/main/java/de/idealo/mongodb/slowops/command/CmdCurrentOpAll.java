package de.idealo.mongodb.slowops.command;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import de.idealo.mongodb.slowops.collector.ProfilingReader;
import de.idealo.mongodb.slowops.dto.CommandResultDto;
import de.idealo.mongodb.slowops.dto.TableDto;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kay.agahd on 03.04.20.
 */
public class CmdCurrentOpAll extends CmdCurrentOp {

    private static final Logger LOG = LoggerFactory.getLogger(CmdCurrentOpAll.class);


    @Override
    public boolean isHostCommand(){
        return true;
    }

    @Override
    public DBObject getQuery(ProfilingReader profilingReader){
        return new BasicDBObject("currentOp", 1);
    };

}
