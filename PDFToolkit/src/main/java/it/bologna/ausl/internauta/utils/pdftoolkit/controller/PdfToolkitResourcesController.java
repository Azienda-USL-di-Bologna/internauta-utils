package it.bologna.ausl.internauta.utils.pdftoolkit.controller;

import it.bologna.ausl.estrattore.exception.ExtractorException;
import it.bologna.ausl.internauta.utils.authorizationutils.session.AuthenticatedSessionData;
import it.bologna.ausl.internauta.utils.authorizationutils.session.AuthenticatedSessionDataBuilder;
import it.bologna.ausl.internauta.utils.authorizationutils.utils.UserInfoUtilities;
import it.bologna.ausl.internauta.utils.pdftoolkit.exceptions.PdfToolkitHttpException;
import it.bologna.ausl.internauta.utils.pdftoolkit.utils.FileUtils;
import it.bologna.ausl.internauta.utils.pdftoolkit.utils.PdfToolkitConfigParams;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.baborg.Utente;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Giuseppe Russo <g.russo@dilaxia.com>
 */
@RestController
@RequestMapping(value = "${pdftoolkit.mapping.url.root}")
public class PdfToolkitResourcesController {
    private static final Logger log = LoggerFactory.getLogger(PdfToolkitResourcesController.class);
    
    @Autowired
    private AuthenticatedSessionDataBuilder authenticatedSessionDataBuilder;
    
    @Autowired
    private PdfToolkitConfigParams pdfToolkitConfigParams;
    
    /**
     * Restituisce una lista di file in un percorso specificato su MinIO.
     *
     * @param codiceAzienda Il codice aziendale associato ai file.
     * @param path Il percorso dei file da ottenere.
     * @return Una lista di oggetti MinIOWrapperFileInfo che rappresentano i file nel percorso specificato.
     * <br>
     * Questo metodo riceve come parametri il codice aziendale e il percorso dei file da ottenere su MinIO.
     * Utilizza l'oggetto MinIOWrapper per ottenere la lista dei file nel percorso specificato.
     * Restituisce la lista di oggetti MinIOWrapperFileInfo che rappresentano i file nel percorso specificato.
     *
     * @throws MinIOWrapperException Se si verifica un errore durante l'interazione con MinIO.
     */
    @RequestMapping(value = "getFilesInPath", method = RequestMethod.GET)
    public List<MinIOWrapperFileInfo> getFilesInPath(@RequestParam("codiceAzienda") String codiceAzienda, @RequestParam("path") String path) throws MinIOWrapperException, PdfToolkitHttpException {
        AuthenticatedSessionData authenticatedUserProperties = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        if (UserInfoUtilities.isSD(authenticatedUserProperties.getUser())) {
            MinIOWrapper minIOWrapper = pdfToolkitConfigParams.getMinIOWrapper();
            List<MinIOWrapperFileInfo> filesInPath = minIOWrapper.getFilesInPath(path, true, true, codiceAzienda);
            return filesInPath;
        } else {
            throw new PdfToolkitHttpException("solo le persone con ruolo SD possono vedere i files");
        }
    }
    
    /**
     * Elimina i file in un percorso specificato su MinIO.
     *
     * @param relativePath Il percorso relativo dei resources del reporter da eliminare.
     * @return Un messaggio di stringa che indica lo stato dell'operazione di eliminazione.
     *
     * Questo metodo riceve come parametro il percorso relativo dei file da eliminare su MinIO del reporter.
     * Utilizza l'oggetto MinIOWrapper per ottenere la lista dei file nel percorso specificato.
     * Successivamente, itera su ogni file nella lista e lo rimuove utilizzando il metodo removeByFileId di MinIOWrapper.
     * Dopo l'eliminazione di ogni file, viene registrato un messaggio di log.
     * Infine, viene restituito un messaggio di completamento dell'operazione.
     *
     * @throws MinIOWrapperException Se si verifica un errore durante l'interazione con MinIO.
     */
    @RequestMapping(value = "deleteResources", method = RequestMethod.POST)
    public String deleteBucketPdfToolkit(@RequestParam(required = false) String relativePath) throws MinIOWrapperException {
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getRealUser() != null ? authenticatedSessionData.getRealUser() : authenticatedSessionData.getUser();
        if (UserInfoUtilities.isSD(user)) {
            MinIOWrapper minIOWrapper = pdfToolkitConfigParams.getMinIOWrapper();
            List<MinIOWrapperFileInfo> filesInPath = minIOWrapper.getFilesInPath("/resources/reporter/" + relativePath, true, true, "pdftoolkit");
            for (MinIOWrapperFileInfo minIOFileInfo : filesInPath) {
                minIOWrapper.removeByFileId(minIOFileInfo.getFileId(), false);
                log.info("File: {} removed.", minIOFileInfo.getFileId());
            }
        }
        return "Operazione completata.";
    }   
    
    /**
     * Gestisce la richiesta HTTP POST per il caricamento dei file.
     *
     * @param bucket Il nome del bucket dove il file verrà caricato.
     * @param codiceAzienda Il codice aziendale associato al file.
     * @param file Il file da caricare.
     * @return Un messaggio di stringa che indica lo stato dell'operazione di caricamento.
     * <br>
     * Questo metodo riceve un file, un nome di bucket e un codice aziendale come parametri. 
     * Crea un file temporaneo e una cartella temporanea, poi copia il file di input nel file temporaneo.
     * I contenuti del file temporaneo vengono quindi estratti nella cartella temporanea.
     * I contenuti della cartella temporanea vengono quindi caricati nel bucket specificato nello storage MinIO.
     * Se si verifica un errore durante queste operazioni, viene restituito un messaggio di errore appropriato.
     * Indipendentemente dall'esito, il file temporaneo e la cartella vengono eliminati prima che il metodo ritorni.
     */
    @RequestMapping(value = "uploadFiles", method = RequestMethod.POST)
    public String uploadPdfToolkitResources(
            @RequestParam("bucket") String bucket, 
            @RequestParam("codiceAzienda") String codiceAzienda, 
            @RequestParam("file") MultipartFile file) {
        
        String responseMessage = "Operazione completata.";
        AuthenticatedSessionData authenticatedSessionData = authenticatedSessionDataBuilder.getAuthenticatedUserProperties();
        Utente user = authenticatedSessionData.getRealUser() != null ? authenticatedSessionData.getRealUser() : authenticatedSessionData.getUser();
        log.info("Api caricamento resources. Check ruolo utente...");
        if (UserInfoUtilities.isSD(user)) {
            MinIOWrapper minIOWrapper = pdfToolkitConfigParams.getMinIOWrapper();
            File folderToSave = null;
            File fileTempToUpload = null;
            
            String fileName = file.getOriginalFilename();
            String extension = ".tmp";
            if (fileName != null) {
                int pos = fileName.lastIndexOf(".");
                if (pos > 0 && pos < (fileName.length() - 1)) {
                    extension = fileName.substring(pos);
                    fileName = fileName.substring(0, pos);
                }
            }    
            try (InputStream stream = file.getInputStream()) {

                folderToSave = FileUtils.getCartellaTemporanea(fileName);
                fileTempToUpload = File.createTempFile(fileName, extension);
                folderToSave.deleteOnExit();
                fileTempToUpload.deleteOnExit();

                org.apache.commons.io.FileUtils.copyInputStreamToFile(stream, fileTempToUpload);
                log.info("File temporanei creati. Estrazione zip...");
                FileUtils.estraiTuttoDalFile(folderToSave, fileTempToUpload, fileTempToUpload.getName());

                log.info("Caricamento resource su minIO");
                uploadToMinIO(folderToSave, folderToSave, minIOWrapper, codiceAzienda, bucket);
                
                // Questa parte non serve più in quanto il job del reporter gira sul service di internauta e non sugli AS
                // In quanto saranno lì poi i templates in locale. Si potrà sistemare nel caso questo metodo diventa un Job.
//                log.info("Upload dei resources completato. Sposto i file nella cartella di lavoro...");
//                File[] listFiles = folderToSave.listFiles();
//                for (File listFile : listFiles) {
//                    FileSystemUtils.copyRecursively(listFile.toPath(), Paths.get(System.getProperty("java.io.tmpdir") + listFile.getName()));
//                }
            } catch (ExtractorException ex) {
                responseMessage = "Errore durante l'estrazione del file zip.";
                log.error(responseMessage);
            } catch (MinIOWrapperException ex) {
                responseMessage = "Errore durante l'upload su minIo.";
                log.error(responseMessage, ex);
            } catch (IOException ex) {
                responseMessage = "Errore durante la creazione dei file temporanei.";
                log.error(responseMessage, ex);
            } finally {
                if (folderToSave != null && folderToSave.exists())
                    FileSystemUtils.deleteRecursively(folderToSave);
                if (fileTempToUpload != null && fileTempToUpload.exists())
                    FileSystemUtils.deleteRecursively(fileTempToUpload);
            }   
        } else {
            log.warn("Utente non abilitato all'operazione.");
        }
        log.info("Operazione di upload completata.");       
        return responseMessage;
    }

    /**
     * Carica un file o una directory di file su MinIO.
     *
     * @param fileToUpload Il file o la directory da caricare.
     * @param folderToSave La cartella dove salvare i file.
     * @param minIOWrapper L'oggetto MinIOWrapper per interagire con MinIO.
     * @param codiceAzienda Il codice aziendale associato al file.
     * @param bucket Il nome del bucket dove il file verrà caricato.
     * <br>
     * Questo metodo verifica se il file da caricare è una directory. 
     * Se è una directory, itera su tutti i file nella directory e li carica ricorsivamente.
     * Se non è una directory, carica il file su MinIO utilizzando il metodo putWithBucket di MinIOWrapper.
     * Il percorso del file viene calcolato come il percorso relativo dal folderToSave al fileToUpload.
     * Dopo il caricamento di ogni file viene registrato un messaggio di log.
     *
     * @throws MinIOWrapperException Se si verifica un errore durante il caricamento su MinIO.
     * @throws IOException Se si verifica un errore durante la lettura del file o della directory.
     */
    private void uploadToMinIO(File fileToUpload, File folderToSave, MinIOWrapper minIOWrapper, String codiceAzienda, String bucket) throws MinIOWrapperException, IOException {
        if (fileToUpload.isDirectory()) {
            File[] listFiles = fileToUpload.listFiles();
            for (File fileList : listFiles) {
                uploadToMinIO(fileList, folderToSave, minIOWrapper, codiceAzienda, bucket);
            }
        } else {
            String path = "/" + folderToSave.toURI().relativize(fileToUpload.toURI()).getPath();
            minIOWrapper.putWithBucket(fileToUpload, codiceAzienda, path, fileToUpload.getName(), null, true, bucket);
            log.info("File uploaded: {} ok", path);
        }
    }
}
