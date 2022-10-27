package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.foo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerData;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class FooWorkerData extends JobWorkerData {
    @JsonIgnore
    private static final Logger log = LoggerFactory.getLogger(FooWorkerData.class);
    
    @JsonIgnore
    private String name = "Foo";

    private Integer params1;
    private String params2;
    private Boolean params3;
    private Map<String, Object> params4;

    public FooWorkerData() {
        params1=1;
        params2="2";
        params3=true;
        params4 = new HashMap();
        params4.put("chiave1", 1);
        params4.put("chiave2", "2");
        params4.put("chiave3", false);
    }

    public FooWorkerData(Integer params1, String params2, Boolean params3) {
        this.params1=params1;
        this.params2=params2;
        this.params3=params3;
        params4 = new HashMap();
        params4.put("chiave1", 1);
        params4.put("chiave2", "2");
        params4.put("chiave3", false);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getParams1() {
        return params1;
    }

    public void setParams1(Integer params1) {
        this.params1 = params1;
    }

    public String getParams2() {
        return params2;
    }

    public void setParams2(String params2) {
        this.params2 = params2;
    }

    public Boolean getParams3() {
        return params3;
    }

    public void setParams3(Boolean params3) {
        this.params3 = params3;
    }

    public Map<String, Object> getParams4() {
        return params4;
    }

    public void setParams4(Map<String, Object> params4) {
        this.params4 = params4;
    }
}
