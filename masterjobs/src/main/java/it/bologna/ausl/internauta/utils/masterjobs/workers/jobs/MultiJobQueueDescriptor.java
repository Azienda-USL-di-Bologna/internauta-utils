package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs;

import it.bologna.ausl.model.entities.masterjobs.Set;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author gdm
 */

public class MultiJobQueueDescriptor {
    private List<JobWorker> workers;
    private String objectId;
    private String objectType;
    private String app;
    private Boolean waitForObject;
    private Boolean skipIfAlreadyPresent;
    private Set.SetPriority priority;

    public MultiJobQueueDescriptor(Builder jobQueueDescriptorBuilder) {
        this.workers = jobQueueDescriptorBuilder.getWorkers();
        this.objectId = jobQueueDescriptorBuilder.getObjectId();
        this.objectType = jobQueueDescriptorBuilder.getObjectType();
        this.app = jobQueueDescriptorBuilder.getApp();
        this.waitForObject = jobQueueDescriptorBuilder.getWaitForObject();
        this.skipIfAlreadyPresent = jobQueueDescriptorBuilder.getSkipIfAlreadyPresent();
        this.priority = jobQueueDescriptorBuilder.getPriority();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public List<JobWorker> getWorkers() {
        return workers;
    }

    public void setWorkers(List<JobWorker> workers) {
        this.workers = workers;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public Boolean getWaitForObject() {
        return waitForObject;
    }

    public void setWaitForObject(Boolean waitForObject) {
        this.waitForObject = waitForObject;
    }

    public Boolean getSkipIfAlreadyPresent() {
        return skipIfAlreadyPresent;
    }

    public void setSkipIfAlreadyPresent(Boolean skipIfAlreadyPresent) {
        this.skipIfAlreadyPresent = skipIfAlreadyPresent;
    }

    public Set.SetPriority getPriority() {
        return priority;
    }

    public void setPriority(Set.SetPriority priority) {
        this.priority = priority;
    }

    public static class Builder {
        private List<JobWorker> workers;
        private String objectId;
        private String objectType;
        private String app;
        private Boolean waitForObject = false;
        private Boolean skipIfAlreadyPresent = false;
        private Set.SetPriority priority = Set.SetPriority.NORMAL;

        public Builder() {
        }

        public MultiJobQueueDescriptor build() {
            return new MultiJobQueueDescriptor(this);
        }

        public List<JobWorker> getWorkers() {
            return workers;
        }

        public void setWorkers(List<JobWorker> workers) {
            this.workers = workers;
        }

        public String getObjectId() {
            return objectId;
        }

        public void setObjectId(String objectId) {
            this.objectId = objectId;
        }

        public String getObjectType() {
            return objectType;
        }

        public void setObjectType(String objectType) {
            this.objectType = objectType;
        }

        public String getApp() {
            return app;
        }

        public void setApp(String app) {
            this.app = app;
        }

        public Boolean getWaitForObject() {
            return waitForObject;
        }

        public void setWaitForObject(Boolean waitForObject) {
            this.waitForObject = waitForObject;
        }

        public Boolean getSkipIfAlreadyPresent() {
            return skipIfAlreadyPresent;
        }

        public void setSkipIfAlreadyPresent(Boolean skipIfAlreadyPresent) {
            this.skipIfAlreadyPresent = skipIfAlreadyPresent;
        }

        public Set.SetPriority getPriority() {
            return priority;
        }

        public void setPriority(Set.SetPriority priority) {
            this.priority = priority;
        }

        public Builder addWorker(JobWorker worker) {
            if (workers == null) {
                this.workers = new ArrayList<>();
            }
            this.workers.add(worker);
            return this;
        }

        public Builder workers(List<JobWorker> workers) {
            this.workers = workers;
            return this;
        }

        public Builder objectId(String objectId) {
            this.objectId = objectId;
            return this;
        }

        public Builder objectType(String objectType) {
            this.objectType = objectType;
            return this;
        }
        
        public Builder app(String app) {
            this.app = app;
            return this;
        }
        
        public Builder waitForObject(Boolean waitForObject) {
            this.waitForObject = waitForObject;
            return this;
        }
        
        public Builder skipIfAlreadyPresent(Boolean skipIfAlreadyPresent) {
            this.skipIfAlreadyPresent = skipIfAlreadyPresent;
            return this;
        }
        
        public Builder priority(Set.SetPriority priority) {
            this.priority = priority;
            return this;
        }
    }
}
