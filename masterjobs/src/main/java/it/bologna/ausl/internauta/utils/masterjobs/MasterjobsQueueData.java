package it.bologna.ausl.internauta.utils.masterjobs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/**
 *
 * @author gdm
 */
public class MasterjobsQueueData {
    private Long set;
    private List<Long> jobs;
    
    @JsonIgnore
    private ObjectMapper objectMapper;
    
    public MasterjobsQueueData(){
    }
    
    public MasterjobsQueueData(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }
    
    public Long getSet() {
        return set;
    }

    public void setSet(Long set) {
        this.set = set;
    }

    public List<Long> getJobs() {
        return jobs;
    }

    public void setJobs(List<Long> jobs) {
        this.jobs = jobs;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    public String dump() throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(this);
    }
}
