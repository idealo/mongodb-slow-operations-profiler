/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.servlet;

import com.google.common.collect.Lists;
import de.idealo.mongodb.slowops.collector.ExampleSlowOpsCache;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;



@WebServlet("/slowop")
public class SlowOpExample extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(SlowOpExample.class);

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<Document> result = Lists.newLinkedList();
	    if(!isEmpty(request, "fp")) {
            String fp=request.getParameter("fp");

			if(!isEmpty(request, "del")) {
				boolean deleted = ExampleSlowOpsCache.INSTANCE.remove(fp);
				request.setAttribute("deleted", deleted);
			}

            result = ExampleSlowOpsCache.INSTANCE.getSlowOp(fp);
        }

	    request.setAttribute("slowop", result);
        
        RequestDispatcher view = request.getRequestDispatcher("/slowop.jsp");
        view.forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    doGet(request, response);
	}

	
	boolean isEmpty(HttpServletRequest request, String param) {
	    return request.getParameter(param) == null || request.getParameter(param).trim().length() == 0; 
	}
	

}
