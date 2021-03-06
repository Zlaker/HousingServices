package ua.training.model.dao.jdbc;

import org.apache.log4j.Logger;
import ua.training.model.dao.WorkerDao;
import ua.training.model.entities.TypeOfWork;
import ua.training.model.entities.person.Worker;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class JdbcWorkerDao extends AbstractJdbcDao implements WorkerDao {

    private static final String SELECT =
            "SELECT * FROM worker " +
                    "JOIN worker_has_type_of_work USING (id_worker) " +
                    "JOIN type_of_work USING (id_type_of_work) ";
    private static final String ORDER_BY = "ORDER BY id_worker ";

    private static final String SELECT_ALL = SELECT + ORDER_BY;
    private static final String SELECT_BY_ID = SELECT + "WHERE id_worker = ?";
    private static final String SELECT_BY_TYPE =
            "SELECT id_worker FROM worker_has_type_of_work " +
                    "WHERE id_type_of_work = ?";

    private static final String INSERT =
            "INSERT INTO worker (name) VALUES (?);";
    private static final String INSERT_TYPE_OF_WORK =
            "INSERT INTO worker_has_type_of_work (id_worker, id_type_of_work) " +
                    "VALUES (?, ?);";
    private static final String DELETE_BY_ID =
            "DELETE FROM worker WHERE id_worker = ?";
    private static final String UPDATE =
            "UPDATE worker SET name = ? WHERE id_worker = ?; " +
                    "DELETE FROM worker_has_type_of_work WHERE id_worker = ?";

    private static final String EXCEPTION_GET_BY_ID
            = "Failed select from 'worker' with id = %d";
    private static final String EXCEPTION_GET_BY_TYPE_OF_WORK_ID
            = "Failed select from 'worker' with id_type_of_work = %d";
    private static final String EXCEPTION_GET_ALL
            = "Failed select from 'worker'";
    private static final String EXCEPTION_ADD
            = "Failed insert into 'worker' value = %s";
    private static final String EXCEPTION_UPDATE
            = "Failed update 'worker' value = %s";

    static final String TABLE_WORKER = "worker";
    static final String WORKER_ID = "id_worker";
    static final String WORKER_NAME = "name";

    JdbcWorkerDao(Connection connection) {
        this.connection = connection;
        logger = Logger.getLogger(JdbcWorkerDao.class);
    }

    @Override
    public Optional<Worker> get(int id) {
        Optional<Worker> worker = Optional.empty();
        try (PreparedStatement statement =
                     connection.prepareStatement(SELECT_BY_ID)) {
            statement.setInt(1, id);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.first()) {
                worker = Optional.of(helper.getWorkerFromResultSet(resultSet));
            }
        } catch (SQLException e) {
            String message = String.format(EXCEPTION_GET_BY_ID, id);
            throw getDaoException(message, e);
        }
        return worker;
    }

    @Override
    public List<Worker> getAll() {
        List<Worker> workers = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(SELECT_ALL)) {
            resultSet.next();
            while (!resultSet.isAfterLast()) {
                workers.add(helper.getWorkerFromResultSet(resultSet));
            }
        } catch (SQLException e) {
            throw getDaoException(EXCEPTION_GET_ALL, e);
        }
        return workers;
    }

    @Override
    public void add(Worker worker) {
        try (PreparedStatement statement =
                     connection.prepareStatement(INSERT,
                             Statement.RETURN_GENERATED_KEYS)) {
            connection.setAutoCommit(false);
            statement.setString(1, worker.getName());
            statement.execute();

            ResultSet resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                worker.setId(resultSet.getInt(1));
            }
            insertTypesOfWork(worker);

            connection.commit();
        } catch (SQLException e) {
            String message = String.format(EXCEPTION_ADD, worker);
            throw getDaoException(message, e);
        }
    }

    @Override
    public void delete(int id) {
        delete(TABLE_WORKER, DELETE_BY_ID, id);
    }

    @Override
    public void update(Worker worker) {
        try (PreparedStatement statement =
                     connection.prepareStatement(UPDATE)) {
            connection.setAutoCommit(false);
            statement.setString(1, worker.getName());
            statement.setInt(2, worker.getId());
            statement.setInt(3, worker.getId());
            statement.execute();

            insertTypesOfWork(worker);
            connection.commit();
        } catch (SQLException e) {
            String message = String.format(EXCEPTION_UPDATE, worker);
            throw getDaoException(message, e);
        }
    }

    @Override
    public List<Worker> getWorkersByTypeOfWork(int typeOfWorkId) {
        List<Worker> workers = new ArrayList<>();
        try (PreparedStatement statement =
                     connection.prepareStatement(SELECT_BY_TYPE)) {
            statement.setInt(1, typeOfWorkId);

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                workers.add(helper.getWorkerFromResultSet(resultSet));
            }
        } catch (SQLException e) {
            String message = String.format(EXCEPTION_GET_BY_TYPE_OF_WORK_ID,
                    typeOfWorkId);
            throw getDaoException(message, e);
        }
        return workers;
    }

    private void insertTypesOfWork(Worker worker) throws SQLException {
        Set<TypeOfWork> typesOfWorks = worker.getTypesOfWork();
        if (!typesOfWorks.isEmpty()) {
            StringBuilder query = new StringBuilder();
            typesOfWorks.forEach(typeOfWork -> query.append(INSERT_TYPE_OF_WORK));
            try (PreparedStatement statement =
                         connection.prepareStatement(query.toString())) {
                int count = 1;
                for (TypeOfWork typeOfWork : typesOfWorks) {
                    statement.setInt(count++, worker.getId());
                    statement.setInt(count++, typeOfWork.getId());
                }
                statement.execute();
            }
        }
    }
}
