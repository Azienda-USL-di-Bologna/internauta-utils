package it.bologna.ausl.riversamento.builder;

import it.bologna.ausl.riversamento.builder.oggetti.ChiaveType;
import it.bologna.ausl.riversamento.builder.oggetti.ComponenteType;
import it.bologna.ausl.riversamento.builder.oggetti.ConfigType;
import it.bologna.ausl.riversamento.builder.oggetti.DatiSpecifici;
import it.bologna.ausl.riversamento.builder.oggetti.DocumentoType;
import it.bologna.ausl.riversamento.builder.oggetti.IntestazioneType;
import it.bologna.ausl.riversamento.builder.oggetti.ObjectFactory;
import it.bologna.ausl.riversamento.builder.oggetti.ProfiloDocumentoType;
import it.bologna.ausl.riversamento.builder.oggetti.ProfiloUnitaDocumentariaType;
import it.bologna.ausl.riversamento.builder.oggetti.StrutturaType;
import it.bologna.ausl.riversamento.builder.oggetti.TipoConservazioneType;
import it.bologna.ausl.riversamento.builder.oggetti.TipoSupportoType;
import it.bologna.ausl.riversamento.builder.oggetti.UnitaDocumentaria;
import it.bologna.ausl.riversamento.builder.oggetti.UnitaDocumentaria.Allegati;
import it.bologna.ausl.riversamento.builder.oggetti.UnitaDocumentaria.Annessi;
import it.bologna.ausl.riversamento.builder.oggetti.UnitaDocumentaria.Annotazioni;
import it.bologna.ausl.riversamento.builder.oggetti.VersatoreType;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import org.json.simple.JSONArray;

/**
 *
 * @author utente
 */
public class UnitaDocumentariaBuilder {

    private static final String versione = "1.3";
    private static final String ambiente = "PARER_TEST";
    private static final String ente = "AUSL_BO";
    private static final String strutturaVersante = "ASL_BO";
    private static final String userID = "gedi_ausl_bo";
    private static final boolean simulaInDB = false;
    private static final boolean setCartaceo = false;
    private static final String tipoComponenteDefault = "Contenuto";

    private Annessi annessi = null;
    private Allegati allegati = null;
    private Annotazioni annotazioni = null;

    private final UnitaDocumentaria unitaDocumentaria;
    private final IntestazioneType intestazione;
    private final VersatoreType versatore;
    private final ChiaveType chiave;
    private final ConfigType config;
    private final ProfiloArchivistico profiloArchivistico;
    private final DatiSpecifici datiSpecifici;
    private final ProfiloUnitaDocumentariaType profiloUnitaDocumentaria;
    private DocumentoType documentoPrincipale;
    private ProfiloDocumentoType profiloDocumento;
    private int contatoreVersamento;
    private final JSONArray identityFiles;
    private final Marshaller marshaller;
    private final String codificaMarshaller;

    public UnitaDocumentariaBuilder(String numDocumento,
            int anno,
            String tipoRegistro,
            String tipoDocumento,
            //boolean forzaConservazione,
            String forzaConservazione,
            //boolean forzaAccettazione,  
            String forzaAccettazione,
            //boolean forzaCollegamento,
            String forzaCollegamento,
            ProfiloArchivistico profiloArchivistico,
            String oggetto,
            String data,
            DatiSpecifici ds,
            String versione,
            String ambiente,
            String ente,
            String strutturaVersante,
            String userID,
            String tipoConservazione,
            String codifica) throws DatatypeConfigurationException, JAXBException, ParseException {

        JAXBContext jaxb = JAXBContext.newInstance(UnitaDocumentaria.class);
        codificaMarshaller = codifica;
        marshaller = jaxb.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, codificaMarshaller);
//        try {
//            marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", new MyNamespaceMapper());
//            //m.setProperty("com.sun.xml.bind.namespacePrefixMapper", new MyNamespaceMapper());
//        } catch (PropertyException e) {
//            // In case another JAXB implementation is used
//        }

        identityFiles = new JSONArray();

        ObjectFactory of = new ObjectFactory();

        unitaDocumentaria = of.createUnitaDocumentaria();
        intestazione = new IntestazioneType();
        versatore = new VersatoreType();
        chiave = new ChiaveType();
        config = new ConfigType();

        // dati specifici
        JAXBElement<DatiSpecifici> jaxbElement = new JAXBElement(
                new QName(DatiSpecifici.class.getSimpleName()), DatiSpecifici.class, ds);

        jaxbElement.setValue(ds);
        unitaDocumentaria.setDatiSpecifici(jaxbElement);
        //---------------

        unitaDocumentaria.setProfiloArchivistico(profiloArchivistico.getProfiloArchivisticoType());

        this.profiloArchivistico = profiloArchivistico;
        this.datiSpecifici = ds;
        profiloUnitaDocumentaria = new ProfiloUnitaDocumentariaType();
        contatoreVersamento = 1;

        intestazione.setVersione(versione);

        versatore.setAmbiente(ambiente);
        versatore.setEnte(ente);
        versatore.setStruttura(strutturaVersante);
        versatore.setUserID(userID);

        intestazione.setVersatore(versatore);
        intestazione.setTipologiaUnitaDocumentaria(tipoDocumento);

        chiave.setNumero(numDocumento);
        chiave.setAnno(anno);
        chiave.setTipoRegistro(tipoRegistro);

        intestazione.setChiave(chiave);

        unitaDocumentaria.setIntestazione(intestazione);

        if (forzaAccettazione.equalsIgnoreCase("true")) {
            config.setForzaAccettazione(true);
        } else if (forzaAccettazione.equalsIgnoreCase("false")) {
            config.setForzaAccettazione(false);
        }

        if (forzaCollegamento.equalsIgnoreCase("true")) {
            config.setForzaCollegamento(true);
        } else if (forzaCollegamento.equalsIgnoreCase("false")) {
            config.setForzaCollegamento(false);
        }

        if (forzaConservazione.equalsIgnoreCase("true")) {
            config.setForzaConservazione(true);
        } else if (forzaConservazione.equalsIgnoreCase("false")) {
            config.setForzaConservazione(false);
        }

        config.setTipoConservazione(getTipoConservazione(tipoConservazione));
        config.setSimulaSalvataggioDatiInDB(simulaInDB);

        unitaDocumentaria.setConfigurazione(config);

        profiloUnitaDocumentaria.setOggetto(oggetto);
        profiloUnitaDocumentaria.setData(data);
        profiloUnitaDocumentaria.setCartaceo(setCartaceo);

        unitaDocumentaria.setProfiloUnitaDocumentaria(profiloUnitaDocumentaria);

    }

    public void addDocumentoPrincipale(String id,
            String tipo,
            String autore,
            String descrizione,
            int ordinePresentazione,
            IdentityFile identityFile,
            String dataRiferimentoTemporale,
            String tipoComponenteDefault,
            String tStruttura,
            String tipoSupportoComponente,
            String descrizioneRifTemporale) throws DatatypeConfigurationException, ParseException {

        documentoPrincipale = new DocumentoType();

        documentoPrincipale.setIDDocumento(id);
        documentoPrincipale.setTipoDocumento(tipo);

        profiloDocumento = new ProfiloDocumentoType();

        profiloDocumento.setDescrizione(descrizione);

        if (autore != null && !autore.equalsIgnoreCase("")) {
            profiloDocumento.setAutore(autore);
        }

        documentoPrincipale.setProfiloDocumento(profiloDocumento);

        StrutturaType struttura = new StrutturaType();
        struttura.setTipoStruttura(tStruttura);

        StrutturaType.Componenti componenti = new StrutturaType.Componenti();

        ComponenteType componente = new ComponenteType();
        componente.setTipoComponente(tipoComponenteDefault);
        //componente.setID(String.valueOf(contatoreVersamento++));
        componente.setID(identityFile.getId());
        componente.setOrdinePresentazione(ordinePresentazione);

        if (tipoSupportoComponente.equalsIgnoreCase("FILE")) {
            componente.setTipoSupportoComponente(TipoSupportoType.FILE);
        } else if (tipoSupportoComponente.equalsIgnoreCase("RIFERIMENTO")) {
            componente.setTipoSupportoComponente(TipoSupportoType.RIFERIMENTO);
        } else if (tipoSupportoComponente.equalsIgnoreCase("METADATI")) {
            componente.setTipoSupportoComponente(TipoSupportoType.METADATI);
        }

        identityFiles.add(identityFile.getJSON());

        componente.setNomeComponente(identityFile.getFileName());
//        TODO: da decommentare se si vuole inserire la dichiarazione del formato file nell'xml versato        
//        componente.setFormatoFileVersato(identityFile.getFormatType());
////        componente.setIDComponenteVersato(identityFile.getId());
////        componente.setIDComponenteVersato(String.valueOf(contatoreVersamento++));
////        componente.setIDComponenteVersato(id);

        if ((identityFile.getUuidMongo() != null) && (!"".equals(identityFile.getUuidMongo()))) {
            componente.setIDComponenteVersato(identityFile.getUuidMongo());
        }

//        componente.setUrnVersato("path_in_mongo");
        componente.setHashVersato(identityFile.getHash());
        componente.setUtilizzoDataFirmaPerRifTemp(Boolean.FALSE);

        componente.setRiferimentoTemporale(dataRiferimentoTemporale);
        componente.setDescrizioneRiferimentoTemporale(descrizioneRifTemporale);

        componenti.getComponente().add(componente);

        struttura.setComponenti(componenti);

        documentoPrincipale.setStrutturaOriginale(struttura);

        unitaDocumentaria.setDocumentoPrincipale(documentoPrincipale);
    }

    // tipoDocumento: se Annesso, Allegato, Annotazione
    public void addDocumentoSecondario(String id,
            String tipo,
            String autore,
            String descrizione,
            int ordinePresentazione,
            IdentityFile identityFile,
            String tipoStruttura,
            String tipoDocumento,
            String tipologiaComponente, //"Contenuto"
            String tipoSupportoComponente, //TipoSupportoType.FILE
            String dataRiferimentoTemporale,
            String descrizioneRifTemporale
    ) throws DatatypeConfigurationException, ParseException {

//        DocumentBuilder documento = new DocumentBuilder(id, 
//                tipo, ordinePresentazione, autore, descrizione, tipoStruttura, 
//                identityFile.getFileName(), identityFile.getId(), identityFile.getFormatType(), 
//                "urn:123", "hash", null, null, null);
        TipoSupportoType tipoSupportoType = null;

        if (tipoSupportoComponente.equalsIgnoreCase("FILE")) {
            tipoSupportoType = TipoSupportoType.FILE;
        } else if (tipoSupportoComponente.equalsIgnoreCase("RIFERIMENTO")) {
            tipoSupportoType = TipoSupportoType.RIFERIMENTO;
        } else if (tipoSupportoComponente.equalsIgnoreCase("METADATI")) {
            tipoSupportoType = TipoSupportoType.METADATI;
        }

        identityFiles.add(identityFile.getJSON());

        if (tipoDocumento.equalsIgnoreCase("Annesso")) {
            DocumentBuilder documento = new DocumentBuilder(id,
                    tipo, ordinePresentazione, autore, descrizione, tipoStruttura,
                    identityFile.getFileName(), identityFile.getUuidMongo(), identityFile.getFormatType(),
                    "urn:123", identityFile.getHash(), tipologiaComponente, tipoSupportoType, "FALSE", dataRiferimentoTemporale, descrizioneRifTemporale);

            addAnnesso(documento.getDoc(), identityFile, ordinePresentazione);
        } else if (tipoDocumento.equalsIgnoreCase("Allegato")) {
            DocumentBuilder documento = new DocumentBuilder(id,
                    tipo, ordinePresentazione, autore, descrizione, tipoStruttura,
                    identityFile.getFileName(), identityFile.getUuidMongo(), identityFile.getFormatType(),
                    "urn:123", identityFile.getHash(), tipologiaComponente, tipoSupportoType, "FALSE", dataRiferimentoTemporale, descrizioneRifTemporale);

            addAllegato(documento.getDoc(), identityFile, ordinePresentazione);
        } else if (tipoDocumento.equalsIgnoreCase("Annotazione")) {
            DocumentBuilder documento = new DocumentBuilder(id,
                    tipo, ordinePresentazione, autore, descrizione, tipoStruttura,
                    identityFile.getFileName(), identityFile.getUuidMongo(), identityFile.getFormatType(),
                    "urn:123", identityFile.getHash(), tipologiaComponente, tipoSupportoType, null, null, null);

            addAnnotazione(documento.getDoc(), identityFile, ordinePresentazione);
        } else {
            throw new IllegalArgumentException("Tipo Documento: " + tipoDocumento + "non trovato");
        }

    }

    public void addAllegato(DocumentoType a, IdentityFile pf, int ordinePresentazione) {
        if (allegati == null) {
            allegati = new Allegati();
            unitaDocumentaria.setAllegati(allegati);
        }
        a.getStrutturaOriginale().getComponenti().getComponente().get(0).setOrdinePresentazione(ordinePresentazione);
        a.getStrutturaOriginale().getComponenti().getComponente().get(0).setID(pf.getId().toString());

        allegati.getAllegato().add(a);
        unitaDocumentaria.setNumeroAllegati(allegati.getAllegato().size());
    }

    public void addAnnesso(DocumentoType a, IdentityFile pf, int ordinePresentazione) {
        if (annessi == null) {
            annessi = new Annessi();
            unitaDocumentaria.setAnnessi(annessi);
        }
        a.getStrutturaOriginale().getComponenti().getComponente().get(0).setOrdinePresentazione(ordinePresentazione);
        a.getStrutturaOriginale().getComponenti().getComponente().get(0).setID(pf.getId().toString());

        annessi.getAnnesso().add(a);
        unitaDocumentaria.setNumeroAnnessi(annessi.getAnnesso().size());

    }

    public void addAnnotazione(DocumentoType a, IdentityFile pf, int ordinePresentazione) {
        if (annotazioni == null) {
            annotazioni = new Annotazioni();
            unitaDocumentaria.setAnnotazioni(annotazioni);
        }
        a.getStrutturaOriginale().getComponenti().getComponente().get(0).setOrdinePresentazione(ordinePresentazione);
        a.getStrutturaOriginale().getComponenti().getComponente().get(0).setID(pf.getId().toString());

        annotazioni.getAnnotazione().add(a);
        unitaDocumentaria.setNumeroAnnotazioni(annotazioni.getAnnotazione().size());
    }

    private static GregorianCalendar buildCalendar(String data) throws ParseException {
        if (data == null) {
            return null;
        }
        GregorianCalendar ilCalendario = new GregorianCalendar();
        SimpleDateFormat laDataFacile = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        Date laData = laDataFacile.parse(data);

        ilCalendario.setTime(laData);
        return ilCalendario;

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

    public JSONArray getIdentityFiles() {
        return identityFiles;
    }

}
