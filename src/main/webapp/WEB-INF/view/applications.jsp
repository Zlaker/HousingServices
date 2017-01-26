<%@ include file="header.jsp"%>
<div class="row">
    <div class="col-lg-offset-3 col-lg-6">
        <h2 align="center">Applications</h2>
    </div>
</div>
<div class="row">
    <div class="col-lg-offset-1 col-lg-10">
        <c:choose>
            <c:when test="${requestScope.applications.size() != 0}">
                <table class="table table-striped" align="center">
                    <thead>
                    <tr>
                        <th>N</th>
                        <th>Type of task</th>
                        <th>Scale of problem</th>
                        <th>Desired date</th>
                        <th>Status</th>
                        <th>Consideration</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="application" items="${requestScope.applications}">
                       <tr>
                            <td>${application.id}</td>
                            <td>${application.typeOfWork.description}</td>
                            <td>${application.scaleOfProblem}</td>
                            <td>${application.desiredTime}</td>
                            <td>${application.status}</td>
                            <td>
                                <a class="btn btn-default" href="/rest/application/${application.id}/add_brigade" role="button">Form a brigade</a>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:when>
            <c:otherwise>
                There no any applications!
            </c:otherwise>
        </c:choose>
    </div>
</div>
<%@ include file="footer.jsp"%>

