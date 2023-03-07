/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.riversamento.builder;

import it.bologna.ausl.riversamento.builder.oggetti.DatiSpecifici;
import java.io.UnsupportedEncodingException;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;

/**
 *
 * @author utente
 */
public class Test2 {
    
    public static void main(String[] args) throws ParserConfigurationException, UnsupportedEncodingException{
        
        DatiSpecificiBuilder dsb = new DatiSpecificiBuilder("1.1"){};
        dsb.insertNewTag("Mittente", "mitt");
        
        dsb.setNewChild("Nome", "Paolo Rossi");
        dsb.setNewChild("Nome", "Paolo Rossi");
        
        dsb.insertNewTagWithChildren("Visti");
        
//        dsb.insertNewTag("Movimento", "IN");
//        dsb.insertNewTag("ModalitaTrasmissione", "indirizzo generico");
//        dsb.insertNewTag("OperatoreDiProtocollo", "operatore");
        
        
        DatiSpecifici ds = dsb.getDatiSpecifici();
        
        String xml = ds.toXML();
        System.out.println(xml);
        
//        DatiSpecifici datiSpecifici = DatiSpecifici.parser(xml);
//       
//        // stampo a video i valori dell'oggetto
//        for(Object object: datiSpecifici.getAny()){
//            Element e = (Element)object;
//            System.out.println(e.getNodeName() + ":" + e.getTextContent());
//        }
        
        
        
    }
    
}
