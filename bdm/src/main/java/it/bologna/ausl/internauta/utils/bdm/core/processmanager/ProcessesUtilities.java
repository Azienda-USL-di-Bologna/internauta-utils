package it.bologna.ausl.internauta.utils.bdm.core.processmanager;

import it.bologna.ausl.internauta.utils.bdm.core.BdmProcess;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author gdm
 */
public class ProcessesUtilities {
    
    /**
     * torna la versione del processo identificato dal nome passato
     * @param processName il nome del processo
     * @return la versione del processo identificato dal nome passato
     * @throws java.lang.NoSuchMethodException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.reflect.InvocationTargetException
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     */
    public static String getProcessVersion(String processName) throws NoSuchMethodException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        BdmProcess p = (BdmProcess) Class.forName("it.bologna.ausl.internauta.utils.bdm.workflows.processes." + processName).getDeclaredConstructor().newInstance();
        return p.getProcessVersion();
    }
}
