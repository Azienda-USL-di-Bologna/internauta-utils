package it.bologna.ausl.internauta.utils.versatore.plugins.parer;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.IdoneitaChecker;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.DocDetail;
import static it.bologna.ausl.model.entities.scripta.DocDetailInterface.TipologiaDoc.PROTOCOLLO_IN_ENTRATA;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class ParerIdoneitaCheckerService extends IdoneitaChecker {

    @Override
    public Boolean checkDocImpl(Integer id, Map<String,Object> params) throws VersatoreProcessingException {
        
        Doc doc = entityManager.find(Doc.class, id);
        DocDetail docDetail = entityManager.find(DocDetail.class, id);
        Boolean idoneo = true;

        /*Faccio :
        - un controllo degli ultimi giorni dalla registrazione, leggendo quanti devono essere dal db
        - controllo se è stato fascicolato*/
        if(params.get("giorniPrimaDiVersarePe")!= null && params.get("giorniPrimaDiVersarePu")!=null && params.get("giorniPrimaDiVersareDeli") !=null && params.get("giorniPrimaDiVersareDete") != null){
            
            Integer giorniPrimaDiVersarePe = Integer.parseInt((String) params.get("giorniPrimaDiVersarePe"));
            Integer giorniPrimaDiVersarePu = Integer.parseInt((String) params.get("giorniPrimaDiVersarePu"));
            Integer giorniPrimaDiVersareDeli = Integer.parseInt(params.get("giorniPrimaDiVersareDeli").toString());
            Integer giorniPrimaDiVersareDete = Integer.parseInt((String) params.get("giorniPrimaDiVersareDete"));
            
            JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(entityManager);
         
            switch (doc.getTipologia()) {
                case DELIBERA:
//                    oggi meno i giorni del parametro non sono prima della data regitrazione, significa che sono passati 7 giorni almeno
                    if(ZonedDateTime.now().minusDays(giorniPrimaDiVersareDeli).isBefore(docDetail.getDataRegistrazione())){ 
                        idoneo = false;
                    }
                    break;
                case DETERMINA:
                    if(ZonedDateTime.now().minusDays(giorniPrimaDiVersareDete).isBefore(docDetail.getDataRegistrazione())){ 
                        idoneo = false;
                    }
                    break;
                case PROTOCOLLO_IN_USCITA:
                    if(ZonedDateTime.now().minusDays(giorniPrimaDiVersarePu).isBefore(docDetail.getDataRegistrazione())){ 
                        idoneo = false;
                    }
                    break;
                case PROTOCOLLO_IN_ENTRATA:
                    if(ZonedDateTime.now().minusDays(giorniPrimaDiVersarePe).isBefore(docDetail.getDataRegistrazione())){ 
                        idoneo = false;
                    }
                    break;
                case RGPICO:
                    idoneo = true;
                    
                    break;
                default: 
                    break;
            }
            
            List<ArchivioDoc> archiviazioni = jPAQueryFactory
                .select(QArchivioDoc.archivioDoc)
                .from(QArchivioDoc.archivioDoc)
                .where(QArchivioDoc.archivioDoc.idDoc.id.eq(doc.getId()).and(QArchivioDoc.archivioDoc.dataEliminazione.isNull()))
                .orderBy(QArchivioDoc.archivioDoc.dataArchiviazione.asc())
                .fetch();
            Map<String,Object> mappaUnitaDocumentaria = new HashMap<>();
        
            if(archiviazioni == null || archiviazioni.isEmpty()) {
                idoneo = false;
            }
            
        } else {
            idoneo = false;
        }
        
        return idoneo;
    }

    @Override
    public Boolean checkArchivioImpl(Integer id, Map<String,Object> params) throws VersatoreProcessingException {
        throw new UnsupportedOperationException("Non supportato dal plugin Parer, spegnere il parametro aziendale");
    }

}
