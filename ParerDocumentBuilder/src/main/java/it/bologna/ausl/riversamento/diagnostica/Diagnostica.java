/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.riversamento.diagnostica;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author utente
 */
public class Diagnostica {
  
    private Document document;

    public Diagnostica() {
    }
    
    public Boolean controllaSegnatura(InputStream is, String numero) throws SAXException, IOException, ParserConfigurationException{
        
        String res = "";
        Boolean esito = false;
        
        document = Diagnostica.readXml(is);
        
        document.getDocumentElement().normalize();
 
	NodeList nList = document.getElementsByTagName("Identificatore");
  
	for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
  
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
		Element eElement = (Element) nNode;		
		res = eElement.getElementsByTagName("NumeroRegistrazione").item(0).getTextContent();
                if (res.equalsIgnoreCase(numero)){
                    esito = true;
                    break;
                }
            }
	}
        
        return esito;
    }
    
    private static Document readXml(InputStream is) throws SAXException, IOException, ParserConfigurationException {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

      dbf.setValidating(false);
      dbf.setIgnoringComments(false);
      dbf.setIgnoringElementContentWhitespace(true);
      dbf.setNamespaceAware(true);
      // dbf.setCoalescing(true);
      // dbf.setExpandEntityReferences(true);

      DocumentBuilder db = null;
      db = dbf.newDocumentBuilder();
      db.setEntityResolver((EntityResolver) new NullResolver());

      // db.setErrorHandler( new MyErrorHandler());

      return db.parse(is);
  }
}

class NullResolver implements EntityResolver {
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException,
      IOException {
    return new InputSource(new StringReader(""));
  }
    
}
