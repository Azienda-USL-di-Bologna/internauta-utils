package it.bologna.ausl.riversamento.builder;

import it.bologna.ausl.riversamento.builder.oggetti.StrutturaFascicoloType;
import it.bologna.ausl.riversamento.builder.oggetti.FascicoloType;
import it.bologna.ausl.riversamento.builder.oggetti.ProfiloArchivisticoType;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author utente
 */

@XmlRootElement(name = "ProfiloArchivistico")
public class ProfiloArchivistico {
    
    //@XmlElement
    private final ProfiloArchivisticoType profiloArchivistico;
    @XmlElement(name = "FascicoloPrincipale")
    private final StrutturaFascicoloType strutturaFascicoloPrincipale;
    //@XmlElement
    private final FascicoloType FascicoloPrincipale;
    @XmlElement(name = "FascicoliSecondari")
    private final ProfiloArchivisticoType.FascicoliSecondari strutturaFascicoliSecondari;
    

    public ProfiloArchivistico() {
        profiloArchivistico = new ProfiloArchivisticoType();
        
        strutturaFascicoloPrincipale = new StrutturaFascicoloType();
        strutturaFascicoliSecondari = new ProfiloArchivisticoType.FascicoliSecondari();
        
        FascicoloPrincipale = new FascicoloType();
        
    }
    
    public void addFascicoloPrincipale(String classifica, 
                                       String anno,
                                       String numeroFascicolo, 
                                       String oggetto, 
                                       String numeroSottoFascicolo,
                                       String oggettoSottoFascicolo,
                                       String numeroInserto,
                                       String oggettoInserto){

        strutturaFascicoloPrincipale.setClassifica(classifica);
        //FascicoloPrincipale.setIdentificativo(classifica + "/" + anno + "/" + numeroFascicolo);
        FascicoloPrincipale.setIdentificativo(anno + "/" + numeroFascicolo);
        FascicoloPrincipale.setOggetto(oggetto);
        strutturaFascicoloPrincipale.setFascicolo(FascicoloPrincipale);
        
        // aggiunta sotto fascicolo
        if((numeroSottoFascicolo != null) && (!"".equals(numeroSottoFascicolo))){
            String identificativo = "";
            if((numeroInserto != null) && (!"".equals(numeroInserto))){
                //identificativo = classifica + "/" + anno + "/" + numeroFascicolo + "/" + numeroSottoFascicolo + "/" + numeroInserto;
                identificativo = anno + "/" + numeroFascicolo + "/" + numeroSottoFascicolo + "/" + numeroInserto;
            }else{
                //identificativo = classifica + "/" + anno + "/" + numeroFascicolo + "/" + numeroSottoFascicolo;
                identificativo = anno + "/" + numeroFascicolo + "/" + numeroSottoFascicolo;
            }

            String valoreOggetto = "";
            if((oggettoInserto != null) && (!"".equals(oggettoInserto))){
                valoreOggetto = oggettoSottoFascicolo + "-" + oggettoInserto;
            }else{
                valoreOggetto = oggettoSottoFascicolo;
            }

            FascicoloType sottoFascicolo = new FascicoloType();
            sottoFascicolo.setIdentificativo(identificativo);
            sottoFascicolo.setOggetto(valoreOggetto);
            
            strutturaFascicoloPrincipale.setSottoFascicolo(sottoFascicolo);
        }
        
        profiloArchivistico.setFascicoloPrincipale(strutturaFascicoloPrincipale);
        
    }
    
    public void addFascicoloSecondario(String classifica, 
                                       String anno,
                                       String numeroFascicolo, 
                                       String oggetto, 
                                       String numeroSottoFascicolo,
                                       String oggettoSottoFascicolo,
                                       String numeroInserto,
                                       String oggettoInserto){
    
       String identificativo, valoreOggetto;  
        
       StrutturaFascicoloType fascicoloSecondario = new StrutturaFascicoloType();
       fascicoloSecondario.setClassifica(classifica);
       
       if((anno != null) && (!"".equals(anno)) && numeroFascicolo != null && (!"".equals(numeroFascicolo)) && oggetto != null && (!"".equals(oggetto))){
           FascicoloType f = new FascicoloType();
           //f.setIdentificativo(classifica + "/" + anno + "/" + numeroFascicolo);
           f.setIdentificativo(anno + "/" + numeroFascicolo);
           f.setOggetto(oggetto);
           fascicoloSecondario.setFascicolo(f);
       }
       
       FascicoloType sottoFascicolo = null;
       // aggiunta sotto fascicolo
        if((numeroSottoFascicolo != null) && (!"".equals(numeroSottoFascicolo))){
            identificativo = anno + "/" + numeroFascicolo + "/" + numeroSottoFascicolo;
            valoreOggetto = oggettoSottoFascicolo;

            sottoFascicolo = new FascicoloType();
            sottoFascicolo.setIdentificativo(identificativo);
            sottoFascicolo.setOggetto(valoreOggetto);
            
            fascicoloSecondario.setSottoFascicolo(sottoFascicolo);
            
            //strutturaFascicoliSecondari.addFascicoloSecondario(fascicoloSecondario);
        } 
                   
        // aggiunta inserto
        if((numeroInserto != null) && (!"".equals(numeroInserto))){
            identificativo = anno + "/" + numeroFascicolo + "/" + numeroSottoFascicolo + "/" + numeroInserto;
            valoreOggetto = oggettoSottoFascicolo + "-" + oggettoInserto;

            FascicoloType inserto = new FascicoloType();
            inserto.setIdentificativo(identificativo);
            inserto.setOggetto(valoreOggetto);
            
            //fascicoloSecondario.setFascicolo(sottoFascicolo);
            fascicoloSecondario.setSottoFascicolo(inserto);
            
            
            //strutturaFascicoliSecondari.addFascicoloSecondario(fascicoloSecondario);
        }
       strutturaFascicoliSecondari.addFascicoloSecondario(fascicoloSecondario);
       
       
       profiloArchivistico.setFascicoliSecondari(strutturaFascicoliSecondari);
    }
    
    public ProfiloArchivisticoType getProfiloArchivisticoType(){
        return profiloArchivistico;
    }
    
    public static ProfiloArchivistico parser(String xml) throws UnsupportedEncodingException{
        
        ProfiloArchivistico ps = null;
        StringReader reader = null;
        
        try{
            JAXBContext jaxbContext = JAXBContext.newInstance(ProfiloArchivistico.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();  
            reader = new StringReader(xml);
                
            ps = (ProfiloArchivistico) jaxbUnmarshaller.unmarshal(reader);
        }
        catch(JAXBException e){
            e.printStackTrace();
        } 
        
        
        return ps;
    }
    
    
    public String toXML(){
        
        String res = "";
        
        try{
            // la classe Marshaller è responsabile di comandare il processo di serializzazione
            // del Java Tree (della classe desiderata)in dato XML
            JAXBContext jaxbContext = JAXBContext.newInstance(ProfiloArchivistico.class);
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
}
