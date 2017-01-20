package ua.training.model.dao.jdbc;

import ua.training.model.dao.BrigadeDao;
import ua.training.model.entities.Brigade;
import ua.training.model.entities.person.Worker;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcBrigadeDao implements BrigadeDao {

    private static final String SELECT_ALL =
            "SELECT * FROM brigade " +
                    "JOIN brigade_has_worker USING (id_brigade) " +
                    "JOIN worker USING (id_worker) " +
                    "JOIN worker_has_type_of_work USING (id_worker)" +
                    "JOIN type_of_work USING (id_type_of_work) " +
                    "ORDER BY id_brigade, id_worker";
    private static final String SELECT_MANAGER =
            "SELECT * FROM worker " +
                    "JOIN worker_has_type_of_work USING (id_worker)" +
                    "JOIN type_of_work USING (id_type_of_work) " +
                    "WHERE id_worker = ?";
    private static final String SELECT_BY_ID =
            "SELECT * FROM brigade " +
                    "JOIN brigade_has_worker USING (id_brigade) " +
                    "JOIN worker USING (id_worker) " +
                    "JOIN worker_has_type_of_work USING (id_worker)" +
                    "JOIN type_of_work USING (id_type_of_work) " +
                    "WHERE id_brigade = ? " +
                    "ORDER BY id_worker";

    private static final String INSERT =
            "INSERT INTO brigade (manager) VALUES (?)";
    private static final String INSERT_WORKER =
            "INSERT INTO brigade_has_worker (id_brigade, id_worker) " +
                    "VALUES (?, ?)";
    private static final String DELETE_BY_ID =
            "DELETE FROM brigade WHERE id_brigade = ?";
    private static final String DELETE_TYPE_OF_WORK_BY_ID =
            "DELETE FROM brigade_has_worker WHERE id_brigade = ?";
    private static final String UPDATE =
            "UPDATE brigade SET manager = ? WHERE id_brigade = ?";

    static final String BRIGADE_ID = "id_brigade";
    static final String MANAGER = "manager";

    private Connection connection;

    JdbcBrigadeDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Brigade> get(int id) {
        Optional<Brigade> brigade = Optional.empty();
        try (PreparedStatement brigadeStatement
                     = connection.prepareStatement(SELECT_BY_ID);
             PreparedStatement managerStatement
                     = connection.prepareStatement(SELECT_MANAGER)) {
            brigadeStatement.setInt(1, id);

            ResultSet brigadeResultSet = brigadeStatement.executeQuery();
            if (brigadeResultSet.first()) {
                managerStatement.setInt(1, brigadeResultSet.getInt(MANAGER));
                Brigade tempBrigade = getBrigadeFromResultSet(brigadeResultSet);
                ResultSet managerResultSet = managerStatement.executeQuery();
                if (managerResultSet.next()) {
                    tempBrigade.setManager(JdbcWorkerDao
                            .getWorkerFromResultSet(managerResultSet));
                }
                brigade = Optional.of(tempBrigade);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return brigade;
    }

    @Override
    public List<Brigade> getAll() {
        List<Brigade> brigades = new ArrayList<>();
        try (Statement statement
                     = connection.createStatement();
             ResultSet brigadeResultSet
                     = statement.executeQuery(SELECT_ALL);
             PreparedStatement managerStatement
                     = connection.prepareStatement(SELECT_MANAGER)) {
            brigadeResultSet.next();
            while (!brigadeResultSet.isAfterLast()) {
                managerStatement.setInt(1, brigadeResultSet.getInt(MANAGER));
                Brigade brigade = getBrigadeFromResultSet(brigadeResultSet);
                ResultSet managerResultSet = managerStatement.executeQuery();
                if (managerResultSet.next()) {
                    brigade.setManager(JdbcWorkerDao
                            .getWorkerFromResultSet(managerResultSet));
                }
                brigades.add(brigade);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return brigades;
    }

    @Override
    public void add(Brigade brigade) {
        try (PreparedStatement brigadeStatement
                     = connection.prepareStatement(INSERT,
                             Statement.RETURN_GENERATED_KEYS)) {
            brigadeStatement.setInt(1, brigade.getManager().getId());
            brigadeStatement.execute();

            ResultSet resultSet = brigadeStatement.getGeneratedKeys();
            if (resultSet.next()) {
                brigade.setId(resultSet.getInt(1));
            }
            insertWorkers(brigade);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(int id) {
        try (PreparedStatement statement
                     = connection.prepareStatement(DELETE_BY_ID)) {
            statement.setInt(1, id);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(Brigade brigade) {
        try (PreparedStatement brigadeStatement
                     = connection.prepareStatement(UPDATE);
             PreparedStatement workerStatement
                     = connection.prepareStatement(DELETE_TYPE_OF_WORK_BY_ID)) {
            brigadeStatement.setInt(1, brigade.getManager().getId());
            brigadeStatement.setInt(2, brigade.getId());
            brigadeStatement.execute();

            workerStatement.setInt(1, brigade.getId());
            workerStatement.execute();

            insertWorkers(brigade);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static Brigade getBrigadeFromResultSet(ResultSet resultSet)
            throws SQLException {
        int currentId = resultSet.getInt(BRIGADE_ID);
        Brigade.Builder builder = new Brigade.Builder()
                .setId(resultSet.getInt(BRIGADE_ID))
                .addWorker(JdbcWorkerDao.getWorkerFromResultSet(resultSet));
        while ((!resultSet.isAfterLast())
                    && (resultSet.getInt(BRIGADE_ID) == currentId)) {
            builder.addWorker(JdbcWorkerDao.getWorkerFromResultSet(resultSet));
        }
        return builder.build();
    }

    private void insertWorkers(Brigade brigade) {
        try (PreparedStatement workerStatement
                     = connection.prepareStatement(INSERT_WORKER)) {
            for (Worker worker : brigade.getWorkers()) {
                workerStatement.setInt(1, brigade.getId());
                workerStatement.setInt(2, worker.getId());
                workerStatement.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}