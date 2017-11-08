/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.dto;

import com.mongodb.ServerAddress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 
 * 
 * @author kay.agahd
 * @since 31.10.2016
 * @version $Id: $
 * @copyright idealo internet GmbH
 */
public class CollectorStatusDto {

    private final int instanceId;
    private final String label;
    private final String replSetName;
    private final ServerAddress serverAddress;
    private final String database;
    private final List<String> collections;
    private long slowMs;
    private boolean isCollecting;
    private boolean isProfiling;
    private final Date lastTs;
    private final ArrayList<Long> doneJobsHistory;
    private final String lastTsFormatted;
    private final String replSetStatus;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public CollectorStatusDto(int instanceId, String label, String replSetName, ServerAddress serverAddress, String database, List<String> collections, boolean isCollecting, boolean isProfiling, long slowMs, String replSetStatus, Date lastTs, ArrayList<Long> doneJobsHistory) {
        this.instanceId = instanceId;
        this.label = label;
        this.replSetName = replSetName;
        this.serverAddress = serverAddress;
        this.database = database;
        this.collections = collections;
        this.slowMs = slowMs;
        this.replSetStatus = replSetStatus;
        this.isCollecting = isCollecting;
        this.isProfiling = isProfiling;
        this.lastTs = lastTs;
        this.doneJobsHistory = doneJobsHistory;
        this.lastTsFormatted = lastTs!=null&&lastTs.getTime()>0?dateFormat.format(lastTs):"";
    }

    public int getInstanceId() {
        return instanceId;
    }

    public String getLabel() {
        return label;
    }

    public String getReplSetName() {
        return replSetName;
    }

    public ServerAddress getServerAddress() {
        return serverAddress;
    }

    public String getServerAddressAsString() {
        return serverAddress.getHost() + ":" + serverAddress.getPort();
    }

    public String getDatabase() {
        return database;
    }

    public List<String> getCollections() {
        return collections;
    }

    public String getCollectionsAsString() {
        StringBuffer result = new StringBuffer();
        for(String c : collections)
        {
            result.append(c).append(",");
        }
        if(result.length() > 0) result.deleteCharAt(result.length()-1);

        return result.toString();
    }

    public long getSlowMs() { return slowMs; }

    public void setSlowMs(long l) { slowMs = l; }

    public String getReplSetStatus() {
        return replSetStatus;
    }

    public boolean isCollecting() { return isCollecting;  }

    public void setCollecting(boolean b) { isCollecting = b;  }

    public boolean isProfiling() { return isProfiling; }

    public void setProfiling(boolean b) {  isProfiling = b; }

    public Date getLastTs() {
        return lastTs;
    }

    public String getLastTsFormatted() { return lastTsFormatted;}

    public ArrayList<Long> getDoneJobsHistory() {
        return doneJobsHistory;
    }
}
