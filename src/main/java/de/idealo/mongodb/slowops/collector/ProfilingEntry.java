/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.collector;

import java.util.*;

import com.mongodb.*;

/**
 * 
 * 
 * @author kay.agahd
 * @since 27.02.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public class ProfilingEntry {

    
    final Date ts;
    final ServerAddress adr;
    final String op;
    final String user;
    final Set<String> fields;
    final Set<String> sort;
    final Integer nret;
    final Integer respLen;
    final Integer millis;
    
    
    
    public ProfilingEntry(Date ts, ServerAddress adr, String op, String user, Set<String> fields, Set<String> sort, Integer nret, Integer respLen, Integer millis) {
        super();
        this.ts = ts;
        this.adr = adr;
        this.op = op;
        this.user = user;
        this.fields = fields;
        this.sort = sort;
        this.nret= nret;
        this.respLen = respLen;
        this.millis = millis;
        
    }

    public DBObject getDBObject() {
        final DBObject obj = new BasicDBObject();
        obj.put("ts", ts);
        obj.put("adr", adr.getHost() + ":" + adr.getPort());
        obj.put("op", op);
        obj.put("user", user);
        obj.put("fields", fields);
        obj.put("sort", sort);
        obj.put("nret", nret);
        obj.put("resplen", respLen);
        obj.put("millis", millis);
        
        return obj;
    }
}
