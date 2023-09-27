package it.bologna.ausl.internauta.utils.versatore.plugins.sdico.builders;

import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.File;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.Files;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.Metadata;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.Singlemetadata;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.Values;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.Versamento;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Level;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
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
    private Files files;
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
            versamento.setMetadata(metadata);
            files = new Files();
            versamento.setFiles(files);
        } catch (JAXBException ex) {
            log.error("errore nella costruzione di VersamentoBuilder", ex);
        }
    }

    public void addSinglemetadata(Singlemetadata singleMetadata) {
        this.metadata.getSinglemetadata().add(singleMetadata);
    }

    public void addSinglemetadataByParams(boolean mandatory, String name, List<String> c, String type) {
        Singlemetadata sm = new Singlemetadata();

        if (Boolean.TRUE.equals(mandatory)) {
            sm.setMandatory("true");
        } else {
            sm.setMandatory("false");
        }

        sm.setName(name);

        Values v = new Values();
        v.getValue().addAll(c);
        sm.setValues(v);
        sm.setType(type);
        this.metadata.getSinglemetadata().add(sm);

    }

    public void setDocType(String docType) {
        this.versamento.setDocType(docType);
    }

    public void addFile(File f) {
        this.files.getFile().add(f);
    }

    /**
     *
     * @param filename nome del file
     * @param mimetype mimetype del file
     * @param hash calcolato in SHA256
     */
    public void addFileByParams(String filename, String mimetype, String hash) {
        File file = new File();
        file.setFilename(filename);
        file.setMimetype(mimetype);
        file.setHash(hash);
        this.files.getFile().add(file);
    }

    public void build() {
        Singlemetadata id_ente_versatore = new Singlemetadata();

        versamento.setDocType("82");

        metadata.getSinglemetadata().add(id_ente_versatore);

        File file = new File();
        file.setFilename("Documento_di_prova.pdf");
        file.setMimetype("application/pdf");
        file.setHash("106cdc73f652bfa0349910d7d1c9a541dc326e729e22d779c7c2a64920034e0f");
        files.getFile().add(file);

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
