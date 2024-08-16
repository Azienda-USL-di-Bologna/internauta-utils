/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.riversamento.builder;

import it.bologna.ausl.riversamento.builder.oggetti.RichiestaAnnullamentoType;
import it.bologna.ausl.riversamento.builder.oggetti.RichiestaAnnullamentoVersamenti;
import it.bologna.ausl.riversamento.builder.oggetti.UnitaDocumentaria;
import it.bologna.ausl.riversamento.builder.oggetti.VersamentiDaAnnullareListType;
import it.bologna.ausl.riversamento.builder.oggetti.VersamentoDaAnnullareType;
import it.bologna.ausl.riversamento.builder.oggetti.VersatoreType;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

/**
 *
 * @author utente
 * Questa è una classe che serve ad annullare delle unita' documentarie già versate,
 * tutto ciò che c'è da fare è:
 *      preparare un csv con le colonne: REGISTRO,ANNO,NUMERO
 *      procurarsi le credenziali dell'ambiente
 *      cambiare il numero della stringa "codice" ad ogni nuova richiesta
 */
public class TestAnnullamento {
   

    public static void main(String[] args) throws FileNotFoundException {
        //Qui impostiamo i valori da inserire nell'xml
        
/*Questo è il path del csv che contiene le ud da annullare*/ 
        String pathCsv = "path.csv";
/*Queste sono le configurazioni per l'ambiente di prod*/        
        String ambiente = "PARER";
        String ente = "AOSP_FE";
        String struttura="AOOAOSP_FE";
        String userID ="GEDI_AOSP_FE";
        String user = "GEDI_AOSP_FE";
        String pw = "mkliSS567";
//        
        String url = "https://parer.regione.emilia-romagna.it/sacer/InvioRichiestaAnnullamentoVersamenti";
        
        
/*Queste sono le configurazioni per l'ambiente di test*/
//        String ambiente = "PARER_TEST";
//        String ente = "AUSL_BO";
//        String struttura = "ASL_BO";
//        String user = "gedi_ausl_bo_pre";
//        String pw = "gedi_ausl_bo_pre_pw0";
//        String userID= "gedi_ausl_bo_pre";
//        String url = "https://parer-pre.regione.emilia-romagna.it/sacer/InvioRichiestaAnnullamentoVersamenti";
//        
        String versioneXml= "1.1";
        String descrizione = "Richiesta di annullamento registro giornaliero";
        String codice = "4/2022";
        String forzaAnnullamento = "true";
        String immediata = "true";
        String motivazione = "Le unita' di cui e' stato richiesto l'annullamento presentano un errore di invio dei file contenuti";
        String preIngest = "true";
        String tipoVersamento = "UNITA' DOCUMENTARIA";
//
        Marshaller marshaller;
//        
//        
//        
//        
//        
//        
//        
//        
//        //qui ci si prepara per la lettura del csv per compilare la sezione dei versamenti da annullare
        ICsvMapReader mapReader = null;
        FileInputStream filedacaricare = new FileInputStream(pathCsv);
        InputStreamReader inputFileStreamReader = new InputStreamReader(filedacaricare);
//        
//        //inizio a costruire l'oggetto per la richiesta dell'annullamento
        RichiestaAnnullamentoType richiesta = new RichiestaAnnullamentoType();
        richiesta.setCodice(codice);
        richiesta.setDescrizione(descrizione);
        richiesta.setForzaAnnullamento(forzaAnnullamento);
        richiesta.setImmediata(immediata);
        richiesta.setMotivazione(motivazione);
        richiesta.setRichiestaDaPreIngest(preIngest);
        
        VersatoreType versatore = new VersatoreType();
        versatore.setAmbiente(ambiente);
        versatore.setEnte(ente);
        versatore.setStruttura(struttura);
        versatore.setUserID(userID);
//        
//        
//        
        List<VersamentoDaAnnullareType> versamentiDaAnnullareList = new ArrayList<>();
        try {
            CsvPreference DELIMITATOR = new CsvPreference.Builder('"', ',', "\r\n").build();
            mapReader = new CsvMapReader(inputFileStreamReader, DELIMITATOR);
            mapReader.getHeader(true);
            String[] headers = new String[]{"REGISTRO", "ANNO", "NUMERO"};
            final CellProcessor[] processors = new CellProcessor[]{
                // new NotNull(new StrRegEx(codiceEnteRegex, new ParseInt())), // codice_ente
                new Optional(), // registro
                new Optional(), // anno
                new Optional(), // numero
            };

            Map<String, Object> documentiMap;
            while ((documentiMap = mapReader.read(headers, processors)) != null) {
                //qui cicli le righe
                VersamentoDaAnnullareType versamento = new VersamentoDaAnnullareType();
                versamento.setTipoVersamento(tipoVersamento);
                String registro = documentiMap.get("REGISTRO").toString();
                String anno = documentiMap.get("ANNO").toString();
                String numero = documentiMap.get("NUMERO").toString();
                versamento.setAnno(anno);
                versamento.setNumero(numero);
                versamento.setTipoRegistro(registro);
                versamentiDaAnnullareList.add(versamento);
                
            }
            
            VersamentiDaAnnullareListType versamentiDaAnnullare =new VersamentiDaAnnullareListType();
            versamentiDaAnnullare.setVersamentoDaAnnullare(versamentiDaAnnullareList);
            RichiestaAnnullamentoVersamenti richiestaAnnullamento = new RichiestaAnnullamentoVersamenti();
            richiestaAnnullamento.setVersioneXmlRichiesta(versioneXml);
            richiestaAnnullamento.setRichiesta(richiesta);
            richiestaAnnullamento.setVersatore(versatore);
            richiestaAnnullamento.setVersamentiDaAnnullare(versamentiDaAnnullare);
//            System.out.println(richiestaAnnullamento.toString()); 
            
            JAXBContext jaxb = JAXBContext.newInstance(RichiestaAnnullamentoVersamenti.class);
            marshaller = jaxb.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8" );
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String out;
            try {
                marshaller.marshal(richiestaAnnullamento, baos);
            } catch (JAXBException e) {
                System.out.println("Document Error unable to serialize: " + e);
            }
            out = new String(baos.toByteArray());
            System.out.println(out);
             //inivio al parer
            HashMap<String, String> pacco =
                new LinkedHashMap<String, String>();
            pacco.put("VERSIONE", "1.1");
            pacco.put("LOGINNAME", user);
            pacco.put("PASSWORD", pw);
            pacco.put("XMLSIP",out);
            
            
            
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();        
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            
            for (Map.Entry<String, String> entry : pacco.entrySet()) {
                StringBody stringBody = new StringBody(entry.getValue()); 
                builder.addPart(entry.getKey(),stringBody);
                System.out.println(entry.getKey() + "/" + entry.getValue());
            }
            
            
            HttpEntity multiPartEntity = builder.build();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(multiPartEntity);
            HttpClient httpclient = new DefaultHttpClient();
            try {
                HttpResponse response = httpclient.execute(httpPost);
                HttpEntity res = response.getEntity();
                if (res != null) {
                    System.out.println(EntityUtils.toString(res));
                    Files.write(Paths.get("./primoLancio.txt"), EntityUtils.toString(res).getBytes());
                } else {
                    httpPost.abort();
                    System.out.println("errore abortisco la chiamata");
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        } catch (Exception e) {
           e.printStackTrace();
        }
//        
//        
//        
//       
    }
    
    
}





 