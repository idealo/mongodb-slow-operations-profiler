/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.jmx;

import java.util.Date;

/**
 * 
 * 
 * @author kay.agahd
 * @since 22.03.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public interface CollectorManagerMBean {
    
    long getNumberOfReads();
    
    long getNumberOfWrites();

    long getNumberOfReadsOfRemovedReaders();

    long getNumberOfWritesOfRemovedWriters();

    Date getRunningSince();

}
