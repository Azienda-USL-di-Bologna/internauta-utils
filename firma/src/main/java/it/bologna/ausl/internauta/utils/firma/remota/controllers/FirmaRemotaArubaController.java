package it.bologna.ausl.internauta.utils.firma.remota.controllers;

import com.querydsl.core.types.Predicate;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaHttpException;
import it.bologna.ausl.internauta.utils.firma.repositories.DominioArubaRepository;
import it.bologna.ausl.model.entities.firma.Configuration;
import it.bologna.ausl.model.entities.firma.DominioAruba;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author gdm
 */

@RestController
@RequestMapping(value = "${firma.remota.mapping.url}/aruba")
public class FirmaRemotaArubaController {
    
    @Autowired
    private DominioArubaRepository dominioArubaRepository;
    
    @RequestMapping(value = "/domini", method = RequestMethod.GET)
    public Iterable<DominioAruba> domini(
            @QuerydslPredicate(root = DominioAruba.class) Predicate predicate) throws FirmaRemotaHttpException, FirmaRemotaConfigurationException {
        Iterable<DominioAruba> res = dominioArubaRepository.findAll(predicate);
        return res;
    }
    
    @RequestMapping(value = "/getHostIdFromDominio", method = RequestMethod.GET)
    public String getHostIdFromDominio(
            DominioAruba dominioAruba) throws FirmaRemotaHttpException, FirmaRemotaConfigurationException {
        return null;
    }
}
