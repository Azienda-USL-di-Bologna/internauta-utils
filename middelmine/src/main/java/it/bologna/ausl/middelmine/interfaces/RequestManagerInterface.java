package it.bologna.ausl.middelmine.interfaces;

import okhttp3.Request;

/**
 *
 * @author Salo
 */
public interface RequestManagerInterface {

    void prepareRequest();

    void prepareRequest(Object object);

    Request getRequest();
}
