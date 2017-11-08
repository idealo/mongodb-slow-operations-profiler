package de.idealo.mongodb.slowops.dto;

import java.util.Date;
import java.util.List;

/**
 * Created by kay.agahd on 31.10.16.
 */
public class ApplicationStatusDto {

    private List<CollectorStatusDto> collectorStatuses;
    private CollectorServerDto collectorServerDto;
    private Date collectorRunningSince;
    private long numberOfReads;
    private long numberOfWrites;
    private long numberOfReadsOfRemovedReaders;
    private long numberOfWritesOfRemovedWriters;
    private String config;
    private Date lastRefresh=null;



    public List<CollectorStatusDto> getCollectorStatuses() {
        return collectorStatuses;
    }

    public void setCollectorStatuses(List<CollectorStatusDto> collectorStatuses) {
        this.collectorStatuses = collectorStatuses;
    }

    public CollectorServerDto getCollectorServerDto() {
        return collectorServerDto;
    }

    public void setCollectorServerDto(CollectorServerDto collectorServerDto) {
        this.collectorServerDto = collectorServerDto;
    }

    public Date getCollectorRunningSince() {
        return collectorRunningSince;
    }

    public void setCollectorRunningSince(Date collectorRunningSince) { this.collectorRunningSince = collectorRunningSince;  }

    public long getNumberOfReads() {
        return numberOfReads;
    }

    public void setNumberOfReads(long numberOfReads) {
        this.numberOfReads = numberOfReads;
    }

    public long getNumberOfWrites() {
        return numberOfWrites;
    }

    public void setNumberOfWrites(long numberOfWrites) {
        this.numberOfWrites = numberOfWrites;
    }

    public long getNumberOfReadsOfRemovedReaders() {
        return numberOfReadsOfRemovedReaders;
    }

    public void setNumberOfReadsOfRemovedReaders(long numberOfReadsOfRemovedReaders) {
        this.numberOfReadsOfRemovedReaders = numberOfReadsOfRemovedReaders;
    }
    public long getNumberOfWritesOfRemovedWriters() {
        return numberOfWritesOfRemovedWriters;
    }

    public void setNumberOfWritesOfRemovedWriters(long numberOfWritesOfRemovedWriters) {
        this.numberOfWritesOfRemovedWriters = numberOfWritesOfRemovedWriters;
    }


    public String getConfig() { return config; }

    public void setConfig(String config) { this.config = config; }


    public Date getLastRefresh() {return lastRefresh; }

    public void setLastRefresh(Date lastRefresh) {this.lastRefresh = lastRefresh; }

    public CollectorStatusDto getCollectorStatus(int id) {
        for(CollectorStatusDto collectorStatus : collectorStatuses){
            if(collectorStatus.getInstanceId() == id) return collectorStatus;
        }
        return null;
    }

}
