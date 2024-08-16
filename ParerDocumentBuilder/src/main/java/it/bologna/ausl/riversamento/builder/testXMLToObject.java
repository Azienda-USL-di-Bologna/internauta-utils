/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.riversamento.builder;

import it.bologna.ausl.riversamento.builder.oggetti.DatiSpecifici;
import it.bologna.ausl.riversamento.builder.oggetti.ProfiloArchivisticoType;
import it.bologna.ausl.riversamento.builder.oggetti.StrutturaFascicoloType;
import java.io.File;
import java.text.ParseException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author utente
 */
public class testXMLToObject {

    public static void main(String[] args) throws DatatypeConfigurationException, JAXBException, ParseException {

//        DATI SPECIFICI
//	 try {
//		File file = new File("C:\\file.xml");
//		JAXBContext jaxbContext = JAXBContext.newInstance(DatiSpecifici.class);
// 
//		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
//		DatiSpecifici ds = (DatiSpecifici) jaxbUnmarshaller.unmarshal(file);
//                
//                for(Object object: ds.getAny()){
//                    Element e = (Element)object;
//                    System.out.println(e.getNodeName() + ":" + e.getTextContent());
//                }
//                
//                
//	  } catch (JAXBException e) {
//		e.printStackTrace();
//	  }
        //        PROFILO ARCHIVISTICO
//	 try {
//		File file = new File("C:\\filePA.xml");
//		JAXBContext jaxbContext = JAXBContext.newInstance(ProfiloArchivistico.class);
// 
//		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
//		ProfiloArchivistico pa = (ProfiloArchivistico) jaxbUnmarshaller.unmarshal(file);
//                
//                ProfiloArchivisticoType pat = pa.getProfiloArchivisticoType();
//                
//                StrutturaFascicoloType principale = pat.getFascicoloPrincipale();
//                
//                String classifica = principale.getClassifica();
//                
//                System.out.println("classifica: "+classifica);
//                
//	  } catch (JAXBException e) {
//		e.printStackTrace();
//	  }
//        ProfiloArchivistico p = new ProfiloArchivistico();
//        p.addFascicoloSecondario("123", null, null, null, null, null, null, null);
//        System.out.println(p.toXML());
        ProfiloArchivistico pa = new ProfiloArchivistico();
        pa.addFascicoloPrincipale("classifica", "2015", "12345", "oggetto", "num_sott", "oggettoSottoFasc", "11", "oggettoInserto");
        pa.addFascicoloSecondario("classificaSecondario", "2015", "123456789", "oggettoSec", "num_sottSec", "oggettoSottoFascSec", "1122", "oggettoInsertoSec");

        UnitaDocumentariaBuilder ud = new UnitaDocumentariaBuilder("11", 218, "PG", "DOC", "true", "true", "true", pa, "oggetto", "06-07-2018 00:00:00", null, "1.3", "AMBIENTE", "ente", "strutturaversante", "userID", "tipo", "ISO-8859-1");
        System.out.println(ud.toString());

    }
}
