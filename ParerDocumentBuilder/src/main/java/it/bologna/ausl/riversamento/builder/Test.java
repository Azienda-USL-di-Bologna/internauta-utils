/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.riversamento.builder;

import it.bologna.ausl.riversamento.builder.oggetti.DatiSpecifici;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.json.simple.JSONArray;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import sun.nio.cs.StandardCharsets;

/**
 *
 * @author utente
 */
public class Test {
    
    
    private static  Element insert(String nameTag, String contentTag) throws ParserConfigurationException{
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element consumerInfo = doc.createElement(nameTag);
        consumerInfo.setTextContent(contentTag);
        doc.appendChild(consumerInfo);
        return doc.getDocumentElement();
       
    } 
   
    
    
    public static void main(String[] args) throws ParserConfigurationException, UnsupportedEncodingException, JAXBException {
 
//        DATI SPECIFICI
//        DatiSpecificiBuilder dsb = new DatiSpecificiBuilder("1.1") {};
//        dsb.insertNewTag("Mittente", "mitt");
//        dsb.insertNewTag("Movimento", "IN");
//        dsb.insertNewTag("ModalitaTrasmissione", "indirizzo generico");
//        dsb.insertNewTag("OperatoreDiProtocollo", "operatore");
//        
//        
// 
//	  try {
// 
//		File file = new File("C:\\file.xml");
//		JAXBContext jaxbContext = JAXBContext.newInstance(DatiSpecifici.class);
//		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
// 
//		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
// 
//                java.io.StringWriter sw = new StringWriter();
//
//                jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
//                jaxbMarshaller.marshal(dsb.getDatiSpecifici(), sw);
//                
//                String res = sw.toString();
//                
//                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
//                
//                InputStream stream = new ByteArrayInputStream(res.getBytes("UTF-8"));
//                
//		DatiSpecifici ds = (DatiSpecifici) jaxbUnmarshaller.unmarshal(stream);
//                
//                for(Object object: ds.getAny()){
//                    Element e = (Element)object;
//                    System.out.println(e.getNodeName() + ":" + e.getTextContent());
//                }
//                
//                
//                //System.out.println(sw.toString());
//
////     return sw.toString();
//                
//                //jaxbMarshaller.marshal(dsb.getDatiSpecifici(), file);
//		
// 
//	      } catch (JAXBException e) {
//		e.printStackTrace();
//	      }
 
//    // PROFILO ARCHIVISTICO
//        ProfiloArchivistico pa = new ProfiloArchivistico();
//        pa.addFascicoloPrincipale("classifica", "2015", "12345", "oggetto", "num_sott", "oggettoSottoFasc", "11", "oggettoInserto");
//        pa.addFascicoloSecondario("classificaSecondario", "2015", "123456789", "oggettoSec", "num_sottSec", "oggettoSottoFascSec", "1122", "oggettoInsertoSec");
//        
//        
//        
// 
//	  try {
// 
//		File file = new File("C:\\filePA.xml");
//		JAXBContext jaxbContext = JAXBContext.newInstance(ProfiloArchivistico.class);
//		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
// 
//		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
// 
//                jaxbMarshaller.marshal(pa, file);
//		jaxbMarshaller.marshal(pa, System.out);
// 
//	      } catch (JAXBException e) {
//		e.printStackTrace();
//	      }    
        
        DatiSpecificiBuilder dsb = new DatiSpecificiBuilder("1.1") {};
        dsb.insertNewTag("Mittente", "mitt");
        dsb.insertNewTag("Movimento", "IN");
        dsb.insertNewTag("ModalitaTrasmissione", "indirizzo generico");
        dsb.insertNewTag("OperatoreDiProtocollo", "operatore");
        
        DatiSpecifici ds = dsb.getDatiSpecifici();
        
        AggiuntaAllegatiBuilder agg = new AggiuntaAllegatiBuilder("123", 2015, "PG", "tipozzo", "true", "true", "1.0", "ambient", "ente", "GEDI", "userID", "proviamo", "abc123", "strutz", "Contenuto", null, 1, "supp", ds, "sostitutiva", "ISO-8859-1");
//        
//        JSONArray array1 = new JSONArray();
//        array1.add("uno");
//        array1.add("due");
//        
//        JSONArray array2 = new JSONArray();
//        array2.add("uno");
//        array2.add("tre");
//        
//        System.out.println(Arrays.toString(subtractList(array1, array2).toArray()));
//        String[] atp = {"Rafael Nadal", "Novak Djokovic", "Stanislas Wawrinka", "David Ferrer", "Roger Federer", "Andy Murray", "Tomas Berdych", "Juan Martin Del Potro"};
//List<String> players =  Arrays.asList(atp);
//       
//// Old looping
//for (String player : players) {
//     System.out.print(player + "; ");
//}
//       
//    // Using lambda expression and functional operations
//    players.forEach((player) -> System.out.print(player + "; "));
// 
//    // Using double colon operator in Java 8
//    players.forEach(System.out::println);
//    
//    array1.forEach((valore) -> );
        
    }
    
        /**
     * torna la differenza tra due Liste
     *
     * @param list1
     * @param list2
     * @return
     */
    public static <T> List<T> subtractList(List<T> list1, List<T> list2) {
        List<T> result = new ArrayList<T>();
        Set<T> set2 = new HashSet<T>(list2);
        for (T t1 : list1) {
            if (!set2.contains(t1)) {
                result.add(t1);
            }
        }
        return result;
    }
}
