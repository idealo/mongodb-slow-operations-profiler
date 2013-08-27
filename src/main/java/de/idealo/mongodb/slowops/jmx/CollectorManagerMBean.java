/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.jmx;

import java.util.*;

/**
 * 
 * 
 * @author kay.agahd
 * @since 22.03.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public interface CollectorManagerMBean {
    
    Map<String, Long> getNumberOfReads();
    
    long getNumberOfWrites();

}
