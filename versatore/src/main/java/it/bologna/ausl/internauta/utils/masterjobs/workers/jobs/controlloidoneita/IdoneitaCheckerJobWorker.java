package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.controlloidoneita;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.versatore.VersatoreFactory;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.IdoneitaChecker;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDetail;
import it.bologna.ausl.model.entities.scripta.QDoc;
import it.bologna.ausl.model.entities.scripta.QDocDetail;
import it.bologna.ausl.model.entities.versatore.Versamento;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;

/**
 *
 * @author gdm
 * 
 * Job che effettua i controlli di idoneità sugli archivi e sui docs
 * Dai parametri del job legge se l'idoneità va controllata sugli archivi, sui docs o su entrambi
 * Il job estrai i candidati per il controllo dell'idoneità e delega il controllo vero e proprio al plugin
 * Per gli archivi i candidati sono quelli chiusi e di livello 1 (fascicoli)
 * Per i doc i candidati sono solo quelli registrati (che hanno il numero di registazione)
 */
@MasterjobsWorker
public class IdoneitaCheckerJobWorker  extends JobWorker<IdoneitaCheckerJobWorkerData, JobWorkerResult> {
    private static final Logger log = LoggerFactory.getLogger(IdoneitaCheckerJobWorker.class);
    
    @Autowired
    private VersatoreFactory versatoreFactory;
    
    @Override
    protected JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        IdoneitaChecker idoneitaCheckerInstance;
        try {
            idoneitaCheckerInstance = versatoreFactory.getIdoneitaCheckerInstance(getWorkerData().getHostId());
        } catch (VersatoreProcessingException ex) {
            String errorMessage = "errore nel reperire il plugin di versamento";
            log.error(errorMessage, ex);
            throw new MasterjobsWorkerException(errorMessage, ex);
        }
        
        if (getWorkerData().getControllaArchivi()) {
            List<Integer> archiviDaControllare = getArchiviDaControllare();
            if (archiviDaControllare != null && !archiviDaControllare.isEmpty()) {
                Boolean isIdoneo = false;
                for (Integer idArchivio : archiviDaControllare) {
                    try {
                        isIdoneo = idoneitaCheckerInstance.checkArchivio(idArchivio);
                    } catch (Throwable ex) {
                        String errorMessage = String.format("errore nel plugin di idoneitaCheck sull'archivio %s", idArchivio);
                        log.error(errorMessage, ex);
                        //TODO: compilare report checker
                    }
                    if (isIdoneo) {
                        setStatoArchivio(idArchivio, Versamento.StatoVersamento.VERSARE);
                    }
                }
            }
        }
        
        if (getWorkerData().getControllaDocs()) {
            List<Integer> docsDaControllare = getDocsDaControllare();
            if (docsDaControllare != null && !docsDaControllare.isEmpty()) {
                Boolean isIdoneo = false;
                for (Integer idDoc : docsDaControllare) {
                    try {
                        isIdoneo = idoneitaCheckerInstance.checkDoc(idDoc);
                    } catch (Throwable ex) {
                        String errorMessage = String.format("errore nel plugin di idoneitaCheck sul doc %s", idDoc);
                        log.error(errorMessage, ex);
                        //TODO: compilare report checker
                    }
                    if (isIdoneo) {
                        setStatoDoc(idDoc, Versamento.StatoVersamento.VERSARE);
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Setta lo stato versamento passato sull'archivio con id passato
     * @param idArchivio l'id dell'archivio su cui settare lo stato versamento
     * @param statoVersamento lo stato versamento da settare
     */
    private void setStatoArchivio(Integer idArchivio, Versamento.StatoVersamento stato) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QArchivio qArchivio = QArchivio.archivio;
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(a -> {
            queryFactory
                .update(qArchivio)
                .set(qArchivio.statoVersamento, stato.toString())
                .where(qArchivio.id.eq(idArchivio))
                .execute();
        });
    }
    
    /**
     * Setta lo stato versamento passato sul doc con id passato
     * @param idDoc l'id del doc su cui settare lo stato versamento
     * @param statoVersamento lo stato versamento da settare
     */
    private void setStatoDoc(Integer idDoc, Versamento.StatoVersamento statoVersamento) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QDoc qDoc = QDoc.doc;
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(a -> {
            queryFactory
                .update(qDoc)
                .set(qDoc.statoVersamento, statoVersamento.toString())
                .where(qDoc.id.eq(idDoc))
                .execute();
        });
    }
    
    /**
     * Reperisce gli archivi candidati al controllo di idoneità
     * Sono tutti gli archivi di livello 1 (fascicoli) che non sono mai stati versati e sono chiusi
     * @return la lista degli id archivi candidati per il controllo di idoneità
     */
    private List<Integer> getArchiviDaControllare() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QArchivioDetail qArchivioDetail = QArchivioDetail.archivioDetail;
        List<Integer> archiviDaControllare = queryFactory
            .select(qArchivioDetail.id)
            .from(qArchivioDetail)
            .where(
                qArchivioDetail.id.eq(1116221).and(
                qArchivioDetail.livello.eq(1).and(
                qArchivioDetail.statoUltimoVersamento.isNull().and(
                qArchivioDetail.stato.eq(Archivio.StatoArchivio.CHIUSO.toString())))
                )
            )
            .fetch();
        return archiviDaControllare;
    }
    
    /**
     * Reperisce i docs candidati al controllo di idoneità
     * Sono tutti i docs che non sono mai stati versati e sono stati registratri
     * @return la lista degli id doc candidati per il controllo di idoneità
     */
    private List<Integer> getDocsDaControllare() {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QDocDetail qDocDetail = QDocDetail.docDetail;
        List<Integer> docsDaControllare = queryFactory
            .select(qDocDetail.id)
            .from(qDocDetail)
            .where(
                qDocDetail.statoUltimoVersamento.isNull().and(
                qDocDetail.numeroRegistrazione.isNotNull())
            )
            .fetch();
        return docsDaControllare;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();        
    }
    
}
