package it.bologna.ausl.riversamento.builder;

import it.bologna.ausl.riversamento.builder.oggetti.ComponenteType;
import it.bologna.ausl.riversamento.builder.oggetti.DocumentoType;
import it.bologna.ausl.riversamento.builder.oggetti.ProfiloDocumentoType;
import it.bologna.ausl.riversamento.builder.oggetti.StrutturaType;
import it.bologna.ausl.riversamento.builder.oggetti.TipoSupportoType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

/**
 *
 * @author andrea
 */
public class DocumentBuilder {

    private final DocumentoType doc;

    @SuppressWarnings("empty-statement")
    public DocumentBuilder(String idDocumento,
            String tipoDocumento,
            int indicePresentazione,
            String autore,
            String descrizione,
            String tipoStruttura,
            String nomeFile,
            String idVersato,
            String formatoFile,
            String URNVersato,
            String hash,
            String tipologiaComponente,//"Contenuto"
            TipoSupportoType tipoSupportoComponente, //TipoSupportoType.FILE   
            String utilizzoDataFirmaPerRifTemporale,
            String riferimentoTemporale,
            String descRifTemporale) throws DatatypeConfigurationException, ParseException {

        doc = new DocumentoType();
        doc.setIDDocumento(idDocumento);
        doc.setTipoDocumento(tipoDocumento);

        ProfiloDocumentoType profiloDocumento = new ProfiloDocumentoType();

        if (autore != null && !"".equals(autore)) {
            profiloDocumento.setAutore(autore);
        }

        profiloDocumento.setDescrizione(descrizione);
        doc.setProfiloDocumento(profiloDocumento);

        StrutturaType struttura = new StrutturaType();
        struttura.setTipoStruttura(tipoStruttura);
        StrutturaType.Componenti componenti = new StrutturaType.Componenti();

        ComponenteType componente = new ComponenteType();
        componente.setTipoComponente(tipologiaComponente);
        componente.setID(String.valueOf(indicePresentazione));
        componente.setTipoSupportoComponente(tipoSupportoComponente);
        componente.setNomeComponente(nomeFile);
//        TODO: da decommentare se si vuole inserire la dichiarazione del formato file nell'xml versato
//        componente.setFormatoFileVersato(formatoFile);

        if ((idVersato != null) && (!"".equals(idVersato))) {
            componente.setIDComponenteVersato(idVersato);
        }

////        if((URNVersato != null) && (!"".equals(URNVersato))){
////            componente.setUrnVersato(URNVersato);
////        }
////        
        if ((hash != null) && (!"".equals(hash))) {
            componente.setHashVersato(hash);
        }

        if ((utilizzoDataFirmaPerRifTemporale != null) && (!"".equals(utilizzoDataFirmaPerRifTemporale))) {
            componente.setUtilizzoDataFirmaPerRifTemp(((utilizzoDataFirmaPerRifTemporale.equalsIgnoreCase("true")) ? Boolean.TRUE : Boolean.FALSE));
        }

        //data di registrazione di protocollo
        if ((riferimentoTemporale != null) && (!"".equals(riferimentoTemporale))) {
            componente.setRiferimentoTemporale(DatatypeFactory.newInstance().newXMLGregorianCalendar(buildCalendar(riferimentoTemporale)));
        }

        // data di protocollazione
        if ((descRifTemporale != null) && (!"".equals(descRifTemporale))) {
            componente.setDescrizioneRiferimentoTemporale(descRifTemporale);
        }

        componenti.getComponente().add(componente);
        struttura.setComponenti(componenti);
        doc.setStrutturaOriginale(struttura);
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

    public DocumentoType getDoc() {
        return doc;
    }

}
