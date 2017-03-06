/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.grapher;


/**
 * Class to compare parts of AggregatedProfilingId for grouping reasons.
 * 
 * @author kay.agahd
 * @since 15.03.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public class AggregatedProfilingGroup extends AggregatedProfilingId {
    
    /**
     * 
     * @return comma separated String of non-empty fields with their names (commas replaced by semicolon)
     */
    public String getLabel() {
        final StringBuffer result = new StringBuffer();
        result.append(getField("adr", getAdr()));
        result.append(getField("op", getOp()));
        result.append(getField("user", getUser()));
        result.append(getField("fields", getFields()!=null?getFields().toString():null));
        result.append(getField("sort", getSort()!=null?getSort().toString():null));
        
        if(result.length() > 0) {
            result.deleteCharAt(result.length()-1);//remove last comma
        }
        
        return result.toString();
    }
    
    private String getField(String name, String field) {
        if(field != null && field.length() > 0) {
            final StringBuffer result = new StringBuffer();
            result.append(name).append("=").append(field.replace(',', ';')).append(",");
            return result.toString();
        }
        return "";
    }

}
