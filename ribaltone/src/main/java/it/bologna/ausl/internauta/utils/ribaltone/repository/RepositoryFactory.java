package it.bologna.ausl.internauta.utils.ribaltone.repository;

import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author Top
 */
@Component
public class RepositoryFactory {

    @Autowired
    private RibaltoneConfiguration ribaltoneConfiguration;

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

    @Autowired
    private RibaltoneValidationCheckRepository ribaltoneValidationCheckRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PermissionManager permissionManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RibaltoneValidationCheckRepository getRibaltoneValidationCheckRepository() {
        return ribaltoneValidationCheckRepository;
    }

    public void setRibaltoneValidationCheckRepository(RibaltoneValidationCheckRepository ribaltoneValidationCheckRepository) {
        this.ribaltoneValidationCheckRepository = ribaltoneValidationCheckRepository;
    }

    public TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public RibaltoneConfiguration getRibaltoneConfiguration() {
        return ribaltoneConfiguration;
    }

    public void setRibaltoneConfiguration(RibaltoneConfiguration ribaltoneConfiguration) {
        this.ribaltoneConfiguration = ribaltoneConfiguration;
    }

}
