/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.collector;

import com.mongodb.ServerAddress;
import org.bson.Document;

import java.util.Date;
import java.util.Set;

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
    final String db;
    final String col;
    final String op;
    final String user;
    final Set<String> fields;
    final Set<String> sort;
    final Integer nret;
    final Integer respLen;
    final Integer millis;
    final Long cId;//cursorId
    String label;
    String replSet;
    final Integer keys;
    final Integer docs;
    final Boolean isSort;
    final Integer del;
    final Integer ins;
    final Integer mod;

    
    
    public ProfilingEntry(Date ts, ServerAddress adr, String db, String col, String op, String user,
                          Set<String> fields, Set<String> sort, Integer nret, Integer respLen, Integer millis,
                          Long cId, Integer keys, Integer docs, Boolean isSort, Integer del, Integer ins,
                          Integer mod) {
        super();
        this.ts = ts;
        this.adr = adr;
        this.db = db;
        this.col = col;
        this.op = op;
        this.user = user;
        this.fields = fields;
        this.sort = sort;
        this.nret= nret;
        this.respLen = respLen;
        this.millis = millis;
        this.cId = cId;
        this.keys = keys;
        this.docs = docs;
        this.isSort = isSort;
        this.del = del;
        this.ins = ins;
        this.mod = mod;
    }

    public void setLabel(String label){
        this.label = label;
    }

    public void setReplSet(String replSet){
        this.replSet = replSet;
    }


    /**
     * @return
     */
    public Document getDocument() {
        final Document obj = new Document();
        if(label!=null && !label.isEmpty()) obj.put("lbl", label);
        if(ts!=null) obj.put("ts", ts);
        if(adr!=null) obj.put("adr", adr.getHost() + ":" + adr.getPort());
        if(replSet!=null && !replSet.isEmpty()) obj.put("rs", replSet);
        if(db!=null && !db.isEmpty()) obj.put("db", db);
        if(col!=null && !col.isEmpty()) obj.put("col", col);
        if(op!=null && !op.isEmpty()) obj.put("op", op);
        if(user!=null && !user.isEmpty()) obj.put("user", user);
        if(fields!=null && !fields.isEmpty()) obj.put("fields", fields);
        if(sort!=null && !sort.isEmpty()) obj.put("sort", sort);
        if(nret!=null) obj.put("nret", nret);
        if(respLen!=null) obj.put("resplen", respLen);
        if(millis!=null) obj.put("millis", millis);
        if(cId!=null) obj.put("cId", cId);
        if(keys!=null) obj.put("keys", keys);
        if(docs!=null) obj.put("docs", docs);
        if(isSort!=null) obj.put("sortstg", isSort);
        if(del!=null) obj.put("del", del);
        if(ins!=null) obj.put("ins", ins);
        if(mod!=null) obj.put("mod", mod);
        
        return obj;
    }
}
