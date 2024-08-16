package it.bologna.ausl.riversamento.builder.oggetti;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.w3c.dom.Element;


/**
 * <p>Java class for DatiSpecifici complex type.
 
 <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DatiSpecifici">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="VersioneDatiSpecifici" type="{}StringNVMax1024Type"/>
 *         &lt;any maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DatiSpecificiType", propOrder = {
    "versioneDatiSpecifici",
    "any"
})

@XmlRootElement
public class DatiSpecifici {

    @XmlElement(name = "VersioneDatiSpecifici", required = true)
    protected String versioneDatiSpecifici;
    @XmlAnyElement(lax = true)
    protected List<Object> any;
    
    /**
     * Gets the value of the versioneDatiSpecifici property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersioneDatiSpecifici() {
        return versioneDatiSpecifici;
    }
    
    /**
     * Sets the value of the versioneDatiSpecifici property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersioneDatiSpecifici(String value) {
        this.versioneDatiSpecifici = value;
    }
    
   /**
     * Gets the value of the any property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the any property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAny().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * 
     * 
     */
    public List<Object> getAny() {
        if (any == null) {
            any = new ArrayList<Object>();
        }
        return this.any;
    }
    
    
    public String toXML(){
        
        String res = "";
        
        try{
            // la classe Marshaller è responsabile di comandare il processo di serializzazione
            // del Java Tree (della classe desiderata)in dato XML
            JAXBContext jaxbContext = JAXBContext.newInstance(DatiSpecifici.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            
            // dice se si vuole l'output formattato
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
 
            // per ritornare String bisogna prima usare StringWriter e poi usare il toString()
            java.io.StringWriter sw = new StringWriter();

            // definizione del tipo di codifica
            jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            
            jaxbMarshaller.marshal(this, sw);
                
            res = sw.toString();
        }
        catch(JAXBException e){
            e.printStackTrace();
        }
        
        return res;
        
    }
    
    public static DatiSpecifici parser(String xml) throws UnsupportedEncodingException{
        
        DatiSpecifici ds = null;
        StringReader reader = null;
        
        try{
            JAXBContext jaxbContext = JAXBContext.newInstance(DatiSpecifici.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();  
            reader = new StringReader(xml);
                
            ds = (DatiSpecifici) jaxbUnmarshaller.unmarshal(reader);
        }
        catch(JAXBException e){
            e.printStackTrace();
        }
        finally{
            org.apache.commons.io.IOUtils.closeQuietly(reader);
        }
        
        return ds;
    }
    
    public String print(){
        // stampo i valori dell'oggetto
        
        String res = "";
        
        for(Object object: any){
            Element e = (Element)object;
            res = res + " [" + e.getNodeName() + ":" + e.getTextContent()+"]";
        }
        
        return res;
    }
}

