/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.dto;

import java.util.Map;

/**
 * 
 * 
 * @author kay.agahd
 * @since 26.03.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public class CollectorDto {

    private final Map<String, Long> numberOfReads;
    private final  long numberOfWrites;
    
    /**
     * @param numberOfReads
     * @param numberOfWrites
     */
    public CollectorDto(Map<String, Long> numberOfReads, long numberOfWrites) {
        this.numberOfReads = numberOfReads;
        this.numberOfWrites = numberOfWrites;
    }

    /**
     * @return the numberOfReads
     */
    public Map<String, Long> getNumberOfReads() {
        return numberOfReads;
    }

    /**
     * @return the numberOfWrites
     */
    public long getNumberOfWrites() {
        return numberOfWrites;
    }
    
    

}
