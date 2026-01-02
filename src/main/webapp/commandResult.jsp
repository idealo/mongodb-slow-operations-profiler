<%@ page import="de.idealo.mongodb.slowops.dto.CommandResultDto" %>
<!DOCTYPE html>
<html>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%
    final CommandResultDto commandResult = (CommandResultDto)request.getAttribute("commandResult");
%>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<link rel="shortcut icon" type="image/x-icon" href="img/mdb.ico">
	<link rel="icon" type="image/png" href="img/mdb.png">
	<script src="js/dojo.js"></script>
	<script src="js/jquery-3.6.4.min.js"></script>
	<script src="js/jquery.dataTables-1.13.6.min.js"></script>
 	<script src="js/jquery.dataTables.sum.js"></script>
 	<script src="js/jquery.number.min.js"></script>
	<script src="js/jquery-ui-1.13.2.min.js"></script>
 	<link rel="stylesheet" type="text/css" href="css/jquery.dataTables.css">
 	<link rel="stylesheet" type="text/css" href="css/jquery-ui.css">
	<title>command result page</title>
		<script>

            var header = <%= commandResult.getTableHeaderAsDatatableJson() %>;
            var ds = <%= commandResult.getTableBodyAsJson() %>;

            $(document).ready(function() {
                $('#main').DataTable( {
                    data: ds.content,
                    columns: header.content,
                    "columnDefs": [
                        {
                            "render": function ( data, type, row ) {
                                if(data) {
                                    return "<pre>" + JSON.stringify(JSON.parse(data), undefined, 4) + "</pre>";
                                }
                                return "";
                            }<%= commandResult.getJsonFormattedColumnAsDatatableCode() %>
                        }
                    ]

                } );
            } );



		</script>
        <style>
            a {
                color: #3174c7;
                cursor: pointer;
                text-decoration: none;
            }
            a:hover {
                text-decoration:underline;
            }
        </style>
	</head>
<body>
<h1>Command result page</h1>



	<table  id="main" class="display" cellspacing="0" width="100%" align="top" cellpadding="10"></table>

<%@ include file="buildInfo.jsp" %>
</body>
</html>
