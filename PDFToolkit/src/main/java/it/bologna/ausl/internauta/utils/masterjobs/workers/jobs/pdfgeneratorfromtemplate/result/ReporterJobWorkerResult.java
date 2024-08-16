package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate.result;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;

/**
 *
 * @author Top
 */
public class ReporterJobWorkerResult extends JobWorkerResult {
    
    private String url;

    public ReporterJobWorkerResult() { }

    public ReporterJobWorkerResult(String url) {
        this.url = url;
    }
    
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }    
}
