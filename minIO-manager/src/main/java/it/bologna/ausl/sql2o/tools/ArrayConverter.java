// Written in 2015 by Thilo Planz 
// To the extent possible under law, I have dedicated all copyright and related and neighboring rights 
// to this software to the public domain worldwide. This software is distributed without any warranty. 
// http://creativecommons.org/publicdomain/zero/1.0/
package it.bologna.ausl.sql2o.tools;

import java.io.InputStream;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;
import org.sql2o.quirks.NoQuirks;
import org.sql2o.quirks.PostgresQuirks;
import org.sql2o.quirks.Quirks;
import org.sql2o.quirks.parameterparsing.SqlParameterParsingStrategy;

/**
 * Sql2o type converter to handle columns of type ARRAY.To get the most out of it, install it as Quirks.
 *<pre>
 * Quirks arraySupport = ArrayConverter.arrayConvertingQuirks(yourNormalQuirks);
 * </pre>
 *
 * @param <T>
 * https://github.com/aaberg/sql2o/issues/199
 *
 * @author Thilo Planz
 * editet by gdm
 */
public class ArrayConverter<T> implements Converter<T[]> {

    public final static ArrayConverter<String> STRING_ARRAY_CONVERTER = new ArrayConverter<String>(
            String.class);

    public final static ArrayConverter<Integer> INTEGER_ARRAY_CONVERTER = new ArrayConverter<Integer>(
            Integer.class);

    private final Class<T> componentType;

    private final Object[] emptyArray;

    public ArrayConverter(Class<T> componentType) {
        this.componentType = componentType;
        this.emptyArray = (Object[]) java.lang.reflect.Array.newInstance(
                componentType, 0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T[] convert(Object val) throws ConverterException {
        if (val == null) {
            return null;
        }

        if (val instanceof Array) {
            try {
                val = ((Array) val).getArray();
            } catch (Exception e) {
                throw new ConverterException(
                        "failed to retrieve data from JDBC array", e);
            }
        }

        if (val.getClass().isArray()) {
            if (val.getClass() == emptyArray.getClass()) {
                return (T[]) val;
            }
            int len = java.lang.reflect.Array.getLength(val);
            if (len == 0) {
                return (T[]) emptyArray;
            }
        }

        throw new ConverterException("Don't know how to convert type "
                + val.getClass().getName() + " to " + componentType.getName()
                + "[]");

    }

    @Override
    public Object toDatabaseParam(T[] val) {
        // https://github.com/aaberg/sql2o/issues/171
        // we cannot call JDBC's "conn#createArrayOf" here, as we have no
        // connection
        // so we just return the same array here.

        // Some databases (like H2DB) can handle that, for others, we need to
        // install a "quirk"
        // (which has access to the connection)
        // H2DB: can handle object arrays, but primitive arrays become OTHER
        // (not ARRAY)
        return val;
    }

    public static Quirks arrayConvertingQuirksForH2DB() {
        return arrayConvertingQuirks(new NoQuirks(), false, false);
    }

    public static Quirks arrayConvertingQuirksForPostgres() {
        return arrayConvertingQuirks(new PostgresQuirks(), true, false);
    }

    private final static Map<Class<?>, Converter<?>> arrayConverters;

    static {
        arrayConverters = new HashMap<>();
        arrayConverters.put(String[].class, STRING_ARRAY_CONVERTER);
        arrayConverters.put(Integer[].class, INTEGER_ARRAY_CONVERTER);
    }

    /**
     * decorates the given Quirks with additional quirks that handle creating
     * SQL arrays.As a result, you should be able to addParameter("name", javaArray).
     *
     *
     *
     *
     * @param databaseQuirks
     * @param useCreateArrayOf
     * @param promotePrimitiveArrays
     * @return  */
    public static Quirks arrayConvertingQuirks(final Quirks databaseQuirks,
            final boolean useCreateArrayOf, final boolean promotePrimitiveArrays) {

        return new Quirks() {
            @Override
            public void closeStatement(Statement arg0) throws SQLException {
                databaseQuirks.closeStatement(arg0);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <E> Converter<E> converterOf(Class<E> arg0) {
                if (arg0.isArray()) {
                    Converter<?> c = arrayConverters.get(arg0);
                    if (c != null) {
                        return (Converter<E>) c;
                    }
                }
                return databaseQuirks.converterOf(arg0);
            }

            @Override
            public String getColumnName(ResultSetMetaData arg0, int arg1)
                    throws SQLException {
                return databaseQuirks.getColumnName(arg0, arg1);
            }

            @Override
            public Object getRSVal(ResultSet arg0, int arg1)
                    throws SQLException {
                return databaseQuirks.getRSVal(arg0, arg1);
            }

            @Override
            public SqlParameterParsingStrategy getSqlParameterParsingStrategy() {
                return databaseQuirks.getSqlParameterParsingStrategy();
            }

            @Override
            public boolean returnGeneratedKeysByDefault() {
                return databaseQuirks.returnGeneratedKeysByDefault();
            }

            @Override
            public void setParameter(PreparedStatement arg0, int arg1,
                    InputStream arg2) throws SQLException {
                databaseQuirks.setParameter(arg0, arg1, arg2);
            }

            @Override
            public void setParameter(PreparedStatement arg0, int arg1, int arg2)
                    throws SQLException {
                databaseQuirks.setParameter(arg0, arg1, arg2);
            }

            @Override
            public void setParameter(PreparedStatement arg0, int arg1,
                    Integer arg2) throws SQLException {
                databaseQuirks.setParameter(arg0, arg1, arg2);
            }

            @Override
            public void setParameter(PreparedStatement arg0, int arg1, long arg2)
                    throws SQLException {
                databaseQuirks.setParameter(arg0, arg1, arg2);
            }

            @Override
            public void setParameter(PreparedStatement arg0, int arg1, Long arg2)
                    throws SQLException {
                databaseQuirks.setParameter(arg0, arg1, arg2);
            }

            @Override
            public void setParameter(PreparedStatement st, int pos, Object val)  throws SQLException {
                if (val != null && Sql2oArray.class.isAssignableFrom(val.getClass())) {
                    val = ((Sql2oArray) val).getArray();
                    Class<?> componentType = val.getClass().getComponentType();
                    if (componentType.isPrimitive() && promotePrimitiveArrays) {
                        // TODO: convert to wrapper instance array
                        throw new UnsupportedOperationException(
                                "primitive arrays are not supported yet");
                    }

                    if (useCreateArrayOf) {
                        if (componentType.isPrimitive()) {
                            // TODO: convert to wrapper instance array
                            throw new UnsupportedOperationException(
                                    "primitive arrays are not supported yet");
                        }
                        Connection conn = st.getConnection();

                        String type;
                        if (componentType == String.class) {
                            type = "varchar";
                        } else if (componentType == Integer.class) {
                            type = "integer";
                        } else {
                            throw new UnsupportedOperationException(
                                    "do not know the SQL type for "
                                    + componentType);
                        }

                        st.setArray(pos,
                                conn.createArrayOf(type, (Object[]) val));
                        return;
                    }
                }

                databaseQuirks.setParameter(st, pos, val);
            }

            @Override
            public void setParameter(PreparedStatement arg0, int arg1, String arg2) throws SQLException {
                databaseQuirks.setParameter(arg0, arg1, arg2);
            }

            @Override
            public void setParameter(PreparedStatement arg0, int arg1, Time arg2)
                    throws SQLException {
                databaseQuirks.setParameter(arg0, arg1, arg2);
            }

            @Override
            public void setParameter(PreparedStatement arg0, int arg1, Timestamp arg2) throws SQLException {
                databaseQuirks.setParameter(arg0, arg1, arg2);
            }

            @Override
            public void setParameter(PreparedStatement ps, int i, boolean bln) throws SQLException {
                databaseQuirks.setParameter(ps, i, bln);
            }

            @Override
            public void setParameter(PreparedStatement ps, int i, Boolean bln) throws SQLException {
                databaseQuirks.setParameter(ps, i, bln);
            }

            @Override
            public void setParameter(PreparedStatement ps, int i, UUID uuid) throws SQLException {
                databaseQuirks.setParameter(ps, i, uuid);
            }

        };
    }

}
