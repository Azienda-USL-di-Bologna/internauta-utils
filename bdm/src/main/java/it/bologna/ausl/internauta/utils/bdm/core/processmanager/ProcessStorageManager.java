package it.bologna.ausl.internauta.utils.bdm.core.processmanager;

import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.StorageException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author andrea
 */
public interface ProcessStorageManager {

    public BdmProcess loadProcess(String id) throws StorageException;

    public void saveProcess(BdmProcess p) throws StorageException;

    public void deleteProcess(String id) throws StorageException;

    public List<String> getProcessList() throws StorageException;

    public List<String> getProcessList(Map<String, Object> queryParams) throws StorageException;

}
