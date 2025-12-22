package it.bologna.ausl.internauta.utils.bdm.core.processmanager;

import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess;
import it.bologna.ausl.internauta.utils.bdm.core.exceptions.StorageException;
import it.bologna.ausl.internauta.utils.bdm.utilities.Bag;
import java.util.List;

/**
 *
 * @author andrea
 */
public interface ProcessStorageManager {

    public BdmProcess loadProcess(String id) throws StorageException;

    public void saveProcess(BdmProcess p) throws StorageException;

    public void deleteProcess(String id) throws StorageException;

    public List<String> getProcessList() throws StorageException;

    public List<String> getProcessList(Bag queryParams) throws StorageException;

}
