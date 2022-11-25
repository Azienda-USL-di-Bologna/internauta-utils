package it.bologna.ausl.internauta.utils.masterjobs.executors.jobs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/**
 *
 * @author gdm
 * 
 * Classe che rappresenta un messaggio letto da redis per l'esecuzione di un set di jobs
 */
public class MasterjobsQueueData {
    private String queue;
    private Long set;
    private List<Long> jobs;
    
    @JsonIgnore
    private ObjectMapper objectMapper;
    
    public MasterjobsQueueData(){
    }
    
    public MasterjobsQueueData(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
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
    
    /**
     * trasforma l'oggetto in una stringa json (per scriverla nella coda redis)
     * @return
     * @throws JsonProcessingException 
     */
    public String dump() throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(this);
    }
}
