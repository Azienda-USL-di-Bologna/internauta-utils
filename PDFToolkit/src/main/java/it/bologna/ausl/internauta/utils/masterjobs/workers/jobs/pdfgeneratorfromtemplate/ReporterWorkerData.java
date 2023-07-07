package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import java.util.Map;


/**
 *
 * @author Top
 */
public class ReporterWorkerData extends JobWorkerData{

    private String codiceAzienda;
    private String templateName;
    
    Map<String,Object> parametriTemplate;
    
    public ReporterWorkerData() {
    }

    public ReporterWorkerData(String codiceAzienda, String templateName, Map<String, Object> parametriTemplate) {
        this.codiceAzienda = codiceAzienda;
        this.templateName = templateName;
        this.parametriTemplate = parametriTemplate;
    }
        
    public String getCodiceAzienda() {
        return codiceAzienda;
    }

    public void setCodiceAzienda(String codiceAzienda) {
        this.codiceAzienda = codiceAzienda;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Map<String, Object> getParametriTemplate() {
        return parametriTemplate;
    }

    public void setParametriTemplate(Map<String, Object> parametriTemplate) {
        this.parametriTemplate = parametriTemplate;
    }    
}
