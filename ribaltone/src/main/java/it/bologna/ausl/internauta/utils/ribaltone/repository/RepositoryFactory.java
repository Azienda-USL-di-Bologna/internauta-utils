/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.blackbox.PermissionManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
public class RepositoryFactory {
    @Autowired
    private DatiImportatiTrasformazioneRepository datiImportatiTrasformazioneRepository;
    
    @Autowired
    private DatiImportatiStrutturaRepository datiImportatiStrutturaRepository;
    
    @Autowired
    private DatiImportatiAppartenenteRepository datiImportatiAppartenenteRepository;
    
    @Autowired
    private DatiImportatiAnagraficaRepository datiImportatiAnagraficaRepository;
    
    @Autowired
    private DatiDaImportareTrasformazioneRepository datiDaImportareTrasformazioneRepository;
    
    @Autowired
    private DatiDaImportareStrutturaRepository datiDaImportareStrutturaRepository;
    
    @Autowired
    private DatiDaImportareAppartenenteRepository datiDaImportareAppartenenteRepository;
    
    @Autowired
    private DatiDaImportareAnagraficaRepository datiDaImportareAnagraficaRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private PermissionManager permissionManager;

    public RepositoryFactory() {
    }
    
    

    public DatiImportatiTrasformazioneRepository getDatiImportatiTrasformazioneRepository() {
        return datiImportatiTrasformazioneRepository;
    }

    public DatiImportatiStrutturaRepository getDatiImportatiStrutturaRepository() {
        return datiImportatiStrutturaRepository;
    }

    public DatiImportatiAppartenenteRepository getDatiImportatiAppartenenteRepository() {
        return datiImportatiAppartenenteRepository;
    }

    public DatiImportatiAnagraficaRepository getDatiImportatiAnagraficaRepository() {
        return datiImportatiAnagraficaRepository;
    }

    public DatiDaImportareTrasformazioneRepository getDatiDaImportareTrasformazioneRepository() {
        return datiDaImportareTrasformazioneRepository;
    }

    public DatiDaImportareStrutturaRepository getDatiDaImportareStrutturaRepository() {
        return datiDaImportareStrutturaRepository;
    }

    public DatiDaImportareAppartenenteRepository getDatiDaImportareAppartenenteRepository() {
        return datiDaImportareAppartenenteRepository;
    }

    public DatiDaImportareAnagraficaRepository getDatiDaImportareAnagraficaRepository() {
        return datiDaImportareAnagraficaRepository;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public void setPermissionManager(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }
    
    
    
}
