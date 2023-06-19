package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;


/**
 *
 * @author Top
 */
public class ReporterWorkerData extends JobWorkerData{
    @JsonIgnore
//    private static final Logger log = LoggerFactory.getLogger(ReporterWorkerData.class);

    private String nomeTemplate;
    
    Map<String,Object> parametriTemplate;

    public ReporterWorkerData() {
    }

    public String getNomeTemplate() {
        return nomeTemplate;
    }

    public void setNomeTemplate(String nomeTemplate) {
        this.nomeTemplate = nomeTemplate;
    }

    public Map<String, Object> getParametriTemplate() {
        return parametriTemplate;
    }

    public void setParametriTemplate(Map<String, Object> parametriTemplate) {
        this.parametriTemplate = parametriTemplate;
    }
    
    
}
