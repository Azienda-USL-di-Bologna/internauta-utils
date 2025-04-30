/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.interceptors.ribaltonedati;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RibaltoneDataConfigurationRepository;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import it.bologna.ausl.model.entities.ribaltonedati.projections.RibaltoneDatiUtils;
import it.nextsw.common.data.annotations.NextSdrInterceptor;
import it.nextsw.common.interceptors.exceptions.AbortLoadInterceptorException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Tommaso
 */
@Component
@NextSdrInterceptor(name = "ribaltonedataconfiguration-interceptor")
public class RibaltoneDataConfigurationInterceptor extends RibaltoneBaseInterceptor {

    @Autowired
    RibaltoneDataConfigurationRepository ribaltoneDataConfigurationRepository;
    @Autowired
    RibaltoneDatiUtils ribaltoneDatiUtils;

    @Autowired
    private PermissionManager permissionManager;

    @Override
    public Class getTargetEntityClass() {
        return RibaltoneDataConfiguration.class;
    }

    @Override
    public Collection<Object> afterSelectQueryInterceptor(Collection<Object> entities, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        for (Object entity : entities) {
            entity = afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass);
        }
        return super.afterSelectQueryInterceptor(entities, additionalData, request, mainEntity, projectionClass);
    }

    
    
    @Override
    public Object afterSelectQueryInterceptor(Object entity, Map<String, String> additionalData, HttpServletRequest request, boolean mainEntity, Class projectionClass) throws AbortLoadInterceptorException {
        RibaltoneDataConfiguration r = (RibaltoneDataConfiguration) entity;
        
        //tolgo dal JSON specifiche le chiavi che contengono dati sensibili
        r.setSpecifiche(ribaltoneDatiUtils.getOnlySpecificheNonSensibili(r));

        return super.afterSelectQueryInterceptor(entity, additionalData, request, mainEntity, projectionClass);
    }

}
