package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate.result;

import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;

public class UrlAndUuidResult extends JobWorkerResult {
    private String url;
    private String uuid;

    public UrlAndUuidResult(String url, String uuid) {
        this.url = url;
        this.uuid = uuid;
    }

    public UrlAndUuidResult() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
