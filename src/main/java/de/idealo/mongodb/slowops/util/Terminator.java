package de.idealo.mongodb.slowops.util;


import de.idealo.mongodb.slowops.collector.Terminable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class Terminator implements Callable<Long> {

    private static final Logger LOG = LoggerFactory.getLogger(Terminator.class);

    private final Terminable terminable;

    public Terminator(Terminable terminable) {
        this.terminable = terminable;
    }

    @Override
    public Long call() throws Exception {
        LOG.debug("terminate called");
        terminable.terminate();
        return terminable.getDoneJobs();
    }
}
