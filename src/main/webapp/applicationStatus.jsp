<%@ page import="de.idealo.mongodb.slowops.util.Util" %>
<!DOCTYPE html>
<html>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
    <link rel="shortcut icon" type="image/x-icon" href="img/mdb.ico">
    <link rel="icon" type="image/png" href="img/mdb.png">
	<script src="js/dojo.js"></script>
	<script src="js/jquery-3.6.4.min.js"></script>
	<script src="js/jquery-ui-1.13.2.min.js"></script>
	<script src="js/popper.min.js"></script>
	<script src="js/bootstrap4.6.2.min.js"></script>
	<script src="js/jquery.dataTables-1.13.6.min.js"></script>
	<script src="js/jquery.dataTables.sum.js"></script>
	<script src="js/jquery.number.min.js"></script>
    <link rel="stylesheet" type="text/css" href="css/jquery.dataTables.css">
	<link rel="stylesheet" type="text/css" href="css/jquery-ui.css">
	<link rel="stylesheet" type="text/css" href="css/bootstrap4.6.2.min.css">
	<link rel="stylesheet" type="text/css" href="css/custom-tooltips.css">

    <title>application status of slow-operations-profiler</title>
		<script>
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
                    {"visible":true, "name": "MaxMB"},
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
                        { "data": "systemProfileMaxSizeInBytes", "render": function ( data, type, full, meta ) { return data==0?0:Math.round(data/1024/1024) } },
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
						if(api.column( 16 ).data().length) {//don't compute sum on initial draw before data is loaded
							// Total over current page so that search results reflect the sum
							var total1 = api.column( 16, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							var total2 = api.column( 17, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							var total3 = api.column( 18, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							var total4 = api.column( 19, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							var total5 = api.column( 20, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							var total6 = api.column( 21, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							var total7 = api.column( 22, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							var total8 = api.column( 23, { page: 'current'} ).data().reduce( function (a, b) {return intVal(a) + intVal(b);});
							// Update footer
							$( api.column( 16 ).footer() ).html('Sum #SlowOps: '+ $.number(total1, 0, ".", ","));
							$( api.column( 17 ).footer() ).html('Sum #10sec: '+ $.number(total2, 0, ".", ","));
							$( api.column( 18 ).footer() ).html('Sum #1Min: '+ $.number(total3, 0, ".", ","));
							$( api.column( 19 ).footer() ).html('Sum #10Min: '+ $.number(total4, 0, ".", ","));
							$( api.column( 20 ).footer() ).html('Sum #30Min: '+ $.number(total5, 0, ".", ","));
							$( api.column( 21 ).footer() ).html('Sum #1Hour: '+ $.number(total6, 0, ".", ","));
							$( api.column( 22 ).footer() ).html('Sum #12Hours: '+ $.number(total7, 0, ".", ","));
							$( api.column( 23 ).footer() ).html('Sum #1Day: '+ $.number(total8, 0, ".", ","));
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
						$('#clr_hosts').html(hostsString.replace(/,/g, "<br>"));
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

                function toggleColumn(columnIndex){
                    // Get the column API object
                    var column = mainTable.column(columnIndex);
                    // Toggle the visibility
                    var isVisible = column.visible();
                    $("#cols_" + column[0]).removeClass("toggle-vis-" + isVisible).addClass("toggle-vis-" + !isVisible);
                    column.visible(!isVisible);
                }

                function makeColumnGroup(label, columnIndexes){
                    $("#cols").append("<a id='cols_" + label + "' class='toggle-vis-true' data-column='" + label + "'>" + label + "</a> - ");
                    $("#cols_" + label).on('click', function (e) {
                        e.preventDefault();
                        columnIndexes.forEach(function(colIndex){
                            toggleColumn(colIndex);
                        });
                    });
                }

                makeColumnGroup("Specs", [5,6,7,8]);
                makeColumnGroup("LastSlowOps", [15,16,17,18,19,20,21,22,23]);






				$('#checkall').click(function () {
					$(':checkbox', mainTable.rows().nodes()).prop('checked', this.checked);
				});

                (function(){
                    var infoMap = {
                        "infoSlowMs": "#infoSlowMsContent",
                        "infoConfig": "#infoConfigContent",
                        "infoOpsCount": "#infoOpsCountContent",
                        "infoRefreshCollector": "#infoRefreshCollectorContent",
                        "infoRefresh": "#infoRefreshContent",
                        "infoAnalyse": "#infoAnalyseContent",
                        "infoCurrentOps": "#infoCurrentOpsContent",
                        "infoCurrentOpsNs": "#infoCurrentOpsNsContent",
                        "infoCurrentOpsAll": "#infoCurrentOpsAllContent",
                        "infoDBStat": "#infoDBStatContent",
                        "infoCollStat": "#infoCollStatContent",
                        "infoIdxAccStats": "#infoIdxAccStatsContent",
                        "infoHostinfo": "#infoHostinfoContent",
                        "infoProfiling": "#infoProfilingContent",
                        "infoCollecting": "#infoCollectingContent",
                        "infoCommand": "#infoCommandContent"
                    };

                    $.each(infoMap, function(cls, contentSelector){
                        var $imgs = $("." + cls + " img");
                        $imgs.removeAttr('title').each(function(){
                            $(this).tooltip({
                                html: true,
                                container: 'body',
                                placement: 'auto',
                                boundary: 'window',
                                sanitize: false,
                                title: function(){ return $(contentSelector).html(); }
                            });
                        });
                    });
                })();



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

            function formatDateUtc(dateObject) {
                var d = new Date(dateObject);
                var day = getTwoDigits(d.getUTCDate());
                var month = getTwoDigits(d.getUTCMonth() + 1);
                var year = d.getUTCFullYear();
                var h = getTwoDigits(d.getUTCHours());
                var m = getTwoDigits(d.getUTCMinutes());
                var s = getTwoDigits(d.getUTCSeconds());
                return year + "/" + month + "/" + day + " " + h + ":" + m + ":" + s;
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

                    var nowLocal = new Date();
                    var nowUtc = new Date(nowLocal.getTime() + (nowLocal.getTimezoneOffset() * 60000));
                    var toDate = new Date(nowUtc.getTime() + (1 * 60 * 60 * 1000));
                    var fromDate = new Date(nowUtc.getTime() - (1 * 60 * 60 * 1000));

                    window.open("<%=request.getContextPath()%>/gui?fromDate=" + encodeURIComponent(formatDateUtc(fromDate)) + "&toDate=" + encodeURIComponent(formatDateUtc(toDate))
                        + "&lbl=" + encodeURIComponent(lbl.values())
                        + "&rs=" + encodeURIComponent(rs.values())
                        + "&adr=" + encodeURIComponent(adr.values())
                        + "&db=" + encodeURIComponent(db.values())
                        + "&col=" + encodeURIComponent(col.values())
                        + "&byLbl=lbl"
                        + "&byDb=db"
                        + "&byCol=col"
                        + "&byOp=op"
                        + "&byFields=fields"
                        + "&bySort=sort"
                        + "&byProj=proj"
                        + "&resolution=minute"
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
                            $('#clr_hosts').html(hostsString.replace(/,/g, "<br>"));
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
        body{
            font-family:auto;
        }

        table.dataTable td {
            white-space:nowrap;
        }

		.toggle-vis-true { color: #3174c7 !important; }
		.toggle-vis-false { color: #6c6c6c !important; }
		a {
			color: #3174c7 !important;
			cursor: pointer;
			text-decoration: none;
		}
		a:hover {
			text-decoration:underline !important;
		}

        .opstable td, .opstable th{
            border: 1px solid black;
            text-align: right;
            padding: 5px;
        }

        .dropdown-no-item {
            margin-left: .5em;
            margin-right: .5em;
            white-space:nowrap;
        }


        .navbar-nav li:hover > ul.dropdown-menu {
            display: block;
        }
        .dropdown-submenu {
            position:relative;
        }
        .dropdown-submenu>.dropdown-menu {
            top: 0;
            left: 100%;
            margin-top:-6px;
        }

        /* rotate caret on hover */
        .dropdown-menu > li > a:hover:after {
            text-decoration: underline;
            transform: rotate(-90deg);
        }

        /* move menu (but not sub-menu) higher to keep the menu open when mouse moves too slowly down to it */
        .nav-item > .dropdown-menu{
            top: 90% !important;
        }

    </style>
<body>
<h2>Application Status </h2>
<div id="accordion">
    <div class="card">
        <div class="card-header" id="headingOne">
            <h5 class="mb-0">
                <button class="btn btn-link" data-toggle="collapse" data-target="#collapseTwo" aria-expanded="true" aria-controls="collapseTwo">
                    Collector
                </button>
            </h5>
        </div>
        <div id="collapseTwo" class="collapse show" aria-labelledby="headingOne" data-parent="#accordion">
            <div class="card-body">

                <table  align="top" >
                    <tr><td>Access point(s)</td><td id="clr_hosts"></td></tr>
                    <tr><td>Namespace</td><td><span id="clr_db"></span>.<span id="clr_col"></span></tr>
                    <tr>
                        <td valign="top">Number of ops</td>
                        <td>
                            <table class="opstable" style="border-collapse:collapse;">
                                <tr>
                                    <td class="infoRefreshCollector"><a href="javascript:refreshCollector();">refresh</a>&nbsp;<img src='img/info.gif' alt='info' title='info'></td>
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
                    <tr><td>Running since</td><td id="clr_date"></td></tr>
                </table>
            </div>
        </div>
    </div>

<%  Object adminToken = session.getAttribute(Util.ADMIN_TOKEN);
    boolean isAdmin = false;
    if(adminToken instanceof Boolean && (Boolean) adminToken == true){
        isAdmin = true;
    }
%>


    <div class="card">
        <div class="card-header" id="headingTwo">
            <h5 class="mb-0">
                <button class="btn btn-link" data-toggle="collapse" data-target="#collapseOne" aria-expanded="true" aria-controls="collapseOne">
                Registered mongoD's and databases
                </button>
            </h5>
        </div>
        <div id="collapseOne" class="collapse show" aria-labelledby="headingTwo" data-parent="#accordion">
            <div class="card-body">

                Status of <span id="clr_refresh_quantity"></span> mongoD's refreshed at: <span id="clr_refresh_ts"></span>
                <form name="input" action="app" method="get">
                    <br/>

                    <nav class="navbar navbar-expand-lg navbar-light bg-light sticky-top">
                        <h3>Actions </h3>
                        <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
                            <span class="navbar-toggler-icon"></span>
                        </button>

                        <div class="collapse navbar-collapse" id="navbarNavDropdown">
                            <ul class="navbar-nav">
                                 <li class="nav-item dropdown"><a class="nav-link dropdown-toggle" href="#" id="navbarDropdownMenuLink" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">command</a>
                                    <ul class="dropdown-menu" aria-labelledby="navbarDropdownMenuLink">
                                        <li class="dropdown-no-item infoCommand"> run against: <img src='img/info.gif' alt='info' title='info'><br>
                                            <input class="dropdown-no-item" type="radio" name="mode" value="dbs" <%=!"mongod".equals(request.getParameter("mode"))?"checked":""%>>DBS (e.g. router)<br>
                                            <input class="dropdown-no-item" type="radio" name="mode" value="mongod" <%="mongod".equals(request.getParameter("mode"))?"checked":""%>>mongoD (e.g. secondary)
                                        </li>
                                        <li class="infoDBStat"><a class="dropdown-item" href="javascript:singleAction('dbstat');">db stats <img src='img/info.gif' alt='info' title='info'></a></li>
                                        <li class="infoCollStat"><a class="dropdown-item" href="javascript:singleAction('collstat');">collection stats <img src='img/info.gif' alt='info' title='info'></a></li>
                                        <li class="infoIdxAccStats"><a class="dropdown-item" href="javascript:singleAction('idxacc');">index access stats <img src='img/info.gif' alt='info' title='info'></a></li>
                                        <li class="dropdown-submenu infoCurrentOps">
                                            <a class="dropdown-item dropdown-toggle" href="#">current ops <img src='img/info.gif' alt='info' title='info'></a>
                                            <ul class="dropdown-menu">
                                                <li class="infoCurrentOpsNs"><a class="dropdown-item" href="javascript:singleAction('copsns');">selected db's <img src='img/info.gif' alt='info' title='info'></a></li>
                                                <li class="infoCurrentOpsAll"><a class="dropdown-item" href="javascript:singleAction('cops');">all db's <img src='img/info.gif' alt='info' title='info'></a></li>
                                            </ul>
                                        </li>
                                        <li class="infoHostinfo"><a class="dropdown-item" href="javascript:singleAction('hostinfo');">host info <img src='img/info.gif' alt='info' title='info'></a></li>
                                    </ul>
                                </li>
                                <%  if(isAdmin){ %>
                                <li class="nav-item dropdown infoProfiling">
                                    <a class="nav-link dropdown-toggle" href="http://example.com" id="navbarDropdownMenuProfiling" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                        profile <img src='img/info.gif' alt='info' title='info'>
                                    </a>
                                    <ul class="dropdown-menu" aria-labelledby="navbarDropdownMenuProfiling">
                                        <li class="dropdown-no-item infoSlowMs">slow ms <input id="slowms" type="text" maxlength="10" size="10">&nbsp;<img src='img/info.gif' alt='info' title='info'></li>
                                        <li><a class="dropdown-item" href="javascript:parallelAction('pstart');">start</a></li>
                                        <li><a class="dropdown-item" href="javascript:parallelAction('pstop');">stop</a></li>
                                    </ul>
                                </li>
                                <li class="nav-item dropdown infoCollecting">
                                    <a class="nav-link dropdown-toggle" href="http://example.com" id="navbarDropdownMenuCollecting" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                        collect <img src='img/info.gif' alt='info' title='info'>
                                    </a>
                                    <ul class="dropdown-menu" aria-labelledby="navbarDropdownMenuCollecting">
                                        <li><a class="dropdown-item" href="javascript:parallelAction('cstart');">start</a></li>
                                        <li><a class="dropdown-item" href="javascript:parallelAction('cstop');">stop</a></li>
                                    </ul>
                                </li>
                                <% } %>
                                <li class="nav-item infoAnalyse"><a class="nav-link" href="javascript:singleAction('analyse');">analyse <img src='img/info.gif' alt='info' title='info'></a></li>
                                <li class="nav-item infoRefresh"><a class="nav-link" href="javascript:parallelAction('refresh');">refresh <img src='img/info.gif' alt='info' title='info'></a></li>

                            </ul>
                        </div>
                    </nav>
                    <p>
                    <div id="cols">Toggle columns: </div>

                    <table  id="main" class="display" cellspacing="0" width="100%" align="top" cellpadding="10">
                        <thead>
                        <tr id="tableHeader"></tr>
                        </thead>
                        <tfoot>
                        <tr id="tableFooter"></tr>
                        </tfoot>
                    </table>
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
            </div>
    </div>
</div>

<span id="infoOpsCountContent" style="display:none">"old" means operations from removed or changed servers due to uploaded configuration changes since (re)start of the webapp</span>
<span id="infoRefreshCollectorContent" style="display:none">Get and show latest data of the collector.</span>
<span id="infoRefreshContent" style="display:none">Get and show latest data of the selected row(s).<br><b>Attention</b>: Requires to request all databases of the selected rows in parallel, which may add stress to the app server.</span>
<span id="infoAnalyseContent" style="display:none">Open the analysis page, preset with the database(s) of the selected row(s) in order to analyze collected slow operations.</span>
<span id="infoCurrentOpsContent" style="display:none">Show all current running operations of the DBS or the mongoD of the selected row(s).</span>
<span id="infoCurrentOpsNsContent" style="display:none">Show current ops of the <b>selected</b> databases of the selected row(s).<br>If there are many current operations, the result may exceed the 16 MB BSON document limit and thus can not be displayed.</span>
<span id="infoCurrentOpsAllContent" style="display:none">Show current ops of <b>all</b> databases belonging to the DBS or mongoD of the selected row(s).<br>If there are many current operations, the result may exceed the 16 MB BSON document limit and thus can not be displayed.</span>
<span id="infoDBStatContent" style="display:none">Show statistics of the databases of the selected row(s).</span>
<span id="infoCollStatContent" style="display:none">Show statistics of the collections of the databases of the selected row(s).</span>
<span id="infoIdxAccStatsContent" style="display:none">Show index access statistics of the databases and their collections of the selected row(s).<br><b>Attention</b>: May slow down the database system, especially if the selected database(s) has/have many collections and indexes!<br><b>Use with care!</b></span>
<span id="infoHostinfoContent" style="display:none">Show info about the host of the DBS or the mongoD of the selected row(s).</span>
<span id="infoProfilingContent" style="display:none">Start or stop profiling.<br>Profiled slow operations are stored in the capped collection "system.profile" of the databases(s) of the selected row(s).<br>Collect them in order to analyze them. </span>
<span id="infoCollectingContent" style="display:none">Start or stop collecting the already profiled slow operations from the databases(s) of the selected row(s).</span>
<span id="infoSlowMsContent" style="display:none">Set the threshold in milliseconds for operations to be profiled. A low threshold may slow down both the profiled mongoD('s) and also the collector because more slow operations need to be read and written.<br>This value is set for <b>all</b> databases for a given mongoD instance. Although slow ops are only written to the capped collection "system.profile" of the respective database, the slow operations of all databases of the respective DBS are written to the log file which therefore can grow very fast (unless logging has been disabled).<br>A value of 0 will result in profiling (and logging) <b>all</b> operations, so <b>use with care!</b></span>
<span id="infoConfigContent" style="display:none">Apply a new configuration. It resolves also all members of all defined database systems. So use this functionality also if shards or replica set servers have been removed or added.<br>The uploaded configuration is not persisted server side and will be lost upon webapp restart.</span>
<span id="infoCommandContent" style="display:none">Choose whether the command should be executed against the DBS of the selected row(s), i.e. router, or against the mongoD('s) of all selected row(s).</span>

<%@ include file="buildInfo.jsp" %>
</body>
</html>
