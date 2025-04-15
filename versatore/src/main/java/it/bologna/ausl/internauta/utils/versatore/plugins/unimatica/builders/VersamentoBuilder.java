package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders;

import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.Metadata;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.Singlemetadata;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.Versamento;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Andrea
 */
public class VersamentoBuilder {

    private static final Logger log = LoggerFactory.getLogger(VersamentoBuilder.class);
    private Versamento versamento;
    private Metadata metadata;
    private Marshaller marshaller;
    private String codificaMarshaller;

    public VersamentoBuilder() {
        try {
            JAXBContext jaxb = JAXBContext.newInstance(Versamento.class);
            codificaMarshaller = "UTF-8";
            marshaller = jaxb.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, codificaMarshaller);

            versamento = new Versamento();
            metadata = new Metadata();
            //versamento.setMetadata(metadata);
        } catch (JAXBException ex) {
            log.error("errore nella costruzione di VersamentoBuilder", ex);
        }
    }

    /*public void addSinglemetadata(Singlemetadata singleMetadata) {
        this.metadata.getSinglemetadata().add(singleMetadata);
    }*/
    public void setDocType(String docType) {
        this.versamento.setDocType(docType);
    }

    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String out;
        try {
            marshaller.marshal(versamento, baos);
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
