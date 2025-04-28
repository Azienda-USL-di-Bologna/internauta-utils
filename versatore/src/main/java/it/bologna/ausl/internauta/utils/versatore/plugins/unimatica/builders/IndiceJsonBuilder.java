package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.builders;

import it.bologna.ausl.internauta.utils.versatore.plugins.unimatica.entities.AllegatoUnimatica;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatorePluginException;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.Registro;
import it.bologna.ausl.model.entities.scripta.RegistroDoc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author boria
 */
public class IndiceJsonBuilder {

    private static final Logger log = LoggerFactory.getLogger(IndiceJsonBuilder.class);

    private Doc doc;
    private AllegatoUnimatica documentoPrincipale;
    private List<AllegatoUnimatica> allegatiSecondariList;
    private Map<String, Object> parametriVersamento;
    private String sha256HexMetadati;

    public IndiceJsonBuilder(Map<String, Object> parametriVersamento, Doc doc, AllegatoUnimatica documentoPrincipale, List<AllegatoUnimatica> allegatiSecondariList, String sha256HexMetadati) {
        this.doc = doc;
        this.documentoPrincipale = documentoPrincipale;
        this.allegatiSecondariList = allegatiSecondariList;
        this.parametriVersamento = parametriVersamento;
        this.sha256HexMetadati = sha256HexMetadati;
    }

    /**
     * Metodo che costruisce l'indice json
     *
     * @return
     */
    public Map<String, Object> build() throws VersatorePluginException {
        Map<String, Object> jsonMap = new HashMap<>();
        Map<String, Object> profilo = new HashMap<>();
        //tenant
        //TODO de hardcodare
        String tenant = "testTenant";
        profilo.put("tenant", tenant);
        //classe documentale
        //Prendo il registro ufficiale e attivo del documento
        List<RegistroDoc> registroDocList = doc.getRegistroDocList();
        Registro registro = null;
        RegistroDoc registroDocDocumento = null;
        for (RegistroDoc registroDoc : registroDocList) {
            if (registroDoc.getIdRegistro().getAttivo() && registroDoc.getIdRegistro().getUfficiale()) {
                registro = registroDoc.getIdRegistro();
                registroDocDocumento = registroDoc;
                break;
            }
        }
        if (registro == null) {
            throw new VersatorePluginException("Non è presente un registro ufficiale e attivo per il documento con id " + doc.getId());
        }
        profilo.put("classeDocumentale", registro.getDescrizione());
        jsonMap.put("profilo", profilo);
        List<Map<String, Object>> documentiList = new ArrayList<>();
        Map<String, Object> documento = new HashMap<>();
        //id del documento
        documento.put("id", doc.getId());
        Map<String, Object> chiave = new HashMap<>();
        //numero registro
        chiave.put("numero", registroDocDocumento.getNumero());
        //anno registro
        chiave.put("anno", registroDocDocumento.getAnno());
        //registro
        chiave.put("registro", registro.getCodice());
        documento.put("chiave", chiave);
        //nome file
        documento.put("nomeFile", documentoPrincipale.getNomeFile());
        //formato file
        documento.put("formatoFile", documentoPrincipale.getFormato());
        Map<String, Object> hashDocumentoPrincipale = new HashMap<>();
        //hash doc principale
        hashDocumentoPrincipale.put("impronta", documentoPrincipale.getImpronta());
        hashDocumentoPrincipale.put("codifica", parametriVersamento.get("codifica"));
        hashDocumentoPrincipale.put("algoritmo", parametriVersamento.get("algoritmo"));
        documento.put("hash", hashDocumentoPrincipale);
        //dati dei metadati
        Map<String, Object> metadati = new HashMap<>();
        metadati.put("nomeFile", doc.getId().toString() + ".xml");
        Map<String, Object> hashMetadati = new HashMap<>();
        hashMetadati.put("impronta", sha256HexMetadati);
        hashMetadati.put("codifica", parametriVersamento.get("codifica"));
        hashMetadati.put("algoritmo", parametriVersamento.get("algoritmo"));
        metadati.put("hash", hashMetadati);
        //parametri
        Map<String, Object> parametriDocumento = new HashMap();
        //TODO vedere come impostarli - da parametri db?, forse se è firmato o meno...
        parametriDocumento.put("aggiungiFirma", (boolean) parametriVersamento.get("aggiungiFirma"));
        parametriDocumento.put("verificaFirma", documentoPrincipale.getFirmato());
        documento.put("parametriDocumento", parametriDocumento);
        //allegati
        List<Map<String, Object>> allegatiList = new ArrayList<>();
        for (AllegatoUnimatica allegatoUnimaticaSecondario : allegatiSecondariList) {
            Map<String, Object> allegato = new HashMap<>();
            allegato.put("id", allegatoUnimaticaSecondario.getIdFile());
            allegato.put("nomeFile", allegatoUnimaticaSecondario.getNomeFile());
            allegato.put("formatoFile", allegatoUnimaticaSecondario.getFormato());
            Map<String, Object> hashAllegato = new HashMap<>();
            hashAllegato.put("impronta", allegatoUnimaticaSecondario.getImpronta());
            hashAllegato.put("codifica", parametriVersamento.get("codifica"));
            hashAllegato.put("algoritmo", parametriVersamento.get("algoritmo"));
            allegato.put("hash", hashAllegato);
            //TODO non penso di passare l'xml per ogni allegato
            Map<String, Object> parametriAllegato = new HashMap();
            //TODO vedere come impostarli - da parametri db?, forse se è firmato o meno...
            parametriAllegato.put("aggiungiFirma", allegatoUnimaticaSecondario.getFirmato());
            parametriAllegato.put("verificaFirma", allegatoUnimaticaSecondario.getFirmato());
            allegato.put("parametriDocumento", parametriAllegato);
            allegatiList.add(allegato);
        }
        documento.put("allegati", allegatiList);
        documentiList.add(documento);
        jsonMap.put("documenti", documentiList);

        return jsonMap;
    }

}
