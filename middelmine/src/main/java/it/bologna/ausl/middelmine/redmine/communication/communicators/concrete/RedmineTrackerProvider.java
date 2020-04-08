/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.middelmine.redmine.communication.communicators.concrete;

import com.taskadapter.redmineapi.bean.Project;
import com.taskadapter.redmineapi.bean.Tracker;
import it.bologna.ausl.middelmine.interfaces.ParametersManagerInterface;
import it.bologna.ausl.middelmine.redmine.communication.communicators.abstracts.RedmineAbstractResourceProvider;
import java.util.Collection;

/**
 *
 * @author Salo
 */
public class RedmineTrackerProvider extends RedmineAbstractResourceProvider {

    Project project;

    public RedmineTrackerProvider(ParametersManagerInterface pm) {
        super(pm);
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Collection<Tracker> getProjectTrackers() {
        return project.getTrackers();
    }

    public Tracker getTrackerByName(String trackerName) {
        Collection<Tracker> projectTrackers = getProjectTrackers();
        for (Tracker projectTracker : projectTrackers) {
            if (projectTracker.getName().equals(trackerName)) {
                return projectTracker;
            }
        }
        return null;
    }
}
