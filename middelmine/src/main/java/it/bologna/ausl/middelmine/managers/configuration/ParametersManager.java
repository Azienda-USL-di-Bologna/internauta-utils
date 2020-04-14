package it.bologna.ausl.middelmine.managers.configuration;

import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Salo
 */
//@Configuration
public class ParametersManager implements ParametersManagerInterface {

    @Value("${redmine-test-mode}")
    private boolean testMode;

    @Value("${redmine-base-url}")
    private String redmineBaseUrl;

    @Value("${new-issue-path}")
    private String newIssuePaht;

    @Value("${new-attachment-path}")
    private String newAttachmentPath;

    @Value("${get-custom-fields-path}")
    private String getCustomFieldPath;

    @Value("${get-trackers-path}")
    private String getTrackersPath;

    @Value("${issue-info-path}")
    private String issueInfoPath;

    @Value("${api-key}")
    private String privateApiKey;

    @Value("${admin-api-key}")
    private String adminApiKey;

    @Value("${x-redmine-api-key-header}")
    private String redmineApiKeyHeader;

    @Value("${content-type-header}")
    private String contentTypeHeader;

    @Value("${application-json-header-value}")
    private String applicationJsonHeaderValue;

    @Value("${application-octet-stream-header-value}")
    private String applicationOctetHeaderValue;

    public ParametersManager() {
        System.out.println("First ParametersManager instantiation");
    }

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

    @Override
    public String getGetTrackersFieldPath() {
        return getTrackersPath;
    }

}
