/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.servlet;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mongodb.ServerAddress;
import de.idealo.mongodb.slowops.collector.CollectorManagerInstance;
import de.idealo.mongodb.slowops.collector.ProfilingReader;
import de.idealo.mongodb.slowops.command.*;
import de.idealo.mongodb.slowops.dto.CommandResultDto;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import de.idealo.mongodb.slowops.dto.TableDto;
import de.idealo.mongodb.slowops.util.JsonParser;
import de.idealo.mongodb.slowops.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@WebServlet(name = "cmd", urlPatterns = {"/cmd", "/cmdj"})
public class CommandResult extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(CommandResult.class);

	/**
     * @see HttpServlet#HttpServlet()
     */
    public CommandResult() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		LOG.debug(">>> doGet");
        final String cmd = request.getParameter("cmd");
        final String mode = request.getParameter("mode");
        final String s_pIds = request.getParameter("pIds");
        CommandResultDto result = getCommandResultDto(cmd, mode, s_pIds);

        String uri = request.getRequestURI();

        if (uri.endsWith("/cmd")) {
            renderHtml(result, request, response);
        } else if (uri.endsWith("/cmdj")) {
            renderJson(result, request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    doGet(request, response);
	}

    private void renderHtml(CommandResultDto result, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        request.setAttribute("commandResult", result);
        RequestDispatcher view = request.getRequestDispatcher("/commandResult.jsp");
        LOG.debug("doGet");
        view.forward(request, response);
        LOG.debug("<<< doGet");
    }

    private void renderJson(CommandResultDto result, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
        String jsonData = jsonParser(result);

        response.setContentType("application/json");
        response.getWriter().write(jsonData);
    }

    private String jsonParser(CommandResultDto result){
        LOG.debug("jsonParser START");
        List<String> headers = result.getTableHeader();
        List<List<Object>> data = result.getTableBody().getTableRows();

        String parsedData = JsonParser.generateJSONString(headers, data);
        LOG.debug("jsonParser END");
        return parsedData;
    }

    private CommandResultDto getCommandResultDto(String cmd, String mode, String s_pIds){
        CommandResultDto result = null;
        if (cmd != null) {
            LOG.info("cmd: {}", cmd);
            if (s_pIds != null) {
                LOG.info("pIds: {}", s_pIds);
                try {
                    final String[] pIds = s_pIds.split(";");//convert string to string array
                    final int[] pIdArray = Arrays.asList(pIds).stream().mapToInt(Integer::parseInt).toArray();//convert string array to int arry
                    final Set<Integer> pIdsSet = Arrays.stream(pIdArray).boxed().collect(Collectors.toSet());//convert int array to Set
                    final List<ProfilingReader> readerList = CollectorManagerInstance.getProfilingReaders(pIdsSet);


                    ICommand command = null;
                    if("cops".equals(cmd)){
                        command = new CmdCurrentOpAll();
                    }else if("copsns".equals(cmd)){
                        command = new CmdCurrentOpNs();
                    }else if("dbstat".equals(cmd)){
                        command = new CmdDatabaseStats();
                    }else if("collstat".equals(cmd)){
                        command = new CmdCollectionStats();
                    }else if("idxacc".equals(cmd)){
                        command = new CmdIdxAccessStats();
                    }else if("hostinfo".equals(cmd)){
                        command = new CmdHostInfo();
                    }

                    result = executeCommand(command, readerList, mode);
                } catch (Exception e) {
                    LOG.error("Exception while building command result", e);
                }
            }

        }

        result = result != null ? result : new CommandResultDto();
        return result;
    }


    private CommandResultDto executeCommand(ICommand command, List<ProfilingReader> readerList, String mode ){
        final CommandResultDto result = command.getCommandResultDto();
        final int poolSize = 1 + Math.min(readerList.size(), Util.MAX_THREADS);
        LOG.info("TableDto poolSize:{} ", poolSize );
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("TableDto-%d")
                .setDaemon(true)
                .build();
        final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize, threadFactory);
        final List<Future<TableDto>> futureTableList = new ArrayList<>();
        final HashSet<ServerAddress> serverAdresses = new HashSet<>();
        final HashMap<ProfiledServerDto, HashSet<String>> dbsEntryPoints = new HashMap<>();
        final boolean isMongod = "mongod".equals(mode);

        for (ProfilingReader reader : readerList) {

            if(command.isHostCommand()) {
                //if it's a host-command, execute command only *once* for any given server address or dbs (depending on mode)
                // because nodes selected by user may have same address but just different registered databases

                if (isMongod) {
                    if(serverAdresses.add(reader.getServerAddress())){
                        submitCommand(threadPool, futureTableList, reader, command, reader.getServerAddress());
                    };
                } else {
                    if(dbsEntryPoints.put(reader.getProfiledServerDto(), new HashSet<>()) == null){
                        submitCommand(threadPool, futureTableList, reader, command, reader.getProfiledServerDto().getHosts());
                    };
                }
            }else{
                //if it's a database specific command, execute for every selected DB when in mongod-mode
                if (isMongod) {
                    submitCommand(threadPool, futureTableList, reader, command, reader.getServerAddress());
                } else {
                    //in dbs-mode execute for all selected DB's only once per DBS
                    //because nodes selected by user may have the same registered databases
                    //but different addresses (i.e. Primary, Secondary or shard1, shard2) of the *same* DBS
                    HashSet<String> dbNames = dbsEntryPoints.get(reader.getProfiledServerDto());
                    if(dbNames == null) dbNames = new HashSet<>();
                    if(!dbNames.contains(reader.getDatabase())){
                        //command was not yet executed against this db of this dbs, so add this dbName to this dbs and execute command once
                        dbNames.add(reader.getDatabase());
                        dbsEntryPoints.put(reader.getProfiledServerDto(), dbNames);
                        submitCommand(threadPool, futureTableList, reader, command, reader.getProfiledServerDto().getHosts());
                    }
                }
            }

        }
        threadPool.shutdown();


        for(Future<TableDto> futureTable : futureTableList){
            try{
                final TableDto table = futureTable.get();
                result.addTableBody(table);
            }
            catch (InterruptedException | ExecutionException e){
                LOG.warn("Exception while getting future command", e);
            }
        }
        threadPool.shutdownNow();


        return result;
    }

    private void submitCommand(ThreadPoolExecutor threadPool, List<Future<TableDto>> futureTableList, ProfilingReader reader, ICommand command, ServerAddress ... serverAddresses){
        final CommandExecutor commandExecutor = new CommandExecutor(reader, command, serverAddresses);
        final Future<TableDto> futureTable = threadPool.submit(commandExecutor);
        futureTableList.add(futureTable);
    }


}
