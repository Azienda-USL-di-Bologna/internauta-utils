package it.bologna.ausl.internauta.utils.bdm.workflows.utils;

import java.time.ZonedDateTime;
import java.util.List;

/**
 *
 * @author gdm
 */
public class ProcessoDucumentaleStepInformation {
    private String stepId; // id dello step
    private String stepType; // nome dello step (lo stepType dello Step)
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
    protected ZonedDateTime executionDate;
    private List<String> users; // utenti attivi nello step, gli utenti che hanno ricevuto l'attività sulla scrivania
    private String stepOnUser; // utente che ha fatto lo step-on (sarà null nel caso di uno step-to(step-back)
    private String stepToUser; // utente che ha fatto lo step-to(step-back) sarà null nel caso di uno step-on

    public ProcessoDucumentaleStepInformation() {
    }

    public ProcessoDucumentaleStepInformation(String stepId, String stepType, ZonedDateTime executionDate, List<String> users, String stepOnUser, String stepToUser) {
        this.stepId = stepId;
        this.stepType = stepType;
        this.executionDate = executionDate;
        this.users = users;
        this.stepOnUser = stepOnUser;
        this.stepToUser = stepToUser;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public ZonedDateTime getExecutionDate() {
        return executionDate;
    }

    public void setExecutionDate(ZonedDateTime executionDate) {
        this.executionDate = executionDate;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public String getStepOnUser() {
        return stepOnUser;
    }

    public void setStepOnUser(String stepOnUser) {
        this.stepOnUser = stepOnUser;
    }

    public String getStepToUser() {
        return stepToUser;
    }

    public void setStepToUser(String stepToUser) {
        this.stepToUser = stepToUser;
    }
}
