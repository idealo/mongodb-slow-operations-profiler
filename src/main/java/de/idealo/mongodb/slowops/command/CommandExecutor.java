package de.idealo.mongodb.slowops.command;



import com.mongodb.ServerAddress;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import de.idealo.mongodb.slowops.dto.TableDto;
import de.idealo.mongodb.slowops.monitor.MongoDbAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;


public class CommandExecutor implements Callable<TableDto> {

    private static final Logger LOG = LoggerFactory.getLogger(CommandExecutor.class);

    private final ProfiledServerDto profiledServerDto;
    private final ICommand command;
    private final ServerAddress serverAdress;

    public CommandExecutor(ProfiledServerDto profiledServerDto, ICommand command, ServerAddress serverAdress) {
        this.profiledServerDto = profiledServerDto;
        this.command = command;
        this.serverAdress = serverAdress;
    }


    @Override
    public TableDto call() throws Exception {
    	MongoDbAccessor mongoDbAccessor = null;
        if(serverAdress!=null){
            mongoDbAccessor = new MongoDbAccessor(profiledServerDto.getAdminUser(), profiledServerDto.getAdminPw(), profiledServerDto.getSsl(), serverAdress);
        }else{
            mongoDbAccessor = new MongoDbAccessor(profiledServerDto.getAdminUser(), profiledServerDto.getAdminPw(), profiledServerDto.getSsl(), profiledServerDto.getHosts());
        }

        return command.runCommand(profiledServerDto, mongoDbAccessor);

    }
}
