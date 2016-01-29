<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:useBean id="wikipage" scope="request" type="wiki.data.Page" />
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>
	<p>
	Are you sure you want to publish this page?
	</p>
	
	<form method="post">
	   <input type="submit" name="publish-button" value="Publish" />
	   <input type="submit" name="cancel-button" value="Cancel" />
	   <input type="hidden" name="name" value="${wikipage.name}" />
	</form>
</body>
</html>