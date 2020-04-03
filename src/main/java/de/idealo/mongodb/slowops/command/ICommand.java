package de.idealo.mongodb.slowops.command;

import de.idealo.mongodb.slowops.collector.ProfilingReader;
import de.idealo.mongodb.slowops.dto.CommandResultDto;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import de.idealo.mongodb.slowops.dto.TableDto;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;

import java.time.format.DateTimeFormatter;

/**
 * Created by kay.agahd on 30.06.17.
 */
public interface ICommand {

    DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	TableDto runCommand(ProfilingReader profilingReader, MongoDbAccessor mongoDbAccessor);

    CommandResultDto getCommandResultDto();

    /**
     * Determines whether the command is to be executed once per host or once per database.
     * For example, getting the host info is or getting the current operations are both host commands
     * because it's not related to an individual database whereas
     * listing collections or getting index access stats is related to a specific database.
     * @return true if the command is to be executed once per host, else returns false
     */
    boolean isHostCommand();

}
