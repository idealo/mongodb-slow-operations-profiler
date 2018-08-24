package de.idealo.mongodb.slowops.dto;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by kay.agahd on 31.10.16.
 */
public class ApplicationStatusDto {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationStatusDto.class);

    public static final long DEFAULT_MAX_WEBLOG_ENTRIES = 100;

    private static final ConcurrentLinkedDeque<String> WEBLOG = new ConcurrentLinkedDeque<String>();
    private static long MAX_WEBLOG_ENTRIES = DEFAULT_MAX_WEBLOG_ENTRIES;
    private static final String DATEFORMAT = ("yyyy/MM/dd HH:mm:ss,SSS");

    private volatile List<CollectorStatusDto> collectorStatuses = Collections.synchronizedList(Lists.newLinkedList());
    private CollectorServerDto collectorServerDto;
    private Date collectorRunningSince;
    private long numberOfReads;
    private long numberOfWrites;
    private long numberOfReadsOfRemovedReaders;
    private long numberOfWritesOfRemovedWriters;
    private String config;
    private Date lastRefresh=null;


    public synchronized void addCollectorStatus(CollectorStatusDto collectorStatusDto){
        collectorStatuses.add(collectorStatusDto);
    }


    public synchronized List<CollectorStatusDto> getCollectorStatuses(){
        LOG.info("getCollectorStatuses size:" + collectorStatuses.size());
        return collectorStatuses;
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

    public synchronized CollectorStatusDto getCollectorStatus(int id) {
        for(CollectorStatusDto collectorStatus : collectorStatuses){
            if(collectorStatus.getInstanceId() == id) return collectorStatus;
        }
        return null;
    }

    public static void setMaxWebLogEntries(long n){
        MAX_WEBLOG_ENTRIES = n;
        while(WEBLOG.size() > MAX_WEBLOG_ENTRIES){
            WEBLOG.poll();
        }
    }

    public static void addWebLog(String msg){
        while(WEBLOG.size() > MAX_WEBLOG_ENTRIES){
            WEBLOG.poll();
        }
        final SimpleDateFormat df = new SimpleDateFormat(DATEFORMAT);
        final long now = System.currentTimeMillis();
        WEBLOG.offer(df.format(now) + " " + msg);
    }

    public String getWebLog(){
        final StringBuilder sb = new StringBuilder();
        for(String entry : WEBLOG){
            sb.append(entry).append(System.lineSeparator());
        }

        return sb.toString();
    }

}
