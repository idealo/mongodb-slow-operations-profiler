/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.dto;

import com.mongodb.ServerAddress;
import org.codehaus.jackson.annotate.JsonProperty;

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
    private long systemProfileMaxSizeInBytes;
    private boolean isCollecting;
    private boolean isProfiling;
    private final Date lastTs;
    private final ArrayList<Long> doneJobsHistory;
    private final String lastTsFormatted;
    private final String replSetStatus;
    private final String cpuArch;
    private final Number numCores;
    private final Number cpuFreqMHz;
    private final Number memSizeMB;
    private final String mongodbVersion;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public CollectorStatusDto(@JsonProperty("instanceId") int instanceId,
                              @JsonProperty("label") String label,
                              @JsonProperty("replSetName") String replSetName,
                              @JsonProperty("serverAddress") ServerAddress serverAddress,
                              @JsonProperty("database") String database,
                              @JsonProperty("collections") List<String> collections,
                              @JsonProperty("isCollecting") boolean isCollecting,
                              @JsonProperty("isProfiling") boolean isProfiling,
                              @JsonProperty("slowMs") long slowMs,
                              @JsonProperty("systemProfileMaxSizeInBytes") long systemProfileMaxSizeInBytes,
                              @JsonProperty("replSetStatus") String replSetStatus,
                              @JsonProperty("lastTs") Date lastTs,
                              @JsonProperty("doneJobsHistory") ArrayList<Long> doneJobsHistory,
                              @JsonProperty("cpuArch") String cpuArch,
                              @JsonProperty("numCores") Number numCores,
                              @JsonProperty("cpuFreqMHz") Number cpuFreqMHz,
                              @JsonProperty("memSizeMB") Number memSizeMB,
                              @JsonProperty("mongodbVersion") String mongodbVersion) {
        this.instanceId = instanceId;
        this.label = label;
        this.replSetName = replSetName;
        this.serverAddress = serverAddress;
        this.database = database;
        this.collections = collections;
        this.slowMs = slowMs;
        this.systemProfileMaxSizeInBytes = systemProfileMaxSizeInBytes;
        this.replSetStatus = replSetStatus;
        this.isCollecting = isCollecting;
        this.isProfiling = isProfiling;
        this.lastTs = lastTs;
        this.doneJobsHistory = doneJobsHistory;
        this.lastTsFormatted = lastTs!=null&&lastTs.getTime()>0?dateFormat.format(lastTs):"";
        this.cpuArch = cpuArch;
        this.numCores = numCores;
        this.cpuFreqMHz = cpuFreqMHz;
        this.memSizeMB = memSizeMB;
        this.mongodbVersion = mongodbVersion;
    }

    //the following getter methods are needed for applicationStatus.jsp dataTable columns

    public int getInstanceId() {
        return instanceId;
    }

    public String getLabel() {
        return label;
    }

    public String getReplSetName() {
        return replSetName;
    }

    public String getReplSetStatus() {
        return replSetStatus;
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

    public long getSystemProfileMaxSizeInBytes() { return systemProfileMaxSizeInBytes; }

    public boolean isProfiling() { return isProfiling; }

    public boolean isCollecting() { return isCollecting;  }

    public String getLastTsFormatted() { return lastTsFormatted;}

    public ArrayList<Long> getDoneJobsHistory() {
        return doneJobsHistory;
    }

    public String getCpuArch() {return cpuArch;}

    public Number getNumCores() {return numCores;}

    public Number getCpuFreqMHz() {return cpuFreqMHz;}

    public Number getMemSizeMB() {return memSizeMB;}

    public String getMongodbVersion() {return mongodbVersion;}
}
