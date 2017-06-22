package de.idealo.mongodb.slowops.collector;

/**
 * Created by kay.agahd on 21.06.17.
 */
public interface Terminable {

    void terminate();
    long getDoneJobs();
}
