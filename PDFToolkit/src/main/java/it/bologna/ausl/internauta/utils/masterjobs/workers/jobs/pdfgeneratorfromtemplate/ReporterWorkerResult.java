package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;

/**
 *
 * @author Top
 */
public class ReporterWorkerResult extends JobWorkerResult {
    
    private String url;

    public ReporterWorkerResult() { }

    public ReporterWorkerResult(String url) {
        this.url = url;
    }
    
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }    
}
