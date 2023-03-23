/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.riversamento.builder;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;

/**
 *
 * @author utente
 */
public class TestIdentityFile {
    public static void main(String[] args) {
        
        // marshall
        IdentityFiles identityFiles = new IdentityFiles();
        
        identityFiles.setidentityFiles(new ArrayList<IdentityFile>());
        
        //crea 2 identityfile
        IdentityFile if1 = new IdentityFile("a", "a", "a", "a", "a");
        IdentityFile if2 = new IdentityFile("b", "b", "b", "b", "b");
        
        // aggiunta identity file alla lista
        identityFiles.getidentityFiles().add(if1);
        identityFiles.getidentityFiles().add(if2);
        
        JAXBContext jaxbContext;
        
        java.io.StringWriter sw = new StringWriter();
                
        try {
            jaxbContext = JAXBContext.newInstance(IdentityFiles.class);
            
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            //Marshal the identityfile list in console
            //jaxbMarshaller.marshal(identityFiles, System.out);
            jaxbMarshaller.marshal(identityFiles, sw);
            
            System.out.println(sw.toString());
            java.io.StringReader sr = new StringReader(sw.toString());
            
            //Marshal the employees list in file
            //jaxbMarshaller.marshal(identityFiles, new File("c:/identityfiles.xml"));
            
            // unmarshall
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
     
            //IdentityFiles identityFileList = (IdentityFiles) jaxbUnmarshaller.unmarshal( new File("c:/identityfiles.xml") );
            IdentityFiles identityFileList = (IdentityFiles) jaxbUnmarshaller.unmarshal(sr);
     
            for(IdentityFile idf : identityFileList.getidentityFiles())
            {
                System.out.println(idf.getFileName());
            }
            
        } catch (JAXBException ex) {
            Logger.getLogger(TestIdentityFile.class.getName()).log(Level.SEVERE, null, ex);
        }

        
        
    }
}
