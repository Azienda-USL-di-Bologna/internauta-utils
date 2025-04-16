package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders;

import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.Documento;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author boria
 */
public class MetadatiBuilder {

    private static final Logger log = LoggerFactory.getLogger(MetadatiBuilder.class);
    private Documento d;
    private Marshaller marshaller;
    private String codificaMarshaller;

    public MetadatiBuilder() {
        try {
            JAXBContext jaxb = JAXBContext.newInstance(Documento.class);
            codificaMarshaller = "UTF-8";
            marshaller = jaxb.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, codificaMarshaller);
            d = new Documento();

        } catch (JAXBException ex) {
            log.error("errore nella costruzione di VersamentoBuilder", ex);
        }
    }

    public void build() {
        //d.setDocType("ciao");
    }

    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String out;
        try {
            marshaller.marshal(d, baos);
        } catch (JAXBException e) {
            return "Document Error unable to serialize: " + e;
        }

        try {
            out = new String(baos.toByteArray(), codificaMarshaller);
        } catch (UnsupportedEncodingException e) {
            return "Document Error unable to serialize with coding " + codificaMarshaller + ": " + e;
        }

        return out;
    }
}
