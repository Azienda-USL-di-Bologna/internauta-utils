/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.parer;

import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.plugins.infocert.InfocertVersatoreService;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.riversamento.builder.IdentityFile;
import it.bologna.ausl.riversamento.builder.UnitaDocumentariaBuilder;
import it.bologna.ausl.riversamento.sender.Pacco;
import it.bologna.ausl.riversamento.sender.PaccoFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.ArrayList;


import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Nodes;
import nu.xom.ParsingException;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author utente
 */
@Component
public class ParerVersatoreService extends VersatoreDocs {

    private static final Logger log = LoggerFactory.getLogger(ParerVersatoreService.class);
    
    @Autowired
    private ParametriAziendeReader parametriAziende;
    
    @Autowired
    private ParerVersatoreMetadatiBuilder parerVersatoreMetadatiBuilder;
    
   
    
//    
    @Override
    protected  VersamentoDocInformation versaImpl(VersamentoDocInformation versamentoInformation) throws VersatoreProcessingException {
        
        Map<String, Object> mappaResultAndAllegati = new HashMap<>();
        try {
            mappaResultAndAllegati = versaDocumentoParer(versamentoInformation);
        } catch (DatatypeConfigurationException ex) {
            java.util.logging.Logger.getLogger(ParerVersatoreService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JAXBException ex) {
            java.util.logging.Logger.getLogger(ParerVersatoreService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (java.text.ParseException ex) {
            java.util.logging.Logger.getLogger(ParerVersatoreService.class.getName()).log(Level.SEVERE, null, ex);
        }
        String response = (String) mappaResultAndAllegati.get("response");
        String xmlVersatoOutput = (String) mappaResultAndAllegati.get("xmlVersato");
        List<VersamentoAllegatoInformation> listaAllegati = (List<VersamentoAllegatoInformation>) mappaResultAndAllegati.get("versamentiAllegatiInformation");
        
        versamentoInformation.setMetadatiVersati(xmlVersatoOutput); 
        versamentoInformation.setDataVersamento(ZonedDateTime.now()); 
        Doc doc = entityManager.find(Doc.class, versamentoInformation.getIdDoc());
        if(response != null && !response.equals("")) {
            Builder parser = new Builder();
            try {
                Document resd = parser.build(response, null);
//                XPathContext context = new XPathContext("EsitoVersamento", "");
                Nodes EsitoVersamento = resd.query("EsitoVersamento");
                Nodes node = resd.query("/EsitoVersamento/EsitoGenerale/CodiceEsito");
                if(resd.query("/EsitoVersamento/XMLVersamento").size() != 0) {
                    Nodes nodeXmlVersato = resd.query("//XMLVersamento");
                    String xmlVersato = nodeXmlVersato.get(0).toXML();
                    versamentoInformation.setMetadatiVersati(xmlVersato);

                }
                if(resd.query("/EsitoVersamento/UnitaDocumentaria/text()").size() != 0) {
//                    Element root = resd.getRootElement();
                    String esitoUnitaDocumentaria = resd.query("/EsitoVersamento/UnitaDocumentaria").get(0).toXML();
                    Document esitoUnitaDocumentariaDocument = parser.build(esitoUnitaDocumentaria, null);
                    Element rootUnitaDoc = esitoUnitaDocumentariaDocument.getRootElement();
//                    Element esitoDocPrincipale  = rootUnitaDoc.getChildElements("DocumentoPrincipale").get(0);
                    Document unitaDocumentaria = parser.build(resd.query("/EsitoVersamento/UnitaDocumentaria").get(0).toXML(),null);
//                    String esitoDocPrincipale = esitoUnitaDocumentariaDocument.query("//DocumentoPrincipale/text()").get(0).;
                    if(unitaDocumentaria.query("/DocumentoPrincipale").size() != 0){
                        String esitoDocPrincipale = unitaDocumentaria.query("/DocumentoPrincipale").get(0).toXML();
                        Document esitoDocPrincipaleDoc = parser.build(esitoDocPrincipale, null);
                        String idDocPrincipale = unitaDocumentaria.query("/DocumentoPrincipale/IDDocumento").get(0).toXML();
                        if(esitoUnitaDocumentariaDocument.query("/Allegati").size() != 0) {
                            String allegatiContainer = esitoUnitaDocumentariaDocument.query("/Allegati").get(0).toXML();
                            Document AllegatiContainerDocument = parser.build(allegatiContainer, null);
                            Element root = AllegatiContainerDocument.getRootElement();
                            Elements allegatiEsiti = root.getChildElements("Allegati");
    //                        Elements allegatiEsiti = AllegatiContainerDocument.query("//Allegato/text()");
                            Map<String, Element> mappaEsitiAllegati = new HashMap<>();
                            mappaEsitiAllegati.put(idDocPrincipale, esitoDocPrincipaleDoc.getRootElement());
                            for (int i = 0 ; i < allegatiEsiti.size() ; i ++) {
                                String idAllegato = allegatiEsiti.get(i).getAttributeValue("IDDocumento");
                                mappaEsitiAllegati.put(idAllegato, allegatiEsiti.get(i));
                            }

                            listaAllegati = parseAllegatiResult(mappaEsitiAllegati, listaAllegati);
                            }
                        }
                    
                    
                }
                
                
                if (node.get(0).toXML().equals("<CodiceEsito>POSITIVO</CodiceEsito>") || node.get(0).toXML().equals("<CodiceEsito>WARNING</CodiceEsito>")) {
                    versamentoInformation.setRapporto(response);
                    versamentoInformation.setStatoVersamentoPrecedente(versamentoInformation.getStatoVersamento());
                    versamentoInformation.setStatoVersamento(Versamento.StatoVersamento.VERSATO);
                    doc.setStatoVersamento(Versamento.StatoVersamento.VERSATO);
                    
                    for(VersamentoAllegatoInformation allegato: listaAllegati) {
                        allegato.setStatoVersamento(Versamento.StatoVersamento.VERSATO);
                    }
                } else {
                    log.error("Creo il nodo errorText...");
                    Nodes errorText = resd.query("/EsitoVersamento/EsitoGenerale/MessaggioErrore/text()");
                    versamentoInformation.setRapporto(response);
                    log.error("Creo il nodo codErrore...");
                    Nodes codErrore = resd.query("/EsitoVersamento/EsitoGenerale/CodiceErrore/text()");
                    String errorCode = codErrore.get(0).toXML();
                    versamentoInformation.setCodiceErrore(errorCode);
                    String utf8ErrorMessage = new String(errorText.get(0).toXML().getBytes("UTF-8"), "UTF-8");
                    versamentoInformation.setDescrizioneErrore(utf8ErrorMessage);
                    versamentoInformation.setStatoVersamentoPrecedente(versamentoInformation.getStatoVersamento());
                    versamentoInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
                    if(errorCode.equals("UD-008-001")|| errorCode.startsWith("FIRMA") && !errorCode.equals("FIRMA-002-001")) {
                        versamentoInformation.setForzabile(Boolean.TRUE);
                    }
                    for(VersamentoAllegatoInformation allegato: listaAllegati) {
                        allegato.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
                    }
                    if (errorCode != null && utf8ErrorMessage != null) {
                        log.error("Codice Errore " + errorCode + ": " + utf8ErrorMessage);
                    }
                    log.error("errorMessage compilato");

                }
                versamentoInformation.setVersamentiAllegatiInformations(listaAllegati);

            } catch (ParsingException ex) {
                java.util.logging.Logger.getLogger(ParerVersatoreService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(ParerVersatoreService.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParseException ex) {
                java.util.logging.Logger.getLogger(ParerVersatoreService.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            versamentoInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE_RITENTABILE);
            versamentoInformation.setCodiceErrore("SERVIZIO");

        }
        
        return versamentoInformation;

    }
    
//    questa funzione crea il pacco di ogni file (inputstream) che viene inviato al parer
    private Pacco creazionePaccoFile(List<IdentityFile> identityFiles, Pacco pacco) throws MinIOWrapperException{
        
        for (IdentityFile identityFile : identityFiles) {
            PaccoFile paccoFile = new PaccoFile();
            try{
            InputStream is = minIOWrapper.getByUuid(identityFile.getUuidMongo());
            paccoFile.setInputStream(is);
            paccoFile.setMime(identityFile.getMime());
            paccoFile.setFileName(identityFile.getFileBase64());
            paccoFile.setId(identityFile.getId());
            paccoFile.setFileName(identityFile.getFileName());
            pacco.addFile(paccoFile);
            } catch(MinIOWrapperException ex) {
                log.error("Errore nel reperire il file da minio");

            }
        }
        return pacco;
    }
    
//    crea il pacco vero e proprio da inviare al parer a cui vengono aggiunti tutti i pacchetti coi file
    private Pacco creazionePacco(String version, String username, String password){
        Pacco pacco = new Pacco();
        pacco.setLoginName(username);
        pacco.setPassword(password);
        pacco.setVersione(version);
        
        return pacco;
    }
    
    private OkHttpClient buildNewClient(OkHttpClient client) {
        X509TrustManager x509TrustManager = new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                log.info("getAcceptedIssuers =============");
                X509Certificate[] empty = {};
                return empty;
            }

            @Override
            public void checkClientTrusted(
                    X509Certificate[] certs, String authType) {
                log.info("checkClientTrusted =============");
            }

            @Override
            public void checkServerTrusted(
                    X509Certificate[] certs, String authType) {
                log.info("checkServerTrusted =============");
            }
        };
        SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        String tlsString = "TLSv1.2";               //  "TLS"
        try {
            SSLContext sslContext = SSLContext.getInstance(tlsString);
            sslContext.init(null, new TrustManager[]{getX509TrustManager()}, new SecureRandom());
            socketFactory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException ex) {
            log.error(ex.getMessage());
        } catch (KeyManagementException ex) {
            log.error(ex.getMessage());
        }
        client = client.newBuilder()
                .sslSocketFactory(socketFactory, x509TrustManager)
                .connectTimeout(600, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .build();
        log.info("Client builded with SSLContext " + tlsString);
        return client;
    }
   
    private X509TrustManager getX509TrustManager() {
        return new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                log.info("getAcceptedIssuers =============");
                return null;
            }

            @Override
            public void checkClientTrusted(
                    X509Certificate[] certs, String authType) {
                log.info("checkClientTrusted =============");
            }

            @Override
            public void checkServerTrusted(
                    X509Certificate[] certs, String authType) {
                log.info("checkServerTrusted =============");
            }
        };
    }
    
//    Questa funzione si occupa di effettuare la chiamata vera e propria al parer, 
//    prende in input il versamentodoc coi suoi allegati e ritorna l'xml di risposta del parer
//    Qui vengono generati i 
    private Map<String, Object> versaDocumentoParer(VersamentoDocInformation versamentoInformation) throws DatatypeConfigurationException, JAXBException, java.text.ParseException {
        log.info("Inizio con il versamento del doc: " + versamentoInformation.getIdDoc().toString());
        Integer idDoc = versamentoInformation.getIdDoc();
        String forzaCollegamento, forzaAccettazione, forzaConservazione;
        Map<String, Object> risultatoEVersamentiAllegati = new HashMap<>();
                
        forzaAccettazione = "false";
        forzaCollegamento = "false";
        forzaConservazione = "true";
        Doc doc = entityManager.find(Doc.class, idDoc);
        if(doc.getStatoVersamento() == Versamento.StatoVersamento.FORZARE) {
            forzaAccettazione = "true";
        }
        DocDetail docDetail = entityManager.find(DocDetail.class, idDoc);
        String enteVersamento = (String) versamentoInformation.getParams().get("ente");
        String userID = (String) versamentoInformation.getParams().get("userID");
        String version = (String) versamentoInformation.getParams().get("versione");
        String username = (String) versamentoInformation.getParams().get("username");
        String password = (String) versamentoInformation.getParams().get("password");
        String urlVersSync = (String) versamentoInformation.getParams().get("urlVersSync");
        
        String ambiente = (String) versamentoInformation.getParams().get("ambiente");
        String struttura = (String) versamentoInformation.getParams().get("struttura");
        String tipoConservazione = (String) versamentoInformation.getParams().get("tipoconservazione");
        String codifica = (String) versamentoInformation.getParams().get("codifica");
        Boolean includiNote = (Boolean) versamentoInformation.getParams().get("includinote");
        String versioneDatiSpecificiPico = (String) versamentoInformation.getParams().get("versionedatispecificipico");
        String versioneDatiSpecificiDete = (String) versamentoInformation.getParams().get("versionedatispecificidete");
        String versioneDatiSpecificiDeli = (String) versamentoInformation.getParams().get("versionedatispecificideli");
     
        String tipoComponenteDefault = (String) versamentoInformation.getParams().get("tipocomponentedefault");
        Map<String, Object> unitaDocConIdentityFiles = parerVersatoreMetadatiBuilder.ParerVersatoreMetadatiBuilder(doc, docDetail, enteVersamento, userID,version, ambiente,struttura, tipoConservazione, codifica, versioneDatiSpecificiPico,versioneDatiSpecificiDete,versioneDatiSpecificiDeli, includiNote, tipoComponenteDefault, forzaCollegamento, forzaAccettazione, forzaConservazione);
        List<JSONObject> identityFiles = (List<JSONObject>) unitaDocConIdentityFiles.get("identityFiles");
        List<IdentityFile> identityFiless = new ArrayList<>();
        for(JSONObject identityFile: identityFiles) {
            IdentityFile identityFilee = IdentityFile.parse(identityFile);
            identityFiless.add(identityFilee);
        }
        UnitaDocumentariaBuilder unitaDoc = (UnitaDocumentariaBuilder) unitaDocConIdentityFiles.get("unitaDocumentaria");
        risultatoEVersamentiAllegati.put("xmlVersato", unitaDoc.toString());
        List<VersamentoAllegatoInformation> versamentiAllegatiInformation = (List<VersamentoAllegatoInformation>) unitaDocConIdentityFiles.get("versamentiAllegatiInformation");
        risultatoEVersamentiAllegati.put("versamentiAllegatiInformation", versamentiAllegatiInformation);
        Pacco pacco = creazionePacco(version, username, password);
        try {
            Pacco paccoConPacchiFiles = creazionePaccoFile(identityFiless, pacco);
            paccoConPacchiFiles.setXmlsip(unitaDoc.toString());
            List<NameValuePair> formparams = paccoConPacchiFiles.getFormValues();
            
            String response = null;
            OkHttpClient client = new OkHttpClient();
            client = buildNewClient(client);
            
             MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
            log.info("Cliclo le chiavi del form...");
            for (NameValuePair nvp : formparams) {
                if (nvp.getValue() != null) {
                    log.info(nvp.getName() + " : " + nvp.getValue());
                    builder.addFormDataPart(nvp.getName(), nvp.getValue());
                } else {
                    log.info(nvp.getName() + " non valorizzato!");
                }
            }

            log.info("Cliclo i file...");
            if (paccoConPacchiFiles.getFiles() != null) {
                for (PaccoFile a : paccoConPacchiFiles.getFiles()) {
                    log.info("Inserisco nel body il file " + a.getId() + ", " + a.getFileName());
                    byte[] bytes;
                    try (InputStream is = a.getInputStream()) {
                        bytes = IOUtils.toByteArray(is);
                        builder.addFormDataPart(a.getId(), a.getFileName(),RequestBody.create( okhttp3.MediaType.parse(a.getMime()), bytes));
                    } catch(Exception ex) {
                        log.error("Problemi con l'inputstream del file", ex);
                    }
                    
                }
            }
            log.info("Buildo il MultiPart...");
            MultipartBody multipartBody = builder.build();

            log.info("Buildo la request...");
            Request request = new Request.Builder()
                    .url(urlVersSync) // pass the url endpoint of api
                    .post(multipartBody) // pass the mulipart object we just created having data
                    .build();
            log.info("Uri " + urlVersSync);
            log.info("Effettuo chiamata.... ");
            try (Response resp = client.newCall(request).execute()) {
                if (resp.isSuccessful()) {
                    log.info("Message" + resp.message());
                    String resBodyString = resp.body().string();
                    log.info(resBodyString);
                    response = resBodyString;
                    risultatoEVersamentiAllegati.put("response", response);
                } else {
                    String message = resp.message();
                    log.error("ERROR: message = " + resp.message());
                    String resBodyString = resp.body().string();
                    log.error(resBodyString);
                    log.error(resp.toString());
                }
                resp.close(); // Close respons
            } catch (Throwable ex) {
                log.error("Errore chiamata riversamento", ex);
                ex.printStackTrace();
                risultatoEVersamentiAllegati.put("response", null);
            }
            return risultatoEVersamentiAllegati;
        } catch (MinIOWrapperException ex) {
            java.util.logging.Logger.getLogger(ParerVersatoreService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            java.util.logging.Logger.getLogger(ParerVersatoreService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ParerVersatoreService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return risultatoEVersamentiAllegati;
    }
    
/**parseAllegatiResult prende gli elementi xml degli allegati e attraverso il campo ID matcha
 * le righe della tabella dei versamenti allegati e va a scriverci l'esito generale dell'allegato
 * e nel caso abbia la verifica delle firme, pure quella, in modo userfriendly e non 
 */
    private List<VersamentoAllegatoInformation> parseAllegatiResult(Map<String, Element> mappaEsitiAllegati, List<VersamentoAllegatoInformation> listaAllegati ) throws ParseException {
        for(VersamentoAllegatoInformation allegato: listaAllegati) {
            IdentityFile idFile;
            JSONParser jsonParser = new JSONParser();
            JSONObject json = (JSONObject) jsonParser.parse(allegato.getMetadatiVersati());
            idFile = IdentityFile.parse(json);
            Element esitoAllegato = (Element) mappaEsitiAllegati.get(idFile.getId());
            try{
                String codiceEsitoGenerale = esitoAllegato.getFirstChildElement("EsitoDocumento").getAttributeValue("CodiceEsito");
                String verificaFirmeComponente = esitoAllegato.getFirstChildElement("EsitoDocumento").getAttributeValue("VerificaFirmeComponente");
                String riassuntoAllegato = "Esito Generale: " + codiceEsitoGenerale;
                if(verificaFirmeComponente != null && !verificaFirmeComponente.equals(""))
                    riassuntoAllegato = riassuntoAllegato + " , Esito Firme " + verificaFirmeComponente;

                allegato.setRapporto(riassuntoAllegato);
                if(codiceEsitoGenerale.equals("POSITIVO")){
                    allegato.setStatoVersamento(Versamento.StatoVersamento.VERSATO);
                } else {
                    allegato.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
                }

            } catch(Exception ex) {
                log.info("l'allegato non ha dettagli esito");
            }
        }
        return listaAllegati;
    }
    
   
    
}
