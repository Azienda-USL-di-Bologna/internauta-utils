package it.bologna.ausl.internauta.utils.bdm.utilities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author andrea
 */
//@JsonTypeInfo(
//        use = JsonTypeInfo.Id.NAME,
//        include = JsonTypeInfo.As.PROPERTY,
//        property = Dumpable.BDM_CLASS_TYPE)
//@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = Dumpable.BDM_CLASS_TYPE)
public class Bag implements Dumpable {

//    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = Dumpable.BDM_CLASS_TYPE)
    private Map<String, Object> parameters;

    public Bag() {
        parameters = new HashMap<>();
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @JsonIgnore
    public Object get(String key) {
        return parameters.getOrDefault(key, null);
    }

    @JsonIgnore
    public void remove(String key) {
        parameters.remove(key);
    }

//    @JsonIgnore
//    public String getJSONString() throws JsonProcessingException {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new JodaModule());
//        mapper.setTimeZone(TimeZone.getDefault());
//        String writeValueAsString = mapper.writeValueAsString(this);
//        return writeValueAsString;
//    }
//    @JsonIgnore
//    public static Bag parse(String value) throws IOException {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new JodaModule());
//        mapper.setTimeZone(TimeZone.getDefault());
//        return mapper.readValue(value, Bag.class);
//    }
    public void put(String key, Object value) {
        parameters.put(key, value);
    }

}
