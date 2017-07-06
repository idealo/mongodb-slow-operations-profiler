/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.servlet;

import de.idealo.mongodb.slowops.collector.CollectorManagerInstance;
import de.idealo.mongodb.slowops.util.ConfigReader;
import de.idealo.mongodb.slowops.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/app")
public class ApplicationStatus extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(ApplicationStatus.class);

	/**
     * @see HttpServlet#HttpServlet()
     */
    public ApplicationStatus() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		LOG.debug(">>> doGet");
        reloadConfig(request);
		RequestDispatcher view = request.getRequestDispatcher("/applicationStatus.jsp");
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

    private void reloadConfig(HttpServletRequest request){
        if(isAuthenticated(request)) {
            final String cfg = request.getParameter("config");
            if (cfg != null && cfg.trim().length() > 0) {
                LOG.info("reload application using a new configuration");
                CollectorManagerInstance.reloadConfig(cfg);
            }
        }
    }

    protected static boolean isAuthenticated(HttpServletRequest req) {
        final HttpSession session = req.getSession(true);

        final Object isAdminToken = session.getAttribute(Util.ADMIN_TOKEN);
        if (isAdminToken != null && isAdminToken instanceof Boolean && (Boolean) isAdminToken) {
            LOG.info("authenticated by session");
            return true;
        }else{
            final String adminToken = req.getParameter(Util.ADMIN_TOKEN);
            if (adminToken != null) {
                if (ConfigReader.getString(ConfigReader.CONFIG, Util.ADMIN_TOKEN, "").equals(adminToken)) {
                    session.setAttribute(Util.ADMIN_TOKEN, true);
                    LOG.info("authenticated by url param succeeded, loggd in");
                    return true;
                }
            }
        }
        LOG.info("not authenticated");
        return false;
    }

}
