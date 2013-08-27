/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.dto;

import de.idealo.mongodb.slowops.util.Util;


/**
 * 
 * 
 * @author kay.agahd
 * @since 19.03.2013
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public class SlowOpsDto {

    private boolean[] visibilityValues;
    private StringBuffer dataGrid;
    private String errorMessage;
    
    private final String scale;
    
    public SlowOpsDto() {
        scale = Util.getProperty(Util.Y_AXIS_SCALE, Util.Y_AXIS_SECONDS);
    }
    
    
    /**
     * @return the visibilityValues
     */
    public boolean[] getVisibilityValues() {
        return visibilityValues;
    }
    /**
     * @param visibilityValues the visibilityValues to set
     */
    public void setVisibilityValues(boolean[] visibilityValues) {
        this.visibilityValues = visibilityValues;
    }
    /**
     * @return the dataGrid
     */
    public StringBuffer getDataGrid() {
        return dataGrid;
    }
    /**
     * @param dataGrid the dataGrid to set
     */
    public void setDataGrid(StringBuffer dataGrid) {
        this.dataGrid = dataGrid;
    }
    /**
     * @param errorMessage
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * @return the scale
     */
    public String getScale() {
        return scale;
    }
}
