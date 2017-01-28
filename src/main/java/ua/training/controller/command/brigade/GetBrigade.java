package ua.training.controller.command.brigade;

import ua.training.controller.command.Command;
import ua.training.model.service.BrigadeService;
import ua.training.model.service.impl.BrigadeServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static ua.training.controller.Attributes.BRIGADE;

public class GetBrigade implements Command {

    private static final String BRIGADE_JSP_PATH = "/WEB-INF/view/brigade.jsp";

    private BrigadeService brigadeService = BrigadeServiceImpl.getInstance();

    @Override
    public String execute(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {
        int brigadeId = Integer.parseInt(getBrigadeIdFromRequest(request));
        return brigadeService.getBrigadeById(brigadeId)
                .map(brigade -> {
                    request.setAttribute(BRIGADE, brigade);
                    return BRIGADE_JSP_PATH;
                })
                .orElseThrow(() -> new RuntimeException("Resource not found!"));
    }

    private String getBrigadeIdFromRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        uri = uri.substring(uri.lastIndexOf('/') + 1);
        return uri;
    }
}
