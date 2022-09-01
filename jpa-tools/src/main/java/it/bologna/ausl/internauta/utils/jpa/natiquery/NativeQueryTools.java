/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.utils.jpa.natiquery;

import com.fasterxml.jackson.annotation.JsonProperty;
import static it.bologna.ausl.internauta.utils.jpa.natiquery.UtilExceptions.declareToThrow;
import static it.bologna.ausl.internauta.utils.jpa.natiquery.UtilExceptions.undeclareCheckedException;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import static java.util.Arrays.stream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.xml.bind.annotation.XmlAttribute;
import org.hibernate.Session;

/**
 *
 * @author gdm
 */
public class NativeQueryTools {

    private final EntityManager em;

    public NativeQueryTools(EntityManager em) {
        this.em = em;
    }
    
    @SuppressWarnings("unchecked")
    private Object optionallyConvertValueToFieldType(final Field field, final Object columnValue) {
        final Class<?> fieldType = field.getType();
        if (columnValue == null) {
            //@formatter:off  
            return Long.TYPE.equals(fieldType) ? Long.valueOf(0l)
                    : Integer.TYPE.equals(fieldType) ? Integer.valueOf(0)
                    : Short.TYPE.equals(fieldType) ? Short.valueOf((short) 0)
                    : Byte.TYPE.equals(fieldType) ? Byte.valueOf((byte) 0)
                    : Double.TYPE.equals(fieldType) ? Double.valueOf(0d)
                    : Float.TYPE.equals(fieldType) ? Float.valueOf(0f)
                    : (Object) null; //note for some reason we get an NPE without the cast  
        }//@formatter:on  
        final Class<?> valueType = columnValue.getClass();
//        if (!fieldType.isAssignableFrom(valueType)) {
//            @SuppressWarnings("rawtypes")
//            final Optional<TypeConverter> optionalConverter = converters.stream().filter(c -> c.canConvert(valueType, fieldType)).findFirst();
//            if (optionalConverter.isPresent()) {
//                return optionalConverter.get().convert(columnValue);
//            }
//        }
        return columnValue;
    }

    private Field getField(final Class<? extends Object> pojoClass, final String columnName) {
        try {
            declareToThrow(NoSuchFieldException.class); // because it sneakily does down at "pojoClass.getDeclaredField"  
            return stream(pojoClass.getDeclaredFields())//  
                    .filter(field -> field.getAnnotation(Column.class) != null)// Column name is dominant  
                    .filter(field -> columnName.equalsIgnoreCase(field.getAnnotation(Column.class).name()))//  
                    .findAny()//  
                    .orElseGet(() -> stream(pojoClass.getDeclaredFields())// @XmlAnnotation is a second choice  
                    .filter(field -> field.getAnnotation(XmlAttribute.class) != null)//  
                    .filter(field -> columnName.equalsIgnoreCase(field.getAnnotation(XmlAttribute.class).name()))//  
                    .findAny()//  
                    .orElseGet(() -> stream(pojoClass.getDeclaredFields())// @JsonProperty is a third choice  
                    .filter(field -> field.getAnnotation(JsonProperty.class) != null)//  
                    .filter(field -> columnName.equalsIgnoreCase(field.getAnnotation(JsonProperty.class).value()))//  
                    .findAny()//  
                    .orElseGet(undeclareCheckedException(() -> pojoClass.getDeclaredField(columnName))))); // field name is our last option  
        } catch (final SecurityException e) {
            throw new IllegalStateException("Could not map column " + columnName + " to class " + pojoClass, e);
        } catch (final NoSuchFieldException e) {
            // ignore, because we will throw an exception anyway  
        }
        throw new IllegalArgumentException("Could not map column to class " + pojoClass
                + ". Reason: No declared field found that is either annotated with @Column(name=\"" + columnName + "\") or @XmlAttribute(name=\"" + columnName
                + "\") or @JsonProperty(\"" + columnName + "\") ignoring case, and no declared Field is named \"" + columnName + "\".");
    }

    private <T> void setToField(final Entry<String, Object> columnNameValueEntry, final T pojo) {
        final String columnName = columnNameValueEntry.getKey();
        Object columnValue = columnNameValueEntry.getValue();
        try {
            final Field field = getField(pojo.getClass(), columnName);
            columnValue = optionallyConvertValueToFieldType(field, columnValue);
            field.set(pojo, columnValue);
        } catch (final IllegalAccessException | IllegalArgumentException | IllegalStateException e) {
            throw new IllegalArgumentException("Could not set Value " + columnValue + (columnValue == null ? "" : "of type: " + columnValue.getClass())
                    + " to pojo of type " + pojo.getClass(), e);
        }
    }

    private Map<String, Object> getColumNameToValueMapFromRowValueArray(final Object[] rowValueArray, final Map<String, Integer> columnNameToIndexMap) {
        // stream().collect(toMap(keyFunct, valueFunct)...) will not accept "null" values, so we do it this way:  
        final Map<String, Object> result = new LinkedHashMap<>();
        columnNameToIndexMap.entrySet().forEach(nameToIndexEntry -> result.put(nameToIndexEntry.getKey(), rowValueArray[nameToIndexEntry.getValue()]));
        return result;
    }

    private <T> T createPojoAndMapValues(final Supplier<T> targetPojoFactory, final Map<String, Object> rowMap) {
        final T pojo = targetPojoFactory.get();
        rowMap.entrySet().stream().forEach(columnNameValueEntry -> setToField(columnNameValueEntry, pojo));
        return pojo;
    }

    public Map<String, Integer> getColumnNameToIndexMap(final String queryString) throws SQLException {
        final Session session = em.unwrap(Session.class); // ATTENTION! This is Hibernate-specific!  
        final AtomicReference<ResultSetMetaData> msRef = new AtomicReference<>();
        session.doWork((c) -> {
            try (final PreparedStatement statement = PreparedStatementFactory.create(c, queryString)) {
                // I'm not setting parameters here, because I just want to find out about the return values' column names  
                msRef.set(statement.getMetaData());
            }
        });
        final ResultSetMetaData metaData = msRef.get();
        // LinkedHashmap preserves order of insertion:  
        final Map<String, Integer> columnNameToColumnIndex = new LinkedHashMap<>();
        for (int t = 0; t < metaData.getColumnCount(); ++t) {
            // important, first index in the metadata is "1", the first index for the result array must be "0"  
            columnNameToColumnIndex.put(metaData.getColumnName(t + 1), t);
        }
        return columnNameToColumnIndex;
    }
    
    public <T> List<T> asMapped(final List<Object[]> queryResultAsListOfObjectArrays, final Map<String, Integer> columnNameToIndexMap, final Function<Map<String, Object>, T> mapToObject) {
        final Function<Object[], Map<String, Object>> arrayToMap = rowValueArray -> {
            return getColumNameToValueMapFromRowValueArray(rowValueArray, columnNameToIndexMap);
        };
        final Function<Object[], T> mapper = arrayToMap.andThen(mapToObject);
        return queryResultAsListOfObjectArrays.stream().collect(mapping(mapper, toList()));
    }

    public List<Map<String, Object>> asListOfMaps(final List<Object[]> queryResultAsListOfObjectArrays, final Map<String, Integer> columnNameToIndexMap) {
        return asMapped(queryResultAsListOfObjectArrays, columnNameToIndexMap, Function.identity());
    }

    public <T> List<T> asListOfPojos(final List<Object[]> queryResultAsListOfObjectArrays, final Map<String, Integer> columnNameToIndexMap, final Supplier<T> targetPojoFactory) {
        final Function<Map<String, Object>, T> mapToPojo = (rowMap) -> createPojoAndMapValues(targetPojoFactory, rowMap);
        return asMapped(queryResultAsListOfObjectArrays, columnNameToIndexMap, mapToPojo);
    }

}
