package it.bologna.ausl.middelmine.interfaces;

/**
 *
 * @author Salo
 */
public interface ParametersManagerInterface {

    boolean isTestMode();

    String getRedmineBaseUrl();

    String getNewIssuePaht();

    String getNewAttachmentPath();

    String getGetCustomFieldPath();

    String getGetTrackersFieldPath();

    String getIssueInfoPath();

    String getPrivateApiKey();

    String getAdminApiKey();

    String getRedmineApiKeyHeader();

    String getContentTypeHeader();

    String getApplicationJsonHeaderValue();

    String getApplicationOctetHeaderValue();
}
