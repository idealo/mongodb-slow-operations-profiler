/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.servlet;

import de.idealo.mongodb.slowops.dto.SlowOpsDto;
import de.idealo.mongodb.slowops.dto.SlowOpsFilterDto;
import de.idealo.mongodb.slowops.grapher.Grapher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


/**
 * Servlet implementation class SlowOps
 */
@WebServlet("/gui")
public class SlowOps extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static final Logger LOG = LoggerFactory.getLogger(SlowOps.class);

	private final Grapher grapher;
	private final String DATEFORMAT = ("yyyy/MM/dd HH:mm:ss");
	
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
	    
	    final SlowOpsDto slowOpsDto = grapher.aggregateSlowQueries(filter.getPipeline(), filter.getParameters(), getGroupExp(request), getGroupTime(request));
        request.setAttribute("slowOpsDto", slowOpsDto);
        
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

        if(!isEmpty(request, "lbl")) {
            pipeline.append("lbl:{$in:[").append(getStringArray(request.getParameter("lbl"))).append("]},");
        }
        if(!isEmpty(request, "adr")) {
            pipeline.append("adr:{$in:[").append(getStringArray(request.getParameter("adr"))).append("]},");
        }
        if(!isEmpty(request, "rs")) {
            pipeline.append("rs:{$in:[").append(getStringArray(request.getParameter("rs"))).append("]},");
        }
        if(!isEmpty(request, "db")) {
            pipeline.append("db:{$in:[").append(getStringArray(request.getParameter("db"))).append("]},");
        }
        if(!isEmpty(request, "col")) {
            pipeline.append("col:{$in:[").append(getStringArray(request.getParameter("col"))).append("]},");
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
        if(!isEmpty(request, "fromMs") || !isEmpty(request, "toMs")) {
            pipeline.append("millis:{");
            if(!isEmpty(request, "fromMs")){
                pipeline.append("$gte:").append(request.getParameter("fromMs")).append(",");
            }
            if(!isEmpty(request, "toMs")){
                pipeline.append("$lt:").append(request.getParameter("toMs")).append(",");
            }
            pipeline.deleteCharAt(pipeline.length()-1);//delete last comma
            pipeline.append("},");
        }

        //fromDate and toDate are required so set it to default if not existent
        Date fromDate = null;
        Date toDate = null;
        if(!isEmpty(request,"fromDate")) {
            fromDate = getDate(request.getParameter("fromDate"));
        }
        if(!isEmpty(request,"toDate")) {
            toDate = getDate(request.getParameter("toDate"));
        }
        if(fromDate == null && toDate == null) {
            toDate = new Date();
            fromDate = new Date(toDate.getTime() - (1000*60*60*24));
        }else if(fromDate == null) {
            toDate = new Date();
            fromDate = new Date(toDate.getTime() - (1000*60*60*24));
        }if(toDate == null) {
            toDate = new Date(fromDate.getTime() + (1000*60*60*24));
        }
        pipeline.append("ts:{$gt: #, $lt: # }");
        params.add(fromDate);
        params.add(toDate);

        final SimpleDateFormat df = new SimpleDateFormat(DATEFORMAT);
        request.setAttribute("fromDate", df.format(fromDate));
        request.setAttribute("toDate", df.format(toDate));

        pipeline.append("}}");
        
        System.out.println(pipeline);
        LOG.debug("pipeline: {}", pipeline);
        LOG.debug("fromDate: {}", request.getParameter("fromDate"));
        LOG.debug("toDate: {}", request.getParameter("toDate"));
        
        result.setPipeline(pipeline);
        result.setParameters(params.toArray(new Date[params.size()]));
        
        return result;
	}
	
	
	boolean isEmpty(HttpServletRequest request, String param) {
	    return request.getParameter(param) == null || request.getParameter(param).trim().length() == 0; 
	}
	
	
	/**
     * @param param
     * @return
     */
    private Date getDate(String param) {
        final SimpleDateFormat df = new SimpleDateFormat(DATEFORMAT);
        
        try {
            return df.parse(param);
        } catch (ParseException e) {
            LOG.error("can't parse date: {}", param, e);
        }
        return null;
    }

    /**
     * @param parameter
     * @return
     */
    private String getStringArray(String parameter) {
        final StringBuffer result = new StringBuffer();
        final String[] params = parameter.replaceAll("'", "").split(";");
        for (int i = 0; i < params.length; i++) {
            result.append("\"").append(params[i].trim()).append("\",");
        }
        result.deleteCharAt(result.length()-1);
        return result.toString();
    }

    private StringBuffer getGroupExp(HttpServletRequest request) {
	    final StringBuffer result = new StringBuffer();

        if(!isEmpty(request, "byLbl")) {
            result.append("lbl:'$lbl',");
        }
        if(!isEmpty(request, "byAdr")) {
            result.append("adr:'$adr',");
        }
        if(!isEmpty(request, "byRs")) {
            result.append("rs:'$rs',");
        }
        if(!isEmpty(request, "byDb")) {
            result.append("db:'$db',");
        }
        if(!isEmpty(request, "byCol")) {
            result.append("col:'$col',");
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
