/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.parer;

import aj.org.objectweb.asm.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.internauta.utils.versatore.plugins.infocert.InfocertVersatoreService;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.AttoreDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.riversamento.builder.DatiSpecificiBuilder;
import it.bologna.ausl.riversamento.builder.IdentityFile;
import it.bologna.ausl.riversamento.builder.InfoDocumento;
import it.bologna.ausl.riversamento.builder.ProfiloArchivistico;
import it.bologna.ausl.riversamento.builder.UnitaDocumentariaBuilder;
import it.bologna.ausl.riversamento.builder.oggetti.DatiSpecifici;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
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
public class ParerVersatoreMetadatiBuilder {
    
       @Autowired
    private ParametriAziendeReader parametriAziende;

    @Autowired
    private EntityManager entityManager;

    private static final Logger log = LoggerFactory.getLogger(InfocertVersatoreService.class);

    ParerVersatoreMetadatiBuilder(Doc doc, DocDetail docDetail) {

        List<ArchivioDoc> archiviazioni = entityManager.createQuery("SELECT * FROM scripta.archivi_docs ad where ad.id_doc = :value1 order by ad.data_archiviziazione ASC")
                .setParameter("value1", doc.getId()).getResultList();

        if (archiviazioni != null) { 
            String dataArchiviazione = archiviazioni.get(0).getDataArchiviazione().toLocalDate().toString();
            try {
                log.info("buildo il profilo archivistico");
                ProfiloArchivistico profiloArchivistico = buildProfiloArchivistico(doc, docDetail, archiviazioni);
                try {
                    log.info("buildo i dati specifici");
                    DatiSpecifici datiSpecifici;
                    if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA) {
                        datiSpecifici = buildDatiSpecifici(doc, docDetail, dataArchiviazione);
                    } else if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGPICO || doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGDELI || doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGDETE) {
                        datiSpecifici = buildDatiSpecificiRegistroGiornaliero(doc, docDetail);
                    }
                    
                    UnitaDocumentariaBuilder unitaDocumentariaBuilder = new UnitaDocumentariaBuilder(docDetail.getNumeroRegistrazione(), docDetail.getAnnoRegistrazione(), dataArchiviazione, dataArchiviazione, dataArchiviazione, dataArchiviazione, dataArchiviazione, profiloArchivistico, dataArchiviazione, dataArchiviazione, datiSpecifici, dataArchiviazione, dataArchiviazione, dataArchiviazione, dataArchiviazione, dataArchiviazione, dataArchiviazione, dataArchiviazione);
                    
                } catch (ParserConfigurationException ex) {
                    java.util.logging.Logger.getLogger(ParerVersatoreService.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (Exception ex) {
                log.error("Qualcosa non ha funzionato nel buildare il profilo archivistico", ex);
            }

        }

    }

    private DatiSpecifici buildDatiSpecifici(Doc doc, DocDetail docDetail, String dataArchiviazione) throws ParserConfigurationException {

        DatiSpecifici datiSpecifici = null;
        ObjectMapper mapper = new ObjectMapper();

        List<ParametroAziende> versioneDatiSpecParametro = parametriAziende.getParameters("versioneDatispecificiParer", new Integer[]{doc.getIdAzienda().getId()});

        datiSpecifici.setVersioneDatiSpecifici(parametriAziende.getValue(versioneDatiSpecParametro.get(0), String.class));
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

    private HashMap<String, Object> buildAllegati(Doc doc) {
        List<Allegato> allegati = entityManager.createQuery("SELECT * FROM scripta.allegati a where a.id_doc = :value1")
                .setParameter("value1", doc.getId()).getResultList();
        IdentityFile identityFilePrincipale;
        Integer i = 1;
        Integer indexCommittente = 1;
        Integer indexAlbo = 1;
        HashMap<String, Object> allegatiTotali = null;
        ArrayList<HashMap<String, Object>> allegatiJson = null;
        ArrayList<HashMap<String, Object>> annessiJson = null;
        ArrayList<HashMap<String, Object>> annotazioniJson = null;
        HashMap<String, Object> allegatoPrincipale = null;
        for (Allegato allegato : allegati) {
            if (allegato.getTipo() != Allegato.TipoAllegato.ANNESSO && allegato.getTipo() != Allegato.TipoAllegato.ANNOTAZIONE) {
                if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA && allegato.getPrincipale() == true) {
                    Allegato.DettaglioAllegato originale = allegato.getDettagli().getOriginale();
                    identityFilePrincipale = new IdentityFile("allegato principale", originale.getIdRepository(), originale.getHashMd5(), originale.getEstensione(), originale.getMimeType());
                    allegatoPrincipale = identityFilePrincipale.getJSON();
                } else if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA && allegato.getFirmato() == true && allegato.getTipo() == Allegato.TipoAllegato.TESTO) {
                    Allegato.DettaglioAllegato originaleFirmato = allegato.getDettagli().getOriginaleFirmato();
                    identityFilePrincipale = new IdentityFile("letterafirmata.pdf", originaleFirmato.getIdRepository(), originaleFirmato.getHashMd5(), "PDF", "application/pdf");
                    allegatoPrincipale = identityFilePrincipale.getJSON();
                } else if ((doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) && allegato.getFirmato() == true && allegato.getTipo() == Allegato.TipoAllegato.TESTO) {
                    Allegato.DettaglioAllegato originaleFirmato = allegato.getDettagli().getOriginaleFirmato();
                    identityFilePrincipale = new IdentityFile("testofirmato.pdf", originaleFirmato.getIdRepository(), originaleFirmato.getHashMd5(), "PDF", "application/pdf");
                    allegatoPrincipale = identityFilePrincipale.getJSON();
                } else if (allegato.getFirmato() == true && allegato.getTipo() != Allegato.TipoAllegato.TESTO) {
                    Allegato.DettaglioAllegato originaleFirmato = allegato.getDettagli().getOriginaleFirmato();
                    identityFilePrincipale = new IdentityFile("allegato_firmato", originaleFirmato.getIdRepository(), originaleFirmato.getHashMd5(), "PDF", "application/pdf");
                    allegatiJson.add(identityFilePrincipale.getJSON());
                    i = i + 1;
                } else if (allegato.getDettagli().getConvertito() != null && allegato.getTipo() != Allegato.TipoAllegato.TESTO) {
                    Allegato.DettaglioAllegato convertito = allegato.getDettagli().getConvertito();
                    identityFilePrincipale = new IdentityFile("allegato_firmato", convertito.getIdRepository(), convertito.getHashMd5(), "PDF", "application/pdf");
                    allegatiJson.add(identityFilePrincipale.getJSON());
                    i = i + 1;
                } else if (allegato.getTipo() == Allegato.TipoAllegato.ALLEGATO) {
                    Allegato.DettaglioAllegato originale = allegato.getDettagli().getOriginale();
                    identityFilePrincipale = new IdentityFile("allegato" + i.toString(), originale.getIdRepository(), originale.getHashMd5(), originale.getEstensione(), originale.getMimeType());
                    allegatiJson.add(identityFilePrincipale.getJSON());
                    i = i + 1;
                }

            } else if (allegato.getTipo() == Allegato.TipoAllegato.ANNESSO && (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA)) {
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.DESTINATARI) {
                    Allegato.DettaglioAllegato destinatari = allegato.getDettagli().getOriginale();
                    identityFilePrincipale = new IdentityFile("destinatari.pdf", destinatari.getIdRepository(), destinatari.getHashMd5(), "PDF", "application/pdf");
                    annessiJson.add(identityFilePrincipale.getJSON());
                }
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RELATA) {
                    if (allegato.getNome().startsWith("relata_COMMITTENTE")) {
                        Allegato.DettaglioAllegato committente = allegato.getDettagli().getOriginale();
                        identityFilePrincipale = new IdentityFile("relata committente " + indexCommittente.toString() + ".pdf", committente.getIdRepository(), committente.getHashMd5(), "PDF", "application/pdf");
                        indexCommittente = indexCommittente + 1;
                        annessiJson.add(identityFilePrincipale.getJSON());
                    } else {
                        Allegato.DettaglioAllegato albo = allegato.getDettagli().getOriginale();
                        identityFilePrincipale = new IdentityFile("relata committente " + indexAlbo.toString() + ".pdf", albo.getIdRepository(), albo.getHashMd5(), "PDF", "application/pdf");
                        indexAlbo = indexAlbo + 1;
                        annessiJson.add(identityFilePrincipale.getJSON());
                    }
                } else if (allegato.getTipo() == Allegato.TipoAllegato.STAMPA_UNICA) {
                    Allegato.DettaglioAllegato stampaUnica = allegato.getDettagli().getOriginale();
                    identityFilePrincipale = new IdentityFile("stampaunica.pdf", stampaUnica.getIdRepository(), stampaUnica.getHashMd5(), "PDF", "application/pdf");
                    annessiJson.add(identityFilePrincipale.getJSON());
                } else if (allegato.getTipo() == Allegato.TipoAllegato.STAMPA_UNICA_OMISSIS) {
                    Allegato.DettaglioAllegato stampaUnicaOmissis = allegato.getDettagli().getOriginale();
                    identityFilePrincipale = new IdentityFile("stampaunicaconomissis.pdf", stampaUnicaOmissis.getIdRepository(), stampaUnicaOmissis.getHashMd5(), "PDF", "application/pdf");
                    annessiJson.add(identityFilePrincipale.getJSON());
                } else if (allegato.getTipo() == Allegato.TipoAllegato.TESTO_OMISSIS && doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA) {
                    Allegato.DettaglioAllegato testoOmissis = allegato.getDettagli().getOriginale();
                    identityFilePrincipale = new IdentityFile("deliberazioneomissis.pdf", testoOmissis.getIdRepository(), testoOmissis.getHashMd5(), "PDF", "application/pdf");
                    annessiJson.add(identityFilePrincipale.getJSON());
                } else if (allegato.getTipo() == Allegato.TipoAllegato.TESTO_OMISSIS && doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) {
                    Allegato.DettaglioAllegato testoOmissis = allegato.getDettagli().getOriginale();
                    identityFilePrincipale = new IdentityFile("testofirmatomissis.pdf", testoOmissis.getIdRepository(), testoOmissis.getHashMd5(), "PDF", "application/pdf");
                    annessiJson.add(identityFilePrincipale.getJSON());
                }
            } else if (allegato.getTipo() == Allegato.TipoAllegato.ANNESSO && (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA)) {
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.DESTINATARI) {
                    Allegato.DettaglioAllegato destinatari = allegato.getDettagli().getOriginale();
                    InfoDocumento infoDocumento = new InfoDocumento(allegato.getId().toString(), "ELENCO DESTINATARI", null, destinatari.getNome(), destinatari.getIdRepository(), "DocumentoGenerico", "annesso");
                    annessiJson.add(infoDocumento.getJSON());
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RICEVUTA_ACCETTAZIONE_PEC) {
                    Allegato.DettaglioAllegato accettazione = allegato.getDettagli().getOriginale();
                    InfoDocumento infoDocumento = new InfoDocumento(allegato.getId().toString(), "RICEVUTA DI ACCETTAZIONE", null, accettazione.getNome(), accettazione.getIdRepository(), "DocumentoGenerico", "annesso");
                    annessiJson.add(infoDocumento.getJSON());
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RICEVUTA_CONSEGNA_PEC) {
                    Allegato.DettaglioAllegato consegna = allegato.getDettagli().getOriginale();
                    InfoDocumento infoDocumento = new InfoDocumento(allegato.getId().toString(), "RICEVUTA DI ACCETTAZIONE", null, consegna.getNome(), consegna.getIdRepository(), "DocumentoGenerico", "annesso");
                    annessiJson.add(infoDocumento.getJSON());
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RICEVUTA_ERRORE_PEC) {
                    Allegato.DettaglioAllegato consegna = allegato.getDettagli().getOriginale();
                    InfoDocumento infoDocumento = new InfoDocumento(allegato.getId().toString(), "RICEVUTA DI ERRORE CONSEGNA", null, consegna.getNome(), consegna.getIdRepository(), "DocumentoGenerico", "annesso");
                    annessiJson.add(infoDocumento.getJSON());
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RELATA) {
                    if (allegato.getNome().startsWith("relata_COMMITTENTE")) {
                        Allegato.DettaglioAllegato committente = allegato.getDettagli().getOriginale();
                        InfoDocumento infoDocumento = new InfoDocumento(allegato.getId().toString(), "RELATA PUBBLICAZIONE PROFILO COMMITTENTE", null, "relata committente " + indexCommittente.toString(), committente.getIdRepository(), "DocumentoGenerico", "annesso");
                        annessiJson.add(infoDocumento.getJSON());
                        indexCommittente = indexCommittente + 1;

                    } else {
                        Allegato.DettaglioAllegato albo = allegato.getDettagli().getOriginale();
                        InfoDocumento infoDocumento = new InfoDocumento(allegato.getId().toString(), "RELATA DI PUBBLICAZIONE", null, "relata " + indexAlbo.toString() + " pubblicazione albo informatico", albo.getIdRepository(), "DocumentoGenerico", "annesso");
                        annessiJson.add(infoDocumento.getJSON());
                        indexAlbo = indexAlbo + 1;
                    }
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SMISTAMENTO) {
                    Allegato.DettaglioAllegato smistamenti = allegato.getDettagli().getOriginale();
                    InfoDocumento infoDocumento = new InfoDocumento(allegato.getId().toString(), "ELENCO SMISTAMENTI", null, "smistamenti", smistamenti.getIdRepository(), "DocumentoGenerico", "annesso");
                    annessiJson.add(infoDocumento.getJSON());
                }
            } else if (allegato.getTipo() == Allegato.TipoAllegato.ANNOTAZIONE && (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA)) {
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SEGNATURA) {
                    Allegato.DettaglioAllegato segnatura = allegato.getDettagli().getOriginale();
                    InfoDocumento infoDocumento = new InfoDocumento(allegato.getId().toString(), "SEGNATURA", null, "segnatura.xml", segnatura.getIdRepository(), "DocumentoGenerico", "annotazione");
                    annotazioniJson.add(infoDocumento.getJSON());
                } else if (allegato.getTipo() == Allegato.TipoAllegato.FRONTESPIZIO) {
                    Allegato.DettaglioAllegato frontespizio = allegato.getDettagli().getOriginale();
                    InfoDocumento infoDocumento = new InfoDocumento(allegato.getId().toString(), "FRONTESPIZIO", null, "Frontespizio", frontespizio.getIdRepository(), "DocumentoGenerico", "annotazione");
                    annotazioniJson.add(infoDocumento.getJSON());
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SINTESI_TRASPARENZA && allegato.getDettagli() != null) {
                    Allegato.DettaglioAllegato schedaSintesiTrasparenza = allegato.getDettagli().getOriginale();
                    InfoDocumento infoDocumento = new InfoDocumento(allegato.getId().toString(), "SCHEDA SINTESI TRASPARENZA", null, "Sintesi Trasparenza", schedaSintesiTrasparenza.getIdRepository(), "DocumentoGenerico", "annotazione");
                    annotazioniJson.add(infoDocumento.getJSON());
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.NOTE_DOCUMENTO) {
                    List<ParametroAziende> includiNoteParer = parametriAziende.getParameters("includiNoteParer", new Integer[]{doc.getIdAzienda().getId()});
                    if (parametriAziende.getValue(includiNoteParer.get(0), String.class)) {
                        Allegato.DettaglioAllegato noteDocumento = allegato.getDettagli().getOriginale();
                        InfoDocumento infoDocumento = new InfoDocumento(allegato.getId().toString(), "NOTE AL DOCUMENTO", null, "Note Al Documento", noteDocumento.getIdRepository(), "DocumentoGenerico", "annotazione");
                        annotazioniJson.add(infoDocumento.getJSON());
                    }
                }
            } else if (allegato.getTipo() == Allegato.TipoAllegato.ANNOTAZIONE && doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA) {
                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SEGNATURA) {
                    Allegato.DettaglioAllegato segnatura = allegato.getDettagli().getOriginale();
                    IdentityFile infoDocumento = new IdentityFile("segnatura.xml", segnatura.getIdRepository(), segnatura.getHashMd5(), segnatura.getEstensione(), segnatura.getMimeType());
                    annotazioniJson.add(infoDocumento.getJSON());
                } else if (allegato.getTipo() == Allegato.TipoAllegato.FRONTESPIZIO) {
                    Allegato.DettaglioAllegato frontespizio = allegato.getDettagli().getOriginale();
                    IdentityFile infoDocumento = new IdentityFile("frontespizio.pdf", frontespizio.getIdRepository(), frontespizio.getHashMd5(), frontespizio.getMimeType());
                    annotazioniJson.add(infoDocumento.getJSON());
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SINTESI_TRASPARENZA && allegato.getDettagli() != null) {
                    Allegato.DettaglioAllegato schedaSintesiTrasparenza = allegato.getDettagli().getOriginale();
                    IdentityFile infoDocumento = new IdentityFile("sintesitrasparenza.pdf", schedaSintesiTrasparenza.getIdRepository(), schedaSintesiTrasparenza.getHashMd5(), schedaSintesiTrasparenza.getMimeType());
                    annotazioniJson.add(infoDocumento.getJSON());
                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.NOTE_DOCUMENTO) {
                    List<ParametroAziende> includiNoteParer = parametriAziende.getParameters("includiNoteParer", new Integer[]{doc.getIdAzienda().getId()});
                    if (parametriAziende.getValue(includiNoteParer.get(0), (Class<T>) String.class)) {
                        Allegato.DettaglioAllegato noteDocumento = allegato.getDettagli().getOriginale();
                        IdentityFile infoDocumento = new IdentityFile("notedelibera.pdf", noteDocumento.getIdRepository(), noteDocumento.getHashMd5(), noteDocumento.getMimeType());
                        annotazioniJson.add(infoDocumento.getJSON());
                    }
                }
            }
            allegatiTotali.put("principalidentityfile", allegatoPrincipale);
            allegatiTotali.put("allegatidentityfile", allegatiJson);
            allegatiTotali.put("informazioniannessi", annessiJson);
            allegatiTotali.put("informazioniannotazioni", annotazioniJson);
        }
        return allegatiTotali;
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
}
