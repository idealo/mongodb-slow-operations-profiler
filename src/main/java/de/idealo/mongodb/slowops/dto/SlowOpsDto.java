/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.dto;

import de.idealo.mongodb.slowops.grapher.AggregatedProfiling;
import de.idealo.mongodb.slowops.util.ConfigReader;
import de.idealo.mongodb.slowops.util.Util;

import java.util.HashMap;


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
    private StringBuffer labels;
    private StringBuffer dataGrid;
    private String errorMessage;
    private HashMap<String, AggregatedProfiling> labelSeries;
    private final String scale;


    public SlowOpsDto() {
        final String scaleStr = ConfigReader.getString(ConfigReader.CONFIG, Util.Y_AXIS_SCALE,  Util.Y_AXIS_MILLISECONDS);
        if(scaleStr.equals(Util.Y_AXIS_MILLISECONDS)){
            scale = Util.Y_AXIS_MILLISECONDS;
        }else {
            scale = Util.Y_AXIS_SECONDS;//default
        }
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

    public StringBuffer getLabels() {return labels;}

    public void setLabels(StringBuffer labels) {this.labels = labels;}

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
     * @param labelSeries
     */
    public void setLabelSeries(HashMap<String, AggregatedProfiling> labelSeries) {
        this.labelSeries = labelSeries;
    }
    
    public HashMap<String, AggregatedProfiling> getLabelSeries() {
        return labelSeries;
    }
    /**
     * @return the scale
     */
    public String getScale() {
        return scale;
    }
}
