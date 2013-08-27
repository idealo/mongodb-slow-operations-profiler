/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.servlet;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import de.idealo.mongodb.slowops.collector.*;
import de.idealo.mongodb.slowops.dto.CollectorDto;

/**
 * Servlet implementation class Collector
 */
@WebServlet("/status")
public class Collector extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	/**
     * @see HttpServlet#HttpServlet()
     */
    public Collector() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		final CollectorDto profilerDto = new CollectorDto(CollectorManagerInstance.getNumberOfReads(), CollectorManagerInstance.getNumberOfWrites()); 
	    
		request.setAttribute("collectorDto", profilerDto);
		
		
        RequestDispatcher view = request.getRequestDispatcher("/collectorStatus.jsp");
        view.forward(request, response);
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    doGet(request, response);
	}

}
