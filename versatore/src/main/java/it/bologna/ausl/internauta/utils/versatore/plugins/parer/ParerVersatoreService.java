/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.parer;

import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.plugins.infocert.InfocertVersatoreService;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.AttoreDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.model.entities.scripta.DocDetailInterface;
import it.bologna.ausl.riversamento.builder.DatiSpecificiBuilder;
import it.bologna.ausl.riversamento.builder.IdentityFile;
import it.bologna.ausl.riversamento.builder.InfoDocumento;
import it.bologna.ausl.riversamento.builder.ProfiloArchivistico;
import it.bologna.ausl.riversamento.builder.oggetti.DatiSpecifici;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author utente
 */
@Component
public class ParerVersatoreService extends VersatoreDocs{

    private static final Logger log = LoggerFactory.getLogger(InfocertVersatoreService.class);
    
    @Autowired
    private ParametriAziendeReader parametriAziende;
    
    @Autowired
    private ParerVersatoreMetadatiBuilder parerVersatoreMetadatiBuilder;
    
//    
    @Override
    protected VersamentoDocInformation versaImpl(VersamentoDocInformation versamentoInformation) throws VersatoreProcessingException {
        Integer idDoc = versamentoInformation.getIdDoc();
        String idUnitaDocumentaria, tipoDocumento, dataDocumento;
        String forzaCollegamento, forzaAccettazione, forzaConservazione;
        
        Doc doc = entityManager.find(Doc.class, idDoc);
        DocDetail docDetail = entityManager.find(DocDetail.class, idDoc);
        String enteVersamento = (String) versamentoInformation.getParams().get("ente");
        String userID = (String) versamentoInformation.getParams().get("userID");
        String ambiente = (String) versamentoInformation.getParams().get("ambiente");
        String struttura = (String) versamentoInformation.getParams().get("struttura");
        
        
//        UnitaDocumentariaBuilder ud = new UnitaDocumentariaBuilder(docDetail.getNumeroRegistrazione().toString(), docDetail.getAnnoRegistrazione(), 'DETE', tipoDocumento, "true", "true",  "true", profiloArchivistico, doc.getOggetto(), docDetail.getDataRegistrazione().toString(), datiSpecifici, "1", "AUSl", "AUSL", "AUSL", "AOSP", "true", "UTF8");
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
   
    
   
    
}
