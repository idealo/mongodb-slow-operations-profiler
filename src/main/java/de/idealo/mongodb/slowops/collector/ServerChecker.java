/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.collector;

/**
 * 
 * 
 * @author kay.agahd
 * @since 28.03.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public interface ServerChecker {
    
    
    void checkForNewOrRemovedMongods(ProfilingReader r);
    
}
