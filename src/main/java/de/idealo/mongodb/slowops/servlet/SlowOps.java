/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.servlet;

import java.io.IOException;
import java.text.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import org.slf4j.*;

import de.idealo.mongodb.slowops.dto.*;
import de.idealo.mongodb.slowops.grapher.Grapher;


/**
 * Servlet implementation class SlowOps
 */
@WebServlet("/gui")
public class SlowOps extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static final Logger LOG = LoggerFactory.getLogger(SlowOps.class);

	private static final String DATE_PATTERN = "yyyy/MM/dd";

	private final Grapher grapher;
	
    /**
     * Default constructor. 
     */
    public SlowOps() {
        grapher = new Grapher();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    
	    final SlowOpsFilterDto filter = getFilter(request);
	    
	    final SlowOpsDto slowQueriesDto = grapher.aggregateSlowQueries(filter.getPipeline(), filter.getParameters(), getGroupExp(request), getGroupTime(request));
        request.setAttribute("slowOpsDto", slowQueriesDto);
        
        RequestDispatcher view = request.getRequestDispatcher("/gui.jsp");
        view.forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    doGet(request, response);
	}
	
	private SlowOpsFilterDto getFilter(HttpServletRequest request) {
	    final SlowOpsFilterDto result = new SlowOpsFilterDto();
	    
	    final String PREFIX = "{$match:{";
	    final StringBuffer pipeline = new StringBuffer(PREFIX);
	    final List<Date> params = new LinkedList<Date>();
        
        
        if(!isEmpty(request, "adr")) {
            pipeline.append("adr:{$in:[").append(getStringArray(request.getParameter("adr"))).append("]},");
        }
        if(!isEmpty(request, "user")) {
            pipeline.append("user:{$in:[").append(getStringArray(request.getParameter("user"))).append("]},");
        }
        if(!isEmpty(request, "op")) {
            pipeline.append("op:{$in:[").append(getStringArray(request.getParameter("op"))).append("]},");
        }
        if(!isEmpty(request, "fields")) {
            pipeline.append("fields:{$all:[").append(getStringArray(request.getParameter("fields"))).append("]},");
        }
        if(!isEmpty(request, "sort")) {
            pipeline.append("sort:{$all:[").append(getStringArray(request.getParameter("sort"))).append("]},");
        }

        if (!isEmpty(request, "exclude")) {
            pipeline.append("millis:{$lt: 1270000000 },");
        }

        if(!isEmpty(request, "fromDate") && !isEmpty(request, "toDate")) {
            final Date fromDate = getDate(request.getParameter("fromDate"));
            final Date toDate = getDate(request.getParameter("toDate"));
            if(fromDate != null && toDate != null) {
                pipeline.append("ts:{$gt: #, $lt: # },");
                params.add(fromDate);
                params.add(toDate);
            }
        }else if(!isEmpty(request,"fromDate")) {
            final Date fromDate = getDate(request.getParameter("fromDate"));
            if(fromDate != null) {
                pipeline.append("ts:{$gt: # },");
                params.add(fromDate);
            }
        }else if(!isEmpty(request,"toDate")) {
            final Date toDate = getDate(request.getParameter("toDate"));
            if(toDate != null) {
                pipeline.append("ts:{$lt: # },");
                params.add(toDate);
            }
        }
        
        if(pipeline.length() == PREFIX.length()) {//default
            pipeline.append("ts:{$gt: #, $lt: # }");
            final Date toDate = new Date(System.currentTimeMillis() + (1000*60*60*24));
            final Date fromDate = new Date(toDate.getTime() - (1000*60*60*24*2));
            params.add(fromDate);
            params.add(toDate);
            addDateToRequest(fromDate, request, "fromDate");
            addDateToRequest(toDate, request, "toDate");
        }else {
            pipeline.deleteCharAt(pipeline.length()-1);//delete last comma
        }
        
        pipeline.append("}}");
        
        System.out.println(pipeline);
        LOG.debug("pipeline: " + pipeline);
        
        result.setPipeline(pipeline);
        result.setParameters(params.toArray(new Date[params.size()]));
        
        return result;
	}
	
	boolean isEmpty(HttpServletRequest request, String param) {
	    return request.getParameter(param) == null || request.getParameter(param).trim().length() == 0; 
	}
	
    private void addDateToRequest(Date date, HttpServletRequest request, String attributeName) {
        final String dateAsString = createDateFormat().format(date);
        request.setAttribute(attributeName, dateAsString);
    }
	
	/**
     * @param parameter
     * @return
     */
    private Date getDate(String param) {
        try {
            return createDateFormat().parse(param);
        } catch (ParseException e) {
            LOG.error("can't parse date: " + param, e);
        }
        return null;
    }

    private SimpleDateFormat createDateFormat() {
        return new SimpleDateFormat(DATE_PATTERN);
    }

    /**
     * @param parameter
     * @return
     */
    private String getStringArray(String parameter) {
        final StringBuffer result = new StringBuffer();
        final String[] params = parameter.replace(';', ',').split(",");
        for (int i = 0; i < params.length; i++) {
            result.append("\"").append(params[i].trim()).append("\",");
        }
        result.deleteCharAt(result.length()-1);
        return result.toString();
    }

    private StringBuffer getGroupExp(HttpServletRequest request) {
	    final StringBuffer result = new StringBuffer();
        
        if(!isEmpty(request, "byAdr")) {
            result.append("adr:'$adr',");
        }
        if(!isEmpty(request, "byOp")) {
            result.append("op:'$op',");
        }
        if(!isEmpty(request, "byUser")) {
            result.append("user:'$user',");
        }
        if(!isEmpty(request, "byFields")) {
            result.append("fields:'$fields',");
        }
        if(!isEmpty(request, "bySort")) {
            result.append("sort:'$sort',");
        }
        //default:
        if(result.length() == 0) {
            result.append("fields:'$fields',");
        }
        return result;
	}
	
	private StringBuffer getGroupTime(HttpServletRequest request) {
        final StringBuffer result = new StringBuffer();
        final String resolution = request.getParameter("resolution");
        
        if("year".equals(resolution)) {
            result.append("year:{$year:'$ts'}");
        }else if("month".equals(resolution)) {
            result.append("year:{$year:'$ts'},");
            result.append("month:{$month:'$ts'}");
        }else if("week".equals(resolution)) {
            result.append("year:{$year:'$ts'},");
            result.append("week:{$week:'$ts'}");
        }else if("day".equals(resolution)) {
            result.append("year:{$year:'$ts'},");
            result.append("month:{$month:'$ts'},");
            result.append("dayOfMonth:{$dayOfMonth:'$ts'}");
        }else if("hour".equals(resolution)) {
            result.append("year:{$year:'$ts'},");
            result.append("month:{$month:'$ts'},");
            result.append("dayOfMonth:{$dayOfMonth:'$ts'},");
            result.append("hour:{$hour:'$ts'}");
        }else if("minute".equals(resolution)) {
            result.append("year:{$year:'$ts'},");
            result.append("month:{$month:'$ts'},");
            result.append("dayOfMonth:{$dayOfMonth:'$ts'},");
            result.append("hour:{$hour:'$ts'},");
            result.append("minute:{$minute:'$ts'}");
        }else { //default:
            result.append("year:{$year:'$ts'},");
            result.append("month:{$month:'$ts'},");
            result.append("dayOfMonth:{$dayOfMonth:'$ts'},");
            result.append("hour:{$hour:'$ts'}");
        }
        
        
        return result;
    }

}
