package it.bologna.ausl.internauta.utils.bdm.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author andrea
 */
public interface Dumpable {

    public static final String BDM_CLASS_TYPE = "__BDM_CLASS_TYPE__";
    
//    public static ObjectMapper buildCustomObjectMapper() {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new JodaModule());
//        mapper.setTimeZone(TimeZone.getDefault());
//        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
//        mapper.enable(SerializationFeature.INDENT_OUTPUT);
//        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
//        return mapper;
//    }
    
    public default <T extends Dumpable> String dump(ObjectMapper objectMapper) throws JsonProcessingException {
        String writeValueAsString = objectMapper.writeValueAsString(this);
        return writeValueAsString;
    }
    
    public default <T extends Dumpable> void dumpToOutputStream(OutputStream out, ObjectMapper objectMapper) throws IOException {
        objectMapper.writeValue(out, this);
    }
    
    public static <T extends Dumpable> T load(String json, Class<T> dumpableCass, ObjectMapper objectMapper) throws IOException {
        return objectMapper.readValue(json, dumpableCass);
    }
    
    public static <T extends Dumpable> T load(InputStream jsonInputStream, Class<T> dumpableCass, ObjectMapper objectMapper) throws IOException {
        return objectMapper.readValue(jsonInputStream, dumpableCass);
    }


}
