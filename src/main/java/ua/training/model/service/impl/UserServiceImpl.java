package ua.training.model.service.impl;

import ua.training.model.dao.DaoConnection;
import ua.training.model.dao.DaoFactory;
import ua.training.model.dao.DispatcherDao;
import ua.training.model.dao.TenantDao;
import ua.training.model.entities.person.Tenant;
import ua.training.model.entities.person.User;
import ua.training.model.service.UserService;

import java.util.Optional;

public class UserServiceImpl implements UserService {

    private DaoFactory daoFactory = DaoFactory.getInstance();

    private UserServiceImpl() {}

    private static class InstanceHolder {
        static final UserService INSTANCE = new UserServiceImpl();
    }

    public static UserService getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public Optional<User> loginEmail(String email, String password) {
        try (DaoConnection connection = daoFactory.getConnection()) {
            connection.begin();

            TenantDao tenantDao = daoFactory.createTenantDao(connection);
            Optional<User> user = tenantDao.getTenantByEmail(email)
                    .filter(tenant -> password.equals(tenant.getPassword()))
                    .map(tenant -> tenant);

            if (!user.isPresent()) {
                DispatcherDao dispatcherDao
                        = daoFactory.createDispatcherDao(connection);
                user = dispatcherDao.getDispatcherByEmail(email)
                        .filter(dispatcher
                                -> password.equals(dispatcher.getPassword()))
                        .map(dispatcher -> dispatcher);
                user.ifPresent(user1 -> dispatcherDao
                        .setDispatcherOnline(user1.getId(), true));
            }

            connection.commit();
            return user;
        }
    }

    @Override
    public Optional<User> loginAccount(int account, String password) {
        try (DaoConnection connection = daoFactory.getConnection()) {
            return daoFactory.createTenantDao(connection)
                    .getTenantByAccount(account)
                    .filter(tenant -> password.equals(tenant.getPassword()))
                    .map(tenant -> tenant);
        }
    }
}
