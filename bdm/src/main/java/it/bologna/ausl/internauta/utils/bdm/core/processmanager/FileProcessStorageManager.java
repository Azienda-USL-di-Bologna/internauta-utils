package it.bologna.ausl.internauta.utils.bdm.core.processmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.StorageException;
import it.bologna.ausl.internauta.utils.bdm.utilities.Bag;
import it.bologna.ausl.internauta.utils.bdm.utilities.Dumpable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author andrea
 */
@Component
public class FileProcessStorageManager implements ProcessStorageManager {

    private final String root;
    private final Logger log = LoggerFactory.getLogger(FileProcessStorageManager.class);

    @Autowired
    private ObjectMapper objectMapper;
    
    public FileProcessStorageManager(String root) {
        this.root = root;
        File r = new File(root);
        if (!r.exists()) {
            r.mkdirs();
        }
    }

    @Override
    public BdmProcess loadProcess(String id) throws StorageException {
        try (FileInputStream in = new FileInputStream(new File(root, id))) {
            return (BdmProcess) Dumpable.load(in, BdmProcess.class, objectMapper);

        } catch (Exception ex) {
            String error = "Error loading Process";
            log.error(error, ex);
            throw new StorageException(error, ex);
        }

    }

    @Override
    public void saveProcess(BdmProcess p) throws StorageException {
        try (FileInputStream in = new FileInputStream(new File(root, p.getProcessId()))) {
                BdmProcess savedProcess = Dumpable.load(in, BdmProcess.class, objectMapper);
                if (savedProcess.getTransactionId() == null || p.getTransactionId() == null || !savedProcess.getTransactionId().equals(p.getTransactionId())) {
                    throw new ConcurrentModificationException("transactionId doesn't match");
                }
                p.setTransactionId(p.getTransactionId() + 1);
        }
        catch (IOException ex) {
            // se da questa eccezione il processo è nuovo e va creato
        }
        catch (ConcurrentModificationException ex) {
            throw ex;
        }
        catch (Exception ex) {
            String error = "Error reading Process transactionId";
            log.error(error, ex);
            throw new StorageException(error, ex);
        }
        
        try (FileOutputStream out = new FileOutputStream(new File(root, p.getProcessId()))) {
            
            try (FileInputStream in = new FileInputStream(new File(root, p.getProcessId()))) {
                BdmProcess savedProcess = Dumpable.load(in, BdmProcess.class, objectMapper);
                if (savedProcess.getTransactionId() == null || p.getTransactionId() == null || !savedProcess.getTransactionId().equals(p.getTransactionId())) {
                    throw new ConcurrentModificationException("transactionId doesn't match");
                }
                p.setTransactionId(p.getTransactionId() + 1);
            }
            catch (IOException ex) {
                // se da questa eccezione il processo è nuovo e va creato
            }
            p.dumpToOutputStream(out, objectMapper);
        }
        catch (Exception ex) {
            String error = "Error saving Process";
            log.error(error, ex);
            throw new StorageException(error, ex);
        }
    }

    @Override
    public void deleteProcess(String id) throws StorageException {

        try {
            Files.delete(Paths.get(root, id));
        } catch (IOException ex) {
            String error = String.format("Error deleting file: %s ", Paths.get(root, id).toString());
            log.error(error, ex);
            throw new StorageException(error, ex);

        }

    }

    @Override
    public List<String> getProcessList() throws StorageException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<String> getProcessList(Bag queryParams) throws StorageException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
