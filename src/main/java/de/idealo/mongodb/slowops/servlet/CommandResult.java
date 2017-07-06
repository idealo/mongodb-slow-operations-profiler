/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.servlet;

import de.idealo.mongodb.slowops.collector.CollectorManagerInstance;
import de.idealo.mongodb.slowops.collector.ProfilingReader;
import de.idealo.mongodb.slowops.command.*;
import de.idealo.mongodb.slowops.dto.CommandResultDto;
import de.idealo.mongodb.slowops.dto.ProfiledServerDto;
import de.idealo.mongodb.slowops.dto.TableDto;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
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
                    final HashSet<ProfiledServerDto> dbsEntryPoints = new HashSet<ProfiledServerDto>();
                    for (ProfilingReader reader : readerList) {
                        dbsEntryPoints.add(reader.getProfiledServerDto());
                    }

                    ICommand command = null;
                    if("cops".equals(cmd)){
                        command = new CmdCurrentOp();
                    }else if("lsdbs".equals(cmd)){
                        command = new CmdListDbCollections();
                    }else if("idxacc".equals(cmd)){
                        command = new CmdIdxAccessStats();
                    }


                    result = executeCommand(command, dbsEntryPoints );


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

    private CommandResultDto executeCommand(ICommand command, HashSet<ProfiledServerDto> dbsEntryPoints ){
        final CommandResultDto result = command.getCommandResultDto();
        final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(dbsEntryPoints.size());
        final List<Future<TableDto>> futureTableList = new ArrayList<>();

        for(ProfiledServerDto dto : dbsEntryPoints){
            CommandExecutor commandExecutor = new CommandExecutor(dto, command);
            Future<TableDto> futureTable = threadPool.submit(commandExecutor);
            futureTableList.add(futureTable);
        }
        
        for(Future<TableDto> futureTable : futureTableList){
            try{
                final TableDto table = futureTable.get();
                result.addTableBody(table);
            }
            catch (InterruptedException | ExecutionException e){
                LOG.warn("Exception while getting future command", e);
            }
        }
        threadPool.shutdown();

        return result;
    }

}
