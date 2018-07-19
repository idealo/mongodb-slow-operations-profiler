package de.idealo.mongodb.slowops.util;


import com.mongodb.ServerAddress;
import de.idealo.mongodb.slowops.collector.CollectorManager;
import de.idealo.mongodb.slowops.collector.ProfilingEntry;
import de.idealo.mongodb.slowops.collector.ProfilingReader;
import de.idealo.mongodb.slowops.collector.ProfilingWriter;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;


public class ProfilingReaderCreator implements Callable<ProfilingReader> {

    private static final Logger LOG = LoggerFactory.getLogger(ProfilingReaderCreator.class);

    private final int id;
    private final ServerAddress address;
    private final ProfiledServerDto dto;
    private final String db;
    private final List<String> colls;
    private final ProfilingWriter writer;
    private final BlockingQueue<ProfilingEntry> jobQueue;
    private final CollectorManager collectorManager;

    public ProfilingReaderCreator(int id, ServerAddress address, ProfiledServerDto dto, String db, List<String> colls, CollectorManager collectorManager) {
        this.id = id;
        this.address = address;
        this.dto = dto;
        this.db = db;
        this.collectorManager = collectorManager;
        this.colls = colls;
        this.writer = collectorManager.getWriter();
        this.jobQueue = collectorManager.getJobQueue();

    }

    @Override
    public ProfilingReader call() throws Exception {
        final Date lastTs = writer.getNewest(address, db);
        final ProfilingReader result = new ProfilingReader(id, jobQueue, address, lastTs, dto, db, colls, !dto.isEnabled(), 0, dto.getSlowMs());

        collectorManager.addAndStartReader(result);

        return result;
    }
}
