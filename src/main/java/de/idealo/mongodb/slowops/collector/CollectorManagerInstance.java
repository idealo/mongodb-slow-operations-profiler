/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.collector;

import java.util.Map;

/**
 * 
 * 
 * @author kay.agahd
 * @since 02.04.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public final class CollectorManagerInstance {

    private static final CollectorManager INSTANCE = new CollectorManager();
    
    private CollectorManagerInstance(){};//no Instances of this!

    public static void init(){
        INSTANCE.startup();
    }

    public static void terminate(){
        INSTANCE.terminate();
    }

    public static long getNumberOfWrites(){
        return  INSTANCE.getNumberOfWrites();
    }

    public static Map<String, Long> getNumberOfReads(){
        return  INSTANCE.getNumberOfReads();
    }

    
}
