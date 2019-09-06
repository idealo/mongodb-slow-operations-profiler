<!DOCTYPE html>
<html>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="java.util.*,de.idealo.mongodb.slowops.dto.SlowOpsDto,de.idealo.mongodb.slowops.grapher.*" %>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
  <meta http-equiv="X-UA-Compatible" content="IE=EmulateIE7; IE=EmulateIE9">
  <script type="text/javascript" src="js/dygraph-combined.js"></script>
  <script type="text/javascript" src="js/jquery-1.11.1.min.js"></script>
  <script type="text/javascript" src="js/bootstrap.min.js"></script>
  <script type="text/javascript" src="js/bootstrap-datetimepicker.min.js"></script>	
  <script type="text/javascript" src="js/jquery.dataTables.min.js"></script>
  <script type="text/javascript" src="js/jquery.dataTables.sum.js"></script>
  <script type="text/javascript" src="js/jquery.number.min.js"></script>
  <link rel="stylesheet" type="text/css" href="css/jquery.dataTables.css">
  <link href="css/bootstrap-combined.css" rel="stylesheet">
  <link href="css/bootstrap-datetimepicker.min.css" rel="stylesheet" type="text/css" media="screen" >
  <title>analyzing slow operations</title>
</head>
<%!boolean isEmpty(HttpServletRequest request, String param) {
    return request.getParameter(param) == null || request.getParameter(param).trim().length() == 0; 
}%>
<%
  final SlowOpsDto slowOpsDto = (SlowOpsDto)request.getAttribute("slowOpsDto");
  final String sortLegend = request.getParameter("sortLegend");
  final String countAsSqrt = request.getParameter("countAsSqrt");
%>
<body>
<form name="input" action="gui" method="get">
	<table  align="top" cellpadding="10">
		<tr>
			<td valign="top"><strong>Filter by</strong>
				<table>
					<tr><td>Earliest date</td><td><div id="datetimepickerFrom" class="date"><input type="text" id="fromDate" name="fromDate" size="30" readonly <% 	if(!isEmpty(request,"fromDate")){out.print("value=\""+request.getParameter("fromDate")+"\"");}else{out.print("value=\""+request.getAttribute("fromDate")+"\"");}%> ><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span></div></td></tr>
					<tr><td>Latest date</td><td><div id="datetimepickerTo" class="date"><input type="text" id="toDate" name="toDate" size="30" readonly <% 			if(!isEmpty(request,"toDate")){out.print("value=\""+request.getParameter("toDate")+"\"");}else{out.print("value=\""+request.getAttribute("toDate")+"\"");}%> ><span class="add-on"><i data-time-icon="icon-time" data-date-icon="icon-calendar"></i></span></div></td></tr>
					<tr><td>Label</td><td><input type="text" name="lbl" size="30" <% 			if(!isEmpty(request,"lbl")){out.print("value=\""+request.getParameter("lbl")+"\"");}%> ><sup>1</sup></td></tr>
					<tr><td>Server address</td><td><input type="text" name="adr" size="30" <% 	if(!isEmpty(request,"adr")){out.print("value=\""+request.getParameter("adr")+"\"");}%> ><sup>1</sup></td></tr>
					<tr><td>ReplicaSet</td><td><input type="text" name="rs" size="30" <% 		if(!isEmpty(request,"rs")){out.print("value=\""+request.getParameter("rs")+"\"");}%> ><sup>1</sup></td></tr>
					<tr><td>Database</td><td><input type="text" name="db" size="30" <% 			if(!isEmpty(request,"db")){out.print("value=\""+request.getParameter("db")+"\"");}%> ><sup>1</sup></td></tr>
					<tr><td>Collection</td><td><input type="text" name="col" size="30" <% 		if(!isEmpty(request,"col")){out.print("value=\""+request.getParameter("col")+"\"");}%> ><sup>1</sup></td></tr>
					<tr><td>User</td><td><input type="text" name="user" size="30" <% 			if(!isEmpty(request,"user")){out.print("value=\""+request.getParameter("user")+"\"");}%> ><sup>1</sup></td></tr>
					<tr><td>Operation</td><td><input type="text" name="op" size="30" <% 		if(!isEmpty(request,"op")){out.print("value=\""+request.getParameter("op")+"\"");}%> ><sup>1</sup></td></tr>
					<tr><td>Queried fields</td><td><input type="text" name="fields" size="30" <%if(!isEmpty(request,"fields")){out.print("value=\""+request.getParameter("fields")+"\"");}%> ><sup>2</sup></td></tr>
					<tr><td>Sorted fields</td><td><input type="text" name="sort" size="30" <% 	if(!isEmpty(request,"sort")){out.print("value=\""+request.getParameter("sort")+"\"");}%> ><sup>2</sup></td></tr>
					<tr><td>Millis from</td><td><input type="text" name="fromMs" size="10" <%    if(!isEmpty(request,"fromMs")){out.print("value=\""+request.getParameter("fromMs")+"\"");}%> > to <input type="text" name="toMs" size="10" <%                if(!isEmpty(request,"toMs")){out.print("value=\""+request.getParameter("toMs")+"\"");}%> ></td></tr>
					<tr><td colspan="2">
						<i>
							Values separated by semicolon are logically combined: </br>
							<sup>1</sup> or </br>
							<sup>2</sup> and
						</i>
					</td></tr>
				</table>
			</td>
			<td valign="top"><strong>Group by</strong>
				<table>
					<tr><td><input type="checkbox" name="byLbl" value="lbl" <%   	if(!isEmpty(request,"byLbl")){out.print("checked=\"checked\"");}%> > Label</td></tr>
					<tr><td><input type="checkbox" name="byAdr" value="adr" <%   	if(!isEmpty(request,"byAdr")){out.print("checked=\"checked\"");}%> > Server address</td></tr>
					<tr><td><input type="checkbox" name="byRs" value="rs" <%   	if(!isEmpty(request,"byRs")){out.print("checked=\"checked\"");}%> > ReplicaSet</td></tr>
					<tr><td><input type="checkbox" name="byDb" value="db" <%   	if(!isEmpty(request,"byDb")){out.print("checked=\"checked\"");}%> > Database</td></tr>
					<tr><td><input type="checkbox" name="byCol" value="col" <%   	if(!isEmpty(request,"byCol")){out.print("checked=\"checked\"");}%> > Collection</td></tr>
					<tr><td><input type="checkbox" name="byUser" value="user" <%	if(!isEmpty(request,"byUser")){out.print("checked=\"checked\"");}%> > User</td></tr>
					<tr><td><input type="checkbox" name="byOp" value="op" <%		if(!isEmpty(request,"byOp")){out.print("checked=\"checked\"");}%> > Operation</td></tr>
					<tr><td><input type="checkbox" name="byFields" value="fields" <%if(!isEmpty(request,"byFields")  || (isEmpty(request,"byLbl") && isEmpty(request,"byAdr") && isEmpty(request,"byDb") && isEmpty(request,"byCol") && isEmpty(request,"byUser") && isEmpty(request,"byOp") && isEmpty(request,"bySort"))){out.print("checked=\"checked\"");}%> > Queried fields</td></tr>
					<tr><td><input type="checkbox" name="bySort" value="sort" <% 	if(!isEmpty(request,"bySort")){out.print("checked=\"checked\"");}%> > Sorted fields</td></tr>
				</table>
			</td>
			<td valign="top"><strong>Resolution by</strong>
				<table>
					<tr><td><input type="radio" name="resolution" value="year" <%   if("year".equals(request.getParameter("resolution"))){out.print("checked=\"checked\"");}%> > Year</td></tr>
					<tr><td><input type="radio" name="resolution" value="month" <%  if("month".equals(request.getParameter("resolution"))){out.print("checked=\"checked\"");}%> > Month</td></tr>
					<tr><td><input type="radio" name="resolution" value="week" <%	if("week".equals(request.getParameter("resolution"))){out.print("checked=\"checked\"");}%> > Week</td></tr>
					<tr><td><input type="radio" name="resolution" value="day" <%    if("day".equals(request.getParameter("resolution"))){out.print("checked=\"checked\"");}%> > Day</td></tr>
					<tr><td><input type="radio" name="resolution" value="hour" <%   if("hour".equals(request.getParameter("resolution")) || isEmpty(request, "resolution")){out.print("checked=\"checked\"");}%> > Hour</td></tr>
					<tr><td><input type="radio" name="resolution" value="minute" <% if("minute".equals(request.getParameter("resolution"))){out.print("checked=\"checked\"");}%>> Minute</td></tr>
                    <tr><td>&nbsp;</td></tr>
                    <tr><td>&nbsp;</td></tr>
                    <tr><td>&nbsp;</td></tr>
                    <tr><td><input type="submit" value="Submit"></td></tr>
				</table>
			</td>
			<td valign="top">
				<table>
                    <tr><td>&nbsp;</td></tr>
                    <tr><td>&nbsp;</td></tr>
                    <tr><td>&nbsp;</td></tr>
                    <tr><td>&nbsp;</td></tr>
                    <tr><td>&nbsp;</td></tr>
                    <tr><td>&nbsp;</td></tr>
                    <tr><td>&nbsp;</td></tr>
                    <tr><td>&nbsp;</td></tr>
                    <tr><td>&nbsp;</td></tr>
                    <tr><td>&nbsp;</td></tr>
                    <tr><td>&nbsp;</td></tr>
                    <tr><td>&nbsp;</td></tr>
                    <tr><td><strong>Diagram configuration</strong></td></tr>
                    <tr><td>y-axis: <input name="yAxis" value="avg" onclick="setYAxis(0);" checked="checked" type="radio"> avg
                    <input name="yAxis" value="min" onclick="setYAxis(2);" type="radio"> min
                    <input name="yAxis" value="max" onclick="setYAxis(3);" type="radio"> max
                    <input name="yAxis" value="sum" onclick="setYAxis(4);" type="radio"> sum </td></tr>
					<tr><td>sort legend by: <input type="radio" name="sortLegend" value="y" onclick="sortLegendBy(this);" <% if(sortLegend==null || "y".equals(sortLegend)){out.print("checked=\"checked\"");}%> > y-value
					<input type="radio" name="sortLegend" value="count" onclick="sortLegendBy(this);" <% if("count".equals(sortLegend)){out.print("checked=\"checked\"");}%> > count-value </td></tr>
					<tr><td><input type="checkbox" name="countAsSqrt" value="countAsSqrt" onclick="setCountAsSqrt(this);" <% if(countAsSqrt!=null){out.print("checked=\"checked\"");}%> > show circles as sqrt of count-value</td></tr>
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
var countAsSqrt = <%="countAsSqrt".equals(countAsSqrt)%>;
var lastMouseEvent;

function sortLegendBy(radioButton){
	if(radioButton.value == "count"){
		sortByCount = true;
	}else{
		sortByCount = false;
	}
	drawLegend();

}
function setCountAsSqrt(checkButton){
	countAsSqrt = checkButton.checked;
	g.setAnnotations(g.annotations()); //redraw graph
}
//newYIndex can be one of the 4 custom fields:
//avg=0
//min=2
//max=3
//sum=4
//stdDevMs=5
//nRet=6
//minRet=7
//maxRet=8
//avgRet=9
//stdDevRet=10
//rKeys=11
//rDocs=12
//wDocs=13
//memSort=14
var initialYIndex=[0,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14];//initial order of fields: timestamp,avg,count,min,max,sum
var currentYIndex=initialYIndex;
function setYAxis(newYIndex){
    currentYIndex = interchangeIndexes(initialYIndex, newYIndex);//save current order to show the correct values in the legend
    var result = makeNewYAxis(newYIndex);
    g.updateOptions({"file": result});
}

function drawLegend(){
    if(lastMouseEvent) {
        var e = lastMouseEvent.e;
        var x = lastMouseEvent.x;
        var pts = lastMouseEvent.pts;
        var row = lastMouseEvent.row;

        var text = "";
        var legend = new Array();
        for (var i = 0; i < pts.length; i++) {
            var rangeY = g.yAxisRange();
            if (pts[i].yval >= rangeY[0] && pts[i].yval <= rangeY[1]) {//don't show labels for series outside of the view
                var seriesProps = g.getPropertiesForSeries(pts[i].name);
                var avg = g.getValue(row, seriesProps.column + currentYIndex[1]);
                var count = g.getValue(row, seriesProps.column + currentYIndex[2]);
                var minSec = g.getValue(row, seriesProps.column + currentYIndex[3]);
                var maxSec = g.getValue(row, seriesProps.column + currentYIndex[4]);
                var sumSec = g.getValue(row, seriesProps.column + currentYIndex[5]);
                var stdDevMs = g.getValue(row, seriesProps.column + currentYIndex[6]);
                var nRet = g.getValue(row, seriesProps.column + currentYIndex[7]);
                var minRet = g.getValue(row, seriesProps.column + currentYIndex[8]);
                var maxRet = g.getValue(row, seriesProps.column + currentYIndex[9]);
                var avgRet = g.getValue(row, seriesProps.column + currentYIndex[10]);
                var stdDevRet = g.getValue(row, seriesProps.column + currentYIndex[11]);
                var rKeys = g.getValue(row, seriesProps.column + currentYIndex[12]);
                var rDocs = g.getValue(row, seriesProps.column + currentYIndex[13]);
                var wDocs = g.getValue(row, seriesProps.column + currentYIndex[14]);
                var memSort = g.getValue(row, seriesProps.column + currentYIndex[15]);
                if (pts[i].yval != 0 && count != 0) {//0-values are necessary to put into the data matrix (instead of empty values) but they are not shown in the legend
                    legend.push([seriesProps.color, pts[i], avg, count, minSec, maxSec, sumSec, stdDevMs, nRet, minRet, maxRet, avgRet, stdDevRet, rKeys, rDocs, wDocs, memSort]);
                }
            }
        }
        if (sortByCount) {
            legend.sort(function (a, b) {
                return b[3] - a[3]
            });//sort by count-values
        } else {
            legend.sort(function (a, b) {
                return b[1].yval - a[1].yval
            });//sort by y-values
        }

        var header="";
        var body ="";
        for (var i = 0; i < legend.length; i++) {
            header = "<span><b>Timestamp</b> " + Dygraph.dateString_(legend[i][1].xval)+"</span><br/>";
            body += "<span style='font-weight: bold; color: " + legend[i][0] + ";'> " + legend[i][1].name + "</span><br/><span>" +
                "<b>Slow-ops </b> count:" + formatNumber(legend[i][3]) +
                "<br/><b>Duration</b> min:" + formatNumber(legend[i][4]) + " max:" + formatNumber(legend[i][5]) + " avg:" + formatNumber(legend[i][2]) + " sum:" + formatNumber(legend[i][6]) + " stdDev:" + formatNumber(legend[i][7]) +
                "<br/><b>Returned </b> min:" + formatNumber(legend[i][9]) + " max:" + formatNumber(legend[i][10]) + " avg:" + formatNumber(legend[i][11]) + " sum:" + formatNumber(legend[i][8]) + " stdDev:" + formatNumber(legend[i][12]) +
                "<br/><b>R/W </b> rKeys:" + formatNumber(legend[i][13]) + " rDocs:" + formatNumber(legend[i][14]) + " wDocs:" + formatNumber(legend[i][15]) + " memSort: " + (legend[i][16]=="1"?"true":"false") +
                "</span><br/>";
        }
        text += header + body;
        document.getElementById("status").innerHTML = text;
    }
}

function makeNewYAxis(newYIndex){
    var result = [];
    for(var i=0; i<arrData.length; i++){
        var line = arrData[i];
        var newLine = interchangeIndexes(line, newYIndex);
        result.push(newLine);
    }
    return result;
}

function interchangeIndexes(line, newYIndex){
    var result = [];
    result.push(line[0]);//leading field is always the first element, here a timestamp
    var c = -1;
    var origY = 0;
    for(var i=1; i<line.length; i++){
        if(++c >= initialYIndex.length-1) c = 0;//we have as many custom fields as defined by initialYIndex minus 1 for the timestamp field

        if(c == 0){//we are at the index where y is saved
            origY = line[i];
            result.push(line[i+newYIndex]);//push element from the current y index to the custom field
        }else if(c == newYIndex){//we are at the custom field which will be the new y
            result.push(origY);//push the original y to the custom field
        }else {
            result.push(line[i]);//just copy without changing the index position
        }
    }
    return result;
}

function formatNumber(data){
    return isNaN(data)?data:(new Number(data)).toLocaleString(undefined, {maximumFractionDigits:2});
}

var arrData = <%= slowOpsDto.getDataGrid()%>;

g = new Dygraph(document.getElementById("graph"),
	arrData,
    {
    labels: [<%= slowOpsDto.getLabels()%>],
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
		lastMouseEvent = {"e":e,
			"x":x,
			"pts":pts,
			"row":row};
		drawLegend();
		
	},
	
	drawPointCallback : function(g, seriesName, ctx, cx, cy, color, pointSize){
		if(lastSeries != seriesName || isNaN(currentRow) ){
			lastSeries = seriesName;
			currentRow = g.getLeftBoundary_() - 1;
		}
		currentRow++;
		var col = g.indexFromSetName(seriesName);
		var count = g.getValue(currentRow, col+1);
		ctx.strokeStyle = color;
		ctx.lineWidth = 0.8;
		ctx.beginPath();
		if(countAsSqrt){
			ctx.arc(cx, cy, Math.sqrt(Math.sqrt(count)/Math.PI), 0, 2 * Math.PI, false); //surface equal to square root of count
		}else{
			ctx.arc(cx, cy, Math.sqrt(count/Math.PI), 0, 2 * Math.PI, false); //surface equal to count
		}
		ctx.closePath();
		ctx.stroke();
	}
});
    	
</script>
<%}%>

<style>
.toggle-vis-true { color: #3174c7; }
.toggle-vis-false { color: #6c6c6c; }
a {
    color: #3174c7;
    cursor: pointer;
    text-decoration: none;
}
a:hover {
    text-decoration:underline;
}
</style>
<script type="text/javascript" >

    $(document).ready(function() {
    
        var colNames = [{"visible":true, "name":"Group"},
                        {"visible":true, "name": "Count"},
                        {"visible":true, "name": "Min ms"},
                        {"visible":true, "name": "Max ms"},
                        {"visible":true, "name": "Avg ms"},
                        {"visible":true, "name": "Sum ms"},
                        {"visible":true, "name": "StdDev ms"},
                        {"visible":true, "name": "Min ret"},
                        {"visible":true, "name": "Max ret"},
                        {"visible":true, "name": "Avg ret"},
                        {"visible":true, "name": "Sum ret"},
                        {"visible":true, "name": "StdDev ret"},
                        {"visible":true, "name": "ret/ms"},
                        {"visible":true, "name": "ms/ret"},
                        {"visible":true, "name": "rKeys"},
                        {"visible":true, "name": "rDocs"},
                        {"visible":true, "name": "wDocs"},
                        {"visible":true, "name": "memSort"}

        ];
        
        var columnDefs = [];
        for(var i in colNames){
            $( "#tableHeader" ).append( "<th>" + colNames[i].name +  "</th>");
            $( "#tableFooter" ).append( "<th>" + colNames[i].name +  "</th>");
            columnDefs.push({"targets": [Number(i)], "visible":colNames[i].visible,
                "mRender": function ( data, type, full ) {
                                return type == "display"?formatNumber(data):data;
                           }
            });
        }

        var table = $('#main').DataTable({
            "sScrollY": "600px",
            "paging": false,
            "columnDefs": columnDefs,
            "footerCallback": function ( row, data, start, end, display ) {
                var api = this.api(), data;
                // Remove the formatting to get integer data for summation
                var intVal = function (i) {
                    return typeof i === 'string' ? i.replace(/[\$,]/g, '') * 1 : typeof i === 'number' ? i : 0;
                };
                if (api.column(1).data().length) {//don't compute sum on initial draw before data is loaded
                    // Total over current page so that search results reflect the sum
                    var total1 = api.column(1, {page: 'current'}).data().reduce(function (a, b) {return intVal(a) + intVal(b);});
                    var total5 = api.column(5, {page: 'current'}).data().reduce(function (a, b) {return intVal(a) + intVal(b);});
                    var total10 = api.column(10, {page: 'current'}).data().reduce(function (a, b) {return intVal(a) + intVal(b);});
                    var total14 = api.column(12, {page: 'current'}).data().reduce(function (a, b) {return intVal(a) + intVal(b);});
                    var total15 = api.column(13, {page: 'current'}).data().reduce(function (a, b) {return intVal(a) + intVal(b);});
                    var total16 = api.column(14, {page: 'current'}).data().reduce(function (a, b) {return intVal(a) + intVal(b);});
                    // Update footer
                    $(api.column(1).footer()).html('Total count: ' + formatNumber(total1));
                    $(api.column(5).footer()).html('Total ms: ' + formatNumber(total5));
                    $(api.column(10).footer()).html('Total ret: ' + formatNumber(total10));
                    $(api.column(14).footer()).html('Total rKeys: ' + formatNumber(total14));
                    $(api.column(15).footer()).html('Total rDocs: ' + formatNumber(total15));
                    $(api.column(16).footer()).html('Total wDocs: ' + formatNumber(total16));
                }
            }
        });

        var t = table.columns().header();
        $.each(t, function(k,v){
            var visible = table.column( $(v) ).visible();
            $( "#cols" ).append( "<a id='cols_"+k+"' class='toggle-vis-" + visible + "' data-column='" + k + "'>" + $(v).html() +  "</a> - ");
            $( "#cols_" + k ).on('click', function (e) {
            e.preventDefault();

            // Get the column API object
            var column = table.column( $(this).attr('data-column') );

            // Toggle the visibility
            var isVisible = column.visible();
            $("#cols_" + column[0]).removeClass("toggle-vis-" + isVisible).addClass("toggle-vis-" + !isVisible);
            column.visible( ! isVisible );
        } );
        })
    } );
</script>

<br/>
    <div id="cols">Toggle columns: </div>

    <table  id="main" class="display" cellspacing="0" width="100%" align="top" cellpadding="10">
   <thead>
     <tr id="tableHeader"></tr>
   </thead>
   <tfoot>
     <tr id="tableFooter"></tr>
   </tfoot>
    <tbody>
    <% 
    final HashMap<String, AggregatedProfiling> labelSeries = slowOpsDto.getLabelSeries();
    for (AggregatedProfiling labelSerie : labelSeries.values()) {%>
        <tr>
            <td valign="top"><%=labelSerie.getId().getLabel(true) %></td>
            <td valign="top"><%=labelSerie.getCount() %></td>
            <td valign="top"><%=labelSerie.getMinMs() %></td>
            <td valign="top"><%=labelSerie.getMaxMs() %></td>
            <td valign="top"><%=labelSerie.getMillis()/labelSerie.getCount() %></td>
            <td valign="top"><%=labelSerie.getMillis() %></td>
            <td valign="top"><%=labelSerie.getStdDevMs() %></td>
            <td valign="top"><%=labelSerie.getMinRet() %></td>
            <td valign="top"><%=labelSerie.getMaxRet() %></td>
            <td valign="top"><%=labelSerie.getNRet()/labelSerie.getCount() %></td>
            <td valign="top"><%=labelSerie.getNRet() %></td>
            <td valign="top"><%=labelSerie.getStdDevRet() %></td>
            <td valign="top"><%=labelSerie.getMillis()>0?(labelSerie.getNRet()/labelSerie.getMillis()):"" %></td>
            <td valign="top"><%=labelSerie.getNRet()>0?(labelSerie.getMillis()/labelSerie.getNRet()):"" %></td>
            <td valign="top"><%=labelSerie.getKeys() %></td>
            <td valign="top"><%=labelSerie.getDocs() %></td>
            <td valign="top"><%=labelSerie.getDocsWrittenCount() %></td>
            <td valign="top"><%=labelSerie.hasSortStage() %></td>
        </tr>
    <%}%>
  <tbody>
 </table>
<%@ include file="buildInfo.jsp" %>
</body>
</html>
