/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.parer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.plugins.infocert.InfocertVersatoreService;
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
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.riversamento.builder.DatiSpecificiBuilder;
import it.bologna.ausl.riversamento.builder.IdentityFile;
import it.bologna.ausl.riversamento.builder.ProfiloArchivistico;
import it.bologna.ausl.riversamento.builder.UnitaDocumentariaBuilder;
import it.bologna.ausl.riversamento.builder.oggetti.DatiSpecifici;
import it.bologna.ausl.riversamento.builder.oggetti.UnitaDocumentaria;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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

    private static final Logger log = LoggerFactory.getLogger(InfocertVersatoreService.class);

    public Map<String,Object> ParerVersatoreMetadatiBuilder(Doc doc, DocDetail docDetail, String enteVersamento, String userID, String version, String ambiente, String struttura, String tipoConservazione, String codifica, String versioneDatiSpecifici, Boolean includiNote, String tipoDocumentoDefault,String forzaCollegamento, String forzaAccettazione, String forzaConservazione)throws DatatypeConfigurationException, JAXBException, ParseException{
       
        List<ArchivioDoc> archiviazioni = entityManager.createQuery("SELECT * FROM scripta.archivi_docs ad where ad.id_doc = :value1 order by ad.data_archiviziazione ASC")
                .setParameter("value1", doc.getId()).getResultList();
        Map<String,Object> mappaUnitaDocumentaria = new HashMap<>();
        
        

        if (archiviazioni != null) { 
            String dataArchiviazione = archiviazioni.get(0).getDataArchiviazione().toLocalDate().toString();
            try {
                log.info("buildo il profilo archivistico");
                ProfiloArchivistico profiloArchivistico = buildProfiloArchivistico(doc, docDetail, archiviazioni);
                try {
                    log.info("buildo i dati specifici");
                    DatiSpecifici datiSpecifici = null;
                    if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA) {
                        datiSpecifici = buildDatiSpecifici(doc, docDetail, dataArchiviazione, versioneDatiSpecifici);
                    } else if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGPICO || doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGDELI || doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGDETE) {
                        datiSpecifici = buildDatiSpecificiRegistroGiornaliero(doc, docDetail);
                    }
                    
                    UnitaDocumentariaBuilder unitaDocumentariaBuilder;
                    
                    unitaDocumentariaBuilder = new UnitaDocumentariaBuilder(docDetail.getNumeroRegistrazione().toString(), docDetail.getAnnoRegistrazione(), traduzioneTipologiaRegistro(doc.getTipologia()), traduzioneTipologiaParer(doc.getTipologia()), "0", "-1", "0", profiloArchivistico, doc.getOggetto(), dataArchiviazione, datiSpecifici, version , ambiente, enteVersamento, struttura, userID, tipoConservazione, codifica);
                    Map<String, Object> mappaUnitaDocumentariaEAllegati = buildAllegati(doc,docDetail, unitaDocumentariaBuilder, tipoDocumentoDefault,includiNote);
                    UnitaDocumentariaBuilder unitaDocumentariaBuilderConAllegati = (UnitaDocumentariaBuilder) mappaUnitaDocumentariaEAllegati.get("unitaDocumentariaBuilder");
                    List<VersamentoAllegatoInformation> versamentiAllegatiInformation = (List<VersamentoAllegatoInformation>) mappaUnitaDocumentariaEAllegati.get("versamentiAllegatiInfo");
                    mappaUnitaDocumentaria.put("unitaDocumentaria", unitaDocumentariaBuilder);
                    mappaUnitaDocumentaria.put("identityFiles", unitaDocumentariaBuilder.getIdentityFiles());
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

    private DatiSpecifici buildDatiSpecifici(Doc doc, DocDetail docDetail, String dataArchiviazione, String versioneDati) throws ParserConfigurationException {

        DatiSpecifici datiSpecifici = null;
        ObjectMapper mapper = new ObjectMapper();

        DatiSpecificiBuilder datiSpecificiBuilder = new DatiSpecificiBuilder();
        if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) {
            datiSpecificiBuilder.insertNewTag("UnitaOperativaProponente", docDetail.getIdStrutturaRegistrazione().getNome());
        }
        List<AttoreDoc> attori = doc.getAttori();
        String nomeResponsabile = "";
        String vistiString = "";
        String firmatariString = "";
        String nomeDirettoreAmministrativo = "";
        String nomeDirettoreSanitario = "";
        String nomeDirettoreGenerale = "";
        String operatoreDiProtocollo = "";
        for (AttoreDoc attore : attori) {
            if (attore.getRuolo() == AttoreDoc.RuoloAttoreDoc.RESPONSABILE_PROCEDIMENTO) {
                nomeResponsabile = attore.getIdPersona().getDescrizione();
            }
            if (attore.getRuolo() == AttoreDoc.RuoloAttoreDoc.VISTATORE) {
                if (!vistiString.equals("")) {
                    vistiString = vistiString + attore.getIdPersona().getDescrizione();
                } else {
                    vistiString = vistiString + "; " + attore.getIdPersona().getDescrizione();
                }
            }
            if (attore.getRuolo() == AttoreDoc.RuoloAttoreDoc.FIRMATARIO) {
                if (!firmatariString.equals("")) {
                    firmatariString = firmatariString + attore.getIdPersona().getDescrizione();
                } else {
                    firmatariString = firmatariString + "; " + attore.getIdPersona().getDescrizione();
                }
            }
            if (attore.getRuolo() == AttoreDoc.RuoloAttoreDoc.DA) {
                nomeDirettoreAmministrativo = attore.getIdPersona().getDescrizione();
            }
            if (attore.getRuolo() == AttoreDoc.RuoloAttoreDoc.DS) {
                nomeDirettoreSanitario = attore.getIdPersona().getDescrizione();
            }
            if (attore.getRuolo() == AttoreDoc.RuoloAttoreDoc.DG) {
                nomeDirettoreGenerale = attore.getIdPersona().getDescrizione();
            }
            if (attore.getRuolo() == AttoreDoc.RuoloAttoreDoc.RICEZIONE) {
                operatoreDiProtocollo = attore.getIdPersona().getCodiceFiscale();
            }
        }
        if (!nomeResponsabile.equals("")) {
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
            if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA) {
                datiSpecificiBuilder.insertNewTag("Proponente", firmatariString);
            }
        }
        if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA) {
            datiSpecificiBuilder.insertNewTag("DirettoreAmministrativo", nomeDirettoreAmministrativo);
            datiSpecificiBuilder.insertNewTag("DirettoreSanitario", nomeDirettoreSanitario);
            datiSpecificiBuilder.insertNewTag("DirettoreGenerale", nomeDirettoreGenerale);
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
            datiSpecificiBuilder.insertNewTag("Destinatari", "Vedi annesso elenco destinatari");
            HashMap<String, Object> additionalDataDoc = new HashMap<String, Object>();
            additionalDataDoc = (HashMap<String, Object>) doc.getAdditionalData();
            HashMap<String, Object> datiPubblicazione = new HashMap<String, Object>();
            datiPubblicazione = (HashMap<String, Object>) additionalDataDoc.get("dati_pubblicazione");
            String dataEsecutivita = (String) datiPubblicazione.get("data_esecutivita");
            if (dataEsecutivita != null) {
                datiSpecificiBuilder.insertNewTag("EsecutivitaData", dataEsecutivita);
            }
            if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA) {
                String noteEsecutivita = (String) additionalDataDoc.get("note_esecutivita");
                datiSpecificiBuilder.insertNewTag("EsecutivitaNote", noteEsecutivita);
            }
            datiSpecificiBuilder.insertNewTag("PubblicazioneRegistro", "ALBO ON LINE");
            datiSpecificiBuilder.insertNewTag("PubblicazioneAnno", (String) datiPubblicazione.get("anno"));
            datiSpecificiBuilder.insertNewTag("PubblicazioneNumero", (String) datiPubblicazione.get("numero"));
            datiSpecificiBuilder.insertNewTag("PubblicazioneInizio", (String) datiPubblicazione.get("inizio_pubblicazione"));
            datiSpecificiBuilder.insertNewTag("PubblicazioneFine", (String) datiPubblicazione.get("fine_pubblicazione"));
            datiSpecificiBuilder.insertNewTag("PubblicazioneTipo", "INTEGRALE");
            datiSpecificiBuilder.insertNewTag("IdentificazioneRepository", "GEDI");
        } else if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA) {
            datiSpecificiBuilder.insertNewTag("Destinatario", "Vedi annesso elenco destinatari");
            datiSpecificiBuilder.insertNewTag("Movimento", "OUT");
            datiSpecificiBuilder.insertNewTag("ModalitaTrasmissione", "BABEL");
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
            datiSpecificiBuilder.insertNewTag("Visibiita", "LIBERA");
            datiSpecificiBuilder.insertNewTag("Consultazione", "NON PRECISATA");

        }
        if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) {
            HashMap<String, Object> additionalDataDoc = new HashMap<String, Object>();
            additionalDataDoc = (HashMap<String, Object>) doc.getAdditionalData();
            HashMap<String, Object> metadatiTrasparenza = new HashMap<String, Object>();
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
        datiSpecifici = datiSpecificiBuilder.getDatiSpecifici();
        return datiSpecifici;
    }

    private ProfiloArchivistico buildProfiloArchivistico(Doc doc, DocDetail docDetail, List<ArchivioDoc> archiviazioni) throws Exception {
        ProfiloArchivistico profiloArchivistico = new ProfiloArchivistico();

        if (archiviazioni != null) {

            for (ArchivioDoc fascicolazioneSecondaria : archiviazioni) {

                if (fascicolazioneSecondaria == archiviazioni.get(0)) {
                    Archivio archivioPrincipale = entityManager.find(Archivio.class, archiviazioni.get(0).getId());
                    if (archivioPrincipale.getLivello() == 1) {
                        profiloArchivistico.addFascicoloPrincipale(
                                archivioPrincipale.getIdTitolo().getClassificazione(),
                                archivioPrincipale.getAnno().toString(),
                                archivioPrincipale.getNumero().toString(),
                                archivioPrincipale.getOggetto(),
                                "",
                                "",
                                "",
                                "");
                    } else if (archivioPrincipale.getLivello() == 2) {
                        profiloArchivistico.addFascicoloPrincipale(
                                archivioPrincipale.getIdTitolo().getClassificazione(),
                                archivioPrincipale.getAnno().toString(),
                                archivioPrincipale.getIdArchivioPadre().getNumero().toString(),
                                archivioPrincipale.getIdArchivioPadre().getOggetto(),
                                archivioPrincipale.getNumero().toString(),
                                archivioPrincipale.getOggetto(),
                                "",
                                "");
                    } else {
                        profiloArchivistico.addFascicoloPrincipale(
                                archivioPrincipale.getIdTitolo().getClassificazione(),
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
                    Archivio archivioSecondario = entityManager.find(Archivio.class, fascicolazioneSecondaria.getId());
                    if (archivioSecondario.getLivello() == 1) {
                        profiloArchivistico.addFascicoloSecondario(
                                archivioSecondario.getIdTitolo().getClassificazione(),
                                archivioSecondario.getAnno().toString(),
                                archivioSecondario.getNumero().toString(),
                                archivioSecondario.getOggetto(),
                                "",
                                "",
                                "",
                                "");
                    } else if (archivioSecondario.getLivello() == 2) {
                        profiloArchivistico.addFascicoloSecondario(
                                archivioSecondario.getIdTitolo().getClassificazione(),
                                archivioSecondario.getAnno().toString(),
                                archivioSecondario.getIdArchivioPadre().getNumero().toString(),
                                archivioSecondario.getIdArchivioPadre().getOggetto(),
                                archivioSecondario.getNumero().toString(),
                                archivioSecondario.getOggetto(),
                                "",
                                "");
                    } else {
                        profiloArchivistico.addFascicoloSecondario(
                                archivioSecondario.getIdTitolo().getClassificazione(),
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

    private  Map<String, Object> buildAllegati(Doc doc,DocDetail docDetail, UnitaDocumentariaBuilder unitaDocumentariaBuilder, String tipoDocumentoDefault, Boolean includiNoteParer) throws DatatypeConfigurationException, ParseException {
        Map<String, Object> mappaPerAllegati = new HashMap<>();
        List<Allegato> allegati = entityManager.createQuery("SELECT * FROM scripta.allegati a where a.id_doc = :value1")
                .setParameter("value1", doc.getId()).getResultList();
//        IdentityFile identityFilePrincipale;
        Integer i = 1;
        Integer indexCommittente = 1;
        Integer indexAlbo = 1;
        List<VersamentoAllegatoInformation> versamentiAllegatiInfo = new ArrayList();
        
        for (Allegato allegato : allegati) {
            if (allegato.getTipo() != Allegato.TipoAllegato.ANNESSO && allegato.getTipo() != Allegato.TipoAllegato.ANNOTAZIONE) {
                if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA && allegato.getPrincipale() == true) {
                    Allegato.DettaglioAllegato originale = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("allegato principale", originale.getIdRepository(), originale.getHashMd5(), originale.getEstensione(), originale.getMimeType());
                    unitaDocumentariaBuilder.addDocumentoPrincipale(doc.getId().toString(), traduzioneTipologiaParer(doc.getTipologia()), "", "", 1, identityFilePrincipale, docDetail.getDataRegistrazione().toString(), tipoDocumentoDefault, "DocumentoGenerico", "FILE", getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                    
                }else  if ((doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGDELI || doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGPICO ||doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGDETE )&& allegato.getPrincipale() == true) {
                    Allegato.DettaglioAllegato originale = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("allegato principale", originale.getIdRepository(), originale.getHashMd5(), originale.getEstensione(), originale.getMimeType());
                    unitaDocumentariaBuilder.addDocumentoPrincipale(doc.getId().toString(), traduzioneTipologiaParer(doc.getTipologia()), "", "", 1, identityFilePrincipale, docDetail.getDataRegistrazione().toString(), tipoDocumentoDefault, "DocumentoGenerico", "FILE", getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA && allegato.getFirmato() == true && allegato.getTipo() == Allegato.TipoAllegato.TESTO) {
                    Allegato.DettaglioAllegato originaleFirmato = allegato.getDettagli().getOriginaleFirmato();
                    IdentityFile identityFilePrincipale = new IdentityFile("letterafirmata.pdf", originaleFirmato.getIdRepository(), originaleFirmato.getHashMd5(), "PDF", "application/pdf");
                    unitaDocumentariaBuilder.addDocumentoPrincipale(doc.getId().toString(), traduzioneTipologiaParer(doc.getTipologia()), "", "", 1, identityFilePrincipale, docDetail.getDataRegistrazione().toString(), tipoDocumentoDefault, "DocumentoGenerico", "FILE", getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegatoFirmato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if ((doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) && allegato.getFirmato() == true && allegato.getTipo() == Allegato.TipoAllegato.TESTO) {
                    Allegato.DettaglioAllegato originaleFirmato = allegato.getDettagli().getOriginaleFirmato();
                    IdentityFile identityFilePrincipale = new IdentityFile("testofirmato.pdf", originaleFirmato.getIdRepository(), originaleFirmato.getHashMd5(), "PDF", "application/pdf");
                    unitaDocumentariaBuilder.addDocumentoPrincipale(doc.getId().toString(), traduzioneTipologiaParer(doc.getTipologia()), "", "", 1, identityFilePrincipale, docDetail.getDataRegistrazione().toString(), tipoDocumentoDefault, "DocumentoGenerico", "FILE", getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegatoFirmato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getFirmato() == true && allegato.getTipo() != Allegato.TipoAllegato.TESTO) {
                    Allegato.DettaglioAllegato originaleFirmato = allegato.getDettagli().getOriginaleFirmato();
                    IdentityFile identityFilePrincipale = new IdentityFile("allegato_firmato", originaleFirmato.getIdRepository(), originaleFirmato.getHashMd5(), "PDF", "application/pdf");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(originaleFirmato.getIdRepository(),"GENERICO", "", originaleFirmato.getNome(), i, identityFilePrincipale, "Documento Generico", "Allegato", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegatoFirmato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getDettagli().getConvertito() != null && allegato.getTipo() != Allegato.TipoAllegato.TESTO) {
                    Allegato.DettaglioAllegato convertito = allegato.getDettagli().getConvertito();
                    IdentityFile identityFilePrincipale = new IdentityFile("allegato_firmato", convertito.getIdRepository(), convertito.getHashMd5(), "PDF", "application/pdf");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(convertito.getIdRepository(),"GENERICO", "", convertito.getNome(), i, identityFilePrincipale, "Documento Generico", "Allegato", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegatoFirmato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getTipo() == Allegato.TipoAllegato.ALLEGATO) {
                    Allegato.DettaglioAllegato originale = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("allegato" + i.toString(), originale.getIdRepository(), originale.getHashMd5(), originale.getEstensione(), originale.getMimeType());
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(originale.getIdRepository(),"GENERICO", "", originale.getNome(), i, identityFilePrincipale, "Documento Generico", "Allegato", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                }

            } else if (allegato.getTipo() == Allegato.TipoAllegato.ANNESSO && (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA)) {
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.DESTINATARI) {
                    Allegato.DettaglioAllegato destinatari = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("destinatari.pdf", destinatari.getIdRepository(), destinatari.getHashMd5(), "PDF", "application/pdf");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(destinatari.getIdRepository(),"ELENCO DESTINATARI", "", destinatari.getNome(), i, identityFilePrincipale, "Documento Generico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                }
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RELATA) {
                    if (allegato.getNome().startsWith("relata_COMMITTENTE")) {
                        Allegato.DettaglioAllegato committente = allegato.getDettagli().getOriginale();
                        IdentityFile identityFilePrincipale = new IdentityFile("relata committente " + indexCommittente.toString() + ".pdf", committente.getIdRepository(), committente.getHashMd5(), "PDF", "application/pdf");
                        indexCommittente = indexCommittente + 1;
                        unitaDocumentariaBuilder.addDocumentoSecondario(committente.getIdRepository(),"RELATA COMMITTENTE", "", committente.getNome(), i, identityFilePrincipale, "Documento Generico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                        versamentiAllegatiInfo.add(allegatoInformation);
                    } else {
                        Allegato.DettaglioAllegato albo = allegato.getDettagli().getOriginale();
                        IdentityFile identityFilePrincipale = new IdentityFile("relata committente " + indexAlbo.toString() + ".pdf", albo.getIdRepository(), albo.getHashMd5(), "PDF", "application/pdf");
                        indexAlbo = indexAlbo + 1;
                        unitaDocumentariaBuilder.addDocumentoSecondario(albo.getIdRepository(),"RELATA DI PUBBLICAZIONE", "", albo.getNome(), i, identityFilePrincipale, "Documento Generico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                        versamentiAllegatiInfo.add(allegatoInformation);
                    }
                } else if (allegato.getTipo() == Allegato.TipoAllegato.STAMPA_UNICA) {
                    Allegato.DettaglioAllegato stampaUnica = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("stampaunica.pdf", stampaUnica.getIdRepository(), stampaUnica.getHashMd5(), "PDF", "application/pdf");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(stampaUnica.getIdRepository(),"STAMPA UNICA", "", stampaUnica.getNome(), i, identityFilePrincipale, "Documento Generico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getTipo() == Allegato.TipoAllegato.STAMPA_UNICA_OMISSIS) {
                    Allegato.DettaglioAllegato stampaUnicaOmissis = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("stampaunicaconomissis.pdf", stampaUnicaOmissis.getIdRepository(), stampaUnicaOmissis.getHashMd5(), "PDF", "application/pdf");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(stampaUnicaOmissis.getIdRepository(),"STAMPA UNICA CON OMISSIS", "", stampaUnicaOmissis.getNome(), i, identityFilePrincipale, "Documento Generico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getTipo() == Allegato.TipoAllegato.TESTO_OMISSIS && doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA) {
                    Allegato.DettaglioAllegato testoOmissis = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("deliberazioneomissis.pdf", testoOmissis.getIdRepository(), testoOmissis.getHashMd5(), "PDF", "application/pdf");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(testoOmissis.getIdRepository(),"DELIBERAIONE CON OMISSIS", "", testoOmissis.getNome(), i, identityFilePrincipale, "Documento Generico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getTipo() == Allegato.TipoAllegato.TESTO_OMISSIS && doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) {
                    Allegato.DettaglioAllegato testoOmissis = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("testofirmatomissis.pdf", testoOmissis.getIdRepository(), testoOmissis.getHashMd5(), "PDF", "application/pdf");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(testoOmissis.getIdRepository(),"DETERMINAZIONE CON OMISSIS", "", testoOmissis.getNome(), i, identityFilePrincipale, "Documento Generico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                }
            } else if (allegato.getTipo() == Allegato.TipoAllegato.ANNESSO && (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA)) {
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.DESTINATARI) {
                    Allegato.DettaglioAllegato destinatari = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("destinatari.pdf", destinatari.getIdRepository(), destinatari.getHashMd5(), "PDF", "application/pdf");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(destinatari.getIdRepository(),"ELENCO DESTINATARI", "", destinatari.getNome(), i, identityFilePrincipale, "Documento Generico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RICEVUTA_ACCETTAZIONE_PEC) {
                    Allegato.DettaglioAllegato accettazione = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("ricevuta_di_accettazione_" + i.toString(), accettazione.getIdRepository(), accettazione.getHashMd5(), "EML", "message/rfc822");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(accettazione.getIdRepository(),"RICEVUTA DI ACCETTAZIONE", "", accettazione.getNome(), i, identityFilePrincipale, "Documento Generico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RICEVUTA_CONSEGNA_PEC) {
                    Allegato.DettaglioAllegato consegna = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("ricevuta_consegna_" + i.toString(), consegna.getIdRepository(), consegna.getHashMd5(), "EML", "message/rfc822");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario( consegna.getIdRepository(),"RICEVUTA DI CONSEGNA", "", consegna.getNome(), i, identityFilePrincipale, "Documento Generico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RICEVUTA_ERRORE_PEC) {
                    Allegato.DettaglioAllegato consegna = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("ricevuta_errore_consegna_" + i.toString(), consegna.getIdRepository(), consegna.getHashMd5(), "EML", "message/rfc822");
                    i = i + 1;
                    unitaDocumentariaBuilder.addDocumentoSecondario(consegna.getIdRepository(),"RICEVUTA DI ERRORE CONSEGNA", "", consegna.getNome(), i, identityFilePrincipale, "Documento Generico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RELATA) {
                    if (allegato.getNome().startsWith("relata_COMMITTENTE")) {
                        Allegato.DettaglioAllegato committente = allegato.getDettagli().getOriginale();
                        indexCommittente = indexCommittente + 1;
                        IdentityFile identityFilePrincipale = new IdentityFile("relata_COMMITTENTE_" + indexCommittente.toString()+ ".pdf", committente.getIdRepository(), committente.getHashMd5(), "PDF", "application/pdf");
                        unitaDocumentariaBuilder.addDocumentoSecondario(committente.getIdRepository(),"RELATA COMMITTENTE", "", committente.getNome(), i, identityFilePrincipale, "Documento Generico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                        versamentiAllegatiInfo.add(allegatoInformation);
                    } else {
                        Allegato.DettaglioAllegato albo = allegato.getDettagli().getOriginale();
                        IdentityFile identityFilePrincipale = new IdentityFile("relata_" + indexCommittente.toString() + ".pdf", albo.getIdRepository(), albo.getHashMd5(), "PDF", "application/pdf");
                        unitaDocumentariaBuilder.addDocumentoSecondario(albo.getIdRepository(),"RELATA DI PUBBLICAZIONE", "", albo.getNome(), i, identityFilePrincipale, "Documento Generico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                        indexAlbo = indexAlbo + 1;
                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                        versamentiAllegatiInfo.add(allegatoInformation);
                    }
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SMISTAMENTO) {
                    Allegato.DettaglioAllegato smistamenti = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("smistamenti.pdf", smistamenti.getIdRepository(), smistamenti.getHashMd5(), "PDF", "application/pdf");
                    unitaDocumentariaBuilder.addDocumentoSecondario(smistamenti.getIdRepository(),"ELENCO SMISTAMENTI", "", smistamenti.getNome(), i, identityFilePrincipale, "Documento Generico", "Annesso", "Contenuto", "FILE", docDetail.getDataRegistrazione().toString(), getDescrizioneRiferimentoTemporale(doc.getTipologia()));
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                }
            } else if (allegato.getTipo() == Allegato.TipoAllegato.ANNOTAZIONE && (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA)) {
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SEGNATURA) {
                    Allegato.DettaglioAllegato segnatura = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("segnatura.xml", segnatura.getIdRepository(), segnatura.getHashMd5(), "XML", "text/xml");
                    unitaDocumentariaBuilder.addDocumentoSecondario(segnatura.getIdRepository(),"SEGNATURA", "", segnatura.getNome(), i, identityFilePrincipale, "Documento Generico", "annotazione", "Contenuto", "FILE", null , null);
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getTipo() == Allegato.TipoAllegato.FRONTESPIZIO) {
                    Allegato.DettaglioAllegato frontespizio = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("frontespizio.pdf", frontespizio.getIdRepository(), frontespizio.getHashMd5(), "PDF", "application/pdf");
                    unitaDocumentariaBuilder.addDocumentoSecondario(frontespizio.getIdRepository(),"FRONTESPIZIO", "", frontespizio.getNome(), i, identityFilePrincipale, "Documento Generico", "annotazione", "Contenuto", "FILE", null , null);
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SINTESI_TRASPARENZA && allegato.getDettagli() != null) {
                    Allegato.DettaglioAllegato schedaSintesiTrasparenza = allegato.getDettagli().getOriginale();
                    IdentityFile identityFilePrincipale = new IdentityFile("sintesitrasparenza.pdf", schedaSintesiTrasparenza.getIdRepository(), schedaSintesiTrasparenza.getHashMd5(), "PDF", "application/pdf");
                    unitaDocumentariaBuilder.addDocumentoSecondario(schedaSintesiTrasparenza.getIdRepository(),"SCHEDA SINTESI TRASPARENZA", "", schedaSintesiTrasparenza.getNome(), i, identityFilePrincipale, "Documento Generico", "annotazione", "Contenuto", "FILE", null , null);
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.NOTE_DOCUMENTO) {
                    if (includiNoteParer) {
                        Allegato.DettaglioAllegato noteDocumento = allegato.getDettagli().getOriginale();
                        IdentityFile identityFilePrincipale = new IdentityFile("notedocumento.pdf", noteDocumento.getIdRepository(), noteDocumento.getHashMd5(), "PDF", "application/pdf");
                        unitaDocumentariaBuilder.addDocumentoSecondario(noteDocumento.getIdRepository(),"NOTE DOCUMENTO", "", noteDocumento.getNome(), i, identityFilePrincipale, "Documento Generico", "annotazione", "Contenuto", "FILE", null , null);
                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
                        versamentiAllegatiInfo.add(allegatoInformation);
                    }
                }
            } else if (allegato.getTipo() == Allegato.TipoAllegato.ANNOTAZIONE && doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA) {
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SEGNATURA) {
                    Allegato.DettaglioAllegato segnatura = allegato.getDettagli().getOriginale();
                    IdentityFile infoDocumento = new IdentityFile("segnatura.xml", segnatura.getIdRepository(), segnatura.getHashMd5(), segnatura.getEstensione(), segnatura.getMimeType());
                    unitaDocumentariaBuilder.addDocumentoSecondario(segnatura.getIdRepository(),"SEGNATURA", "", segnatura.getNome(), i, infoDocumento, "Documento Generico", "annotazione", "Contenuto", "FILE", null , null);
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), infoDocumento);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getTipo() == Allegato.TipoAllegato.FRONTESPIZIO) {
                    Allegato.DettaglioAllegato frontespizio = allegato.getDettagli().getOriginale();
                    IdentityFile infoDocumento = new IdentityFile("frontespizio.pdf", frontespizio.getIdRepository(), frontespizio.getHashMd5(), frontespizio.getMimeType());
                    unitaDocumentariaBuilder.addDocumentoSecondario(frontespizio.getIdRepository(),"FRONTESPIZIO", "", frontespizio.getNome(), i, infoDocumento, "Documento Generico", "annotazione", "Contenuto", "FILE", null , null);
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), infoDocumento);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SINTESI_TRASPARENZA && allegato.getDettagli() != null) {
                    Allegato.DettaglioAllegato schedaSintesiTrasparenza = allegato.getDettagli().getOriginale();
                    IdentityFile infoDocumento = new IdentityFile("sintesitrasparenza.pdf", schedaSintesiTrasparenza.getIdRepository(), schedaSintesiTrasparenza.getHashMd5(), schedaSintesiTrasparenza.getMimeType());
                    unitaDocumentariaBuilder.addDocumentoSecondario(schedaSintesiTrasparenza.getIdRepository(),"SCHEDA SINTESI TRASPARENZA", "", schedaSintesiTrasparenza.getNome(), i, infoDocumento, "Documento Generico", "annotazione", "Contenuto", "FILE", null , null);
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), infoDocumento);
                    versamentiAllegatiInfo.add(allegatoInformation);
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.NOTE_DOCUMENTO) {
                    if (includiNoteParer) {
                        Allegato.DettaglioAllegato noteDocumento = allegato.getDettagli().getOriginale();
                        IdentityFile infoDocumento = new IdentityFile("notedelibera.pdf", noteDocumento.getIdRepository(), noteDocumento.getHashMd5(), noteDocumento.getMimeType());
                        unitaDocumentariaBuilder.addDocumentoSecondario(noteDocumento.getIdRepository(),"NOTE DOCUMENTO", "", noteDocumento.getNome(), i, infoDocumento, "Documento Generico", "annotazione", "Contenuto", "FILE", null , null);
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

    public DatiSpecifici buildDatiSpecificiRegistroGiornaliero(Doc doc, DocDetail docDetail) throws ParserConfigurationException {
        DatiSpecifici datiSpecifici;
        DatiSpecificiBuilder datiSpecificiBuilder = new DatiSpecificiBuilder();
        Pattern pattern = Pattern.compile("\"n.\\s(.*)\\sal\\sn.\\s(.*)\\sdel\\s(.*)");
        Matcher matcher = pattern.matcher(doc.getOggetto());
        String numeroIniziale = matcher.group(1);
        String numeroFinale = matcher.group(2);
        String giorno = matcher.group(3);
        Integer documenti = 0;
        Integer documentiAnnullati = 0;
        String applicativo = "";
        if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGPICO) {
            documenti = (Integer) entityManager.createQuery("select count(*) "
                    + "from scripta.docs_details dd2 "
                    + "where dd2.data_registrazione >=  :value1  and data_registrazione < (':value1::date + interval '1 days') "
                    + " and tipologia in ('PROTOCOLLO_IN_USCITA', 'PROTOCOLLO_IN_ENTRATA') "
                    + "and annullato = false")
                    .setParameter("value1", giorno).getSingleResult();
            documentiAnnullati = (Integer) entityManager.createQuery("select count(*) "
                    + "from scripta.docs_details dd2 "
                    + "where dd2.data_registrazione >=  :value1  and data_registrazione < (':value1::date + interval '1 days') "
                    + " and tipologia in ('PROTOCOLLO_IN_USCITA', 'PROTOCOLLO_IN_ENTRATA') "
                    + "and annullato = false")
                    .setParameter("value1", doc.getId()).getSingleResult();
            applicativo = "procton";
        } else if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGDETE) {
            documenti = (Integer) entityManager.createQuery("select count(*) "
                    + "from scripta.docs_details dd2 "
                    + "where dd2.data_registrazione >=  :value1  and data_registrazione < (':value1::date + interval '1 days') "
                    + "and tipologia = 'DETERMINA' "
                    + "and annullato = false")
                    .setParameter("value1", giorno).getSingleResult();
            documentiAnnullati = (Integer) entityManager.createQuery("select count(*) "
                    + "from scripta.docs_details dd2 "
                    + "where dd2.data_registrazione >=  :value1  and data_registrazione < (':value1::date + interval '1 days') "
                    + " and tipologia = 'DETERMINA' "
                    + "and annullato = false")
                    .setParameter("value1", doc.getId()).getSingleResult();
            applicativo = "dete";
        } else if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGDELI) {
            documenti = (Integer) entityManager.createQuery("select count(*) "
                    + "from scripta.docs_details dd2 "
                    + "where dd2.data_registrazione >=  :value1  and data_registrazione < (':value1::date + interval '1 days') "
                    + " and tipologia in ('DELIBERA') "
                    + "and annullato = false")
                    .setParameter("value1", giorno).getSingleResult();
            documentiAnnullati = (Integer) entityManager.createQuery("select count(*) "
                    + "from scripta.docs_details dd2 "
                    + "where dd2.data_registrazione >=  :value1  and data_registrazione < (':value1::date + interval '1 days') "
                    + " and tipologia = 'DELIBERA' "
                    + "and annullato = false")
                    .setParameter("value1", doc.getId()).getSingleResult();
            applicativo = "deli";

        }
        List<AttoreDoc> attori = entityManager.createQuery("SELECT * FROM scripta.attori_docs ad where ad.id_doc = :value1 ")
                .setParameter("value1", doc.getId()).getResultList();
        datiSpecificiBuilder.insertNewTag("VersioneDatiSpecifici", "1.0");
        datiSpecificiBuilder.insertNewTag("NumeroIniziale", numeroIniziale);
        datiSpecificiBuilder.insertNewTag("NumeroFinale", numeroFinale);
        datiSpecificiBuilder.insertNewTag("DataInizioRegistrazioni", giorno);
        datiSpecificiBuilder.insertNewTag("DataFineRegistrazioni", giorno);
        datiSpecificiBuilder.insertNewTag("Originatore", attori.get(0).getIdStruttura().getNome());
        datiSpecificiBuilder.insertNewTag("Responsabile", attori.get(0).getIdPersona().getDescrizione());
        datiSpecificiBuilder.insertNewTag("Operatore", "SISTEMA");
        datiSpecificiBuilder.insertNewTag("NumeroDocumentiRegistrati", documenti.toString());
        datiSpecificiBuilder.insertNewTag("NumeroDocumentiAnnullati", documentiAnnullati.toString());
        datiSpecificiBuilder.insertNewTag("DenominazioneApplicativo", applicativo);
        datiSpecificiBuilder.insertNewTag("VersioneApplicativo", "0.1");
        datiSpecificiBuilder.insertNewTag("ProduttoreApplicativo", "vuoto");
        datiSpecificiBuilder.insertNewTag("DenominazioneSistemaGestioneBaseDati", "PostgresSQL");
        datiSpecificiBuilder.insertNewTag("VersioneSistemaGestioneBaseDati", "12");
        datiSpecificiBuilder.insertNewTag("VersioneSistemaGestioneBaseDati", "The PostgreSQL Global Development Group");

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
    
    public String traduzioneTipologiaParer(DocDetailInterface.TipologiaDoc tipo){
        String tipologia = "";
        switch(tipo) {
            case PROTOCOLLO_IN_USCITA:
                tipologia = "PROTOCOLLO IN USCITA";
                   break;
               case PROTOCOLLO_IN_ENTRATA:
                   tipologia = "PROTOCOLLO IN ENTRATA";
                   break;
               case DETERMINA:
                   tipologia = "DETERMINAZIONE";
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
}
