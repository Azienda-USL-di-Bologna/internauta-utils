/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders;

import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.plugins.parer.ParerVersatoreMetadatiBuilder;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.riversamento.builder.IdentityFile;
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

    //TODO vedere se l'autowired funziona
    private static VersatoreRepositoryConfiguration versatoreRepositoryConfiguration;
    public AllegatiBuilder(VersatoreRepositoryConfiguration versatoreRepositoryConfiguration) {
        this.versatoreRepositoryConfiguration = versatoreRepositoryConfiguration;
    }
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
     * Metodo che crea i dati per gli allegati (IdentityFile) e le loro
     * informazioni per il versatore (VersatoreAllegatoInformation)
     *
     * @return
     */
    public Map<String, Object> buildAllegati(Doc doc, DocDetail docDetail, List<Allegato> allegati, VersamentoBuilder versamentoBuilder) throws MinIOWrapperException {
        Map<String, Object> mappaPerAllegati = new HashMap<>();
        List<VersamentoAllegatoInformation> versamentiAllegatiInfo = new ArrayList<>();
        List<IdentityFile> identityFiles = new ArrayList<>();
        
        for (Allegato allegato : allegati) {
            //prendo l'allegato originale (se è firmato scelgo quello firmato)
            //TODO vedere se va bene SPRITZ e capire il numero allegati quale deve essere e capire quando c'è il segnaposto e vedere altri casi come P7m
            if (allegato.getFirmato()) {
                Allegato.DettaglioAllegato originaleFirmato = allegato.getDettagli().getOriginaleFirmato();
                IdentityFile identityFile = getAllegatoInformation(originaleFirmato);
                identityFiles.add(identityFile);
                VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFile, versamentoBuilder, allegato.getFirmato());
                versamentiAllegatiInfo.add(allegatoInformation);
            } else {
                Allegato.DettaglioAllegato originale = allegato.getDettagli().getOriginale();
                IdentityFile identityFile = getAllegatoInformation(originale);
                identityFiles.add(identityFile);
                VersamentoAllegatoInformation allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFile, versamentoBuilder, allegato.getFirmato());
                versamentiAllegatiInfo.add(allegatoInformation);
                //se c'è prendo anche il converito
                Allegato.DettaglioAllegato convertito = allegato.getDettagli().getConvertito();
                if (convertito != null) {
                    identityFile = getAllegatoInformation(convertito);
                    identityFiles.add(identityFile);
                    allegatoInformation = createVersamentoAllegato(allegato.getId(), identityFile, versamentoBuilder, allegato.getFirmato());
                    versamentiAllegatiInfo.add(allegatoInformation);
                }
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
    private IdentityFile getAllegatoInformation(Allegato.DettaglioAllegato dettaglio) throws MinIOWrapperException {
        //Controllo se nel nome del file è già inserita l'estensione
        int lastDotIndex = dettaglio.getNome().lastIndexOf(".");
        String estensione = "";
        if (lastDotIndex >= 0) {
            estensione = dettaglio.getNome().substring(lastDotIndex + 1);
        }
        //Nel caso dei documenti provenienti da argo l'estensione del file è già inserita nel nome del file; per quelli invece caricati da internauta
        //in "dettagli" nome del file ed estensione sono separati. Al momento solo i doc GEDI (tiplogia DOCUMENT_UTENTE) vengo caricati direttamente da
        //internauta, ecco il perché dell'aggiunta seguente nella stringa che formerà il nome del file.
        IdentityFile identityFile = new IdentityFile(dettaglio.getNome()
                + (!(dettaglio.getEstensione().equals(estensione)) ? "." + dettaglio.getEstensione() : ""),
                getUuidMinIObyFileId(dettaglio.getIdRepository()),
                dettaglio.getHashSha256() != null ? dettaglio.getHashSha256() : dettaglio.getHashMd5(), //TODO originale.getHashSha256() hash 256 non presente, solo hashMd5 :: bisogna calcolarlo - chiedere GDM - fuori versatore?
                null,
                dettaglio.getMimeType());
        //if (identityFile.getUuidMongo() == null) {
        identityFile.setFileBase64(dettaglio.getIdRepository());
        //}
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
     * @param firmato
     * @return
     */
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
        //aggiungo i metadati dell'allegato al file XML
        versamentoBuilder.addFileByParams(identityFile.getFileName(), identityFile.getMime(), identityFile.getHash());
        return allegatoInformation;
    }

}
