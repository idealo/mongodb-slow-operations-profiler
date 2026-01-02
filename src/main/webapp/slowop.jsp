<%@ page import="org.bson.Document" %>
<%@ page import="org.bson.json.JsonWriterSettings" %>
<%@ page import="java.util.List" %>
<!DOCTYPE html>
<html>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%
    final List<Document> slowOps = (List<Document>)request.getAttribute("slowop");
	final boolean deleted = Boolean.valueOf(String.valueOf(request.getAttribute("deleted")));
%>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<link rel="shortcut icon" type="image/x-icon" href="img/mdb.ico">
	<link rel="icon" type="image/png" href="img/mdb.png">
	<link rel="stylesheet" type="text/css" href="css/bootstrap4.6.2.min.css">

	<title>slow operation example</title>

	</head>
<style>
	body{
		font-family:auto;
	}
</style>
<body>
<div class="container">
	<h2>slow operation example</h2>
	<%if(slowOps.isEmpty()){
		if(deleted){
			out.print("<p><div class=\"alert alert-success\" role=\"alert\">The slow operation example document has been successfully deleted.</div>");
		}else{
			out.print("<p><div class=\"alert alert-warning\" role=\"alert\">The slow operation document with this fingerprint could not be found in the database!<br>You should tick <b>more</b> checkboxes such as 'Label', 'Database', 'Collection', 'Operation', 'Queried fields', 'Sorted fields' and/or 'Projected fields' in the 'Group by' section to get an existing slow operation example document which matches the query shape for a query executed within the same database and collection.</div>");
		}

	}else{%>
	<span>For more complex queries it may help to see an example of the profiled operation to understand better how to interpret the shown fields.<br/>
		The shown document is a profiled slow operation of the same dbs label, database, and collection which has the same query shape as the clicked one which is defined by:
		<ul>
			<li>Operation</li>
			<li>Queried fields</li>
			<li>Sorted fields</li>
			<li>Projected fields</li>
		</ul>
		You may <a href="<%=request.getContextPath()%>/slowop?fp=<%=request.getParameter("fp")%>&del=1">delete this</a> example document e.g. if indexes have changed and you want to reflect them in this slow operations example document.<br>
		A new example document will be re-created as soon as such an operation is collected again.
	</span>
	<pre><%JsonWriterSettings.Builder settingsBuilder = JsonWriterSettings.builder().indent(true);
		for(Document doc : slowOps){
			out.print(doc.toJson(settingsBuilder.build()));
		}
	}%></pre>
</div>

<%@ include file="buildInfo.jsp" %>
</body>
</html>
