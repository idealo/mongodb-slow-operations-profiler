<!DOCTYPE html>
<html>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="java.util.*,javax.servlet.*,de.idealo.mongodb.slowops.dto.SlowOpsDto" %>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<meta http-equiv="X-UA-Compatible" content="IE=EmulateIE7; IE=EmulateIE9">
	<script type="text/javascript" src="js/dygraph-combined.js"></script>
	<script type="text/javascript" src="js/jquery.min.js"></script>
	<script type="text/javascript" src="js/bootstrap.min.js"></script>
	<script type="text/javascript" src="js/bootstrap-datetimepicker.min.js"></script>
	<link href="css/bootstrap-combined.css" rel="stylesheet">
	<link href="css/bootstrap-datetimepicker.min.css" rel="stylesheet" type="text/css" media="screen" >
	<title>slow operations</title>
</head>
<%! 
boolean isEmpty(HttpServletRequest request, String param) {
    return request.getParameter(param) == null || request.getParameter(param).trim().length() == 0; 
}
%>
<%
  final SlowOpsDto slowOpsDto = (SlowOpsDto)request.getAttribute("slowOpsDto");
  final String sortLegend = request.getParameter("sortLegend");
%>
<body>
<form name="input" action="gui" method="get">
	<table  align="top" cellpadding="10">
		<tr>
			<td valign="top"><strong>Filter by</strong>
				<table>
					<tr><td>Earliest date</td><td><div id="datetimepickerFrom" class="date"><input type="text" id="fromDate" name="fromDate" size="20" readonly <% if(!isEmpty(request,"fromDate")){out.print("value=\""+request.getParameter("fromDate")+"\"");}else{out.print("value=\""+request.getAttribute("fromDate")+"\"");}%> ><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span></div></td></tr>
					<tr><td>Latest date</td><td><div id="datetimepickerTo" class="date"><input type="text" id="toDate" name="toDate" size="20" readonly <% if(!isEmpty(request,"toDate")){out.print("value=\""+request.getParameter("toDate")+"\"");}else{out.print("value=\""+request.getAttribute("toDate")+"\"");}%> ><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span></div></td></tr>
					<tr><td>Server address</td><td><input type="text" name="adr" size="20" <% 								if(!isEmpty(request,"adr")){out.print("value=\""+request.getParameter("adr")+"\"");}%> >(or)</td></tr>
					<tr><td>User</td><td><input type="text" name="user" size="20" <% 										if(!isEmpty(request,"user")){out.print("value=\""+request.getParameter("user")+"\"");}%> >(or)</td></tr>
					<tr><td>Operation</td><td><input type="text" name="op" size="20" <% 									if(!isEmpty(request,"op")){out.print("value=\""+request.getParameter("op")+"\"");}%> >(or)</td></tr>
					<tr><td>Queried fields</td><td><input type="text" name="fields" size="20" <% 							if(!isEmpty(request,"fields")){out.print("value=\""+request.getParameter("fields")+"\"");}%> >(and)</td></tr>
					<tr><td>Sorted fields</td><td><input type="text" name="sort" size="20" <% 								if(!isEmpty(request,"sort")){out.print("value=\""+request.getParameter("sort")+"\"");}%> >(and)</td></tr>
					<tr><td>Millis from</td><td><input type="text" name="fromMs" size="6" <% if(!isEmpty(request,"fromMs")){out.print("value=\""+request.getParameter("fromMs")+"\"");}%> > to <input type="text" name="toMs" size="6" <% if(!isEmpty(request,"toMs")){out.print("value=\""+request.getParameter("toMs")+"\"");}%> ></td></tr>
				</table>
			</td>
			<td valign="top"><strong>Group by</strong>
				<table>
					<tr><td><input type="checkbox" name="byAdr" value="adr" <%   	if(!isEmpty(request,"byAdr")){out.print("checked=\"checked\"");}%> >Server address</td></tr>
					<tr><td><input type="checkbox" name="byUser" value="user" <%	if(!isEmpty(request,"byUser")){out.print("checked=\"checked\"");}%> >User</td></tr>
					<tr><td><input type="checkbox" name="byOp" value="op" <%		if(!isEmpty(request,"byOp")){out.print("checked=\"checked\"");}%> >Operation</td></tr>
					<tr><td><input type="checkbox" name="byFields" value="fields" <%if(!isEmpty(request,"byFields")  || (isEmpty(request,"byAdr") && isEmpty(request,"byUser") && isEmpty(request,"byOp") && isEmpty(request,"bySort"))){out.print("checked=\"checked\"");}%> >Queried fields</td></tr>
					<tr><td><input type="checkbox" name="bySort" value="sort" <% 	if(!isEmpty(request,"bySort")){out.print("checked=\"checked\"");}%> >Sorted fields</td></tr>
				</table>
			</td>
			<td valign="top"><strong>Resolution by</strong>
				<table>
					<tr><td><input type="radio" name="resolution" value="year" <%   if("year".equals(request.getParameter("resolution"))){out.print("checked=\"checked\"");}%> >Year</td></tr>
					<tr><td><input type="radio" name="resolution" value="month" <%  if("month".equals(request.getParameter("resolution"))){out.print("checked=\"checked\"");}%> >Month</td></tr>
					<tr><td><input type="radio" name="resolution" value="week" <%	if("week".equals(request.getParameter("resolution"))){out.print("checked=\"checked\"");}%> >Week</td></tr>
					<tr><td><input type="radio" name="resolution" value="day" <%    if("day".equals(request.getParameter("resolution"))){out.print("checked=\"checked\"");}%> >Day</td></tr>
					<tr><td><input type="radio" name="resolution" value="hour" <%   if("hour".equals(request.getParameter("resolution")) || isEmpty(request, "resolution")){out.print("checked=\"checked\"");}%> >Hour</td></tr>
					<tr><td><input type="radio" name="resolution" value="minute" <% if("minute".equals(request.getParameter("resolution"))){out.print("checked=\"checked\"");}%>>Minute</td></tr>
				</table>
			</td>
			<td valign="top"><strong>Options</strong>
				<table>
					<tr><td><input type="checkbox" name="exclude" value="exclude" <% if(request.getParameter("exclude")!=null){out.print("checked=\"checked\"");}%> >exclude 14-days-operations</td></tr>
					<tr><td><input type="radio" name="sortLegend" value="y" onclick="sortLegendBy(this);" <% if(sortLegend==null || "y".equals(sortLegend)){out.print("checked=\"checked\"");}%> >sort legend by y-value</td></tr>
					<tr><td><input type="radio" name="sortLegend" value="count" onclick="sortLegendBy(this);" <% if("count".equals(sortLegend)){out.print("checked=\"checked\"");}%> >sort legend by count-value</td></tr>
					<tr><td>&nbsp;</td></tr>
					<tr><td>&nbsp;</td></tr>
					<tr><td>&nbsp;</td></tr>
					<tr><td><a href="status">collector status</a></td></tr>
					<tr><td><input type="submit" value="Submit"></td></tr>
				</table>
			</td>
		</tr>
	</table>	
</form>
<%
final String errorMsg = slowOpsDto.getErrorMessage(); 
if(errorMsg!=null){%>
	<div style="color:red; padding-top:5px;">An error occurred. <%=errorMsg.contains("\"code\" : 16389")?"Filter more and/or group less to decrease size of result document! ":""%><br/><%=errorMsg%></div>
<%}else{%>	

	<table><tr><td>
		<div id="graph"></div>
	</td>
	<td valign="top">
		<div id="status" style="max-height:480px; overflow-y:scroll; font-size:0.8em; padding-top:5px;"></div>
	</td></tr>
	</table>

<script type="text/javascript">
			$('#datetimepickerFrom').datetimepicker({
				format: 'yyyy/MM/dd hh:mm:ss',
				weekStart: 1
			});
			$('#datetimepickerTo').datetimepicker({
				format: 'yyyy/MM/dd hh:mm:ss',
				weekStart: 1
			});
</script>

<script type="text/javascript">
var currentRow=0;
var lastSeries;
var sortByCount = <%="count".equals(sortLegend)%>;

function sortLegendBy(radioButton){
	if(radioButton.value == "count"){
		sortByCount = true;
	}else{
		sortByCount = false;
	}
}
g = new Dygraph(document.getElementById("graph"),
	<%= slowOpsDto.getDataGrid()%>
	{
	visibility:<%= Arrays.toString(slowOpsDto.getVisibilityValues()) %>,//hide count columns

	//highlightSeriesOpts: true,
	showLabelsOnHighlight:false,
	hideOverlayOnMouseOut:false,
	labelsSeparateLines: true,
	labelsKMB: true,
	legend: "always",
	strokeWidth: 0.0,
	width: 640,
	height: 480,
	title: "Slow operations",
	xlabel: "Date",
	ylabel: "<%=slowOpsDto.getScale()%>",
	axisLineColor: "white",
	drawXGrid: true,
	drawPoints: true,
	animatedZooms: true,
	
	/*
	//Highlight the circle on mouseover.
	//requires "highlightSeriesOpts: true" which doesn't seem to work well
	drawHighlightPointCallback: function(g, seriesName, ctx, cx, cy, color, pointSize){
		if(lastSeries != seriesName || isNaN(currentRow) ){
			lastSeries = seriesName;
			currentRow = g.getLeftBoundary_() - 1;
		}
		currentRow++;
		var col = g.indexFromSetName(seriesName);
		var count = g.getValue(currentRow, col+1);
		ctx.strokeStyle = color;
		ctx.lineWidth = 2.8;
		ctx.beginPath();
		ctx.arc(cx, cy, Math.sqrt(count/Math.PI), 0, 2 * Math.PI, false); //surface equal to count
		ctx.closePath();
		ctx.stroke();
		
	},*/
	
	highlightCallback: function(e, x, pts, row) {
		var text = "";
		var legend = new Array();
		for (var i = 0; i < pts.length; i++) {
			var rangeY = g.yAxisRange();
			if(pts[i].yval >= rangeY[0] && pts[i].yval <= rangeY[1]){//don't show labels for series outside of the view
				var seriesProps = g.getPropertiesForSeries(pts[i].name);
				var count = g.getValue(row, seriesProps.column+1);
				var minSec = g.getValue(row, seriesProps.column+2);
				var maxSec = g.getValue(row, seriesProps.column+3);
				if(pts[i].yval != 0 && count != 0){//0-values are necessary to put into the data matrix (instead of empty values) but they are not shown in the legend
					legend.push([seriesProps.color, pts[i], count, minSec, maxSec]);
				}
			}
		}
		if(sortByCount){
			legend.sort(function(a,b){return b[2]-a[2]});//sort by count-values
		}else{
			legend.sort(function(a,b){return b[1].yval-a[1].yval});//sort by y-values
		}
		for (var i = 0; i < legend.length; i++) {
			text += "<span style='font-weight: bold; color: "+legend[i][0]+";'> "+legend[i][1].name + "</span><br/><span>"+Dygraph.dateString_(legend[i][1].xval)+" count:" + legend[i][2] +" min:" + legend[i][3] +" max:" + legend[i][4] + " avg:"+legend[i][1].yval+"</span><br/>";
		}
		document.getElementById("status").innerHTML = text;
		
	},
	
	drawPointCallback : function(g, seriesName, ctx, cx, cy, color, pointSize){
		if(lastSeries != seriesName || isNaN(currentRow) ){
			lastSeries = seriesName;
			currentRow = g.getLeftBoundary_() - 1;
		}
		currentRow++;
		var col = g.indexFromSetName(seriesName);
		var avg = g.getValue(currentRow, col);
		if(avg > 0){
			var count = g.getValue(currentRow, col+1);
			ctx.strokeStyle = color;
			ctx.lineWidth = 0.8;
			ctx.beginPath();
			ctx.arc(cx, cy, Math.sqrt(count/Math.PI), 0, 2 * Math.PI, false); //surface equal to count
			ctx.closePath();
			ctx.stroke();
		}
	}
});
    	
</script>
<%}%>

</body>
</html>