/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.dto;


/**
 * 
 * 
 * @author kay.agahd
 * @since 20.03.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public class SlowOpsFilterDto {

    private StringBuffer pipeline;
    private Object[] parameters;
    
    /**
     * @param pipeline
     */
    public void setPipeline(StringBuffer pipeline) {
        this.pipeline = pipeline;
        
    }

    /**
     * @param array
     */
    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    /**
     * @return
     */
    public StringBuffer getPipeline() {
        return pipeline;
    }

    /**
     * @return
     */
    public Object[] getParameters() {
        return parameters;
    }


}
