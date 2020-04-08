/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.middelmine.managers.configuration;

import it.bologna.ausl.middelmine.configuration.ApplicationParamsReader;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author Salo
 */
public class LoadableParametersManager implements ParametersManagerInterface {

    private boolean testMode;

    private String redmineBaseUrl;

    private String newIssuePaht;

    private String newAttachmentPath;

    private String getCustomFieldPath;

    private String getTrackersPath;

    private String issueInfoPath;

    private String privateApiKey;

    private String adminApiKey;

    private String redmineApiKeyHeader;

    private String contentTypeHeader;

    private String applicationJsonHeaderValue;

    private String applicationOctetHeaderValue;

    @Override
    public boolean isTestMode() {
        return testMode;
    }

    @Override
    public String getRedmineBaseUrl() {
        return redmineBaseUrl;
    }

    @Override
    public String getNewIssuePaht() {
        return newIssuePaht;
    }

    @Override
    public String getNewAttachmentPath() {
        return newAttachmentPath;
    }

    @Override
    public String getGetCustomFieldPath() {
        return getCustomFieldPath;
    }

    @Override
    public String getGetTrackersFieldPath() {
        return getTrackersPath;
    }

    @Override
    public String getPrivateApiKey() {
        return privateApiKey;
    }

    @Override
    public String getAdminApiKey() {
        return adminApiKey;
    }

    @Override
    public String getRedmineApiKeyHeader() {
        return redmineApiKeyHeader;
    }

    @Override
    public String getContentTypeHeader() {
        return contentTypeHeader;
    }

    @Override
    public String getApplicationJsonHeaderValue() {
        return applicationJsonHeaderValue;
    }

    @Override
    public String getApplicationOctetHeaderValue() {
        return applicationOctetHeaderValue;
    }

    @Override
    public String getIssueInfoPath() {
        return issueInfoPath;
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    public void setRedmineBaseUrl(String redmineBaseUrl) {
        this.redmineBaseUrl = redmineBaseUrl;
    }

    public void setNewIssuePaht(String newIssuePaht) {
        this.newIssuePaht = newIssuePaht;
    }

    public void setNewAttachmentPath(String newAttachmentPath) {
        this.newAttachmentPath = newAttachmentPath;
    }

    public void setGetCustomFieldPath(String getCustomFieldPath) {
        this.getCustomFieldPath = getCustomFieldPath;
    }

    public void setGetTrackersPath(String getTrackersPath) {
        this.getTrackersPath = getTrackersPath;
    }

    public void setPrivateApiKey(String privateApiKey) {
        this.privateApiKey = privateApiKey;
    }

    public void setAdminApiKey(String adminApiKey) {
        this.adminApiKey = adminApiKey;
    }

    public void setRedmineApiKeyHeader(String redmineApiKeyHeader) {
        this.redmineApiKeyHeader = redmineApiKeyHeader;
    }

    public void setContentTypeHeader(String contentTypeHeader) {
        this.contentTypeHeader = contentTypeHeader;
    }

    public void setApplicationJsonHeaderValue(String applicationJsonHeaderValue) {
        this.applicationJsonHeaderValue = applicationJsonHeaderValue;
    }

    public void setApplicationOctetHeaderValue(String applicationOctetHeaderValue) {
        this.applicationOctetHeaderValue = applicationOctetHeaderValue;
    }

    public void setIssueInfoPath(String issueInfoPath) {
        this.issueInfoPath = issueInfoPath;
    }

}
