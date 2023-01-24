/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.parer;

import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.plugins.infocert.InfocertVersatoreService;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import it.bologna.ausl.riversamento.builder.ProfiloArchivistico;
import it.bologna.ausl.riversamento.builder.UnitaDocumentariaBuilder;
import it.bologna.ausl.riversamento.builder.oggetti.DatiSpecifici;
import java.util.List;
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
    
//    
    @Override
    protected VersamentoDocInformation versaImpl(VersamentoDocInformation versamentoInformation) throws VersatoreProcessingException {
        Integer idDoc = versamentoInformation.getIdDoc();
        String idUnitaDocumentaria, tipoDocumento, dataDocumento;
        String forzaCollegamento, forzaAccettazione, forzaConservazione;
        DatiSpecifici datiSpecifici;
        ProfiloArchivistico profiloArchivistico;
        Doc doc = entityManager.find(Doc.class, idDoc);
        DocDetail docDetail = entityManager.find(DocDetail.class, idDoc);
        tipoDocumento = doc.getTipologia().toString();
        
        
        log.info("setto le info del documento");
        idUnitaDocumentaria = doc.getId().toString();
//        UnitaDocumentariaBuilder ud = new UnitaDocumentariaBuilder(docDetail.getNumeroRegistrazione().toString(), docDetail.getAnnoRegistrazione(), 'DETE', tipoDocumento, "true", "true",  "true", profiloArchivistico, doc.getOggetto(), docDetail.getDataRegistrazione().toString(), datiSpecifici, "1", "AUSl", "AUSL", "AUSL", "AOSP", "true", "UTF8");
        
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
//    public buildProfiloArchivistico(){
//        List<ArchivioDoc> archiviazioni = entityManager.createNativeQuery("select  ")
//        ProfiloArchivistico profiloArch ;
//        
//        profiloArch.addFascicoloPrincipale(classifica, anno, numeroFascicolo, oggetto, numeroSottoFascicolo, oggettoSottoFascicolo, numeroInserto, oggettoInserto);
//    }
    
   
    
}
