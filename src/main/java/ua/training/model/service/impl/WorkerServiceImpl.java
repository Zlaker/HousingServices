package ua.training.model.service.impl;

import ua.training.model.dao.DaoFactory;
import ua.training.model.dao.WorkerDao;
import ua.training.model.entities.TypeOfWork;
import ua.training.model.entities.person.Worker;
import ua.training.model.service.WorkerService;

import java.util.List;
import java.util.Optional;

public class WorkerServiceImpl implements WorkerService {

    private DaoFactory daoFactory = DaoFactory.getInstance();

    private WorkerServiceImpl() {}

    private static class InstanceHolder {
        static final WorkerService INSTANCE = new WorkerServiceImpl();
    }

    public static WorkerService getInstance() {
        return InstanceHolder.INSTANCE;
    }

    @Override
    public Optional<Worker> getWorkerById(int id) {
        WorkerDao workerDao = daoFactory.createWorkerDao();
        return workerDao.get(id);
    }

    @Override
    public List<Worker> getWorkersByTypeOfWork(TypeOfWork typeOfWork) {
        WorkerDao workerDao = daoFactory.createWorkerDao();
        return workerDao.getWorkersByTypeOfWork(typeOfWork);
    }

    @Override
    public List<Worker> getAllWorkers() {
        WorkerDao workerDao = daoFactory.createWorkerDao();
        return workerDao.getAll();
    }

    @Override
    public void createNewWorker(Worker worker) {
        WorkerDao workerDao = daoFactory.createWorkerDao();
        workerDao.add(worker);
    }
}