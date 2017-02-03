package ua.training.model.service.impl;

import org.apache.log4j.Logger;
import ua.training.exception.AccessForbiddenException;
import ua.training.exception.ResourceNotFoundException;
import ua.training.model.dao.*;
import ua.training.model.entities.Application;
import ua.training.model.entities.TypeOfWork;
import ua.training.model.entities.person.User;
import ua.training.model.service.ApplicationService;

import java.util.List;
import java.util.function.Supplier;

public class ApplicationServiceImpl implements ApplicationService {

    private static final String EXCEPTION_USER_WITH_ID_NOT_FOUND
            = "User with id = %d not found";
    private static final String EXCEPTION_TYPE_OF_WORK_WITH_ID_NOT_FOUND
            = "Type of work with id = %d not found";
    private static final String EXCEPTION_APPLICATION_WITH_ID_NOT_FOUND
            = "Application with id = %d not found";

    private DaoFactory daoFactory = DaoFactory.getInstance();
    private Logger logger = Logger.getLogger(ApplicationServiceImpl.class);

    private ApplicationServiceImpl() {}

    private static class InstanceHolder {
        static final ApplicationService INSTANCE = new ApplicationServiceImpl();
    }

    public static ApplicationService getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public Application getApplicationById(int id) {
        try (DaoConnection connection = daoFactory.getConnection()) {
            ApplicationDao applicationDao
                    = daoFactory.createApplicationDao(connection);
            return applicationDao.get(id)
                    .orElseThrow(
                            getResourceNotFoundExceptionSupplier(
                                    EXCEPTION_APPLICATION_WITH_ID_NOT_FOUND, id
                            )
                    );
        }
    }

    @Override
    public List<Application> getApplicationsByTypeOfWork(String typeOfWork) {
        try (DaoConnection connection = daoFactory.getConnection()) {
            ApplicationDao applicationDao
                    = daoFactory.createApplicationDao(connection);
            return applicationDao.getApplicationsByTypeOfWork(typeOfWork);
        }
    }

    @Override
    public List<Application> getApplicationsByUserId(int userId) {
        try (DaoConnection connection = daoFactory.getConnection()) {
            ApplicationDao applicationDao
                    = daoFactory.createApplicationDao(connection);
            return applicationDao.getApplicationsByUserId(userId);
        }
    }

    @Override
    public List<Application> getApplicationsByStatus(Application.Status status) {
        try (DaoConnection connection = daoFactory.getConnection()) {
            ApplicationDao applicationDao
                    = daoFactory.createApplicationDao(connection);
            return applicationDao.getApplicationsByStatus(status);
        }
    }

    @Override
    public List<Application> getAllApplications(User.Role role) {
        if (role.equals(User.Role.DISPATCHER)) {
            try (DaoConnection connection = daoFactory.getConnection()) {
                ApplicationDao applicationDao
                        = daoFactory.createApplicationDao(connection);
                return applicationDao.getAll();
            }
        } else {
            AccessForbiddenException e = new AccessForbiddenException();
            logger.warn(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void createNewApplication(Application application) {
        try (DaoConnection connection = daoFactory.getConnection()) {
            UserDao userDao = daoFactory.createUserDao(connection);
            TypeOfWorkDao typeOfWorkDao
                    = daoFactory.createTypeOfWorkDao(connection);
            ApplicationDao applicationDao
                    = daoFactory.createApplicationDao(connection);

            connection.begin();
            User user = getUser(userDao, application.getTenant().getId());
            TypeOfWork typeOfWork = getTypeOfWork(typeOfWorkDao,
                    application.getTypeOfWork().getId());
            application.setTenant(user);
            application.setTypeOfWork(typeOfWork);
            application.setStatus(Application.Status.NEW);
            applicationDao.add(application);
            connection.commit();
        }
    }

    @Override
    public void deleteApplication(int applicationId, int userId) {
        try (DaoConnection connection = daoFactory.getConnection()) {
            ApplicationDao applicationDao
                    = daoFactory.createApplicationDao(connection);
            connection.begin();
            applicationDao.get(applicationId)
                    .filter(application
                            -> application.getTenant().getId() == userId)
                    .orElseThrow(
                            () -> {
                                AccessForbiddenException e
                                        = new AccessForbiddenException();
                                logger.info(e.getMessage(), e);
                                return e;
                            }
                    );
            applicationDao.delete(applicationId);
            connection.commit();
        }
    }

    private User getUser(UserDao userDao, int id) {
        return userDao.get(id)
                .filter(user -> user.getRole().equals(User.Role.TENANT))
                .orElseThrow(
                        getResourceNotFoundExceptionSupplier(
                                EXCEPTION_USER_WITH_ID_NOT_FOUND, id
                        )
                );
    }

    private TypeOfWork getTypeOfWork(TypeOfWorkDao typeOfWorkDao,
                                     int id) {
        return typeOfWorkDao.get(id)
                .orElseThrow(
                        getResourceNotFoundExceptionSupplier(
                                EXCEPTION_TYPE_OF_WORK_WITH_ID_NOT_FOUND, id
                        )
                );
    }

    private Supplier<ResourceNotFoundException>
                getResourceNotFoundExceptionSupplier(String blankMessage,
                                                     int id) {
        return () -> {
            ResourceNotFoundException e = new ResourceNotFoundException();
            String message = String.format(blankMessage, id);
            logger.info(message, e);
            return e;
        };
    }
}
