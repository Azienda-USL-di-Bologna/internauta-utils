package it.bologna.ausl.riversamento.builder;

import it.bologna.ausl.riversamento.builder.oggetti.DatiSpecifici;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author utente
 */
public class DatiSpecificiBuilder {

    private DatiSpecifici datiSpecifici;
    private ArrayList<KeyValue> elements;
    
    public DatiSpecificiBuilder() {
        datiSpecifici = new DatiSpecifici();
        elements = new ArrayList<KeyValue>();
    }
    
    public DatiSpecificiBuilder(String versione) {
        this();
        datiSpecifici.setVersioneDatiSpecifici(versione);
    }
    
    public void insertNewTag(String nameTag, String contentTag) throws ParserConfigurationException{
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element consumerInfo = doc.createElement(nameTag);
        consumerInfo.setTextContent(contentTag);
        
        doc.appendChild(consumerInfo);
        datiSpecifici.getAny().add(doc.getDocumentElement());
    }
    
    public void setNewChild(String nameTag, String contentTag) throws ParserConfigurationException{
        KeyValue el = new KeyValue(nameTag, contentTag);
        elements.add(el);
    }
    
    public void clearChildren(){
        elements.clear();
    }
    
    public void insertNewTagWithChildren(String nameTag) throws ParserConfigurationException{
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element consumerInfo = doc.createElement(nameTag);
 
        Iterator<KeyValue> iterator = elements.iterator();
        while (iterator.hasNext()) {
            KeyValue tmp = (KeyValue)iterator.next();
            Element el = doc.createElement(tmp.getKey());  
            el.setTextContent(tmp.getValue());
            consumerInfo.appendChild(el);
	}
               
        doc.appendChild(consumerInfo);
        datiSpecifici.getAny().add(doc.getDocumentElement());
    }

    public DatiSpecifici getDatiSpecifici(){
        return datiSpecifici;
    }
    
    private class KeyValue {
        private String key;
        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }
        
    }
    
}