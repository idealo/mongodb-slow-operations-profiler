package de.idealo.mongodb.slowops.command;



import com.mongodb.ServerAddress;
import de.idealo.mongodb.slowops.collector.ProfilingReader;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import de.idealo.mongodb.slowops.dto.TableDto;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;


public class CommandExecutor implements Callable<TableDto> {

    private static final Logger LOG = LoggerFactory.getLogger(CommandExecutor.class);

    private final ProfilingReader profilingReader;
    private final ICommand command;
    private final ServerAddress[] serverAddresses;

    public CommandExecutor(ProfilingReader profilingReader, ICommand command, ServerAddress ... serverAddresses) {
        this.profilingReader = profilingReader;
        this.command = command;
        this.serverAddresses = serverAddresses;
    }


    @Override
    public TableDto call() {
        final ProfiledServerDto profiledServerDto = profilingReader.getProfiledServerDto();
        final MongoDbAccessor mongoDbAccessor = new MongoDbAccessor(profiledServerDto.getAdminUser(), profiledServerDto.getAdminPw(), profiledServerDto.getSsl(), serverAddresses);

        return command.runCommand(profilingReader, mongoDbAccessor);
    }
}
