package it.bologna.ausl.internauta.utils.bdm.core.processmanager;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.model.entities.bdm.Process;
import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess;
import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess.BdmStatus;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.BdmRuntimeExceptionContainer;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.StorageException;
import it.bologna.ausl.internauta.utils.bdm.utilities.Bag;
import it.bologna.ausl.model.entities.bdm.QProcess;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author gdm
 */
@Component
public class DbProcessStorageManager implements ProcessStorageManager {

    private final Logger log = LoggerFactory.getLogger(DbProcessStorageManager.class);
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final QProcess qProcess = QProcess.process;

    @Override
    public BdmProcess loadProcess(String id) throws StorageException {
        try {
            JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            return transactionTemplate.execute(a -> {
//                BdmProcess bdmProcess = null;
                BdmProcess bdmProcess = queryFactory.select(qProcess.jsonProcess).from(qProcess).where(qProcess.id.eq(id)).fetchOne();
                if (bdmProcess == null) {
                    String error = String.format("Process with id %s not found", id);
                    log.error(error);
                    throw new BdmRuntimeExceptionContainer(new StorageException(error));
                }
                return bdmProcess;
            });
        } catch (BdmRuntimeExceptionContainer ex) {
            throw new StorageException(ex.getException());
        } catch (Throwable ex) {
            String error = "Unable to load process";
            log.error(error, ex);
            throw new StorageException(error, ex);
        }
    }

    @Override
    public void saveProcess(BdmProcess p) throws StorageException {
        try {
            String idProcess = p.getProcessId();
            BdmStatus statusProcess = p.getStatus();
            JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            transactionTemplate.executeWithoutResult(a -> {
//                BdmProcess readBdmProcess = null;
                BdmProcess readBdmProcess = 
                    queryFactory.query().setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .select(qProcess.jsonProcess)
                        .from(qProcess)
                        .where(qProcess.id.eq(idProcess)).fetchOne();
//                BdmProcess testJson = new SampleProcess();
                if (readBdmProcess != null) {
                    if (readBdmProcess.getTransactionId() == null || p.getTransactionId() == null) {
                        String error = "one of the transactionId is null";
                        log.error(error);
                        throw new ConcurrentModificationException(error);
                    } else if (readBdmProcess.getTransactionId().equals(p.getTransactionId())) {
                        p.setTransactionId(p.getTransactionId() + 1);
                        log.info("json processo:");
                        try {
                            log.info(objectMapper.writeValueAsString(p));
                        } catch (JacksonException ex) {
                            log.error("errore json", ex);
                        }
                        // il processo esiste e transactionId fanno match, devo fare l'update
                        long updatedRows = queryFactory
                            .update(qProcess)
                            .set(qProcess.jsonProcess, p)
//                            .set(qProcess.jsonProcess, testJson)
                            .set(qProcess.status, statusProcess)
                            .where(qProcess.id.eq(idProcess))
                            .execute();
                        if (updatedRows != 1) {
                            String error = "process to update not found";
                            log.error(error);
                            throw new BdmRuntimeExceptionContainer(new StorageException(error));
                        }
                    } else {
                        String error = "transactionId doesn't match";
                        log.error(error);
                        throw new ConcurrentModificationException(error);
                    }
                } else {
                    Process process = new Process();
                    process.setId(idProcess);
                    process.setJsonProcess(p);
                    process.setStatus(statusProcess);
                    try {
                        entityManager.persist(process);
                    } catch (Exception ex) {
                        String error = "no rows inserted";
                        log.error(error, ex);
                        throw new BdmRuntimeExceptionContainer(new StorageException(error, ex));
                    }
                }
            });
        } catch (BdmRuntimeExceptionContainer ex) {
            throw new StorageException(ex.getException());
        } catch (Throwable ex) {
            String error = "storage error";
            log.error(error, ex);
            throw new StorageException(ex);
        }
    }

    @Override
    public void deleteProcess(String id) throws StorageException {
        try {
            JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            transactionTemplate.executeWithoutResult(a -> {
                queryFactory
                    .delete(qProcess)
                    .where(qProcess.id.eq(id))
                    .execute();
            });
        } catch (Throwable ex) {
            String error = "Error saving process";
            log.error(error, ex);
            throw new StorageException(error, ex);
        }
    }

    @Override
    public List<String> getProcessList() throws StorageException {
        try {
            JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
            List<String> res = queryFactory.select(qProcess.id).from(qProcess).orderBy(qProcess.id.asc()).fetch();
            return res;
        } catch (Throwable ex) {
            String error = "Unable to list porcesses";
            log.error(error, ex);
            throw new StorageException(error, ex);
        }
    }

    @Override
    public List<String> getProcessList(Bag queryParams) throws StorageException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
