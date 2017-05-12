package ua.training.controller.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import ua.training.controller.validator.DateTimeValidator;
import ua.training.controller.validator.Validator;
import ua.training.exception.ApplicationException;
import ua.training.model.entities.Application;
import ua.training.model.entities.ProblemScale;
import ua.training.model.entities.TypeOfWork;
import ua.training.model.entities.person.User;
import ua.training.model.service.ApplicationService;
import ua.training.model.service.TypeOfWorkService;
import ua.training.model.service.impl.ApplicationServiceImpl;
import ua.training.model.service.impl.TypeOfWorkServiceImpl;

import java.beans.PropertyEditorSupport;
import java.time.LocalDateTime;
import java.util.List;

import static ua.training.controller.Attributes.*;

@Controller
@RequestMapping("/rest")
public class ApplicationController {

    private static final String TENANT_APPLICATIONS_PATH
            = "redirect:/rest/user/application";

    private Validator dateTimeValidator = new DateTimeValidator();

    private ApplicationService applicationService;
    private TypeOfWorkService typeOfWorkService;

    @Autowired
    public ApplicationController(ApplicationService applicationService,
                                 TypeOfWorkService typeOfWorkService) {
        this.applicationService = applicationService;
        this.typeOfWorkService = typeOfWorkService;
    }

    @GetMapping("/application")
    public String getAllApplication(@SessionAttribute User user, Model model) {
        model.addAttribute(STATUS_NEW, Application.Status.NEW);
        model.addAttribute(APPLICATIONS,
                applicationService.getAllApplications(user.getRole()));
        return "application/applications";
    }

    @PostMapping("/application")
    public String addApplication(@RequestParam TypeOfWork typeOfWork,
                                 @RequestParam ProblemScale problemScale,
                                 @RequestParam LocalDateTime dateTime,
                                 @RequestParam String address,
                                 @SessionAttribute("user") User sessionUser,
                                 Model model) {
        String pageToGo;
        try {
            Application application = new Application.Builder()
                    .setTenant(new User.Builder()
                            .setId(sessionUser.getId())
                            .build())
                    .setTypeOfWork(typeOfWork)
                    .setProblemScale(problemScale)
                    .setDesiredTime(dateTime)
                    .setAddress(address)
                    .build();
            applicationService.createNewApplication(application);
            pageToGo = TENANT_APPLICATIONS_PATH;
        } catch (ApplicationException e) {
            pageToGo = getPageToGo(model, e);
        }
        return pageToGo;
    }

    private LocalDateTime getLocalDateTime(String paramDateTime) {
        if (!paramDateTime.isEmpty()) {
            dateTimeValidator.validate(paramDateTime);
            return LocalDateTime.parse(paramDateTime);
        }
        return null;
    }

    private String getPageToGo(Model model,
                               ApplicationException e) {
        if (e.isUserMessage()) {
            model.addAttribute(TYPE_OF_WORK,
                    typeOfWorkService.getAllTypesOfWork());
            model.addAttribute(PROBLEM_SCALE, ProblemScale.values());
            model.addAttribute(MESSAGE, e.getUserMessage());
            List<String> parameters = e.getParameters();
            if (e.getParameters().size() != 0) {
                model.addAttribute(PARAMS, parameters);
            }
            return "application/new_application";
        } else {
            throw e;
        }
    }

    @GetMapping("/user/application")
    public String getUserApplication(@SessionAttribute User user, Model model) {
        model.addAttribute(STATUS_NEW, Application.Status.NEW);
        model.addAttribute(APPLICATIONS,
                applicationService.getApplicationsByUserId(user.getId()));
        return "application/tenant_applications";
    }

    @GetMapping("/new_application")
    public String getNewApplicationPage(Model model) {
        model.addAttribute(TYPE_OF_WORK, typeOfWorkService.getAllTypesOfWork());
        model.addAttribute(PROBLEM_SCALE, ProblemScale.values());
        return "application/new_application";
    }

    @PostMapping("/application/{applicationId}/delete")
    public String deleteApplication(@PathVariable int applicationId,
                                    @SessionAttribute User user) {
        applicationService.deleteApplication(applicationId, user.getId());
        return "redirect:/rest/user/application";
    }

    @InitBinder
    public void localDateTimeBinder(WebDataBinder binder) {
        binder.registerCustomEditor(LocalDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String s) throws IllegalArgumentException {
                LocalDateTime localDateTime = null;
                if (!s.isEmpty()) {
                    dateTimeValidator.validate(s);
                    localDateTime = LocalDateTime.parse(s);
                }
                setValue(localDateTime);
            }
        });
    }
}
