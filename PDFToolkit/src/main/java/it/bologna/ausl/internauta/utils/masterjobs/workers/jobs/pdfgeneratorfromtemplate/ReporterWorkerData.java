package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import java.util.Map;


/**
 * Classe che contiene i dati necessari al worker del reporter per generare i PDF.
 * 
 * @author Giuseppe Russo <g.russo@dilaxia.com>
 */
public class ReporterWorkerData extends JobWorkerData{
    //
    private String codiceAzienda;
    private String templateName;
    private String fileName;
    
    Map<String,Object> parametriTemplate;
    
    public ReporterWorkerData() {
    }

    /**
     * Costruttore con tutti i parametri
     * 
     * @param codiceAzienda Il codice azienda per generare il pdf. Verrà utilizzato per identificare il giusto template.
     * @param templateName Il nome del template eg. gd_frontespizio
     * @param fileName Il nome con il quale verrà generato il pdf.
     * @param parametriTemplate I parametri che andranno a compilare il template.
     */
    public ReporterWorkerData(String codiceAzienda, String templateName, String fileName, Map<String, Object> parametriTemplate) {
        this.codiceAzienda = codiceAzienda;
        this.templateName = templateName;
        this.fileName = fileName;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Map<String, Object> getParametriTemplate() {
        return parametriTemplate;
    }

    public void setParametriTemplate(Map<String, Object> parametriTemplate) {
        this.parametriTemplate = parametriTemplate;
    }    
}
