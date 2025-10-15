/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.interceptors;

import it.bologna.ausl.internauta.utils.authorizationutils.session.AuthenticatedSessionData;
import it.bologna.ausl.internauta.utils.authorizationutils.session.AuthenticatedSessionDataBuilder;
import it.nextsw.common.interceptors.NextSdrEmptyControllerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Top
 */
public abstract class RibaltoneBaseInterceptor extends NextSdrEmptyControllerInterceptor {

    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;

    private static final Logger LOGGER = LoggerFactory.getLogger(RibaltoneBaseInterceptor.class);

    protected AuthenticatedSessionData getAuthenticatedUserProperties() {
        try {
            AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();

            return authenticatedUserProperties;

        } catch (Exception ex) {
            LOGGER.error("errore nel reperimento delle AuthenticatedUserProperties", ex);
            return null;
        }
    }

}
