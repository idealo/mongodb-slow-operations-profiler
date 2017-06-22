/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.collector;

import de.idealo.mongodb.slowops.dto.ApplicationStatusDto;

import java.util.List;

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

    public static ApplicationStatusDto getApplicationStatus(){ return  INSTANCE.getApplicationStatus();   }

    public static ApplicationStatusDto getApplicationStatus(List<Integer> idList){ return  INSTANCE.getApplicationStatus(idList);   }

    public static void startStopProfilingReaders(List<Integer> idList, boolean stop){ INSTANCE.startStopProfilingReaders(idList, stop); };

    public static void setSlowMs(List<Integer> idList, String ms){ INSTANCE.setSlowMs(idList, ms); };

    public static void reloadConfig(String cfg){ INSTANCE.reloadConfig(cfg); }

}
