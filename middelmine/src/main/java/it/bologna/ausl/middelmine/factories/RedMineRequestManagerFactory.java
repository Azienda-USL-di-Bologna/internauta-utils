package it.bologna.ausl.middelmine.factories;

import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.managers.RedmineRequestManager;
import it.bologna.ausl.middelmine.managers.requests.RedMineGetCustomFieldsRequestManager;
import it.bologna.ausl.middelmine.managers.requests.RedMineGetTrackersRequestManager;
import it.bologna.ausl.middelmine.managers.requests.RedMineNewAttachmentRequestManager;
import it.bologna.ausl.middelmine.managers.requests.RedMineNewIssueRequestManager;
import it.bologna.ausl.middelmine.managers.requests.RedmineGetIssueInfoRequest;

/**
 *
 * @author Salo
 */
public class RedMineRequestManagerFactory {

    public static RedmineRequestManager getNewIssueRequestManager() {
        return new RedMineNewIssueRequestManager();
    }

    public static RedmineRequestManager getNewIssueRequestManager(ParametersManagerInterface parametersManager) {
        return new RedMineNewIssueRequestManager(parametersManager);
    }

    public static RedmineRequestManager getGetCustomFieldsRequestManager() {
        return new RedMineGetCustomFieldsRequestManager();
    }

    public static RedmineRequestManager getGetCustomFieldsRequestManager(ParametersManagerInterface parametersManager) {
        return new RedMineGetCustomFieldsRequestManager(parametersManager);
    }

    public static RedmineRequestManager getGetTrackersRequestManager(ParametersManagerInterface parametersManager) {
        return new RedMineGetTrackersRequestManager(parametersManager);
    }

    public static RedmineRequestManager getNewAttachmentRequestManager(ParametersManagerInterface parametersManager) {
        return new RedMineNewAttachmentRequestManager(parametersManager);
    }

    public static RedmineRequestManager getIssueInfo(ParametersManagerInterface parametersManager) {
        return new RedmineGetIssueInfoRequest(parametersManager);
    }
}
