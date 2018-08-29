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

@WebServlet("/cmd")
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
        CommandResultDto result = null;
        if (cmd != null) {
            LOG.info("cmd: {}", cmd);
            final String s_pIds = request.getParameter("pIds");
            if (s_pIds != null) {
                LOG.info("pIds: {}", s_pIds);
                try {
                    final String[] pIds = s_pIds.split(";");//convert string to string array
                    final int[] pIdArray = Arrays.asList(pIds).stream().mapToInt(Integer::parseInt).toArray();//convert string array to int arry
                    final Set<Integer> pIdsSet = Arrays.stream(pIdArray).boxed().collect(Collectors.toSet());//convert int array to Set
                    final List<ProfilingReader> readerList = CollectorManagerInstance.getProfilingReaders(pIdsSet);


                    ICommand command = null;
                    if("cops".equals(cmd)){
                        command = new CmdCurrentOp();
                    }else if("lsdbs".equals(cmd)){
                        command = new CmdListDbCollections();
                    }else if("idxacc".equals(cmd)){
                        command = new CmdIdxAccessStats();
                    }

                    result = executeCommand(command, readerList, mode);


                } catch (Exception e) {
                    LOG.error("Exception while building command result", e);
                }
            }

        }

        request.setAttribute("commandResult", result!=null?result:new CommandResultDto());
		RequestDispatcher view = request.getRequestDispatcher("/commandResult.jsp");
		LOG.debug("doGet");
		view.forward(request, response);
		LOG.debug("<<< doGet");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    doGet(request, response);
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
        final HashSet<ServerAddress> serverAdresses = new HashSet<ServerAddress>();
        final HashSet<ProfiledServerDto> dbsEntryPoints = new HashSet<ProfiledServerDto>();
        final boolean isMongod = "mongod".equals(mode);

        for (ProfilingReader reader : readerList) {
            //execute command only *once* for any given server address or dbs (depending on mode)
            // because nodes selected by user may have same address but just different registered databases
            boolean isFirst = false;
            if(isMongod){
                isFirst = serverAdresses.add(reader.getServerAddress());
            }else{
                isFirst = dbsEntryPoints.add(reader.getProfiledServerDto());
            }

            if (isFirst) {
                CommandExecutor commandExecutor = new CommandExecutor(reader.getProfiledServerDto(), command, isMongod?reader.getServerAddress():null);
                Future<TableDto> futureTable = threadPool.submit(commandExecutor);
                futureTableList.add(futureTable);
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


}
