/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders;

import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreSdicoException;
import it.bologna.ausl.internauta.utils.versatore.plugins.parer.ParerVersatoreMetadatiBuilder;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.riversamento.builder.IdentityFile;
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
public class AllegatiBuilder {

    private static VersatoreRepositoryConfiguration versatoreRepositoryConfiguration;

    public AllegatiBuilder(VersatoreRepositoryConfiguration versatoreRepositoryConfiguration) {
        this.versatoreRepositoryConfiguration = versatoreRepositoryConfiguration;
    }
//    @Autowired
//    VersatoreRepositoryConfiguration versatoreRepositoryConfiguration;
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ParerVersatoreMetadatiBuilder.class);

    public Map<String, Object> buildMappaAllegati(Doc doc, DocDetail docDetail, List<Allegato> allegati, VersamentoBuilder versamentoBuilder) throws VersatoreSdicoException {
        Map<String, Object> mappaAllegati = new HashMap<>();
        try {
            mappaAllegati = buildAllegati(doc, docDetail, allegati, versamentoBuilder);
        } catch (MinIOWrapperException ex) {
            log.error("Errore di comunicazione nel recuperare i dati degli allegati");
            throw new VersatoreSdicoException("Errore di comunicazione nel recuperare i dati degli allegati");
        }

        return mappaAllegati;
    }

    /**
     * Metodo che crea i dati per gli allegati (IdentityFile) e le loro
     * informazioni per il versatore (VersatoreAllegatoInformation)
     *
     * @return
     */
    public Map<String, Object> buildAllegati(Doc doc, DocDetail docDetail, List<Allegato> allegati, VersamentoBuilder versamentoBuilder) throws MinIOWrapperException, VersatoreSdicoException {
        Map<String, Object> mappaPerAllegati = new HashMap<>();
        List<VersamentoAllegatoInformation> versamentiAllegatiInfo = new ArrayList<>();
        List<IdentityFile> identityFiles = new ArrayList<>();

        for (Allegato allegato : allegati) {
            log.warn("Raccologo i dati dell'allegato ID " + allegato.getId());
            //prendo l'allegato originale (se è firmato scelgo quello firmato)
            if (allegato.getFirmato()) {
                Allegato.DettaglioAllegato originaleFirmato = allegato.getDettagli().getOriginaleFirmato();
                IdentityFile identityFile = getAllegatoInformation(originaleFirmato);
                identityFiles.add(identityFile);
                Allegato.DettagliAllegato.TipoDettaglioAllegato tipoAllegato = Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE_FIRMATO;
                VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFile, versamentoBuilder, tipoAllegato);
                versamentiAllegatiInfo.add(allegatoInformation);

            } else {
                Allegato.DettaglioAllegato originale = allegato.getDettagli().getOriginale();
                IdentityFile identityFile = getAllegatoInformation(originale);
                identityFiles.add(identityFile);
                Allegato.DettagliAllegato.TipoDettaglioAllegato tipoAllegato = Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE;
                VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFile, versamentoBuilder, tipoAllegato);
                versamentiAllegatiInfo.add(allegatoInformation);
            }
            //se ci sono prendo gli altri tipi di allegato
            Allegato.DettaglioAllegato convertito = allegato.getDettagli().getConvertito();
            if (convertito != null) {
                IdentityFile identityFile = getAllegatoInformation(convertito);
                identityFiles.add(identityFile);
                Allegato.DettagliAllegato.TipoDettaglioAllegato tipoAllegato = Allegato.DettagliAllegato.TipoDettaglioAllegato.CONVERTITO;
                VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFile, versamentoBuilder, tipoAllegato);
                versamentiAllegatiInfo.add(allegatoInformation);
            }
            Allegato.DettaglioAllegato convertitoFirmato = allegato.getDettagli().getConvertitoFirmato();
            if (convertitoFirmato != null) {
                IdentityFile identityFile = getAllegatoInformation(convertitoFirmato);
                identityFiles.add(identityFile);
                Allegato.DettagliAllegato.TipoDettaglioAllegato tipoAllegato = Allegato.DettagliAllegato.TipoDettaglioAllegato.CONVERTITO_FIRMATO;
                VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFile, versamentoBuilder, tipoAllegato);
                versamentiAllegatiInfo.add(allegatoInformation);
            }
            Allegato.DettaglioAllegato convertitoFirmatoP7M = allegato.getDettagli().getConvertitoFirmatoP7m();
            if (convertitoFirmatoP7M != null) {
                IdentityFile identityFile = getAllegatoInformation(convertitoFirmatoP7M);
                identityFiles.add(identityFile);
                Allegato.DettagliAllegato.TipoDettaglioAllegato tipoAllegato = Allegato.DettagliAllegato.TipoDettaglioAllegato.CONVERTITO_FIRMATO_P7M;
                VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFile, versamentoBuilder, tipoAllegato);
                versamentiAllegatiInfo.add(allegatoInformation);
            }
            Allegato.DettaglioAllegato originaleFirmatoP7M = allegato.getDettagli().getOriginaleFirmatoP7m();
            if (originaleFirmatoP7M != null) {
                IdentityFile identityFile = getAllegatoInformation(originaleFirmatoP7M);
                identityFiles.add(identityFile);
                Allegato.DettagliAllegato.TipoDettaglioAllegato tipoAllegato = Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE_FIRMATO_P7M;
                VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFile, versamentoBuilder, tipoAllegato);
                versamentiAllegatiInfo.add(allegatoInformation);
            }
        }
        mappaPerAllegati.put("versamentiAllegatiInfo", versamentiAllegatiInfo);
        mappaPerAllegati.put("identityFiles", identityFiles);
        return mappaPerAllegati;
    }

    /**
     * Mettodo che prende e inserisce in un IdentityFile i dati di un allegato
     *
     * @param dettaglio
     * @return
     * @throws MinIOWrapperException
     */
    private IdentityFile getAllegatoInformation(Allegato.DettaglioAllegato dettaglio) throws MinIOWrapperException, VersatoreSdicoException {
        //Controllo se nel nome del file è già inserita l'estensione
        int lastDotIndex = dettaglio.getNome().lastIndexOf(".");
        String estensione = "";
        if (lastDotIndex >= 0) {
            estensione = dettaglio.getNome().substring(lastDotIndex + 1);
        }
        //Nel caso dei documenti provenienti da argo l'estensione del file è già inserita nel nome del file; per quelli invece caricati da internauta
        //in "dettagli" nome del file ed estensione sono separati. Al momento solo i doc GEDI (tiplogia DOCUMENT_UTENTE) vengono caricati direttamente da
        //internauta, ecco il perché dell'aggiunta seguente nella stringa che formerà il nome del file.
        IdentityFile identityFile = new IdentityFile(dettaglio.getNome()
                + (!(dettaglio.getEstensione().equals(estensione)) ? "." + dettaglio.getEstensione() : ""),
                getUuidMinIObyFileId(dettaglio.getIdRepository()),
                dettaglio.getHashSha256(),
                null,
                dettaglio.getMimeType());
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
            IdentityFile identityFile, 
            VersamentoBuilder versamentoBuilder, 
            Allegato.DettagliAllegato.TipoDettaglioAllegato tipoAllegato) {
        VersamentoAllegatoInformation allegatoInformation = new VersamentoAllegatoInformation();
        allegatoInformation.setIdAllegato(idAllegato);
        allegatoInformation.setTipoDettaglioAllegato(tipoAllegato);
        allegatoInformation.setStatoVersamento(it.bologna.ausl.model.entities.versatore.Versamento.StatoVersamento.IN_CARICO);
        allegatoInformation.setDataVersamento(ZonedDateTime.now());
        allegatoInformation.setMetadatiVersati(identityFile.getJSON().toJSONString());
        //aggiungo i metadati dell'allegato al file XML
        versamentoBuilder.addFileByParams(identityFile.getFileName(), identityFile.getMime(), identityFile.getHash());
        return allegatoInformation;
    }

    /**
     * Metodo provvisorio per calcolare l'HashSHA256 di un file allegato
     * @param identityFile
     * @return
     * @throws MinIOWrapperException
     * @throws VersatoreSdicoException 
     */
    private IdentityFile calcolaSHA256(IdentityFile identityFile) throws MinIOWrapperException, VersatoreSdicoException {
        MinIOWrapper minIOWrapper = versatoreRepositoryConfiguration.getVersatoreRepositoryManager().getMinIOWrapper();
        InputStream is = identityFile.getUuidMongo() != null
                ? minIOWrapper.getByUuid(identityFile.getUuidMongo())
                : minIOWrapper.getByFileId(identityFile.getFileBase64());
        try {
            identityFile.setHash(org.apache.commons.codec.digest.DigestUtils.sha256Hex(is));
        } catch (IOException ex) {
            throw new VersatoreSdicoException("Errore nel calcoalre l'hashSHA256");
        }
        return identityFile;
    }

}
