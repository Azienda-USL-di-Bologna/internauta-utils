package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatorePluginException;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatorePluginExceptionRitentabile;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders.AllegatiBuilderUnimatica;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders.AllegatoUnimatica;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders.IndiceJsonBuilder;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders.MetadatiBuilder;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.versatore.QVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author boria
 */
@Component
public class UnimaticaVersatoreService extends VersatoreDocs {

    private static final Logger log = LoggerFactory.getLogger(UnimaticaVersatoreService.class);
    private static final String UNIMATICA_VERSATORE_SERVICE = "UnimaticaVersatoreService";

    private String unimaticaServizioVersamentoURI;

    @Override
    public void init(VersatoreConfiguration versatoreConfiguration) {
        super.init(versatoreConfiguration);
        Map<String, Object> versatoreConfigurationMap = this.versatoreConfiguration.getParams();
        Map<String, Object> unimaticaServiceConfiguration = (Map<String, Object>) versatoreConfigurationMap.get(UNIMATICA_VERSATORE_SERVICE);
        unimaticaServizioVersamentoURI = unimaticaServiceConfiguration.get("UnimaticaServizioVersamentoURI").toString();
        log.info("Unimatica servizio versamento URI: {}", unimaticaServizioVersamentoURI);
    }

    @Override
    public VersamentoDocInformation versaImpl(VersamentoDocInformation versamentoDocInformation) throws VersatoreProcessingException {
        Map<String, Object> mappaResultAndAllegati = new HashMap<>();
        //Reperisoco i risultati del versamento
        //TODO continuo con mappa allegati
        //       mappaResultAndAllegati = versaDocumentoUnimatica(versamentoDocInformation, null);

        return versamentoDocInformation;
    }

    /**
     * Metodo che si occupa di fare la chiamata al Gateway di Unimatica per versare il documento
     * indicato in versamentoDocInformation. Restituisce una mappa contenete ((TODO
     * l'indice json e)) i metadati versati, la response proveniente da Unimatica e la lista dei
     * VersamentoAllegatoInformation dei file versati.
     *
     * @param versamentoDocInformation
     * @return
     */
    //TODO togliere entity manager e vers conf e object mapper
    public Map<String, Object> versaDocumentoUnimatica(VersamentoDocInformation versamentoDocInformation, EntityManager entityManager, VersatoreRepositoryConfiguration versatoreRepositoryConfiguration, ObjectMapper objectMapper) throws VersatoreProcessingException {
        log.info("Inizio con il versamento del doc: " + Integer.toString(versamentoDocInformation.getIdDoc()));
        //preparo i dati
        Integer idDoc = versamentoDocInformation.getIdDoc();
        Map<String, Object> risultatoEVersamentiAllegati = new HashMap<>();
        Map<String, Object> parametriVersamento = versamentoDocInformation.getParams();
        try {
            Doc doc = entityManager.find(Doc.class, idDoc);
            try {
                //prendo l'archivio e il repsonsabile della gestione documentale
                Archivio archivio = new Archivio();
                List<ArchivioDoc> listaArchivioDocs = doc.getArchiviDocList();
                if (listaArchivioDocs.isEmpty()) {
                    throw new VersatorePluginException("Il documento non è collegato ad alcun fasciolo");
                }
                Persona responsabileGestioneDocumentale = new Persona();
                if (doc.getTipologia() != Doc.TipologiaDoc.RGPICO) {
                    //controllo che il documento non sia già stato versato per questo archivio radice
                    if (!(numeroVersamentiDocPerArchivio(idDoc, versamentoDocInformation.getIdArchivio(), entityManager) > 0)) {
                        archivio = entityManager.find(Archivio.class, versamentoDocInformation.getIdArchivio());
                        if (archivio.getId() != null && !listaArchivioDocs.isEmpty()) {
                            Archivio archivioDaLista = new Archivio();
                            for (ArchivioDoc archivioDoc : listaArchivioDocs) {
                                //controllo se il documento appartiene a un sottofasicolo o a un inserto anziché al fasciolo radice
                                //e controllo che non sia stato eliminato logicamente
                                if (archivioDoc.getIdArchivio().getIdArchivioRadice().getId().equals(archivio.getId()) && archivioDoc.getDataEliminazione() == null) {
                                    archivioDaLista = archivioDoc.getIdArchivio();
                                }
                            }
                            if (archivioDaLista.getId() != null) {
                                archivio = archivioDaLista;
                            } else {
                                //se archivioDaLista è vuoto allora il documento è stato eliminato logicamente dal fasicolo, e non è collegato ad altri fascicoli,
                                //in quel caso non verso il documento
                                //TODO response unimatica
                                //response.setErrorMessage("Il documento è stato cancellato logicamente dal fascicolo");
                                //response.setResponseCode(CANCELLATO);
                                //risultatoEVersamentiAllegati.put("response", response);
                                return risultatoEVersamentiAllegati;
                            }
                        } else {
                            throw new VersatorePluginException("Il documento non è collegato ad alcun fasciolo");
                        }
                    } else {
                        //se il documento è già stato versato per questo fascicolo radice non proseguo con il versamento
                        //TODO response unimatica
                        //response.setErrorMessage("Il documento è già stato versato per questa fasicolazione");
                        //response.setResponseCode(CANCELLATO);
                        //risultatoEVersamentiAllegati.put("response", response);
                        return risultatoEVersamentiAllegati;
                    }
                } else {
                    if (listaArchivioDocs.get(0).getId() != null) {
                        archivio = listaArchivioDocs.get(0).getIdArchivio();
                    } else {
                        throw new VersatorePluginException("Il documento non è collegato ad alcun fasciolo");
                    }
                    String codiceFiscaleResponsabileGestioneDocumentale = (String) parametriVersamento.get("codiceFiscaleResponsabileGestioneDocumentale");
                    if (!codiceFiscaleResponsabileGestioneDocumentale.isEmpty()
                        && codiceFiscaleResponsabileGestioneDocumentale != null
                        && codiceFiscaleResponsabileGestioneDocumentale != "") {
                        responsabileGestioneDocumentale = personaDaCodiceFiscaleEAzienda(codiceFiscaleResponsabileGestioneDocumentale, doc.getIdAzienda().getId(), entityManager);
                    } else {
                        throw new VersatorePluginException("Non è stato indicato il Responsabile della Gestione Documentale");
                    }
                }
                log.info("accedo ai dati degli allegati e li inserisco nell'XML");
                List<Allegato> allegati = doc.getAllegati();
                //TODO cambia configuration
                AllegatiBuilderUnimatica allegatiBuild = new AllegatiBuilderUnimatica(versatoreRepositoryConfiguration);
                Map<String, Object> mappaDatiAllegati = allegatiBuild.buildMappaAllegati(doc, allegati);
                AllegatoUnimatica documentoPrincipale = (AllegatoUnimatica) mappaDatiAllegati.get("documentoPrincipale");
                List<AllegatoUnimatica> allegatiSecondariList = (List<AllegatoUnimatica>) mappaDatiAllegati.get("allegatiSecondari");
                //creazione dell xml dei metadati
                MetadatiBuilder metadatiBuilder = new MetadatiBuilder(parametriVersamento, doc, archivio, documentoPrincipale, allegatiSecondariList);
                metadatiBuilder.build();
                String metadati = metadatiBuilder.toString();
                risultatoEVersamentiAllegati.put("metadati", metadati);
                byte[] fileMetadati = metadati.getBytes(StandardCharsets.UTF_8);
                //calcolo lo sha 256 del file di metadati
                String sha256HexMetadati = "";
                try (InputStream is = new ByteArrayInputStream(fileMetadati)) {
                    sha256HexMetadati = DigestUtils.sha256Hex(is);
                } catch (IOException ex) {
                    log.error("Errore nel calcoalre l'hashSHA256 di metadati.xml", ex);
                    throw new VersatorePluginException("Errore nel calcoalre l'hashSHA256 di metadati.xml");
                }
                //creazione dell'indice json
                IndiceJsonBuilder indiceJsonBuilder = new IndiceJsonBuilder(parametriVersamento, doc, documentoPrincipale, allegatiSecondariList, sha256HexMetadati);
                Map<String, Object> indiceJsonMap = indiceJsonBuilder.build();
                String indiceJsonString = objectMapper.writeValueAsString(indiceJsonMap);
                risultatoEVersamentiAllegati.put("indiceJson", indiceJsonString);
            } catch (VersatorePluginException e) {
                log.error("Errore:", e);
                //TODO fare una response per unimatica
                //response.setErrorMessage(e.getMessage());
                //response.setResponseCode(ERRORE_PLUG_IN);
            } catch (VersatorePluginExceptionRitentabile e) {
                log.error("Errore:", e);
                //TODO fare una response per unimatica
                //response.setErrorMessage(e.getMessage());
                //response.setResponseCode(ERRORE_PLUG_IN_RITENTABILE);
            }
        } catch (Exception e) {
            //TODO fare una response per unimatica
            //response.setErrorMessage("Causa errore: " + e.getCause() + ", messaggio: " + e.getMessage());
            //response.setResponseCode(ERRORE_PLUG_IN);
            log.error("Causa errore: " + e.getCause() + ", messaggio: " + e.getMessage(), e);
        }
        //chiamata autenticazione
        //creazione del json
        //stream dei file
        //creazione del multipart
        //invio
        //lettura esito
        return risultatoEVersamentiAllegati;
    }

    /**
     * Metodo che conta quante volte un documento è stato versato per uno stesso
     * archivio
     *
     * @param idDoc
     * @param idArchivio
     * @return
     */
    //TODO levare entity manager
    private Long numeroVersamentiDocPerArchivio(Integer idDoc, Integer idArchivio, EntityManager entityManager) {
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        Long numeroDocVersati = jPAQueryFactory.select(QVersamento.versamento.count())
            .from(QVersamento.versamento)
            .where(QVersamento.versamento.idDoc.id.eq(idDoc)
                .and(QVersamento.versamento.idArchivio.id.eq(idArchivio))
                .and(QVersamento.versamento.stato.eq(Versamento.StatoVersamento.VERSATO.toString())))
            .fetchOne();
        return numeroDocVersati;
    }

    /**
     * Metodo che dato un codice fiscale ti restituisce la persona
     *
     * @param codiceFiscale
     * @return
     */
    //TODO levare entity manager
    private Persona personaDaCodiceFiscaleEAzienda(String codiceFiscale, Integer idAzienda, EntityManager entityManager) {
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        Persona persona = jPAQueryFactory.select(QPersona.persona)
            .from(QPersona.persona)
            .where(QPersona.persona.codiceFiscale.eq(codiceFiscale)
                .and(QPersona.persona.idAziendaDefault.id.eq(idAzienda)))
            .fetchOne();
        return persona;
    }
}
