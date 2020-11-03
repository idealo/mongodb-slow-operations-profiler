/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.servlet;

import com.google.common.collect.Lists;
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
import java.util.regex.Pattern;


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
	    final StringBuffer pipeline = new StringBuffer("{$match:{");
	    final List params = Lists.newArrayList();
	    final Pattern matchAlphaNumericWord=Pattern.compile("\\w");

        if(isEmpty(request, "lbl")) {
            pipeline.append("lbl:#,");
            params.add(matchAlphaNumericWord);
        }else{
            //to use the index {lbl:1, db:1, ts:-1}, regexp has to be /^lbl/
            //see: https://docs.mongodb.com/manual/reference/operator/query/regex/#index-use
            pipeline.append("lbl:{$in:#},");
            params.add(getBeginsWithPatternArray(replaceUnwantedChars(request.getParameter("lbl"))));
        }
        if(isEmpty(request, "db")) {
            pipeline.append("db:#,");
            params.add(matchAlphaNumericWord);
        }else{
            //to use the index {lbl:1, db:1, ts:-1}, regexp has to be /^db/
            //see: https://docs.mongodb.com/manual/reference/operator/query/regex/#index-use
            pipeline.append("db:{$in:#},");
            params.add(getBeginsWithPatternArray(replaceUnwantedChars(request.getParameter("db"))));
        }
        if(!isEmpty(request, "adr")) {
            pipeline.append("adr:{$in:#},");
            params.add(getPatternArray(request.getParameter("adr")));
        }
        if(!isEmpty(request, "rs")) {
            pipeline.append("rs:{$in:#},");
            params.add(getPatternArray(request.getParameter("rs")));
        }
        if(!isEmpty(request, "col")) {
            pipeline.append("col:{$in:#},");
            params.add(getPatternArray(replaceUnwantedChars(request.getParameter("col"))));
        }
        if(!isEmpty(request, "user")) {
            pipeline.append("user:{$in:#},");
            params.add(getPatternArray(request.getParameter("user")));
        }
        if(!isEmpty(request, "op")) {
            pipeline.append("op:{$in:#},");
            params.add(getPatternArray(request.getParameter("op")));
        }
        if(!isEmpty(request, "fields")) {
            pipeline.append("fields:{$all:#},");
            params.add(getPatternArray(request.getParameter("fields")));
        }
        if(!isEmpty(request, "sort")) {
            pipeline.append("sort:{$all:#},");
            params.add(getPatternArray(request.getParameter("sort")));
        }
        if(!isEmpty(request, "proj")) {
            pipeline.append("proj:{$all:#},");
            params.add(getPatternArray(request.getParameter("proj")));
        }
        if(!isEmpty(request, "fromMs") || !isEmpty(request, "toMs")) {
            pipeline.append("millis:{");
            if(!isEmpty(request, "fromMs")){
                pipeline.append("$gte:#,");
                params.add(getAbsLong(request.getParameter("fromMs")));
            }
            if(!isEmpty(request, "toMs")){
                pipeline.append("$lt:#,");
                params.add(getAbsLong(request.getParameter("toMs")));
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
            fromDate = new Date(toDate.getTime() - (1000*60*60));//-1h
        }else if(fromDate == null) {
            toDate = new Date();
            fromDate = new Date(toDate.getTime() - (1000*60*60));//-1h
        }if(toDate == null) {
            toDate = new Date(fromDate.getTime() + (1000*60*60));//+1h
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
        result.setParameters(params.toArray(new Object[params.size()]));
        
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
     * @param param
     * @return
     */
    private Long getAbsLong(String param) {

        try {
            return Math.abs(Long.parseLong(param));
        } catch (NumberFormatException e) {
            LOG.error("can't parse String as Long: {}", param, e);
        }
        return 0L;
    }

    private List<Pattern> getBeginsWithPatternArray(String parameter) {
        return getPatternArray("^" , parameter, "");
    }

    private List<Pattern> getPatternArray(String parameter) {
        return getPatternArray("" , parameter , "");
    }

    /**
     * Transforms a String containing tokens, separated by semicolon,
     * into a List of Pattern
     *
     * @param prefix, parameter, suffix
     * @return
     */
    private List<Pattern> getPatternArray(String prefix, String parameter, String suffix) {
        final List<Pattern> result = Lists.newArrayList();
        final String[] params = parameter.replaceAll("'", "").split(";");
        for (int i = 0; i < params.length; i++) {
            result.add(Pattern.compile(prefix + params[i].trim() + suffix));
        }
        return result;
    }

    private String replaceUnwantedChars(String input) {
        return input.replace(',', ';').replace('{',' ').replace('}', ' ');
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
        if(!isEmpty(request, "byProj")) {
            result.append("proj:'$proj',");
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
        }else if("second".equals(resolution)) {
            result.append("year:{$year:'$ts'},");
            result.append("month:{$month:'$ts'},");
            result.append("dayOfMonth:{$dayOfMonth:'$ts'},");
            result.append("hour:{$hour:'$ts'},");
            result.append("minute:{$minute:'$ts'},");
            result.append("second:{$second:'$ts'}");
        }else { //default:
            result.append("year:{$year:'$ts'},");
            result.append("month:{$month:'$ts'},");
            result.append("dayOfMonth:{$dayOfMonth:'$ts'},");
            result.append("hour:{$hour:'$ts'}");
        }
        
        
        return result;
    }

}
