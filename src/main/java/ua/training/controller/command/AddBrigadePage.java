package ua.training.controller.command;

import ua.training.model.service.WorkerService;
import ua.training.model.service.impl.WorkerServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddBrigadePage implements Command {

    private static final String ADD_BRIGADE_JSP_PATH
            = "/WEB-INF/view/add_brigade.jsp";

    private static final String APPLICATION_ID_REGEXP
            = "(?<=/application/)[\\d]+(?=/add_brigade)";

    private WorkerService workerService = WorkerServiceImpl.getInstance();

    @Override
    public String execute(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {
        Pattern pattern = Pattern.compile(APPLICATION_ID_REGEXP);
        Matcher matcher = pattern.matcher(request.getRequestURI());
        if (matcher.find()) {
            int applicationId = Integer.parseInt(matcher.group());
            request.setAttribute("application", applicationId);
            request.setAttribute("workers", workerService.getAllWorkers());
            return ADD_BRIGADE_JSP_PATH;
        } else {
            throw new RuntimeException("Invalid URL");
        }
    }
}
