<!DOCTYPE html>
<html>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"
    import="java.util.*,de.idealo.mongodb.slowops.dto.CollectorDto" %>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>collector status</title>
</head>
<%
    final CollectorDto collectorDto = (CollectorDto)request.getAttribute("collectorDto");
%>
<body>
<a href="gui">slow ops GUI</a>
<form name="input" action="status" method="get">
	<table  align="top" cellpadding="10">
		<tr>
			<td colspan="2"><strong>Collector status</strong></td>
		</tr>
		<tr>
			<td valign="top"><strong>Reads</strong></td>
			<td valign="top">
			<%
			final Map<String, Long> map = collectorDto.getNumberOfReads();
			for (String key : map.keySet()) {%>
	            <%= key%> = <%= map.get(key)%><br/>
	        <%}%>
			</td>
		</tr>
		<tr>
			<td valign="top"><strong>Writes</strong></td>
			<td valign="top">
			    <%= collectorDto.getNumberOfWrites()%><br/>
			</td>
		</tr>
		<tr>
			<td><input type="submit" value="Refresh"></td>
		</tr>
	</table>	
</form>
</body>
</html>