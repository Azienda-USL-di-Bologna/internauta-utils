package it.bologna.ausl.internauta.utils.ribaltone.basedata;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.userreport.UserReport.UserReportType;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.userreport.UserReportManager;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneCacheConfig;
import java.util.List;

/**
 *
 * @author Top
 */
public class Operations {
    
    

    List<Operation> listOfOperation;
    RibaltoneCacheConfig ribaltoneCacheConfig;

    public Operations(List<Operation> listOfOperation, RibaltoneCacheConfig ribaltoneCacheConfig) {
        this.listOfOperation = listOfOperation;
        this.ribaltoneCacheConfig = ribaltoneCacheConfig;
    }
    
    public Operations(DatiDaImportare datiDaImportare, RibaltoneCacheConfig ribaltoneCacheConfig){
    
    }
    

    public void execute() {
        for (Operation operation : listOfOperation) {
            operation.esegui();
        }
    }

    
    public UserReportManager generateUserReport(UserReportType userReportType) {
        
        
        return null;
    }
    public UserReportManager generateUserReport() {
        
        
        return null;
    }
}
