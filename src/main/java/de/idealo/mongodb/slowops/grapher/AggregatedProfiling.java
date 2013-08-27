/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.grapher;

import java.util.Date;

/**
 * 
 * 
 * @author kay.agahd
 * @since 14.03.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public class AggregatedProfiling {
    
    
    private AggregatedProfilingId _id;   
    private int count;
    private long millis;
    private double avgMs;
    private double minMs;
    private double maxMs;
    private double avgRet;
    private double minRet;
    private double maxRet;
    private Date firstts;
    
    private AggregatedProfiling(){
        
    }

    /**
     * @return the _id
     */
    public AggregatedProfilingId getId() {
        return _id;
    }

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * @return the millis
     */
    public long getMillis() {
        return millis;
    }

    /**
     * @return the avgMs
     */
    public double getAvgMs() {
        return avgMs;
    }

    /**
     * @return the minMs
     */
    public double getMinMs() {
        return minMs;
    }

    /**
     * @return the maxMs
     */
    public double getMaxMs() {
        return maxMs;
    }

    /**
     * @return the avgRet
     */
    public double getAvgRet() {
        return avgRet;
    }

    /**
     * @return the minRet
     */
    public double getMinRet() {
        return minRet;
    }

    /**
     * @return the maxRet
     */
    public double getMaxRet() {
        return maxRet;
    }

    /**
     * @return the firstts
     */
    public Date getFirstts() {
        return firstts;
    }

    
}
