package it.bologna.ausl.sql2o.tools;

import java.util.List;

/**
 *
 * @author gdm
 */
public class Sql2oArray {
    private Object[] array;

    public Sql2oArray(Object[] array) {
        this.array = array;
    }

    public Sql2oArray(List<Object> list) {
        list.toArray();
    }

    public Object[] getArray() {
        return array;
    }

    public void setArray(Object[] array) {
        this.array = array;
    }
}
