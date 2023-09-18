/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.sdico;

import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.plugins.parer.ParerVersatoreMetadatiBuilder;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.riversamento.builder.IdentityFile;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author boria
 */
@Component
public class AllegatiBuilder {

    private static VersatoreRepositoryConfiguration versatoreRepositoryConfiguration;

    public AllegatiBuilder(VersatoreRepositoryConfiguration versatoreRepositoryConfiguration) {
        this.versatoreRepositoryConfiguration = versatoreRepositoryConfiguration;
    }

    //TODO riaggiungere
//    @Autowired
//    VersatoreRepositoryConfiguration versatoreRepositoryConfiguration;
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ParerVersatoreMetadatiBuilder.class);

    public Map<String, Object> buildMappaAllegati(Doc doc, DocDetail docDetail, List<Allegato> allegati, VersamentoBuilder versamentoBuilder) {
        Map<String, Object> mappaAllegati = new HashMap<>();
        try {
            mappaAllegati = buildAllegati(doc, docDetail, allegati, versamentoBuilder);
        } catch (MinIOWrapperException ex) {
            log.error("Errore di comunicazione con MinIO per recuperare i dati degli allegati");
        }

        return mappaAllegati;
    }

    /**
     * Metodo che crea i dati per gli allegati (SdicoFile) e le loro
     * informazioni per il versatore (VersatoreAllegatoInformation
     *
     * @return
     */
    public Map<String, Object> buildAllegati(Doc doc, DocDetail docDetail, List<Allegato> allegati, VersamentoBuilder versamentoBuilder) throws MinIOWrapperException {
        Map<String, Object> mappaPerAllegati = new HashMap<>();
        Integer i = 1;
        Integer indexCommittente = 1;
        Integer indexAlbo = 1;
        List<VersamentoAllegatoInformation> versamentiAllegatiInfo = new ArrayList<>();
        //Map<Integer, IdentityFile> identityFiles = new HashMap<>();
        List<IdentityFile> identityFiles = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS[xxx]");

        for (Allegato allegato : allegati) {
            Allegato.DettaglioAllegato originale = allegato.getDettagli().getOriginale();
            IdentityFile identityFile = new IdentityFile(originale.getNome(),
                    getUuidMinIObyFileId(originale.getIdRepository()),
                    originale.getHashMd5(), //TODO originale.getHashSha256() hash 256 non presente, solo hashMd5
                    null, //TODO da chiedere se posso lasciarlo vuoto
                    originale.getMimeType());
            identityFiles.add(identityFile);
            VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFile, versamentoBuilder, allegato.getFirmato());
            versamentiAllegatiInfo.add(allegatoInformation);
        }

        mappaPerAllegati.put("versamentiAllegatiInfo", versamentiAllegatiInfo);
        mappaPerAllegati.put("identityFiles", identityFiles);

//        for (Allegato allegato : allegati) {/*
//            if (allegato.getTipo() != Allegato.TipoAllegato.ANNESSO && allegato.getTipo() != Allegato.TipoAllegato.ANNOTAZIONE) {
//                if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA && allegato.getPrincipale() == true) {
//                    Allegato.DettaglioAllegato originale = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("allegato principale",
//                            getUuidMinIObyFileId(originale.getIdRepository()),
//                            originale.getHashMd5(),
//                            originale.getEstensione(),
//                            originale.getMimeType());
//                    identityFiles.add(identityFilePrincipale);
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA 
//                        && allegato.getFirmato() == true && allegato.getTipo() == Allegato.TipoAllegato.TESTO) {
//                    Allegato.DettaglioAllegato originaleFirmato = allegato.getDettagli().getOriginaleFirmato();
//                    IdentityFile identityFilePrincipale = new IdentityFile("letterafirmata.pdf", 
//                            getUuidMinIObyFileId(originaleFirmato.getIdRepository()),
//                            originaleFirmato.getHashMd5(), 
//                            "PDF", 
//                            "application/pdf"); 
//                    identityFiles.add(identityFilePrincipale);
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegatoFirmato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if ((doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGDELI 
//                        || doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGPICO 
//                        ||doc.getTipologia() == DocDetailInterface.TipologiaDoc.RGDETE )
//                        && allegato.getPrincipale() == true) {
//                    Allegato.DettaglioAllegato originale = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("allegato principale",
//                            getUuidMinIObyFileId(originale.getIdRepository()), 
//                            originale.getHashMd5(), 
//                            originale.getEstensione(),
//                            originale.getMimeType());
//                    identityFiles.add(identityFilePrincipale);
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if ((doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA
//                        || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA)
//                        && allegato.getFirmato() == true
//                        && allegato.getTipo() == Allegato.TipoAllegato.TESTO) {
//                    Allegato.DettaglioAllegato originaleFirmato = allegato.getDettagli().getOriginaleFirmato();
//                    IdentityFile identityFilePrincipale = new IdentityFile("testofirmato.pdf",
//                            getUuidMinIObyFileId(originaleFirmato.getIdRepository()),
//                            originaleFirmato.getHashMd5(),
//                            "PDF",
//                            "application/pdf");
//                    identityFiles.add(identityFilePrincipale);
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegatoFirmato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getFirmato() == true && allegato.getTipo() != Allegato.TipoAllegato.TESTO) {
//                    Allegato.DettaglioAllegato originaleFirmato = allegato.getDettagli().getOriginaleFirmato();
//                    IdentityFile identityFilePrincipale = new IdentityFile("allegato_firmato", 
//                            getUuidMinIObyFileId(originaleFirmato.getIdRepository()), 
//                            originaleFirmato.getHashMd5(), 
//                            originaleFirmato.getEstensione(), 
//                            originaleFirmato.getMimeType());
//                    identityFiles.add(identityFilePrincipale);
//                    i = i + 1;
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegatoFirmato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getDettagli().getConvertito() != null && allegato.getTipo() != Allegato.TipoAllegato.TESTO) {
//                    Allegato.DettaglioAllegato convertito = allegato.getDettagli().getConvertito();
//                    IdentityFile identityFilePrincipale = new IdentityFile("allegato_convertito", 
//                            getUuidMinIObyFileId(convertito.getIdRepository()), 
//                            convertito.getHashMd5(),
//                            "PDF",
//                            "application/pdf");
//                    identityFiles.add(identityFilePrincipale);
//                    i = i + 1;
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getTipo() == Allegato.TipoAllegato.ALLEGATO) {
//                    Allegato.DettaglioAllegato originale = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("allegato" + i.toString(), 
//                            getUuidMinIObyFileId(originale.getIdRepository()), 
//                            originale.getHashMd5(), 
//                            originale.getEstensione(), 
//                            originale.getMimeType());
//                    identityFiles.add(identityFilePrincipale);
//                    i = i + 1;
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                }
//            } else if (doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) {
//                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.DESTINATARI) {
//                    Allegato.DettaglioAllegato destinatari = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("destinatari.pdf", 
//                            getUuidMinIObyFileId(destinatari.getIdRepository()),
//                            destinatari.getHashMd5(), 
//                            "PDF", 
//                            "application/pdf");
//                    identityFiles.add(identityFilePrincipale);
//                    i = i + 1;
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                }
//                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RELATA) {
//                    if (allegato.getNome().startsWith("relata_COMMITTENTE")) {
//                        Allegato.DettaglioAllegato committente = allegato.getDettagli().getOriginale();
//                        IdentityFile identityFilePrincipale = new IdentityFile("relata committente " + indexCommittente.toString() + ".pdf",
//                                getUuidMinIObyFileId(committente.getIdRepository()),
//                                committente.getHashMd5(), 
//                                "PDF",
//                                "application/pdf");
//                        identityFiles.add(identityFilePrincipale);
//                        indexCommittente = indexCommittente + 1;
//                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                        versamentiAllegatiInfo.add(allegatoInformation);
//                    } else {
//                        Allegato.DettaglioAllegato albo = allegato.getDettagli().getOriginale();
//                        IdentityFile identityFilePrincipale = new IdentityFile("relata committente " + indexAlbo.toString() + ".pdf", 
//                                getUuidMinIObyFileId(albo.getIdRepository()), 
//                                albo.getHashMd5(), 
//                                "PDF",
//                                "application/pdf");
//                        identityFiles.add(identityFilePrincipale);
//                        indexAlbo = indexAlbo + 1;
//                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                        versamentiAllegatiInfo.add(allegatoInformation);
//                    }
//                } else if (allegato.getTipo() == Allegato.TipoAllegato.STAMPA_UNICA) {
//                    Allegato.DettaglioAllegato stampaUnica = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("stampaunica.pdf", 
//                            getUuidMinIObyFileId(stampaUnica.getIdRepository()),
//                            stampaUnica.getHashMd5(), 
//                            "PDF",
//                            "application/pdf");
//                    identityFiles.add(identityFilePrincipale);
//                    i = i + 1;
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getTipo() == Allegato.TipoAllegato.STAMPA_UNICA_OMISSIS) {
//                    Allegato.DettaglioAllegato stampaUnicaOmissis = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("stampaunicaconomissis.pdf", 
//                            getUuidMinIObyFileId(stampaUnicaOmissis.getIdRepository()),
//                            stampaUnicaOmissis.getHashMd5(), 
//                            "PDF",
//                            "application/pdf");
//                    identityFiles.add(identityFilePrincipale);
//                    i = i + 1;
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getTipo() == Allegato.TipoAllegato.TESTO_OMISSIS && doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA) {
//                    Allegato.DettaglioAllegato testoOmissis = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("deliberazioneomissis.pdf", 
//                            getUuidMinIObyFileId(testoOmissis.getIdRepository()), 
//                            testoOmissis.getHashMd5(), 
//                            "PDF", 
//                            "application/pdf");
//                    identityFiles.add(identityFilePrincipale);
//                    i = i + 1;
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getTipo() == Allegato.TipoAllegato.TESTO_OMISSIS && doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA) {
//                    Allegato.DettaglioAllegato testoOmissis = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("testofirmatomissis.pdf", 
//                            getUuidMinIObyFileId(testoOmissis.getIdRepository()), 
//                            testoOmissis.getHashMd5(), 
//                            "PDF", 
//                            "application/pdf");
//                    identityFiles.add(identityFilePrincipale);
//                    i = i + 1;
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                }
//            } else if ( doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA) {
//                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.DESTINATARI) {
//                    Allegato.DettaglioAllegato destinatari = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("destinatari.pdf", 
//                            getUuidMinIObyFileId(destinatari.getIdRepository()),
//                            destinatari.getHashMd5(),
//                            "PDF", "application/pdf");
//                    identityFiles.add(identityFilePrincipale);
//                    i = i + 1;
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RICEVUTA_ACCETTAZIONE_PEC) {
//                    Allegato.DettaglioAllegato accettazione = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("ricevuta_di_accettazione_" + i.toString(), 
//                            getUuidMinIObyFileId(accettazione.getIdRepository()), 
//                            accettazione.getHashMd5(), 
//                            "EML", 
//                            "message/rfc822");
//                    identityFiles.add(identityFilePrincipale);
//                    i = i + 1;
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RICEVUTA_CONSEGNA_PEC) {
//                    Allegato.DettaglioAllegato consegna = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("ricevuta_consegna_" + i.toString(),
//                            getUuidMinIObyFileId(consegna.getIdRepository()), 
//                            consegna.getHashMd5(), 
//                            "EML", 
//                            "message/rfc822");
//                    identityFiles.add(identityFilePrincipale);
//                    i = i + 1;
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RICEVUTA_ERRORE_PEC) {
//                    Allegato.DettaglioAllegato consegna = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("ricevuta_errore_consegna_" + i.toString(), 
//                            getUuidMinIObyFileId(consegna.getIdRepository()), 
//                            consegna.getHashMd5(), 
//                            "EML", 
//                            "message/rfc822");
//                    identityFiles.add(identityFilePrincipale);
//                    i = i + 1;
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.RELATA) {
//                    if (allegato.getNome().startsWith("relata_COMMITTENTE")) {
//                        Allegato.DettaglioAllegato committente = allegato.getDettagli().getOriginale();
//                        indexCommittente = indexCommittente + 1;
//                        IdentityFile identityFilePrincipale = new IdentityFile("relata_COMMITTENTE_" + indexCommittente.toString()+ ".pdf", 
//                                getUuidMinIObyFileId(committente.getIdRepository()), 
//                                committente.getHashMd5(), 
//                                "PDF", 
//                                "application/pdf");
//                        identityFiles.add(identityFilePrincipale);
//                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                        versamentiAllegatiInfo.add(allegatoInformation);
//                    } else {
//                        Allegato.DettaglioAllegato albo = allegato.getDettagli().getOriginale();
//                        IdentityFile identityFilePrincipale = new IdentityFile("relata_" + indexCommittente.toString() + ".pdf",
//                                getUuidMinIObyFileId(albo.getIdRepository()), 
//                                albo.getHashMd5(), 
//                                "PDF", 
//                                "application/pdf");
//                        identityFiles.add(identityFilePrincipale);
//                        indexAlbo = indexAlbo + 1;
//                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                        versamentiAllegatiInfo.add(allegatoInformation);
//                    }
//                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SMISTAMENTO) {
//                    Allegato.DettaglioAllegato smistamenti = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("smistamenti.pdf", 
//                            getUuidMinIObyFileId(smistamenti.getIdRepository()), 
//                            smistamenti.getHashMd5(), 
//                            "PDF", 
//                            "application/pdf");
//                    identityFiles.add(identityFilePrincipale);
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                }
//            } else if ( (doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_USCITA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA || doc.getTipologia() == DocDetailInterface.TipologiaDoc.DETERMINA)) {
//                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SEGNATURA) {
//                    Allegato.DettaglioAllegato segnatura = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("segnatura.xml", 
//                            getUuidMinIObyFileId(segnatura.getIdRepository()), 
//                            segnatura.getHashMd5(), 
//                            "XML",
//                            "text/xml");
//                    identityFiles.add(identityFilePrincipale);
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getTipo() == Allegato.TipoAllegato.FRONTESPIZIO) {
//                    Allegato.DettaglioAllegato frontespizio = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("frontespizio.pdf", 
//                            getUuidMinIObyFileId(frontespizio.getIdRepository()), 
//                            frontespizio.getHashMd5(), 
//                            "PDF",
//                            "application/pdf");
//                    identityFiles.add(identityFilePrincipale);
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SINTESI_TRASPARENZA && allegato.getDettagli() != null) {
//                    Allegato.DettaglioAllegato schedaSintesiTrasparenza = allegato.getDettagli().getOriginale();
//                    IdentityFile identityFilePrincipale = new IdentityFile("sintesitrasparenza.pdf",
//                            getUuidMinIObyFileId(schedaSintesiTrasparenza.getIdRepository()), 
//                            schedaSintesiTrasparenza.getHashMd5(), 
//                            "PDF", 
//                            "application/pdf");
//                    identityFiles.add(identityFilePrincipale);
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.NOTE_DOCUMENTO) {
//                    //TODO vedere se è roba che ci interessa
//                    /*if (includiNoteParer) {
//                        Allegato.DettaglioAllegato noteDocumento = allegato.getDettagli().getOriginale();
//                        IdentityFile identityFilePrincipale = new IdentityFile("notedocumento.pdf", getUuidMinIObyFileId(noteDocumento.getIdRepository()), noteDocumento.getHashMd5(), "PDF", "application/pdf");
//                        identityFiles.add(identityFilePrincipale);
//                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(noteDocumento.getIdRepository()),"NOTE DOCUMENTO", "", noteDocumento.getNome(), i, identityFilePrincipale, "DocumentoGenerico", "annotazione", "Contenuto", "FILE", null , null);
//                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFilePrincipale);
//                        versamentiAllegatiInfo.add(allegatoInformation);
//                    }*/
//                }
//            } else if ( doc.getTipologia() == DocDetailInterface.TipologiaDoc.DELIBERA) {
//                if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SEGNATURA) {
//                    Allegato.DettaglioAllegato segnatura = allegato.getDettagli().getOriginale();
//                    IdentityFile infoDocumento = new IdentityFile("segnatura.xml", 
//                            getUuidMinIObyFileId(segnatura.getIdRepository()), 
//                            segnatura.getHashMd5(),
//                            segnatura.getEstensione(),
//                            segnatura.getMimeType());
//                    identityFiles.add(infoDocumento);
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), infoDocumento);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getTipo() == Allegato.TipoAllegato.FRONTESPIZIO) {
//                    Allegato.DettaglioAllegato frontespizio = allegato.getDettagli().getOriginale();
//                    //TODO da verificare
//                    IdentityFile infoDocumento = new IdentityFile("frontespizio.pdf", 
//                            getUuidMinIObyFileId(frontespizio.getIdRepository()), 
//                            frontespizio.getHashMd5(),
//                            frontespizio.getMimeType());
//                    identityFiles.add(infoDocumento);
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), infoDocumento);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.SINTESI_TRASPARENZA && allegato.getDettagli() != null) {
//                    Allegato.DettaglioAllegato schedaSintesiTrasparenza = allegato.getDettagli().getOriginale();
//                    //TODO come sopra
//                    IdentityFile infoDocumento = new IdentityFile("sintesitrasparenza.pdf", 
//                            getUuidMinIObyFileId(schedaSintesiTrasparenza.getIdRepository()), 
//                            schedaSintesiTrasparenza.getHashMd5(), 
//                            schedaSintesiTrasparenza.getMimeType());
//                    identityFiles.add(infoDocumento);
//                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), infoDocumento);
//                    versamentiAllegatiInfo.add(allegatoInformation);
//                } else if (allegato.getSottotipo() == Allegato.SottotipoAllegato.NOTE_DOCUMENTO) {
//                    //TODO vedere se è roba che ci interessa
//                    /*
//                    if (includiNoteParer) {
//                        Allegato.DettaglioAllegato noteDocumento = allegato.getDettagli().getOriginale();
//                        IdentityFile infoDocumento = new IdentityFile("notedelibera.pdf", getUuidMinIObyFileId(noteDocumento.getIdRepository()), noteDocumento.getHashMd5(), noteDocumento.getMimeType());
//                        identityFiles.add(infoDocumento);
//                    unitaDocumentariaBuilder.addDocumentoSecondario(getUuidMinIObyFileId(noteDocumento.getIdRepository()),"NOTE DOCUMENTO", "", noteDocumento.getNome(), i, infoDocumento, "DocumentoGenerico", "annotazione", "Contenuto", "FILE", null , null);
//                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), infoDocumento);
//                        versamentiAllegatiInfo.add(allegatoInformation);
//                    }*/
//                }
////            }*/
//            mappaPerAllegati.put("versamentiAllegatiInfo", versamentiAllegatiInfo);
//            mappaPerAllegati.put("identityFiles", identityFiles);
//        }
        return mappaPerAllegati;
    }

    private String getUuidMinIObyFileId(String fileId) throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = versatoreRepositoryConfiguration.getVersatoreRepositoryManager().getMinIOWrapper();
        MinIOWrapperFileInfo fileInfoByFileId = minIOWrapper.getFileInfoByFileId(fileId);
        return fileInfoByFileId.getMongoUuid();
    }

//    private VersamentoAllegatoInformation createVersamentoAllegato(Integer idAllegato, IdentityFile identityFile) {
//        VersamentoAllegatoInformation allegatoInformation = new VersamentoAllegatoInformation();
//        allegatoInformation.setIdAllegato(idAllegato);
//        allegatoInformation.setTipoDettaglioAllegato(Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE);
//        allegatoInformation.setStatoVersamento(it.bologna.ausl.model.entities.versatore.Versamento.StatoVersamento.IN_CARICO);
//        allegatoInformation.setDataVersamento(ZonedDateTime.now());
//        //allegatoInformation.setMetadatiVersati(identityFile.getJSON().toJSONString());
//        VersamentoBuilder versamentoBuilder = new VersamentoBuilder();
//        versamentoBuilder.addFileByParams(identityFile.getFileName(), identityFile.getMime(), identityFile.getHash());
//        allegatoInformation.setMetadatiVersati(versamentoBuilder.toString());
//        return allegatoInformation;
//    }

//     private VersamentoAllegatoInformation createVersamentoAllegato(Integer idAllegato, IdentityFile identityFile) {
//        VersamentoAllegatoInformation allegatoInformation = new VersamentoAllegatoInformation();
//        allegatoInformation.setIdAllegato(idAllegato);
//        allegatoInformation.setTipoDettaglioAllegato(Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE);
//        allegatoInformation.setStatoVersamento(it.bologna.ausl.model.entities.versatore.Versamento.StatoVersamento.IN_CARICO);
//        allegatoInformation.setDataVersamento(ZonedDateTime.now());
//        //allegatoInformation.setMetadatiVersati(identityFile.getJSON().toJSONString());
//        return allegatoInformation;
//    }
    private VersamentoAllegatoInformation createVersamentoAllegato(Integer idAllegato, IdentityFile identityFile, VersamentoBuilder versamentoBuilder, Boolean firmato) {
        VersamentoAllegatoInformation allegatoInformation = new VersamentoAllegatoInformation();
        allegatoInformation.setIdAllegato(idAllegato);
        if (firmato) {
            allegatoInformation.setTipoDettaglioAllegato(Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE_FIRMATO);
        } else {
            allegatoInformation.setTipoDettaglioAllegato(Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE);
        }
        allegatoInformation.setStatoVersamento(it.bologna.ausl.model.entities.versatore.Versamento.StatoVersamento.IN_CARICO);
        allegatoInformation.setDataVersamento(ZonedDateTime.now());
        allegatoInformation.setMetadatiVersati(identityFile.getJSON().toJSONString());
//        VersamentoBuilder versamentoBuilder = new VersamentoBuilder();
        versamentoBuilder.addFileByParams(identityFile.getFileName(), identityFile.getMime(), identityFile.getHash());
//        allegatoInformation.setMetadatiVersati(versamentoBuilder.toString());
        return allegatoInformation;
    }

}
