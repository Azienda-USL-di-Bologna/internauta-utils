package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders;

import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities.AllegatoUnimatica;
import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatorePluginException;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatorePluginExceptionRitentabile;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities.IdentityFileUnimatica;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.AllegatoInterface;
import it.bologna.ausl.model.entities.scripta.Doc;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author boria
 */
@Component
public class AllegatiBuilderUnimatica {

    private static VersatoreRepositoryConfiguration versatoreRepositoryConfiguration;

    public AllegatiBuilderUnimatica(VersatoreRepositoryConfiguration versatoreRepositoryConfiguration) {
        this.versatoreRepositoryConfiguration = versatoreRepositoryConfiguration;
    }

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(AllegatiBuilderUnimatica.class);

    public Map<String, Object> buildMappaAllegati(Doc doc, List<Allegato> allegatiList) throws VersatorePluginExceptionRitentabile, VersatorePluginException {
        Map<String, Object> mappaAllegati = new HashMap<>();
        try {
            mappaAllegati = buildAllegati(allegatiList, doc);
        } catch (MinIOWrapperException ex) {
            log.error("Errore di comunicazione nel recuperare i dati degli allegati", ex);
            throw new VersatorePluginExceptionRitentabile("Errore di comunicazione nel recuperare i dati degli allegati");
        }

        return mappaAllegati;
    }

    public Map<String, Object> buildAllegati(List<Allegato> allegatiList, Doc doc) throws MinIOWrapperException, VersatorePluginExceptionRitentabile, VersatorePluginException {
        Map<String, Object> mappaPerAllegati = new HashMap<>();
        List<VersamentoAllegatoInformation> versamentiAllegatiInfo = new ArrayList<>();
        List<IdentityFileUnimatica> identityFiles = new ArrayList<>();
        AllegatoUnimatica documentoPrincipale = new AllegatoUnimatica();
        List<AllegatoUnimatica> allegatiSecondariList = new ArrayList<>();
        for (Allegato allegato : allegatiList) {
            if (!allegato.getEliminato()) {
                log.info("Raccologo i dati dell'allegato ID " + allegato.getId());
                if (allegato.getFirmato()) {
                    //guardo se è firmato e in tal caso lo processo
                    Allegato.DettaglioAllegato originaleFirmato = allegato.getDettagli().getOriginaleFirmato();
                    IdentityFileUnimatica identityFile = getAllegatoInformation(originaleFirmato, allegato.getId());
                    identityFiles.add(identityFile);
                    Allegato.DettagliAllegato.TipoDettaglioAllegato tipoAllegato = Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE_FIRMATO;
                    VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFile, tipoAllegato);
                    versamentiAllegatiInfo.add(allegatoInformation);
                    AllegatoUnimatica allegatoUnimatica = new AllegatoUnimatica(allegato.getId(),
                        originaleFirmato.getNome(),
                        identityFile.getHash(),
                        allegato.getFirmato(),
                        originaleFirmato.getMimeType()
                    );
                    //assegno il documento principale
                    if (doc.getTipologia().equals(Doc.TipologiaDoc.PROTOCOLLO_IN_ENTRATA) && allegato.getPrincipale()
                        || doc.getTipologia().equals(Doc.TipologiaDoc.PROTOCOLLO_IN_USCITA) && allegato.getTipo().equals(Allegato.TipoAllegato.TESTO)) {
                        //se sono in un pe guardo se è l'allegato principale
                        //oppure sono in un pu ed è di tipo testo (la lettera),
                        //in quel caso lo aggiungo come allegato principale
                        documentoPrincipale = allegatoUnimatica;
                    } else {
                        //altrimenti lo aggiungo agli allegati secondari
                        allegatiSecondariList.add(allegatoUnimatica);
                    }
                } else {
                    if (allegato.getTipo().equals(Allegato.TipoAllegato.STAMPA_UNICA)
                        || ((doc.getTipologia().equals(Doc.TipologiaDoc.PROTOCOLLO_IN_ENTRATA) || doc.getTipologia().equals(Doc.TipologiaDoc.RGPICO)) && allegato.getPrincipale())
                        || ((doc.getTipologia().equals(Doc.TipologiaDoc.DETERMINA) || doc.getTipologia().equals(Doc.TipologiaDoc.DELIBERA))
                        && (allegato.getTipo().equals(Allegato.TipoAllegato.TESTO_OMISSIS) || allegato.getTipo().equals(Allegato.TipoAllegato.STAMPA_UNICA_OMISSIS)))
                        || AllegatoInterface.SottotipoAllegato.SEGNATURA.equals(allegato.getSottotipo())) {
                        //guardo se è la stampa unica
                        //oppure l'allegato principale di un pe o di un rgpico
                        //oppure il testo omissis o la stampa unica omissis di una dete o una deli
                        //oppure è la segnatura
                        //in quel caso la processo
                        Allegato.DettaglioAllegato originale = allegato.getDettagli().getOriginale();
                        IdentityFileUnimatica identityFile = getAllegatoInformation(originale, allegato.getId());
                        identityFiles.add(identityFile);
                        Allegato.DettagliAllegato.TipoDettaglioAllegato tipoAllegato = Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE;
                        VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFile, tipoAllegato);
                        versamentiAllegatiInfo.add(allegatoInformation);
                        //assegno il documento principale
                        AllegatoUnimatica allegatoUnimatica = new AllegatoUnimatica(allegato.getId(),
                            originale.getNome(),
                            identityFile.getHash(),
                            allegato.getFirmato(),
                            originale.getMimeType()
                        );
                        if ((doc.getTipologia().equals(Doc.TipologiaDoc.PROTOCOLLO_IN_ENTRATA) || doc.getTipologia().equals(Doc.TipologiaDoc.RGPICO)) && allegato.getPrincipale()) {
                            //se sono in un pe o in un rgpico guardo se è l'allegato principale,
                            //in quel caso lo aggiungo come allegato principale
                            documentoPrincipale = allegatoUnimatica;
                        } else {
                            //altrimenti lo aggiungo agli allegati secondari
                            allegatiSecondariList.add(allegatoUnimatica);
                        }
                    }
                }
            }
        }
        mappaPerAllegati.put("versamentiAllegatiInfo", versamentiAllegatiInfo);
        mappaPerAllegati.put("identityFiles", identityFiles);
        mappaPerAllegati.put("allegatiSecondari", allegatiSecondariList);
        //controllo il documento principale sia presente e in quel caso lo inserisco nella mappa
        if (documentoPrincipale.getIdFile() != null) {
            mappaPerAllegati.put("documentoPrincipale", documentoPrincipale);
        } else {
            log.error("Il documento non ha un allegato da definire come Documento principale");
            throw new VersatorePluginException("Il documento non ha un allegato da definire come Documento principale");
        }
        return mappaPerAllegati;
    }

    /**
     * Metodo che prende e inserisce in un IdentityFile i dati di un allegato
     *
     * @param dettaglio
     * @return
     * @throws MinIOWrapperException
     */
    private IdentityFileUnimatica getAllegatoInformation(Allegato.DettaglioAllegato dettaglio, Integer idAllegato) throws MinIOWrapperException, VersatorePluginException {
        //Controllo se nel nome del file è già inserita l'estensione
        int lastDotIndex = dettaglio.getNome().lastIndexOf(".");
        String estensione = "";
        if (lastDotIndex >= 0) {
            estensione = dettaglio.getNome().substring(lastDotIndex + 1);
        }
        //Nel caso dei documenti provenienti da argo l'estensione del file è già inserita nel nome del file; per quelli invece caricati da internauta
        //in "dettagli" nome del file ed estensione sono separati. Al momento solo i doc GEDI (tiplogia DOCUMENT_UTENTE) vengono caricati direttamente da
        //internauta, ecco il perché dell'aggiunta seguente nella stringa che formerà il nome del file.
        IdentityFileUnimatica identityFile = new IdentityFileUnimatica(dettaglio.getNome()
            + (!(dettaglio.getEstensione().equals(estensione)) ? "." + dettaglio.getEstensione() : ""),
            getUuidMinIObyFileId(dettaglio.getIdRepository()),
            dettaglio.getHashSha256(),
            null,
            dettaglio.getMimeType(),
            idAllegato
        );
        //if (identityFile.getUuidMongo() == null) {
        identityFile.setFileBase64(dettaglio.getIdRepository());
        //}
        //TODO la funzione per calcolare lo SHA256 la richiamo qui, controllando se hash256 è nullo, da togliere una volta che lo sha256 sarà sempre presente
        if (identityFile.getHash() == null) {
            identityFile = calcolaSHA256(identityFile);
        }
        return identityFile;
    }

    /**
     * Metodo che recupera il MongoUuid dell'allegato da MinIO
     *
     * @param fileId
     * @return
     * @throws MinIOWrapperException
     */
    private String getUuidMinIObyFileId(String fileId) throws MinIOWrapperException {
        MinIOWrapper minIOWrapper = versatoreRepositoryConfiguration.getVersatoreRepositoryManager().getMinIOWrapper();
        MinIOWrapperFileInfo fileInfoByFileId = minIOWrapper.getFileInfoByFileId(fileId);
        //se fileInfoByFileId è vuoto potrebbe essere perché su minIo il file risulta deleted = true
        return fileInfoByFileId.getMongoUuid();
    }

    /**
     * Metodo che inserisce le informazioni relative all'allegato in
     * VersamentoAllegatoInformatio e aggiunge al versamentoBuilder la parte di
     * XML relativa
     *
     * @param idAllegato
     * @param identityFile
     * @param versamentoBuilder
     * @param tipoAllegato
     * @return
     */
    private VersamentoAllegatoInformation createVersamentoAllegato(Integer idAllegato,
        IdentityFileUnimatica identityFile,
        Allegato.DettagliAllegato.TipoDettaglioAllegato tipoAllegato) {
        VersamentoAllegatoInformation allegatoInformation = new VersamentoAllegatoInformation();
        allegatoInformation.setIdAllegato(idAllegato);
        allegatoInformation.setTipoDettaglioAllegato(tipoAllegato);
        allegatoInformation.setStatoVersamento(it.bologna.ausl.model.entities.versatore.Versamento.StatoVersamento.IN_CARICO);
        allegatoInformation.setDataVersamento(ZonedDateTime.now());
        allegatoInformation.setMetadatiVersati(identityFile.getJSON().toJSONString());
        return allegatoInformation;
    }

    /**
     * Metodo provvisorio per calcolare l'HashSHA256 di un file allegato
     * @param identityFile
     * @return
     * @throws MinIOWrapperException
     * @throws VersatorePluginException
     */
    private IdentityFileUnimatica calcolaSHA256(IdentityFileUnimatica identityFile) throws MinIOWrapperException, VersatorePluginException {
        MinIOWrapper minIOWrapper = versatoreRepositoryConfiguration.getVersatoreRepositoryManager().getMinIOWrapper();
        InputStream is = identityFile.getUuidMongo() != null
            ? minIOWrapper.getByUuid(identityFile.getUuidMongo())
            : minIOWrapper.getByFileId(identityFile.getFileBase64());
        try {
            identityFile.setHash(org.apache.commons.codec.digest.DigestUtils.sha256Hex(is));
        } catch (IOException ex) {
            log.error("Errore nel calcoalre l'hashSHA256", ex);
            throw new VersatorePluginException("Errore nel calcoalre l'hashSHA256");
        }
        return identityFile;
    }
}
