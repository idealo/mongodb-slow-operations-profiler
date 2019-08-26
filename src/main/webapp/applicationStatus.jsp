<%@ page import="de.idealo.mongodb.slowops.util.Util" %>
<!DOCTYPE html>
<html>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<script type="text/javascript" src="js/dojo.js"></script>
	<script type="text/javascript" src="js/jquery-1.11.1.min.js"></script>
	<script type="text/javascript" src="js/jquery.dataTables.min.js"></script>
	<script type="text/javascript" src="js/jquery.dataTables.sum.js"></script>
	<script type="text/javascript" src="js/jquery.number.min.js"></script>
	<script type="text/javascript" src="js/jquery-ui.min.js"></script>
	<link rel="stylesheet" type="text/css" href="css/jquery.dataTables.css">
	<link rel="stylesheet" type="text/css" href="css/jquery-ui.css">
	<title>slow operations</title>
		<script type="text/javascript" >
			var mainTable;
			$(document).ready(function() {

				var colNames = [{"visible":true, "name":"Select"},
					{"visible":true, "name":"DBS Label"},
					{"visible":true, "name": "ReplSet"},
					{"visible":true, "name": "Status"},
					{"visible":true, "name": "Host"},
					{"visible":true, "name": "NumCores"},
					{"visible":true, "name": "CpuFreqMHz"},
					{"visible":true, "name": "MemSizeMB"},
					{"visible":true, "name": "Mongodb"},
					{"visible":true, "name": "Database"},
					{"visible":true, "name": "Collections"},
					{"visible":true, "name": "SlowMs"},
					{"visible":true, "name": "Profiling"},
					{"visible":true, "name": "Collecting"},
					{"visible":true, "name": "LastTS"},
					{"visible":true, "name": "#SlowOps"},
					{"visible":true, "name": "#10sec"},
					{"visible":true, "name": "#1Min"},
					{"visible":true, "name": "#10Min"},
					{"visible":true, "name": "#30Min"},
					{"visible":true, "name": "#1Hour"},
					{"visible":true, "name": "#12Hours"},
					{"visible":true, "name": "#1Day"}
				];

				var columnDefs = [];
				for(var i in colNames){
					$( "#tableHeader" ).append( "<th>" + (i==0?"<input type='checkbox' id='checkall' title='Select all'/>":colNames[i].name)+  "</th>");
					$( "#tableFooter" ).append( "<th>" + colNames[i].name +  "</th>");
					columnDefs.push({"targets": [Number(i)], "visible":colNames[i].visible, "orderable":i==0?false:true});
				}

				mainTable = $('#main').DataTable({
					"ajax": {
						url: "<%=request.getContextPath()%>/rest/action?cmd=refresh&<%=Util.ADMIN_TOKEN + "=" + request.getParameter(Util.ADMIN_TOKEN)%>",
						dataSrc: "collectorStatuses"
					},
					"columns": [
						{ "data": "instanceId", "render": function ( data, type, full, meta ) { return '<input id="chk_' + data + '" type="checkbox" name="chk" value="' + data + '">'}},
						{ "data": "label" },
						{ "data": "replSetName" },
						{ "data": "replSetStatus" },
						{ "data": "serverAddressAsString" },
						{ "data": "numCores" },
						{ "data": "cpuFreqMHz" },
						{ "data": "memSizeMB" },
						{ "data": "mongodbVersion" },
						{ "data": "database" },
						{ "data": "collections", "render" : "[, ]"},//create a comma separated list from an array of objects
						{ "data": "slowMs" },
						{ "data": "profiling" },
						{ "data": "collecting", "render": function ( data, type, full, meta ) { return !data }},
						{ "data": "lastTsFormatted" },
						{ "data": "doneJobsHistory.0" },
						{ "data": "doneJobsHistory.1" },
						{ "data": "doneJobsHistory.2" },
						{ "data": "doneJobsHistory.3" },
						{ "data": "doneJobsHistory.4" },
						{ "data": "doneJobsHistory.5" },
						{ "data": "doneJobsHistory.6" },
						{ "data": "doneJobsHistory.7" }
					],
					"paging": false,
					"order": [[ 1, "asc" ], [ 2, "asc" ],[ 3, "asc" ],[ 4, "asc" ],[ 9, "asc" ]],
					"columnDefs": columnDefs,
					"footerCallback": function ( row, data, start, end, display ) {
						var api = this.api(), data;
						// Remove the formatting to get integer data for summation
						var intVal = function ( i ) {
							return typeof i === 'string' ? i.replace(/[\$,]/g, '')*1 : typeof i === 'number' ? i : 0;
						};
						if(api.column( 15 ).data().length) {//don't compute sum on initial draw before data is loaded
							// Total over current page so that search results reflect the sum
							var total1 = api.column( 15, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							var total2 = api.column( 16, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							var total3 = api.column( 17, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							var total4 = api.column( 18, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							var total5 = api.column( 19, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							var total6 = api.column( 20, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							var total7 = api.column( 21, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							var total8 = api.column( 22, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							// Update footer
							$( api.column( 15 ).footer() ).html('Sum #SlowOps: '+ $.number(total1, 0, ".", ","));
							$( api.column( 16 ).footer() ).html('Sum #10sec: '+ $.number(total2, 0, ".", ","));
							$( api.column( 17 ).footer() ).html('Sum #1Min: '+ $.number(total3, 0, ".", ","));
							$( api.column( 18 ).footer() ).html('Sum #10Min: '+ $.number(total4, 0, ".", ","));
							$( api.column( 19 ).footer() ).html('Sum #30Min: '+ $.number(total5, 0, ".", ","));
							$( api.column( 20 ).footer() ).html('Sum #1Hour: '+ $.number(total6, 0, ".", ","));
							$( api.column( 21 ).footer() ).html('Sum #12Hours: '+ $.number(total7, 0, ".", ","));
							$( api.column( 22 ).footer() ).html('Sum #1Day: '+ $.number(total8, 0, ".", ","));
						}
					},
					"drawCallback": function ( settings ) {
						var api = this.api();
						var rows = api.rows( {page:'current'} ).nodes();
						var last=null;

						api.column(1, {page:'current'} ).data().each( function ( group, i ) {
							if ( last !== group ) {
								$(rows).eq( i ).before(
										'<tr class="group"><td colspan="'+colNames.length+'">'+group+'</td></tr>'
								);

								last = group;
							}
						} );
					},
                    "search": {
                        "search": "<%=request.getParameter("lbl")!=null?request.getParameter("lbl"):""%>"
                    }
				});

				// Order by the grouping
				$('#main tbody').on( 'click', 'tr.group', function () {
					var currentOrder = mainTable.order()[0];
					if ( currentOrder[0] === 1 && currentOrder[1] === 'asc' ) {
						mainTable.order( [ 1, 'desc' ] ).draw();
					}
					else {
						mainTable.order( [ 1, 'asc' ] ).draw();
					}
				} );


				//update external elements:
				mainTable.on( 'xhr', function () {//xhr event is fired when ajax event is completed
					var json = mainTable.ajax.json();

					if(json && json.collectorServerDto) {
						var hostsString = "";
						json.collectorServerDto.hosts.forEach(function (u) {
							hostsString += u.host + ":" + u.port + ", ";
						})
						if (hostsString.length > 0) {
							hostsString = hostsString.slice(0, -2);//remove last ", "
						}
						$('#clr_hosts').html(hostsString);
						$('#clr_db').html(json.collectorServerDto.db);
						$('#clr_col').html(json.collectorServerDto.collection);
						$('#clr_reads').html(json.numberOfReads);
						$('#clr_dead').html(json.numberOfReadsOfRemovedReaders);
                        $('#clr_total').html((json.numberOfReads+json.numberOfReadsOfRemovedReaders));
                        $('#clr_writes').html(json.numberOfWrites);
                        $('#clr_oldwrites').html(json.numberOfWritesOfRemovedWriters);
                        $('#clr_totalwrites').html((json.numberOfWrites + json.numberOfWritesOfRemovedWriters));
                        $('#clr_date').html(formatDate(new Date(json.collectorRunningSince)));
						$('#clr_refresh_ts').html(formatDate(new Date(json.lastRefresh)));
						$('#clr_refresh_quantity').html('all');
                        $("textarea#config").val(JSON.stringify(JSON.parse(json.config), undefined, 4));
						$("textarea#weblog").val(json.webLog);
					}
				});



				var t = mainTable.columns().header();
				$.each(t, function(k,v){
					if(k!=0) {
						var visible = mainTable.column($(v)).visible();
						$("#cols").append("<a id='cols_" + k + "' class='toggle-vis-" + visible + "' data-column='" + k + "'>" + $(v).html() + "</a> - ");
						$("#cols_" + k).on('click', function (e) {
							e.preventDefault();

							// Get the column API object
							var column = mainTable.column($(this).attr('data-column'));

							// Toggle the visibility
							var isVisible = column.visible();
							$("#cols_" + column[0]).removeClass("toggle-vis-" + isVisible).addClass("toggle-vis-" + !isVisible);
							column.visible(!isVisible);
						});
					}
				})

				$('#checkall').click(function () {
					$(':checkbox', mainTable.rows().nodes()).prop('checked', this.checked);
				});

                $('#actionsLink').click(function() {
                    $('#actionsTable').toggle('slow');
                });

				$(".infoSlowMs").tooltip({content:function(){return $("#infoSlowMsContent").html();}});
                $(".infoConfig").tooltip({content:function(){return $("#infoConfigContent").html();}});
                $(".infoOpsCount").tooltip({content:function(){return $("#infoOpsCountContent").html();}});
                $(".infoRefreshCollector").tooltip({content:function(){return $("#infoRefreshCollectorContent").html();}});
                $(".infoRefresh").tooltip({content:function(){return $("#infoRefreshContent").html();}});
                $(".infoAnalyse").tooltip({content:function(){return $("#infoAnalyseContent").html();}});
                $(".infoCurrentOps").tooltip({content:function(){return $("#infoCurrentOpsContent").html();}});
                $(".infoListDbs").tooltip({content:function(){return $("#infoListDbsContent").html();}});
                $(".infoIdxAccStats").tooltip({content:function(){return $("#infoIdxAccStatsContent").html();}});
				$(".infoHostinfo").tooltip({content:function(){return $("#infoHostinfoContent").html();}});
                $(".infoCollecting").tooltip({content:function(){return $("#infoCollectingContent").html();}});

			} );


			function parallelAction(cmd){
				var slowMs = $("#slowms").val();
				var count = 0;

				$("input:checkbox[name=chk]:checked").each(function(){
					var selectedRow = $(this).parent().parent();
					var pId = $(this).val();
					count++;

					dojo.xhrGet({
						url: "<%=request.getContextPath()%>/rest/action?cmd="+cmd+"&p="+pId+"&ms="+slowMs,
						headers: { "Accept": "application/json" },
						handleAs: "json",
						load: function(json) {
							if(json && json != null) {
								var statuses = json.collectorStatuses;
								if (statuses != null) {
									statuses.forEach(function (u) {
										mainTable.row(selectedRow).data(u).draw();
										$("#chk_" + u.instanceId).prop( "checked", true );
									})
								}
								$('#clr_reads').html(json.numberOfReads);
								$('#clr_dead').html(json.numberOfReadsOfRemovedReaders);
                                $('#clr_total').html((json.numberOfReads+json.numberOfReadsOfRemovedReaders));
                                $('#clr_writes').html(json.numberOfWrites);
                                $('#clr_oldwrites').html(json.numberOfWritesOfRemovedWriters);
                                $('#clr_totalwrites').html((json.numberOfWrites + json.numberOfWritesOfRemovedWriters));
                                $('#clr_date').html(formatDate(new Date(json.collectorRunningSince)));
								$('#clr_refresh_ts').html(formatDate(new Date(json.lastRefresh)));
								$('#clr_refresh_quantity').html('some');
                                $("textarea#config").val(JSON.stringify(JSON.parse(json.config), undefined, 4));
								$("textarea#weblog").val(json.webLog);
							}
						}
					});

				});
			}

            function singleAction(cmd) {

                if(cmd == "analyse") {
                    var lbl = new StringSet();
                    var rs = new StringSet();
                    var adr = new StringSet();
                    var db = new StringSet();
                    var col = new StringSet();

                    $("input:checkbox[name=chk]:checked").each(function () {
                        var selectedRow = $(this).parent().parent();
                        var row = mainTable.row(selectedRow).data();

                        lbl.add(row.label);
                        rs.add(row.replSetName);
                        adr.add(row.serverAddressAsString);
                        db.add(row.database);
                        var colAsString = row.collectionsAsString.replace(",", ";");
                        if(colAsString != "*") {
                            col.add(colAsString);
                        }

                    });

                    var toDate = new Date();
                    var fromDate = new Date(toDate.getTime() - (24 * 60 * 60 * 1000));
                    window.open("<%=request.getContextPath()%>/gui?fromDate=" + formatDate(fromDate) + "&toDate=" + formatDate(toDate)
                            + "&lbl=" + lbl.values()
                            + "&rs=" + rs.values()
                            + "&adr=" + adr.values()
                            + "&db=" + db.values()
                            + "&col=" + col.values()
                            + "&byLbl=lbl"
                            + "&byAdr=adr"
                            + "&byRs=rs"
                            + "&byDb=db"
                            + "&byCol=col"
                            + "&byFields=fields"
                            + "&resolution=hour"
                            + "&sortLegend=y"
                            , "_blank");
                }else{
					var mode = $("input[name='mode']:checked").val();
                    var pIds = new StringSet();

                    $("input:checkbox[name=chk]:checked").each(function () {
                        var selectedRow = $(this).parent().parent();
                        var pId = $(this).val();
                        pIds.add(pId);
                    });
                    if(pIds.size() > 0) {
                        window.open("<%=request.getContextPath()%>/cmd?&cmd=" + cmd + "&pIds=" + pIds.values() + "&mode=" + mode, "_blank");
                    }else{
                        alert("Tick one or more checkboxes first in order to execute this action.");
                    }
                }
            }

            function refreshCollector(){

                dojo.xhrGet({
                    url: "<%=request.getContextPath()%>/rest/action?cmd=rc",
                    headers: { "Accept": "application/json" },
                    handleAs: "json",
                    load: function(json) {
                        if(json && json.collectorServerDto) {
                            var hostsString = "";
                            json.collectorServerDto.hosts.forEach(function (u) {
                                hostsString += u.host + ":" + u.port + ", ";
                            })
                            if (hostsString.length > 0) {
                                hostsString = hostsString.slice(0, -2);//remove last ", "
                            }
                            $('#clr_hosts').html(hostsString);
                            $('#clr_db').html(json.collectorServerDto.db);
                            $('#clr_col').html(json.collectorServerDto.collection);
                            $('#clr_reads').html(json.numberOfReads);
                            $('#clr_dead').html(json.numberOfReadsOfRemovedReaders);
                            $('#clr_total').html((json.numberOfReads+json.numberOfReadsOfRemovedReaders));
                            $('#clr_writes').html(json.numberOfWrites);
                            $('#clr_oldwrites').html(json.numberOfWritesOfRemovedWriters);
                            $('#clr_totalwrites').html((json.numberOfWrites + json.numberOfWritesOfRemovedWriters));
                            $('#clr_date').html(formatDate(new Date(json.collectorRunningSince)));
                            $("textarea#config").val(JSON.stringify(JSON.parse(json.config), undefined, 4));
							$("textarea#weblog").val(json.webLog);
                        }
                    }
                });

            }

			function formatDate(dateObject) {
				var d = new Date(dateObject);
				var day = getTwoDigits(d.getDate());
				var month = getTwoDigits(d.getMonth() + 1);
				var year = d.getFullYear();
				var h = getTwoDigits(d.getHours()); // 0-24 format
				var m = getTwoDigits(d.getMinutes());
				var s = getTwoDigits(d.getSeconds());
				var date = year + "/" + month + "/" + day + " " + h + ":" + m + ":" + s;

				return date;
			};

			function getTwoDigits(n){
				if (n < 10) {
					n = "0" + n;
				}
				return n;
			}

			function setInnerHTML(id, value){
				var elem = document.getElementById(id);
				if(elem){
					elem.innerHTML = value;
				}
			}

			function StringSet() {
				var setObj = {}, val = {};

				this.add = function(str) {
					if(str && str != "")setObj[str] = val;
				};

				this.contains = function(str) {
					return setObj[str] === val;
				};

				this.remove = function(str) {
					delete setObj[str];
				};

				this.values = function() {
					var values = "";
					for (var i in setObj) {
						if (setObj[i] === val) {
							values += i + ";";
						}
					}
					if(values.length > 0) values = values.substring(0, values.length - 1);

					return values;
				};

                this.size = function() {
                    var result = 0;
                    for (var i in setObj) {
                        result++;
                    }
                    return result;
                }

			}

		</script>
	</head>
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

        .opstable td, .opstable th{
            border: 1px solid black;
            text-align: right;
            padding: 5px;
        }
        .actions {
            padding: 5px 10px;
            border: 1px solid #999;
            background: #f7f7f7;
            position: fixed;
            top: 0; right: 0;
        }
	</style>
<body>
<h1>Application Status </h1>

<h2>Collector</h2>

<table  align="top" >
	<tr><td>Access point(s)</td><td id="clr_hosts"></td></tr>
	<tr><td>Database</td><td id="clr_db"></td></tr>
	<tr><td>Collection</td><td id="clr_col"></td></tr>
    <tr>
        <td valign="top">Number of ops</td>
        <td>
            <table class="opstable" style="border-collapse:collapse;">
                <tr>
                    <th>&nbsp;</th>
                    <th>current</th>
                    <th class="infoOpsCount">old&nbsp;<img src='img/info.gif' alt='info' title='info'></th>
                    <th>total</th>
                </tr>
                <tr>
                    <td>Reads</td>
                    <td id="clr_reads"></td>
                    <td id="clr_dead"></td>
                    <td id="clr_total"></td>
                </tr>
                <tr>
                    <td>Writes</td>
                    <td id="clr_writes"></td>
                    <td id="clr_oldwrites"></td>
                    <td id="clr_totalwrites"></td>
                </tr>
            </table>
        </td>
    </tr>
	<tr><td>Collector running since</td><td id="clr_date"></td></tr>
	<tr><td>&nbsp;</td><td class="infoRefreshCollector"><a href="javascript:refreshCollector();">refresh</a>&nbsp;<img src='img/info.gif' alt='info' title='info'></td></tr>

</table>

<br/>

<%  Object adminToken = session.getAttribute(Util.ADMIN_TOKEN);
    boolean isAdmin = false;
    if(adminToken != null && adminToken instanceof Boolean && (Boolean)adminToken == true){
        isAdmin = true;
    }
%>

<h2>Registered mongod's and databases</h2>
Status of <span id="clr_refresh_quantity"></span> mongod's refreshed at: <span id="clr_refresh_ts"></span>
<form name="input" action="app" method="get">
	<br/>
	<div id="cols">Toggle columns: </div>

	<table  id="main" class="display" cellspacing="0" width="100%" align="top" cellpadding="10">
		<thead>
		<tr id="tableHeader"></tr>
		</thead>
		<tfoot>
		<tr id="tableFooter"></tr>
		</tfoot>
	</table>
    <div class="actions">
        <h2>Actions&nbsp;<img id="actionsLink" src="res/showhide.png" title="show/hide" alt="show/hide"/></h2>
        <table id="actionsTable" align="top" border="1" cellpadding="10">
            <tr>
                <td class='infoRefresh'><a href="javascript:parallelAction('refresh');">refresh</a>&nbsp;<img src='img/info.gif' alt='info' title='info'></td>
                <td class='infoAnalyse'><a href="javascript:singleAction('analyse');">analyse</a>&nbsp;<img src='img/info.gif' alt='info' title='info'></td>
                <td class='infoCurrentOps'><a href="javascript:singleAction('cops');">current ops</a>&nbsp;<img src='img/info.gif' alt='info' title='info'></td>
                <td class='infoListDbs'><a href="javascript:singleAction('lsdbs');">list db.collections</a>&nbsp;<img src='img/info.gif' alt='info' title='info'></td>
                <td class='infoIdxAccStats'><a href="javascript:singleAction('idxacc');">index access stats</a>&nbsp;<img src='img/info.gif' alt='info' title='info'></td>
				<td class='infoHostinfo'><a href="javascript:singleAction('hostinfo');">host info</a>&nbsp;<img src='img/info.gif' alt='info' title='info'></td>
                <%  if(isAdmin){ %>
                <td>
                    <table>
                        <tr>
                            <td colspan="2" class='infoCollecting'>Collecting&nbsp;<img src='img/info.gif' alt='info' title='info'></td>
                        </tr>
                        <tr>
                            <td><a href="javascript:parallelAction('start');">start</a></td>
                            <td><a href="javascript:parallelAction('stop');">stop</a></td>
                        </tr>
                    </table>
                </td>
                <td class='infoSlowMs'>
                    <a href="javascript:parallelAction('slowms');">set slowMs </a><input id="slowms" type="text" maxlength="10" size="10">&nbsp;<img src='img/info.gif' alt='info' title='info'><br/>
                    <small>negative values stop, positive values start profiling</small>
                </td>
                <%}%>
            </tr>
			<tr>
				<td colspan="2">&nbsp;</td>
				<td colspan="4">
					run command against:
					<input type="radio" name="mode" value="dbs" <%=!"mongod".equals(request.getParameter("mode"))?"checked":""%>> dbs of selected node(s)
					<input type="radio" name="mode" value="mongod" <%="mongod".equals(request.getParameter("mode"))?"checked":""%>> selected node(s)
				</td>
				<%  if(isAdmin){ %>
				<td colspan="2">&nbsp;</td>
				<%}%>
			</tr>
        </table>
     </div>
	<br/>
</form>
<br>
Last log messages:<br>
<textarea id="weblog" name="weblog" rows="10" cols="160"></textarea><br/>

<%  if(isAdmin){ %>
        <form name="configForm" action="app" method="post" class="infoConfig">
            <input type="submit" value="upload new config">&nbsp;<img src='img/info.gif' alt='info' title='info'><br/>
            <textarea id="config" name="config" rows="10" cols="120"></textarea><br/>
        </form>
<%}%>


<span id="infoOpsCountContent" style="display:none">"old" means operations from removed or changed servers due to uploaded configuration changes since (re)start of the webapp</span>
<span id="infoRefreshCollectorContent" style="display:none">Get and show latest data of the collector.</span>
<span id="infoRefreshContent" style="display:none">Get and show latest data of the selected node(s).<br><b>Attention</b>: Requires to request each selected node. If many nodes are selected, <b>use with care!</b></span>
<span id="infoAnalyseContent" style="display:none">Open the analyse page, preset with the selected node(s) for the last 24 hours.</span>
<span id="infoCurrentOpsContent" style="display:none">Show all current running operations of the selected node(s).</span>
<span id="infoListDbsContent" style="display:none">Show all databases and their collections of the selected node(s).</span>
<span id="infoIdxAccStatsContent" style="display:none">Show index access statistics of all databases and their collections of the selected node(s).<br><b>Attention</b>: May slow down the database system, especially if the database system has many collections and indexes!<br><b>Use with care!</b></span>
<span id="infoHostinfoContent" style="display:none">Show info about the host.<br>Choose whether the command should be run against the DBS of the selected nodes (i.e. only router) or against all selected nodes (mongod's).</span>
<span id="infoCollectingContent" style="display:none">Start or stop collecting slow operations of the selected node(s).</span>
<span id="infoSlowMsContent" style="display:none">Set the threshold in milliseconds for operations to be profiled. Low slowMs values may slow down both the profiled mongod('s) and also the collector because more slow operations need to be read and written.<br>This value is set for <b>all</b> databases for a given mongod instance.<br>Negative values stop, positive values start profiling. A value of 0 will result in profiling <b>all</b> operations.</span>
<span id="infoConfigContent" style="display:none">Apply a new configuration. It resolves also all members of all defined database systems. So use this functionality also if shards or replica set servers have been removed or added.<br>The uploaded configuration is not persisted server side and will be lost upon webapp restart.</span>

<%@ include file="buildInfo.jsp" %>
</body>
</html>
