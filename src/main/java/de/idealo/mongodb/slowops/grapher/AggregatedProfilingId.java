/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.grapher;

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
    
    private String adr;
    private String op;
    private String user;
    private Set<String> fields;
    private Set<String> sort;
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

        
    /**
     * @return the adr
     */
    public String getAdr() {
        return adr;
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
    public Set<String> getFields() {
        return fields;
    }




    /**
     * @return the sort
     */
    public Set<String> getSort() {
        return sort;
    }




    /**
     * @return the second
     */
    public String getSecond() {
        return second;
    }


    public String getLabel() {
        final StringBuffer result = new StringBuffer();
        result.append(getField("adr", adr));
        result.append(getField("op", op));
        result.append(getField("user", user));
        result.append(getField("fields", fields!=null?fields.toString():null));
        result.append(getField("sort", sort!=null?sort.toString():null));
        
        if(result.length() > 0) {
            result.deleteCharAt(result.length()-1);//remove last char
        }else {
            result.append("empty");
        }
        
        return result.toString();
    }
    
    private String getField(String name, String field) {
        if(field != null && field.length() > 0) {
            final StringBuffer result = new StringBuffer();
            result.append(name).append("=").append(field.replace(',', ';')).append(";");
            return result.toString();
        }
        return "";
    }
    
    public Calendar getCalendar() {
        final Calendar result = new GregorianCalendar();
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
        
        return result;
    }
    
}
