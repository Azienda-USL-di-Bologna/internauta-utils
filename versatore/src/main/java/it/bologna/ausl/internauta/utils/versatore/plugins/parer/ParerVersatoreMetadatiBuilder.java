/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.parer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.plugins.infocert.InfocertVersatoreService;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.AttoreDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import static it.bologna.ausl.model.entities.scripta.DocDetailInterface.TipologiaDoc.DELIBERA;
import static it.bologna.ausl.model.entities.scripta.DocDetailInterface.TipologiaDoc.DETERMINA;
import static it.bologna.ausl.model.entities.scripta.DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA;
import static it.bologna.ausl.model.entities.scripta.DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA;
import static it.bologna.ausl.model.entities.scripta.DocDetailInterface.TipologiaDoc.RGDELI;
import static it.bologna.ausl.model.entities.scripta.DocDetailInterface.TipologiaDoc.RGDETE;
import static it.bologna.ausl.model.entities.scripta.DocDetailInterface.TipologiaDoc.RGPICO;
import it.bologna.ausl.model.entities.scripta.QAllegato;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QAttoreDoc;
import it.bologna.ausl.model.entities.scripta.QDocDetail;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.riversamento.builder.DatiSpecificiBuilder;
import it.bologna.ausl.riversamento.builder.IdentityFile;
import it.bologna.ausl.riversamento.builder.ProfiloArchivistico;
import it.bologna.ausl.riversamento.builder.UnitaDocumentariaBuilder;
import it.bologna.ausl.riversamento.builder.oggetti.DatiSpecifici;
import it.bologna.ausl.riversamento.builder.oggetti.UnitaDocumentaria;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author utente
 */
@Component
public final class ParerVersatoreMetadatiBuilder {
    
    @Autowired
    private ParametriAziendeReader parametriAziende;

    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private VersatoreRepositoryConfiguration versatoreRepositoryConfiguration;
    
    

    private static final Logger log = LoggerFactory.getLogger(ParerVersatoreMetadatiBuilder.class);

    public Map<String,Object> ParerVersatoreMetadatiBuilder(Doc doc, DocDetail docDetail, String enteVersamento, String userID, String version, String ambiente, String struttura, String tipoConservazione, String codifica, String versioneDatiSpecificiPico,String versioneDatiSpecificiDete,String versioneDatiSpecificiDeli,String versioneDatiSpecificiRg, Boolean includiNote, String tipoDocumentoDefault,String forzaCollegamento, String forzaAccettazione, String forzaConservazione)throws DatatypeConfigurationException, JAXBException, ParseException{
       
//        List<ArchivioDoc> archiviazioni = entityManager.createQuery("SELECT * FROM scripta.archivi_docs ad where ad.id_doc = :value1 order by ad.data_archiviziazione ASC")
//                .setParameter("value1", doc.getId()).getResultList();
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        List<ArchivioDoc> archiviazioni = jPAQueryFactory
                .select(QArchivioDoc.archivioDoc)
                .from(QArchivioDoc.archivioDoc)
                .where(QArchivioDoc.archivioDoc.idDoc.id.eq(doc.getId()).and(QArchivioDoc.archivioDoc.dataEliminazione.isNull()))
                .orderBy(QArchivioDoc.archivioDoc.dataArchiviazione.asc())
                .fetch();
        Map<String,Object> mappaUnitaDocumentaria = new HashMap<>();
        
        
 
        
        if (archiviazioni != null && archiviazioni.size() != 0) { 
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS[xxx]");
            String dataArchiviazione = archiviazioni.get(0).getDataArchiviazione().format(formatter);
            String dataPerDatiSpecifici = archiviazioni.get(0).getDataArchiviazione().toLocalDate().toString();
            try {
                log.info("buildo il profilo archivistico");
                ProfiloArchivistico profiloArchivistico = buildProfiloArchivistico(doc, docDetail, archiviazioni);
                try {
                    log.info("buildo i dati specifici");
                    DatiSpecifici datiSpecifici = null;
                    if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA) {
                        datiSpecifici = buildDatiSpecifici(doc, docDetail, dataPerDatiSpecifici, versioneDatiSpecificiPico, versioneDatiSpecificiDete, versioneDatiSpecificiDeli);
                    } else if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGPICO || doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGDELI || doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGDETE) {
                        datiSpecifici = buildDatiSpecificiRegistroGiornaliero(doc, docDetail, versioneDatiSpecificiRg);
                    }
                    
                    UnitaDocumentariaBuilder unitaDocumentariaBuilder;
                    DecimalFormat df = new DecimalFormat("0000000");
                    String numeroRegistrazione = df.format(docDetail.getNumeroRegistrazione());
                    unitaDocumentariaBuilder = new UnitaDocumentariaBuilder(numeroRegistrazione, docDetail.getAnnoRegistrazione(), traduzioneTipologiaRegistro(doc.getTipologia()), traduzioneTipologiaParerPerDatiSpecifici(doc.getTipologia()), forzaConservazione, forzaAccettazione, forzaCollegamento, profiloArchivistico, doc.getOggetto(), dataArchiviazione, datiSpecifici, version , ambiente, enteVersamento, struttura, userID, tipoConservazione, codifica);
                    Map<String, Object> mappaUnitaDocumentariaEAllegati = buildAllegati(doc,docDetail, unitaDocumentariaBuilder, tipoDocumentoDefault,includiNote);
                    UnitaDocumentariaBuilder unitaDocumentariaBuilderConAllegati = (UnitaDocumentariaBuilder) mappaUnitaDocumentariaEAllegati.get("unitaDocumentariaBuilder");
                    List<VersamentoAllegatoInformation> versamentiAllegatiInformation = (List<VersamentoAllegatoInformation>) mappaUnitaDocumentariaEAllegati.get("versamentiAllegatiInfo");
                    mappaUnitaDocumentaria.put("unitaDocumentaria", unitaDocumentariaBuilderConAllegati);
                    mappaUnitaDocumentaria.put("identityFiles", unitaDocumentariaBuilderConAllegati.getIdentityFiles());
                    mappaUnitaDocumentaria.put("versamentiAllegatiInformation", versamentiAllegatiInformation);
                    return mappaUnitaDocumentaria;
                    
                } catch (ParserConfigurationException ex) {
                    java.util.logging.Logger.getLogger(ParerVersatoreService.class.getName()).log(Level.SEVERE, null, ex);
                } 
            } catch (Exception ex) {
                log.error("Qualcosa non ha funzionato nel buildare il profilo archivistico", ex);
            }

        }
        return null;

    }

    private DatiSpecifici buildDatiSpecifici(Doc doc, DocDetail docDetail, String dataArchiviazione, String versioneDatiSpecificiPico,String versioneDatiSpecificiDete,String versioneDatiSpecificiDeli) throws ParserConfigurationException {

        DatiSpecifici datiSpecifici = new DatiSpecifici();
        ObjectMapper mapper = new ObjectMapper();

        DatiSpecificiBuilder datiSpecificiBuilder = new DatiSpecificiBuilder();

        if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) {
            datiSpecificiBuilder.insertNewTag("UnitaOperativaProponente", docDetail.getIdStrutturaRegistrazione().getNome());
        }
        List<AttoreDoc> attori = doc.getAttoriList();
        String nomeResponsabile = "";
        String vistiString = "";
        String firmatariString = "";
        String nomeDirettoreAmministrativo = "";
        String nomeDirettoreSanitario = "";
        String nomeDirettoreGenerale = "";
        String operatoreDiProtocollo = "";
        if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA) {
            datiSpecificiBuilder.insertNewTag("Destinatario", "Vedi annesso elenco destinatari");
            datiSpecificiBuilder.insertNewTag("Movimento", "OUT");
            datiSpecificiBuilder.insertNewTag("ModalitaTrasmissione", "BABEL");
        }
        for (AttoreDoc attore : attori) {
            if (attore.getRuolo() == AttoreDoc.RuoloAttoreDoc.RESPONSABILE_PROCEDIMENTO) {
                nomeResponsabile = attore.getIdPersona().getDescrizione() + " (" + attore.getIdStruttura().getNome() + ")";
            }
            if (attore.getRuolo() == AttoreDoc.RuoloAttoreDoc.VISTI) {
                if (vistiString.equals("")) {
                    vistiString = vistiString + attore.getIdPersona().getDescrizione();
                } else {
                    vistiString = vistiString + "; " + attore.getIdPersona().getDescrizione();
                }
            }
            if (attore.getRuolo() == AttoreDoc.RuoloAttoreDoc.FIRMA) {
                if (firmatariString.equals("")) {
                    firmatariString = firmatariString + "&amp;lt;nominativo&amp;gt;" + attore.getIdPersona().getDescrizione() + "&amp;lt;/nominativo&amp;gt;";
                } else {
                    firmatariString = firmatariString + "; " + "&amp;lt;nominativo&amp;gt;" + attore.getIdPersona().getDescrizione()+ "&amp;lt;/nominativo&amp;gt;";
                }
            }
            if (attore.getRuolo() == AttoreDoc.RuoloAttoreDoc.DIRETTORE_AMMINISTRATIVO) {
                nomeDirettoreAmministrativo = attore.getIdPersona().getDescrizione();
            }
            if (attore.getRuolo() == AttoreDoc.RuoloAttoreDoc.DIRETTORE_SANITARIO) {
                nomeDirettoreSanitario = attore.getIdPersona().getDescrizione();
            }
            if (attore.getRuolo() == AttoreDoc.RuoloAttoreDoc.DIRETTORE_GENERALE) {
                nomeDirettoreGenerale = attore.getIdPersona().getDescrizione();
            }
            if (attore.getRuolo() == AttoreDoc.RuoloAttoreDoc.RICEZIONE) {
                operatoreDiProtocollo = attore.getIdPersona().getCodiceFiscale();
            }
        }
        if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA && !firmatariString.equals("")) {
                datiSpecificiBuilder.insertNewTag("Proponente", firmatariString);
            }
        if (!nomeResponsabile.equals("") && (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA)) {
            datiSpecificiBuilder.insertNewTag("ResponsabileDelProcedimento", nomeResponsabile);
        }
        if (!nomeResponsabile.equals("") && (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA)) {
            datiSpecificiBuilder.insertNewTag("ResponsabileProcedimento", nomeResponsabile);
        }
        if (!vistiString.equals("")) {
            datiSpecificiBuilder.insertNewTag("Visti", vistiString);
        }
        if (!firmatariString.equals("")) {
            if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA) {
                datiSpecificiBuilder.insertNewTag("Firmatario", firmatariString);
            }
            if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) {
                datiSpecificiBuilder.insertNewTag("FirmatarioAtto", firmatariString);
                datiSpecificiBuilder.insertNewTag("UnitaOperativaFirmatarioAtto", docDetail.getIdStrutturaRegistrazione().getNome());
            }
            
        }
        if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA) {
            if(nomeDirettoreAmministrativo != null) {
                datiSpecificiBuilder.insertNewTag("DirettoreAmministrativo", nomeDirettoreAmministrativo);
            } else {
                datiSpecificiBuilder.insertNewTag("CausaleAssenzaDA", "assente");
            }
            if(nomeDirettoreSanitario != null) {
                datiSpecificiBuilder.insertNewTag("DirettoreSanitario", nomeDirettoreSanitario);
            } else {
                datiSpecificiBuilder.insertNewTag("CausaleAssenzaDS", "assente");
            }
            if(nomeDirettoreGenerale != null) {
                datiSpecificiBuilder.insertNewTag("DirettoreGenerale", nomeDirettoreGenerale);
            } else {
                datiSpecificiBuilder.insertNewTag("CausaleAssenzaDG", "assente");
            }
            HashMap<String, Object> additionalDataDoc = new HashMap<String, Object>();

            additionalDataDoc = (HashMap<String, Object>) doc.getAdditionalData();
//            additionalDataDoc = (HashMap<String, Object>) doc.getAdditionalData();
            Boolean controlloRegionale = (Boolean) additionalDataDoc.get("controllo_regionale");
            if (controlloRegionale) {
                datiSpecificiBuilder.insertNewTag("ControlloRegionale", "SI");
            } else {
                datiSpecificiBuilder.insertNewTag("ControlloRegionale", "NO");
            }
        }

        if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) {
            if(doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) {
                datiSpecificiBuilder.insertNewTag("Destinatari", "Vedi annesso elenco destinatari");
            }
            HashMap<String, Object> additionalDataDoc = new HashMap<String, Object>();
            additionalDataDoc = (HashMap<String, Object>) doc.getAdditionalData();
            HashMap<String, Object> datiPubblicazione = new HashMap<String, Object>();
            datiPubblicazione = (HashMap<String, Object>) additionalDataDoc.get("dati_pubblicazione");
            LocalDateTime dataEsecutivita = null;
            LocalDateTime inizioPubblicazione = null;
            LocalDateTime finePubblicazione = null;
            if(datiPubblicazione != null) {
                dataEsecutivita = (LocalDateTime) LocalDateTime.parse((String) datiPubblicazione.get("data_esecutivita"));
                inizioPubblicazione = (LocalDateTime) LocalDateTime.parse((String) datiPubblicazione.get("inizio_pubblicazione"));
                finePubblicazione = (LocalDateTime) LocalDateTime.parse((String) datiPubblicazione.get("fine_pubblicazione"));
            }
            
            
            if (dataEsecutivita != null) {
                datiSpecificiBuilder.insertNewTag("EsecutivitaData", dataEsecutivita.toLocalDate().toString());
            }
            if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA) {
                String noteEsecutivita = (String) additionalDataDoc.get("note_esecutivita");
                datiSpecificiBuilder.insertNewTag("EsecutivitaNote", noteEsecutivita);
            }
            if(doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA) {
                datiSpecificiBuilder.insertNewTag("Destinatari", "Vedi annesso elenco destinatari");
            }
            datiSpecificiBuilder.insertNewTag("PubblicazioneRegistro", "ALBO ON LINE");
            if(datiPubblicazione != null) {
                datiSpecificiBuilder.insertNewTag("PubblicazioneAnno", datiPubblicazione.get("anno").toString());
                datiSpecificiBuilder.insertNewTag("PubblicazioneNumero", datiPubblicazione.get("numero").toString());
                datiSpecificiBuilder.insertNewTag("PubblicazioneInizio", inizioPubblicazione.toLocalDate().toString());
                datiSpecificiBuilder.insertNewTag("PubblicazioneFine", finePubblicazione.toLocalDate().toString());
            }
            
            if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) {
                datiSpecificiBuilder.insertNewTag("PubblicazioneTipo", "INTEGRALE");
            }
            
            datiSpecificiBuilder.insertNewTag("IdentificazioneRepository", "GEDI");
        } 
        if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA) {
            datiSpecificiBuilder.insertNewTag("Mittente", docDetail.getMittente());
            datiSpecificiBuilder.insertNewTag("Movimento", "IN");
            datiSpecificiBuilder.insertNewTag("ModalitaTrasmissione", "BABEL");
            datiSpecificiBuilder.insertNewTag("OperatoreDiProtocollo", operatoreDiProtocollo);
        }
        if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA) {
            datiSpecificiBuilder.insertNewTag("DataFascicolazione", dataArchiviazione);
            datiSpecificiBuilder.insertNewTag("IdentificazioneRepository", "GEDI");
            datiSpecificiBuilder.insertNewTag("Visibilita", "LIBERA");
            datiSpecificiBuilder.insertNewTag("Consultabilita", "NON PRECISATA");

        }
        if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) {
            HashMap<String, Object> additionalDataDoc = new HashMap<String, Object>();
            additionalDataDoc = (HashMap<String, Object>) doc.getAdditionalData();
            HashMap<String, Object> metadatiTrasparenza = new HashMap<String, Object>();
            if(additionalDataDoc != null && additionalDataDoc.get("metadati_trasparenza") != null) {
                metadatiTrasparenza = (HashMap<String, Object>) additionalDataDoc.get("metadati_trasparenza");
                String descrizione = (String) metadatiTrasparenza.get("descrizione");
                String tipoProvvedimento = (String) metadatiTrasparenza.get("id_tipo_provvedimento");
                if (descrizione != null || !descrizione.equals("")) {
                    datiSpecificiBuilder.insertNewTag("TipologiaAtto", descrizione);
                }
                if (!tipoProvvedimento.equals("non_rilevante       ")) {
                    String spesa = (String) metadatiTrasparenza.get("spesa");
                    String contenuto = (String) metadatiTrasparenza.get("contenuto");
                    String estremiDocumenti = (String) metadatiTrasparenza.get("estremi_documento");
                    if (contenuto != null) {
                        datiSpecificiBuilder.insertNewTag("Contenuto", contenuto);
                    }
                    if (spesa != null) {
                        datiSpecificiBuilder.insertNewTag("SpesaPrevista", spesa);
                    }
                    if (estremiDocumenti != null) {
                        datiSpecificiBuilder.insertNewTag("PrincipaliDocumenti", estremiDocumenti);
                    }
                }
            }
            
            
        }
        datiSpecifici = datiSpecificiBuilder.getDatiSpecifici();
        if(doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA) {
            datiSpecifici.setVersioneDatiSpecifici(versioneDatiSpecificiPico);
        } else if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) {
            datiSpecifici.setVersioneDatiSpecifici(versioneDatiSpecificiDete);
        } else {
            datiSpecifici.setVersioneDatiSpecifici(versioneDatiSpecificiDeli);
        }

        return datiSpecifici;
    }

    private ProfiloArchivistico buildProfiloArchivistico(Doc doc, DocDetail docDetail, List<ArchivioDoc> archiviazioni) throws Exception {
        ProfiloArchivistico profiloArchivistico = new ProfiloArchivistico();

        if (archiviazioni != null) {

            for (ArchivioDoc fascicolazioneSecondaria : archiviazioni) {

                if (fascicolazioneSecondaria == archiviazioni.get(0)) {
                    JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
                    Archivio archivioPrincipale = jPAQueryFactory
                    .select(QArchivio.archivio)
                    .from(QArchivio.archivio)
                    .where(QArchivio.archivio.id.eq(archiviazioni.get(0).getIdArchivio().getId()))
                    .fetchOne();
//                    Archivio archivioPrincipale = entityManager.find(Archivio.class, archiviazioni.get(0).getIdArchivio());
                    if (archivioPrincipale.getLivello() == 1) {
                        profiloArchivistico.addFascicoloPrincipale(
                                archivioPrincipale.getIdTitolo().getClassificazione().replaceAll("\\-", "\\."),
                                archivioPrincipale.getAnno().toString(),
                                archivioPrincipale.getNumero().toString(),
                                archivioPrincipale.getOggetto(),
                                "",
                                "",
                                "",
                                "");
                    } else if (archivioPrincipale.getLivello() == 2) {
                        profiloArchivistico.addFascicoloPrincipale(
                                archivioPrincipale.getIdTitolo().getClassificazione().replaceAll("\\-", "\\."),
                                archivioPrincipale.getAnno().toString(),
                                archivioPrincipale.getIdArchivioPadre().getNumero().toString(),
                                archivioPrincipale.getIdArchivioPadre().getOggetto(),
                                archivioPrincipale.getNumero().toString(),
                                archivioPrincipale.getOggetto(),
                                "",
                                "");
                    } else {
                        profiloArchivistico.addFascicoloPrincipale(
                                archivioPrincipale.getIdTitolo().getClassificazione().replaceAll("\\-", "\\."),
                                archivioPrincipale.getIdArchivioRadice().getAnno().toString(),
                                archivioPrincipale.getIdArchivioRadice().getNumero().toString(),
                                archivioPrincipale.getIdArchivioRadice().getOggetto(),
                                archivioPrincipale.getIdArchivioPadre().getNumero().toString(),
                                archivioPrincipale.getIdArchivioPadre().getOggetto(),
                                archivioPrincipale.getNumero().toString(),
                                archivioPrincipale.getOggetto()
                        );

                    }
                } else {
                    JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
                    Archivio archivioSecondario = jPAQueryFactory
                    .select(QArchivio.archivio)
                    .from(QArchivio.archivio)
                    .where(QArchivio.archivio.id.eq(fascicolazioneSecondaria.getIdArchivio().getId()))
                    .fetchOne();
                    if (archivioSecondario.getLivello() == 1) {
                        profiloArchivistico.addFascicoloSecondario(
                                archivioSecondario.getIdTitolo().getClassificazione().replaceAll("\\-", "\\."),
                                archivioSecondario.getAnno().toString(),
                                archivioSecondario.getNumero().toString(),
                                archivioSecondario.getOggetto(),
                                "",
                                "",
                                "",
                                "");
                    } else if (archivioSecondario.getLivello() == 2) {
                        profiloArchivistico.addFascicoloSecondario(
                                archivioSecondario.getIdTitolo().getClassificazione().replaceAll("\\-", "\\."),
                                archivioSecondario.getAnno().toString(),
                                archivioSecondario.getIdArchivioPadre().getNumero().toString(),
                                archivioSecondario.getIdArchivioPadre().getOggetto(),
                                archivioSecondario.getNumero().toString(),
                                archivioSecondario.getOggetto(),
                                "",
                                "");
                    } else {
                        profiloArchivistico.addFascicoloSecondario(
                                archivioSecondario.getIdTitolo().getClassificazione().replaceAll("\\-", "\\."),
                                archivioSecondario.getIdArchivioRadice().getAnno().toString(),
                                archivioSecondario.getIdArchivioRadice().getNumero().toString(),
                                archivioSecondario.getIdArchivioRadice().getOggetto(),
                                archivioSecondario.getIdArchivioPadre().getNumero().toString(),
                                archivioSecondario.getIdArchivioPadre().getOggetto(),
                                archivioSecondario.getNumero().toString(),
                                archivioSecondario.getOggetto()
                        );

                    }
                }

            }

        } else {
            throw new Exception("Non ho trovato nessuna archiviazione");
        }

        return profiloArchivistico;
    }

    private  Map<String, Object> buildAllegati(Doc doc,DocDetail docDetail, UnitaDocumentariaBuilder unitaDocumentariaBuilder, String tipoDocumentoDefault, Boolean includiNoteParer) throws DatatypeConfigurationException, ParseException, MinIOWrapperException {
        Map<String, Object> mappaPerAllegati = new HashMap<>();
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);

        List<Allegato> allegati = jPAQueryFactory
                    .select(QAllegato.allegato)
                    .from(QAllegato.allegato)
                    .where(QAllegato.allegato.idDoc.id.eq(doc.getId()))
                    .fetch();
//        List<Allegato> allegati = entityManager.createQuery("SELECT * FROM scripta.allegati a where a.id_doc = :value1")
//                .setParameter("value1", doc.getId()).getResultList();
//        IdentityFile identityFilePrincipale;
        Integer i = 1;
        Integer indexCommittente = 1;
        Integer indexAlbo = 1;
        List<VersamentoAllegatoInformation> versamentiAllegatiInfo = new ArrayList();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS[xxx]");
        for (Allegato allegato : allegati) {
            if (allegato.getTipo() != Allegato.TipoAllegato.ANNESSO && allegato.getTipo() != Allegato.TipoAllegato.ANNOTAZIONE) {
                if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA && allegato.getPrincipale() == true) {
                    Allegato.DettaglioAllegato originale = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("allegato principale", getUuidMinIObyFileId(originale.getIdRepository()), originale.getHashMd5(), originale.getEstensione(), originale.getMimeType());
                    unitaDocumentariaBuilder.addDocumentoPrincipale(getUuidMinIObyFileId(originale.getIdRepository()), traduzioneTipologiaParer(doc.getTipologia()), "", "", 1, identityFilePrincipale, docDetail.getDataRegistrazione().format(formatter), tipoDocumentoDefault, "DocumentoGenerico", "FILE", getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                    
                }else  if ((doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGDELI || doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGPICO ||doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGDETE )&& allegato.getPrincipale() == true) {
                    Allegato.DettaglioAllegato originale = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("allegato principale", getUuidMinIObyFileId(originale.getIdRepository()), originale.getHashMd5(), originale.getEstensione(), originale.getMimeType());
                    unitaDocumentariaBuilder.addDocumentoPrincipale(getUuidMinIObyFileId(originale.getIdRepository()), traduzioneTipologiaParer(doc.getTipologia()), "", "", 1, identityFilePrincipale, docDetail.getDataRegistrazione().format(formatter), tipoDocumentoDefault, "DocumentoGenerico", "FILE", getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA && allegato.getFirmato() == true && allegato.getTipo() == Allegato.TipoAllegato.TESTO) {
                    Allegato.DettaglioAllegato originaleFirmato = allegato.getDettagli().getOriginaleFirmato();
                    IdentityFile identityFilePrincipale = new IdentityFile("letterafirmata.pdf", getUuidMinIObyFileId(originaleFirmato.getIdRepository()), originaleFirmato.getHashMd5(), "PDF", "application/pdf");
                    unitaDocumentariaBuilder.addDocumentoPrincipale(getUuidMinIObyFileId(originaleFirmato.getIdRepository()), traduzioneTipologiaParer(doc.getTipologia()), "", "", 1, identityFilePrincipale, docDetail.getDataRegistrazione().format(formatter), tipoDocumentoDefault, "DocumentoGenerico", "FILE", getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegatoFirmato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if ((doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) && allegato.getFirmato() == true && allegato.getTipo() == Allegato.TipoAllegato.TESTO) {
                    Allegato.DettaglioAllegato originaleFirmato = allegato.getDettagli().getOriginaleFirmato();
                    IdentityFile identityFilePrincipale = new IdentityFile("testofirmato.pdf", getUuidMinIObyFileId(originaleFirmato.getIdRepository()), originaleFirmato.getHashMd5(), "PDF", "application/pdf");
                    unitaDocumentariaBuilder.addDocumentoPrincipale(getUuidMinIObyFileId(originaleFirmato.getIdRepository()), traduzioneTipologiaParer(doc.getTipologia()), "", "", 1, identityFilePrincipale, docDetail.getDataRegistrazione().format(formatter), tipoDocumentoDefault, "DocumentoGenerico", "FILE", getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegatoFirmato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getFirmato() == true && allegato.getTipo() != Allegato.TipoAllegato.TESTO) {
                    Allegato.DettaglioAllegato originaleFirmato = allegato.getDettagli().getOriginaleFirmato();
                    IdentityFile identityFilePrincipale = new IdentityFile("allegato_firmato", getUuidMinIObyFileId(originaleFirmato.getIdRepository()), originaleFirmato.getHashMd5(), originaleFirmato.getEstensione(), originaleFirmato.getMimeType());
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(originaleFirmato.getIdRepository()),"GENERICO", "", originaleFirmato.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Allegato", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegatoFirmato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getDettagli().getConvertito() != null && allegato.getTipo() != Allegato.TipoAllegato.TESTO) {
//                    Allegato.DettaglioAllegato convertito = allegato.getDettagli().getConvertito();
//                    IdentityFile identityFilePrincipale = new IdentityFile("allegato_convertito", getUuidMinIObyFileId(convertito.getIdRepository()), convertito.getHashMd5(), "PDF", "application/pdf");
//                    i = i + 1;
//                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(convertito.getIdRepository()),"GENERICO", "", convertito.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Allegato", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getTipo() == Allegato.TipoAllegato.ALLEGATO) {
                    Allegato.DettaglioAllegato originale = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("allegato" + i.toString(), getUuidMinIObyFileId(originale.getIdRepository()), originale.getHashMd5(), originale.getEstensione(), originale.getMimeType());
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(originale.getIdRepository()),"GENERICO", "", originale.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Allegato", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                }

            } else if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) {
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.DESTINATARI) {
                    Allegato.DettaglioAllegato destinatari = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("destinatari.pdf", getUuidMinIObyFileId(destinatari.getIdRepository()), destinatari.getHashMd5(), "PDF", "application/pdf");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(destinatari.getIdRepository()),"ELENCO DESTINATARI", "", destinatari.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                }
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RELATA) {
                    if (allegato.getNome().startsWith("relata_COMMITTENTE")) {
                        Allegato.DettaglioAllegato committente = allegato.getDettagli().getOriginale();
                        IdentityFile identityFilePrincipale = new IdentityFile("relata committente " + indexCommittente.toString() + ".pdf", getUuidMinIObyFileId(committente.getIdRepository()), committente.getHashMd5(), "PDF", "application/pdf");
                        indexCommittente = indexCommittente + 1;
                        unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(committente.getIdRepository()),"RELATA PUBBLICAZIONE PROFILO COMMITTENTE", "", committente.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                        versamentiAllegatiInfo.add(allegatoInformation);
                    } else {
                        Allegato.DettaglioAllegato albo = allegato.getDettagli().getOriginale();
                        IdentityFile identityFilePrincipale = new IdentityFile("relata committente " + indexAlbo.toString() + ".pdf", getUuidMinIObyFileId(albo.getIdRepository()), albo.getHashMd5(), "PDF", "application/pdf");
                        indexAlbo = indexAlbo + 1;
                        unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(albo.getIdRepository()),"RELATA DI PUBBLICAZIONE", "", albo.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                        versamentiAllegatiInfo.add(allegatoInformation);
                    }
                } else if (allegato.getTipo() == Allegato.TipoAllegato.STAMPA_UNICA) {
                    Allegato.DettaglioAllegato stampaUnica = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("stampaunica.pdf", getUuidMinIObyFileId(stampaUnica.getIdRepository()), stampaUnica.getHashMd5(), "PDF", "application/pdf");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(stampaUnica.getIdRepository()),"STAMPA UNICA", "", stampaUnica.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getTipo() == Allegato.TipoAllegato.STAMPA_UNICA_OMISSIS) {
                    Allegato.DettaglioAllegato stampaUnicaOmissis = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("stampaunicaconomissis.pdf", getUuidMinIObyFileId(stampaUnicaOmissis.getIdRepository()), stampaUnicaOmissis.getHashMd5(), "PDF", "application/pdf");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(stampaUnicaOmissis.getIdRepository()),"STAMPA UNICA CON OMISSIS", "", stampaUnicaOmissis.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getTipo() == Allegato.TipoAllegato.TESTO_OMISSIS && doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA) {
                    Allegato.DettaglioAllegato testoOmissis = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("deliberazioneomissis.pdf", getUuidMinIObyFileId(testoOmissis.getIdRepository()), testoOmissis.getHashMd5(), "PDF", "application/pdf");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(testoOmissis.getIdRepository()),"DELIBERAIONE CON OMISSIS", "", testoOmissis.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getTipo() == Allegato.TipoAllegato.TESTO_OMISSIS && doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) {
                    Allegato.DettaglioAllegato testoOmissis = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("testofirmatomissis.pdf", getUuidMinIObyFileId(testoOmissis.getIdRepository()), testoOmissis.getHashMd5(), "PDF", "application/pdf");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(testoOmissis.getIdRepository()),"DETERMINAZIONE CON OMISSIS", "", testoOmissis.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                }
            } else if ( doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA) {
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.DESTINATARI) {
                    Allegato.DettaglioAllegato destinatari = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("destinatari.pdf", getUuidMinIObyFileId(destinatari.getIdRepository()), destinatari.getHashMd5(), "PDF", "application/pdf");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(destinatari.getIdRepository()),"ELENCO DESTINATARI", "", destinatari.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RICEVUTA_ACCETTAZIONE_PEC) {
                    Allegato.DettaglioAllegato accettazione = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("ricevuta_di_accettazione_" + i.toString(), getUuidMinIObyFileId(accettazione.getIdRepository()), accettazione.getHashMd5(), "EML", "message/rfc822");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(accettazione.getIdRepository()),"RICEVUTA DI ACCETTAZIONE", "", accettazione.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RICEVUTA_CONSEGNA_PEC) {
                    Allegato.DettaglioAllegato consegna = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("ricevuta_consegna_" + i.toString(), getUuidMinIObyFileId(consegna.getIdRepository()), consegna.getHashMd5(), "EML", "message/rfc822");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario( getUuidMinIObyFileId(consegna.getIdRepository()),"RICEVUTA DI CONSEGNA", "", consegna.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RICEVUTA_ERRORE_PEC) {
                    Allegato.DettaglioAllegato consegna = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("ricevuta_errore_consegna_" + i.toString(), getUuidMinIObyFileId(consegna.getIdRepository()), consegna.getHashMd5(), "EML", "message/rfc822");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(consegna.getIdRepository()),"RICEVUTA DI ERRORE CONSEGNA", "", consegna.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RELATA) {
                    if (allegato.getNome().startsWith("relata_COMMITTENTE")) {
                        Allegato.DettaglioAllegato committente = allegato.getDettagli().getOriginale();
                        indexCommittente = indexCommittente + 1;
                        IdentityFile identityFilePrincipale = new IdentityFile("relata_COMMITTENTE_" + indexCommittente.toString()+ ".pdf", getUuidMinIObyFileId(committente.getIdRepository()), committente.getHashMd5(), "PDF", "application/pdf");
                        unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(committente.getIdRepository()),"RELATA PUBBLICAZIONE PROFILO COMMITTENTE", "", committente.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                        versamentiAllegatiInfo.add(allegatoInformation);
                    } else {
                        Allegato.DettaglioAllegato albo = allegato.getDettagli().getOriginale();
                        IdentityFile identityFilePrincipale = new IdentityFile("relata_" + indexCommittente.toString() + ".pdf", getUuidMinIObyFileId(albo.getIdRepository()), albo.getHashMd5(), "PDF", "application/pdf");
                        unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(albo.getIdRepository()),"RELATA DI PUBBLICAZIONE", "", albo.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                        indexAlbo = indexAlbo + 1;
                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                        versamentiAllegatiInfo.add(allegatoInformation);
                    }
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SMISTAMENTO) {
                    Allegato.DettaglioAllegato smistamenti = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("smistamenti.pdf", getUuidMinIObyFileId(smistamenti.getIdRepository()), smistamenti.getHashMd5(), "PDF", "application/pdf");
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(smistamenti.getIdRepository()),"ELENCO SMISTAMENTI", "", smistamenti.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().format(formatter), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                }
            } else if ( (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA)) {
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SEGNATURA) {
                    Allegato.DettaglioAllegato segnatura = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("segnatura.xml", getUuidMinIObyFileId(segnatura.getIdRepository()), segnatura.getHashMd5(), "XML", "text/xml");
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(segnatura.getIdRepository()),"SEGNATURA", "", segnatura.getNome(), i, identityFilePrincipale, "Documento Generico", "annotazione", "Contenuto", "FILE", null , null);
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getTipo() == Allegato.TipoAllegato.FRONTESPIZIO) {
                    Allegato.DettaglioAllegato frontespizio = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("frontespizio.pdf", getUuidMinIObyFileId(frontespizio.getIdRepository()), frontespizio.getHashMd5(), "PDF", "application/pdf");
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(frontespizio.getIdRepository()),"FRONTESPIZIO", "", frontespizio.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "annotazione", "Contenuto", "FILE", null , null);
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SINTESI_TRASPARENZA && allegato.getDettagli() != null) {
                    Allegato.DettaglioAllegato schedaSintesiTrasparenza = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("sintesitrasparenza.pdf", getUuidMinIObyFileId(schedaSintesiTrasparenza.getIdRepository()), schedaSintesiTrasparenza.getHashMd5(), "PDF", "application/pdf");
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(schedaSintesiTrasparenza.getIdRepository()),"SCHEDA SINTESI TRASPARENZA", "", schedaSintesiTrasparenza.getNome(), i, identityFilePrincipale, "Documento Generico", "annotazione", "Contenuto", "FILE", null , null);
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.NOTE_DOCUMENTO) {
                    if (includiNoteParer) {
                        Allegato.DettaglioAllegato noteDocumento = allegato.getDettagli().getOriginale();
                        IdentityFile identityFilePrincipale = new IdentityFile("notedocumento.pdf", getUuidMinIObyFileId(noteDocumento.getIdRepository()), noteDocumento.getHashMd5(), "PDF", "application/pdf");
                        unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(noteDocumento.getIdRepository()),"NOTE DOCUMENTO", "", noteDocumento.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "annotazione", "Contenuto", "FILE", null , null);
                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                        versamentiAllegatiInfo.add(allegatoInformation);
                    }
                }
            } else if ( doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA) {
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SEGNATURA) {
                    Allegato.DettaglioAllegato segnatura = allegato.getDettagli().getOriginale();
                    IdentityFile infoDocumento = new IdentityFile("segnatura.xml", getUuidMinIObyFileId(segnatura.getIdRepository()), segnatura.getHashMd5(), segnatura.getEstensione(), segnatura.getMimeType());
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(segnatura.getIdRepository()),"SEGNATURA", "", segnatura.getNome(), i, infoDocumento, "DocumentoGenerico", "annotazione", "Contenuto", "FILE", null , null);
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), infoDocumento);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getTipo() == Allegato.TipoAllegato.FRONTESPIZIO) {
                    Allegato.DettaglioAllegato frontespizio = allegato.getDettagli().getOriginale();
                    IdentityFile infoDocumento = new IdentityFile("frontespizio.pdf", getUuidMinIObyFileId(frontespizio.getIdRepository()), frontespizio.getHashMd5(), frontespizio.getMimeType());
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(frontespizio.getIdRepository()),"FRONTESPIZIO", "", frontespizio.getNome(), i, infoDocumento, "DocumentoGenerico", "annotazione", "Contenuto", "FILE", null , null);
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), infoDocumento);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SINTESI_TRASPARENZA && allegato.getDettagli() != null) {
                    Allegato.DettaglioAllegato schedaSintesiTrasparenza = allegato.getDettagli().getOriginale();
                    IdentityFile infoDocumento = new IdentityFile("sintesitrasparenza.pdf", getUuidMinIObyFileId(schedaSintesiTrasparenza.getIdRepository()), schedaSintesiTrasparenza.getHashMd5(), schedaSintesiTrasparenza.getMimeType());
                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(schedaSintesiTrasparenza.getIdRepository()),"SCHEDA SINTESI TRASPARENZA", "", schedaSintesiTrasparenza.getNome(), i, infoDocumento, "DocumentoGenerico", "annotazione", "Contenuto", "FILE", null , null);
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), infoDocumento);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.NOTE_DOCUMENTO) {
                    if (includiNoteParer) {
                        Allegato.DettaglioAllegato noteDocumento = allegato.getDettagli().getOriginale();
                        IdentityFile infoDocumento = new IdentityFile("notedelibera.pdf", getUuidMinIObyFileId(noteDocumento.getIdRepository()), noteDocumento.getHashMd5(), noteDocumento.getMimeType());
                        unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(noteDocumento.getIdRepository()),"NOTE DOCUMENTO", "", noteDocumento.getNome(), i, infoDocumento, "DocumentoGenerico", "annotazione", "Contenuto", "FILE", null , null);
                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), infoDocumento);
                        versamentiAllegatiInfo.add(allegatoInformation);
                    }
                }
            }
            mappaPerAllegati.put("unitaDocumentariaBuilder", unitaDocumentariaBuilder);
            mappaPerAllegati.put("versamentiAllegatiInfo", versamentiAllegatiInfo);
        }
        return mappaPerAllegati;
    }

    public DatiSpecifici buildDatiSpecificiRegistroGiornaliero(Doc doc, DocDetail docDetail, String versioneDatiSpecificiRg) throws ParserConfigurationException, ParseException {
        DatiSpecifici datiSpecifici;
        DatiSpecificiBuilder datiSpecificiBuilder = new DatiSpecificiBuilder();
        Pattern pattern = Pattern.compile("(.*)n\\.\\s(.*)\\sal\\sn\\.\\s(.*)\\sdel\\s(.*)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(doc.getOggetto());
        matcher.matches();
        matcher.groupCount();
        String numeroIniziale = matcher.group(2);
        String numeroFinale = matcher.group(3);
        String giorno = matcher.group(4);
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(giorno); 
        ZonedDateTime d = ZonedDateTime.ofInstant(date.toInstant(),ZoneId.systemDefault());
        ZonedDateTime dsuccessivo = d.plusDays(1);
        long documenti = 0;
        long documentiAnnullati = 0;
        String applicativo = "";
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
        if (null != doc.getTipologia()) switch (doc.getTipologia()) {
               case RGPICO:
                   documenti = jPAQueryFactory.select(QDocDetail.docDetail.count()).from(QDocDetail.docDetail)
                           .where(QDocDetail.docDetail.dataRegistrazione.goe(d).and(QDocDetail.docDetail.dataRegistrazione.lt(dsuccessivo))
                                   .and(QDocDetail.docDetail.tipologia.in(DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA, DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA))
                                   .and(QDocDetail.docDetail.annullato.eq(Boolean.FALSE))).fetchOne();
                   //            documenti = (Integer) entityManager.createQuery("select count(*) "
//                    + "from scripta.docs_details dd2 "
//                    + "where dd2.data_registrazione >=  :value1  and data_registrazione < (':value1::date + interval '1 days') "
//                    + " and tipologia in ('PROTOCOLLO_IN_USCITA', '') "
//                    + "and annullato = false")
//                    .setParameter("value1", d ).getSingleResult();
                   documentiAnnullati = jPAQueryFactory.select(QDocDetail.docDetail.count()).from(QDocDetail.docDetail)
                           .where(QDocDetail.docDetail.dataRegistrazione.goe(d).and(QDocDetail.docDetail.dataRegistrazione.lt(dsuccessivo))
                                   .and(QDocDetail.docDetail.tipologia.in(DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA, DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA))
                                   .and(QDocDetail.docDetail.annullato.eq(Boolean.TRUE))).fetchOne();
                   //            documentiAnnullati = (Integer) entityManager.createQuery("select count(*) "
//                    + "from scripta.docs_details dd2 "
//                    + "where dd2.data_registrazione >=  :value1  and data_registrazione < (':value1::date + interval '1 days') "
//                    + " and tipologia in ('PROTOCOLLO_IN_USCITA', 'PROTOCOLLO_IN_ENTRATA') "
//                    + "and annullato = true")
//                    .setParameter("value1", doc.getId()).getSingleResult();
                   applicativo = "procton";
                   break;
               case RGDETE:
                   documenti = (Integer) entityManager.createQuery("select count(*) "
                           + "from scripta.docs_details dd2 "
                           + "where dd2.data_registrazione >=  :value1  and data_registrazione < (':value1::date + interval '1 days') "
                           + "and tipologia = 'DETERMINA' "
                           + "and annullato = false")
                           .setParameter("value1", d).getSingleResult();
                   documentiAnnullati = (Integer) entityManager.createQuery("select count(*) "
                           + "from scripta.docs_details dd2 "
                           + "where dd2.data_registrazione >=  :value1  and data_registrazione < (':value1::date + interval '1 days') "
                           + " and tipologia = 'DETERMINA' "
                           + "and annullato = true")
                           .setParameter("value1", doc.getId()).getSingleResult();
                   applicativo = "dete";
                   break;
               case RGDELI:
                   documenti = (Integer) entityManager.createQuery("select count(*) "
                           + "from scripta.docs_details dd2 "
                           + "where dd2.data_registrazione >=  :value1  and data_registrazione < (':value1::date + interval '1 days') "
                           + " and tipologia in ('DELIBERA') "
                           + "and annullato = false")
                           .setParameter("value1", d).getSingleResult();
                   documentiAnnullati = (Integer) entityManager.createQuery("select count(*) "
                           + "from scripta.docs_details dd2 "
                           + "where dd2.data_registrazione >=  :value1  and data_registrazione < (':value1::date + interval '1 days') "
                           + " and tipologia = 'DELIBERA' "
                           + "and annullato = true")
                           .setParameter("value1", doc.getId()).getSingleResult();
                   applicativo = "deli";
                   break;
               default:
                   break;
           }
        List<AttoreDoc> attori = jPAQueryFactory.select(QAttoreDoc.attoreDoc).from(QAttoreDoc.attoreDoc)
                .where(QAttoreDoc.attoreDoc.idDoc.id.eq(doc.getId())).fetch();
                
//                entityManager.createQuery("SELECT * FROM scripta.attori_docs ad where ad.id_doc = :value1 ")
//                .setParameter("value1", doc.getId()).getResultList();
        datiSpecificiBuilder.insertNewTag("VersioneDatiSpecifici", versioneDatiSpecificiRg);
        datiSpecificiBuilder.insertNewTag("NumeroIniziale", numeroIniziale);
        datiSpecificiBuilder.insertNewTag("NumeroFinale", numeroFinale);
        datiSpecificiBuilder.insertNewTag("DataInizioRegistrazioni", giorno);
        datiSpecificiBuilder.insertNewTag("DataFineRegistrazioni", giorno);
        datiSpecificiBuilder.insertNewTag("Originatore", attori.get(0).getIdStruttura().getNome());
        datiSpecificiBuilder.insertNewTag("Responsabile", attori.get(0).getIdPersona().getDescrizione());
        datiSpecificiBuilder.insertNewTag("Operatore", "SISTEMA");
        datiSpecificiBuilder.insertNewTag("NumeroDocumentiRegistrati", String.valueOf(documenti));
        datiSpecificiBuilder.insertNewTag("NumeroDocumentiAnnullati", String.valueOf(documentiAnnullati));
        datiSpecificiBuilder.insertNewTag("DenominazioneApplicativo", applicativo);
        datiSpecificiBuilder.insertNewTag("VersioneApplicativo", "0.1");
        datiSpecificiBuilder.insertNewTag("ProduttoreApplicativo", "vuoto");
        datiSpecificiBuilder.insertNewTag("DenominazioneSistemaGestioneBaseDati", "PostgresSQL");
        datiSpecificiBuilder.insertNewTag("VersioneSistemaGestioneBaseDati", "12");
        datiSpecificiBuilder.insertNewTag("ProduttoreSistemaGestioneBaseDati", "The PostgreSQL Global Development Group");

        datiSpecifici = datiSpecificiBuilder.getDatiSpecifici();
        return datiSpecifici;
    }
    
    public String traduzioneTipologiaRegistro(DocDetailInterface.TipologiaDoc tipo){
        String registro = "";
        switch(tipo) {
            case PROTOCOLLO_IN_USCITA:
                registro = "PG";
                   break;
               case PROTOCOLLO_IN_ENTRATA:
                   registro = "PG";
                   break;
               case DETERMINA:
                   registro = "DETE";
                   break;
               case DELIBERA:
                   registro = "DELI";
                   break;
               case RGPICO:
                   registro = "RGPICO";
                   break;
               case RGDETE:
                   registro = "RGDETE";
                   break;
               case RGDELI:
                   registro = "RGDELI";
                   break;
               default:
                   throw new AssertionError(tipo.name());
                   
        }
        return registro;   
        
    }
    
    public String traduzioneTipologiaParerPerDatiSpecifici(DocDetailInterface.TipologiaDoc tipo){
        String tipologia = "";
        switch(tipo) {
            case PROTOCOLLO_IN_USCITA:
                tipologia = "DOCUMENTO PROTOCOLLATO IN USCITA";
                   break;
               case PROTOCOLLO_IN_ENTRATA:
                   tipologia = "DOCUMENTO PROTOCOLLATO IN ENTRATA";
                   break;
               case DETERMINA:
                   tipologia = "DETERMINA";
                   break;
               case DELIBERA:
                   tipologia = "DELIBERAZIONE";
                   break;
               case RGPICO:
                   tipologia = "REGISTRO GIORNALIERO";
                   break;
               case RGDETE:
                   tipologia = "REGISTRO GIORNALIERO";
                   break;
               case RGDELI:
                   tipologia = "REGISTRO GIORNALIERO";
                   break;
               default:
                   throw new AssertionError(tipo.name());
                   
        }
        return tipologia;   
        
    }
    
    
     public String traduzioneTipologiaParer(DocDetailInterface.TipologiaDoc tipo){
        String tipologia = "";
        switch(tipo) {
            case PROTOCOLLO_IN_USCITA:
                tipologia = "DOCUMENTO PROTOCOLLATO";
                   break;
               case PROTOCOLLO_IN_ENTRATA:
                   tipologia = "DOCUMENTO PROTOCOLLATO";
                   break;
               case DETERMINA:
                   tipologia = "DETERMINA";
                   break;
               case DELIBERA:
                   tipologia = "DELIBERAZIONE";
                   break;
               case RGPICO:
                   tipologia = "REGISTRO GIORNALIERO";
                   break;
               case RGDETE:
                   tipologia = "REGISTRO GIORNALIERO";
                   break;
               case RGDELI:
                   tipologia = "REGISTRO GIORNALIERO";
                   break;
               default:
                   throw new AssertionError(tipo.name());
                   
        }
        return tipologia;   
        
    }
    
    private String getDescrizioneRiferimentoTemporale(DocDetailInterface.TipologiaDoc tipoDocumento) {
        String res = null;

        switch (tipoDocumento) {
            case PROTOCOLLO_IN_USCITA:
            case PROTOCOLLO_IN_ENTRATA:
                res = "DATA_DI_PROTOCOLLAZIONE";
                break;

            case DETERMINA:
            case RGPICO:
            case RGDELI:
            case RGDETE:
                res = "DATA_DI_REGISTRAZIONE";
                break;

            case DELIBERA:
                res = "DATA DI INIZIO PUBBLICAZIONE";
                break;

            default:
                res = "";
        }
        return res;
    }
    
    private VersamentoAllegatoInformation createVersamentoAllegato(Integer idAllegato, IdentityFile identityFile){
        VersamentoAllegatoInformation allegatoInformation = new VersamentoAllegatoInformation();
        allegatoInformation.setIdAllegato(idAllegato);
        allegatoInformation.setTipoDettaglioAllegato(Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE);
        allegatoInformation.setStatoVersamento(Versamento.StatoVersamento.IN_CARICO);
        allegatoInformation.setDataVersamento(ZonedDateTime.now());
        allegatoInformation.setMetadatiVersati(identityFile.getJSON().toJSONString());
        return allegatoInformation;
    }
    
    private VersamentoAllegatoInformation createVersamentoAllegatoFirmato(Integer idAllegato, IdentityFile identityFile){
        VersamentoAllegatoInformation allegatoInformation = new VersamentoAllegatoInformation();
        allegatoInformation.setIdAllegato(idAllegato);
        allegatoInformation.setTipoDettaglioAllegato(Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE_FIRMATO);
        allegatoInformation.setStatoVersamento(Versamento.StatoVersamento.IN_CARICO);
        allegatoInformation.setDataVersamento(ZonedDateTime.now());
        allegatoInformation.setMetadatiVersati(identityFile.getJSON().toJSONString());
        return allegatoInformation;
    }
    
    
    private String getUuidMinIObyFileId(String fileId) throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = versatoreRepositoryConfiguration.getVersatoreRepositoryManager().getMinIOWrapper();
        MinIOWrapperFileInfo fileInfoByFileId = minIOWrapper.getFileInfoByFileId(fileId);
        return fileInfoByFileId.getMongoUuid();
    }
}
