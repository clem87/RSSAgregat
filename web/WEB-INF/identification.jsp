<%-- 
    Document   : identification
    Created on : 28 août 2013, 17:52:13
    Author     : clem
--%>

<%@page import="rssagregator.dao.DAOFactory"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>GEOMEDIA - Identification</title>
        <link href="/RSSAgregate/ress/style.css" type="text/css" rel="stylesheet" media="all"/>
    </head>
    <body>
            <div id="banner">
                <a href ="/RSSAgregate/index">
                    <%
                        if (DAOFactory.getInstance().getDAOConf().getConfCourante().getMaster()) {
                            out.println("<img src=\"/RSSAgregate/ress/img/logo_mastervert.png\"/>");
                            request.setAttribute("master", true);
                        } else {
                            out.println("<img src=\"/RSSAgregate/ress/img/logo_masterrouge.png\"/>");
                            request.setAttribute("master", false);
                        }

                    %>
                </a>
            </div>
                    <div class="post">

            <c:choose>
                <c:when test="${not empty redirmap}">
                    <c:import url="/WEB-INF/redirJspJavascriptPart.jsp" />
                </c:when>
                <c:when test="${empty redirmap}">


                    <h1>Vous devez vous identifier !</h1>
                    <p>
                        Pour accéder à la page 
                    </p>
                   
                    <form method="POST" action="/RSSAgregate/ident/login">
                        <span class="erreur">${err}</span>
                        <label>Email : </label>
                        <input type="text" name="mail"/><br />
                        <label>Mot de passe : </label>
                        <input type="password" name="pass" /><br />
                        <input type="hidden" name="askurl" value="${askurl}" />
                        <input type="submit"/>
                    </form>
                </c:when>
            </c:choose>
                    </div>
       
    </body>
</html>
