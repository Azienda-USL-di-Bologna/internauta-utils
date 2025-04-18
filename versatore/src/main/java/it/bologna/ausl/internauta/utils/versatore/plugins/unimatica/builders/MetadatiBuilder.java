package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders;

import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.Documento;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.DocumentoAmministrativoInformaticoType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.IdDocType;
import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.ImprontaCrittograficaDelDocumentoType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author boria
 */
public class MetadatiBuilder {

    private static final Logger log = LoggerFactory.getLogger(MetadatiBuilder.class);
    private Map<String, Object> parametriVersamento;
    private AllegatoUnimatica documentoPrincipale;
    private Documento documento;
    private Documento.Intestazione intestazione;
    private Documento.Profilo profilo;
    private Documento.Profilo.MetadatiAGID metadatiAGID;
    private Marshaller marshaller;
    private String codificaMarshaller;

    public MetadatiBuilder(Map<String, Object> parametriVersamento, AllegatoUnimatica documentoPrincipale) {
        try {
            this.documentoPrincipale = documentoPrincipale;
            this.parametriVersamento = parametriVersamento;

            JAXBContext jaxb = JAXBContext.newInstance(Documento.class);
            codificaMarshaller = "UTF-8";
            marshaller = jaxb.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, codificaMarshaller);
            documento = new Documento();
            intestazione = new Documento.Intestazione();
            profilo = new Documento.Profilo();
            metadatiAGID = new Documento.Profilo.MetadatiAGID();
            documento.setIntestazione(intestazione);
            profilo.setMetadatiAGID(metadatiAGID);
            documento.setProfilo(profilo);

        } catch (JAXBException ex) {
            log.error("errore nella costruzione di VersamentoBuilder", ex);
        }
    }

    public void build() {
        //creo l'intestazione del documento principale
        intestazione.setIdFile(documentoPrincipale.getIdFile().toString());
        intestazione.setNomeFile(documentoPrincipale.getNomeFile());
        intestazione.setPrincipale(true);
        //creo il profilo
        //TODO devo inserire i metadati specifici?
        //creo i metadati agid creando il Documento Amministrativo Informatico e settandone i campi
        DocumentoAmministrativoInformaticoType documentoAmministrativoInformatico = new DocumentoAmministrativoInformaticoType();
        IdDocType idDoc = new IdDocType();
        ImprontaCrittograficaDelDocumentoType improntaCrittograficaDelDocumento = new ImprontaCrittograficaDelDocumentoType();
        improntaCrittograficaDelDocumento.setImpronta(documentoPrincipale.getImpronta());
        improntaCrittograficaDelDocumento.setAlgoritmo((String) parametriVersamento.get("algoritmo"));
        idDoc.setImprontaCrittograficaDelDocumento(improntaCrittograficaDelDocumento);
        //TODO identificativo cosa prendo?
        //TODO e segnatura?
        documentoAmministrativoInformatico.setIdDoc(idDoc);
    }

    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String out;
        try {
            marshaller.marshal(documento, baos);
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
