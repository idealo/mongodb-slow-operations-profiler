/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.grapher;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 
 * 
 * @author kay.agahd
 * @since 14.03.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public class AggregatedProfilingId {

    private String lbl;
    private String adr;
    private String rs;
    private String db;
    private String col;
    private String op;
    private String user;
    private LinkedHashSet<String> fields;
    private LinkedHashSet<String> sort;
    private LinkedHashSet<String> proj;
    private String year;
    private String month;
    private String week;
    private String dayOfMonth;
    private String hour;
    private String minute;
    private String second;
    private String milliseconds;
    
    public AggregatedProfilingId(){      
    }

    public AggregatedProfilingId(String lbl, String db, String col, String op, LinkedHashSet<String> fields, LinkedHashSet<String> sort, LinkedHashSet<String> proj) {
        this.lbl = lbl;
        this.db = db;
        this.col = col;
        this.op = op;
        this.fields = fields;
        this.sort = sort;
        this.proj = proj;
    }

    /**
     * This defines a "fully qualified query shape".
     *
     * This is not foolproof, e.g. a query executed on different collections may be different
     * (because the same named field can be of different type and semantic)
     * even if all other characteristics below are identical.
     * But since we want to store only a minimum of example queries, this should be good enough.
     *
     * @return a fingerprint of the query shape
     */
    public String getFingerprint(){
        final StringBuffer result = new StringBuffer();

        //separate each characteristics by . so even if some are null, the fingerprint will change
        if(op!=null) result.append(lbl).append(".").append(db).append(".").append(col).append(".").append(op);
        appendValues(fields, result);
        appendValues(sort, result);
        appendValues(proj, result);

        return Hashing.murmur3_128().hashString(result.toString(), StandardCharsets.UTF_8).toString();
    }

    private void appendValues(LinkedHashSet<String> fields, StringBuffer result){
        result.append(".");
        if(fields!=null) {
            final Iterator<String> fi = fields.iterator();
            while(fi.hasNext()) {
                result.append(fi.next());
            }
        };
    }

    /**
     * Returns true if it make sense to show a slow operations document as an example.
     * This is the case, if at least the label, database, collection and operation are known.
     *
     * The queried, sorted and projected fields may be empty, and as such take part in defining a "fully qualified query shape".
     *
     * @return
     */
    public boolean isFingerprintable(){
        return lbl!=null && !lbl.isEmpty() &&
               db!=null && !db.isEmpty() &&
               col!=null && !col.isEmpty()  &&
               op!=null && !op.isEmpty();
    }

    /**
     * @return the lbl
     */
    public String getLbl() {
        return lbl;
    }



    /**
     * @return the adr
     */
    public String getAdr() {
        return adr;
    }


    /**
     * @return the rs
     */
    public String getRs() {
        return rs;
    }


    /**
     * @return the db
     */
    public String getDb() {
        return db;
    }


    /**
     * @return the col
     */
    public String getCol() {
        return col;
    }


    /**
     * @return the op
     */
    public String getOp() {
        return op;
    }




    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }




    /**
     * @return the fields
     */
    public LinkedHashSet<String> getFields() {
        return fields;
    }




    /**
     * @return the sort
     */
    public LinkedHashSet<String> getSort() {
        return sort;
    }

    /**
     * @return the proj
     */
    public LinkedHashSet<String> getProj() {
        return proj;
    }



    /**
     * @return the second
     */
    public String getSecond() {
        return second;
    }

    public String getLabel(boolean isHtml) {
        final StringBuffer result = new StringBuffer();
        result.append(getField("lbl", lbl, isHtml));
        result.append(getField("adr", adr, isHtml));
        result.append(getField("rs", rs, isHtml));
        result.append(getField("db", db, isHtml));
        result.append(getField("col", col, isHtml));
        result.append(getField("op", op, isHtml));
        result.append(getField("user", user, isHtml));
        result.append(getField("fields", getStringList(fields), isHtml));
        result.append(getField("sort", getStringList(sort), isHtml));
        result.append(getField("proj", getStringList(proj), isHtml));
        
        if(result.length() > 0) {
            if(isHtml) {
                result.delete(result.length() - 4, result.length());//remove last <br>
            }else {
                result.deleteCharAt(result.length() - 2);//remove last ;SPACE
            }
        }else {
            result.append("empty");
        }
        
        return result.toString();
    }

    private String getStringList(LinkedHashSet<String> str){
        if(str == null) return null;
        final StringBuffer result = new StringBuffer();
        for (String s: str) {
            result.append("'").append(s).append("'").append("; ");
        }
        if(result.length()>0) result.deleteCharAt(result.length() - 2);//remove last ;SPACE

        return result.toString();
    }
    
    private String getField(String name, String field, boolean isHtml) {
        if(field != null && field.length() > 0) {
            final StringBuffer result = new StringBuffer();
            if(isHtml){
                result.append(name).append("=").append(field).append("<br>");
            }else {
                result.append(name).append("=").append(field).append("; ");
            }
            return result.toString();
        }
        return "";
    }
    
    public Calendar getCalendar() {
        final Calendar result = new GregorianCalendar();
        final int offset = (result.get(Calendar.ZONE_OFFSET) + result.get(Calendar.DST_OFFSET));
        
        result.setTimeInMillis(0);//reset
        
        if(year != null) {
            try {
                result.set(Calendar.YEAR, Integer.parseInt(year));
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if(month != null) {
            try {
                result.set(Calendar.MONTH, Integer.parseInt(month)-1);
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if(week != null) {
            try {
                result.set(Calendar.WEEK_OF_YEAR, Integer.parseInt(week));
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if(dayOfMonth != null) {
            try {
                result.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dayOfMonth));
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if(hour != null) {
            try {
                result.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if(minute != null) {
            try {
                result.set(Calendar.MINUTE, Integer.parseInt(minute));
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if(second != null) {
            try {
                result.set(Calendar.SECOND, Integer.parseInt(second));
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if(milliseconds != null) {
            try {
                result.set(Calendar.MILLISECOND, Integer.parseInt(milliseconds));
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        result.add(Calendar.MILLISECOND, offset);
        
        return result;
    }
    
}
