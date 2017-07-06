package de.idealo.mongodb.slowops.command;

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

	TableDto runCommand(ProfiledServerDto profiledServerDto, MongoDbAccessor mongoDbAccessor);

    CommandResultDto getCommandResultDto();

}
