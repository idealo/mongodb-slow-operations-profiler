/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.collector;

import javax.servlet.*;

import org.slf4j.*;

/**
 * 
 * 
 * @author kay.agahd
 * @since 26.03.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public class StartStopHook implements ServletContextListener{

    private static final Logger LOG = LoggerFactory.getLogger(StartStopHook.class);
    
    
    /* (non-Javadoc)
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        LOG.info(">>> contextInitialized");
        CollectorManagerInstance.init();
        LOG.info("<<< contextInitialized");
        
        
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        LOG.info(">>> contextDestroyed");
        CollectorManagerInstance.terminate();
        LOG.info("<<< contextDestroyed");
        
    }

    

}
