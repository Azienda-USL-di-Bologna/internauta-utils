package it.bologna.ausl.riversamento.builder;

import it.bologna.ausl.riversamento.builder.oggetti.ChiaveType;
import it.bologna.ausl.riversamento.builder.oggetti.ComponenteType;
import it.bologna.ausl.riversamento.builder.oggetti.ConfigAggAllType;
import it.bologna.ausl.riversamento.builder.oggetti.DatiSpecifici;
import it.bologna.ausl.riversamento.builder.oggetti.DocumentoType;
import it.bologna.ausl.riversamento.builder.oggetti.IntestazioneAggAllType;
import it.bologna.ausl.riversamento.builder.oggetti.ObjectFactory;
import it.bologna.ausl.riversamento.builder.oggetti.ProfiloDocumentoType;
import it.bologna.ausl.riversamento.builder.oggetti.StrutturaType;
import it.bologna.ausl.riversamento.builder.oggetti.TipoConservazioneType;
import it.bologna.ausl.riversamento.builder.oggetti.TipoSupportoType;
import it.bologna.ausl.riversamento.builder.oggetti.UnitaDocAggAllegati;
import it.bologna.ausl.riversamento.builder.oggetti.VersatoreType;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import org.json.simple.JSONArray;

public class AggiuntaAllegatiBuilder {

    private final UnitaDocAggAllegati unitaDocumentaria;
    private final ChiaveType chiave;
    private final ConfigAggAllType configurazione;
    private final IntestazioneAggAllType intestazione;
    private final VersatoreType versatore;
    private final DocumentoType annesso;
    private final Marshaller marshaller;
    private final String codificaMarshaller;
    private final JSONArray identityFiles;
    private final ProfiloDocumentoType profilo;
    private final StrutturaType struttura;
    private final ComponenteType componente;

    public AggiuntaAllegatiBuilder(String numDocumento,
            int anno,
            String tipoRegistro,
            String tipoDocumento,
            String forzaConservazione,
            String forzaAccettazione,
            String versione,
            String ambiente,
            String ente,
            String strutturaVersante,
            String userID,
            //String autore,
            String descrizione,
            String id,
            String tipoStruttura,
            String tipoComponenteDefault,
            IdentityFile identityFile,
            int ordinePresentazione,
            String tipoSupportoComponente,
            DatiSpecifici ds,
            String tipoConservazione,
            String codifica) throws JAXBException {

        // setta la codifica
        JAXBContext jaxb = JAXBContext.newInstance(UnitaDocAggAllegati.class);
        codificaMarshaller = codifica;
        marshaller = jaxb.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, codificaMarshaller);

        // inizializzazioni
        identityFiles = new JSONArray();
        ObjectFactory objectFactory = new ObjectFactory();
        unitaDocumentaria = objectFactory.createUnitaDocAggAllegati();
        intestazione = new IntestazioneAggAllType();
        versatore = new VersatoreType();
        chiave = new ChiaveType();
        configurazione = new ConfigAggAllType();
        annesso = new DocumentoType();
        profilo = new ProfiloDocumentoType();
        struttura = new StrutturaType();
        componente = new ComponenteType();

        // dati specifici
        JAXBElement<DatiSpecifici> jaxbElement = new JAXBElement(
                new QName(DatiSpecifici.class.getSimpleName()), DatiSpecifici.class, ds);
        jaxbElement.setValue(ds);

        // setta id e tipo documento dell'annesso
        annesso.setIDDocumento(id);
        annesso.setTipoDocumento(tipoDocumento);

        // assegna i dati specifici
        annesso.setDatiSpecifici(jaxbElement);

        // imposta la chiave
        chiave.setNumero(numDocumento);
        chiave.setAnno(anno);
        chiave.setTipoRegistro(tipoRegistro);

        // intestazione
        intestazione.setChiave(chiave);
        intestazione.setVersione(versione);

        // versatore
        versatore.setAmbiente(ambiente);
        versatore.setEnte(ente);
        versatore.setStruttura(strutturaVersante);
        versatore.setUserID(userID);
        intestazione.setVersatore(versatore);
        unitaDocumentaria.setIntestazione(intestazione);

        // imposta configurazione
        if (forzaConservazione.equalsIgnoreCase("true")) {
            configurazione.setForzaConservazione(true);
        } else if (forzaConservazione.equalsIgnoreCase("false")) {
            configurazione.setForzaConservazione(false);
        }

        if (forzaAccettazione.equalsIgnoreCase("true")) {
            configurazione.setForzaAccettazione(true);
        } else if (forzaConservazione.equalsIgnoreCase("false")) {
            configurazione.setForzaAccettazione(false);
        }

        configurazione.setTipoConservazione(getTipoConservazione(tipoConservazione));
        unitaDocumentaria.setConfigurazione(configurazione);

        // setta il profilo
        //profilo.setAutore(autore);
        profilo.setDescrizione(descrizione);
        annesso.setProfiloDocumento(profilo);

        // definizione struttura
        struttura.setTipoStruttura(tipoStruttura);

        // costruzione del componente
        if (identityFile != null && ("".equals(identityFile))) {
            componente.setTipoComponente(tipoComponenteDefault);
            componente.setNomeComponente(identityFile.getFileName());
//          TODO: da decommentare se si vuole inserire la dichiarazione del formato file nell'xml versato
//            componente.setFormatoFileVersato(identityFile.getFormatType());
            componente.setHashVersato(identityFile.getHash());
            componente.setID(identityFile.getId());
            if ((identityFile.getUuidMongo() != null) && (!"".equals(identityFile.getUuidMongo()))) {
                componente.setIDComponenteVersato(identityFile.getUuidMongo());
            }
            identityFiles.add(identityFile.getJSON());
        } else {
            IdentityFile idf = new IdentityFile();
            componente.setID(idf.generateID());
        }

        componente.setOrdinePresentazione(ordinePresentazione);

        if (tipoSupportoComponente.equalsIgnoreCase("FILE")) {
            componente.setTipoSupportoComponente(TipoSupportoType.FILE);
        } else if (tipoSupportoComponente.equalsIgnoreCase("RIFERIMENTO")) {
            componente.setTipoSupportoComponente(TipoSupportoType.RIFERIMENTO);
        } else if (tipoSupportoComponente.equalsIgnoreCase("METADATI")) {
            componente.setTipoSupportoComponente(TipoSupportoType.METADATI);
        }

        struttura.setComponenti(new StrutturaType.Componenti());
        StrutturaType.Componenti componenti = struttura.getComponenti();
        componenti.getComponente().add(componente);
        struttura.setComponenti(componenti);

        annesso.setStrutturaOriginale(struttura);

        unitaDocumentaria.setAnnesso(annesso);
    }

    private TipoConservazioneType getTipoConservazione(String tipoConservazione) {

        TipoConservazioneType result = null;

        if (tipoConservazione.equalsIgnoreCase("SOSTITUTIVA")) {
            result = TipoConservazioneType.SOSTITUTIVA;
        } else if (tipoConservazione.equalsIgnoreCase("VERSAMENTO_ANTICIPATO")) {
            result = TipoConservazioneType.VERSAMENTO_ANTICIPATO;
        } else if (tipoConservazione.equalsIgnoreCase("FISCALE")) {
            result = TipoConservazioneType.FISCALE;
        } else if (tipoConservazione.equalsIgnoreCase("MIGRAZIONE")) {
            result = TipoConservazioneType.MIGRAZIONE;
        } else {
            result = null;
        }

        return result;
    }

    public JSONArray getIdentityFiles() {
        return identityFiles;
    }

    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String out;
        try {
            marshaller.marshal(unitaDocumentaria, baos);
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
