package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.masterjobs.annotations.MasterjobsWorker;
import it.bologna.ausl.internauta.utils.masterjobs.exceptions.MasterjobsWorkerException;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorker;
import it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.JobWorkerResult;
import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.VersatoreFactory;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreConfigurationException;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.QAllegato;
import it.bologna.ausl.model.entities.scripta.QArchivio;
import it.bologna.ausl.model.entities.scripta.QArchivioDoc;
import it.bologna.ausl.model.entities.scripta.QDoc;
import it.bologna.ausl.model.entities.versatore.QVersamento;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;

/**
 *
 * @author gdm
 */
@MasterjobsWorker
public class VersatoreJobWorker extends JobWorker<VersatoreJobWorkerData> {
    private static final Logger log = LoggerFactory.getLogger(VersatoreJobWorker.class);
    
    @Autowired
    private VersatoreFactory versatoreFactory;
    
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
    
    @Override
    public JobWorkerResult doRealWork() throws MasterjobsWorkerException {
        log.info("sono in doWork()");
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        VersatoreDocs versatoreDocsInstance = versatoreFactory.getVersatoreDocsInstance(getWorkerData().getHostId());
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        
        Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare = new HashMap<>();
        versamentiDaEffettuare = addDocsDaVersareDaArchivi(versamentiDaEffettuare, queryFactory);
        versamentiDaEffettuare = addDocsDaVersareDaDocs(versamentiDaEffettuare, queryFactory);
        versamentiDaEffettuare = addDocsDaRitentare(versamentiDaEffettuare, queryFactory);
    }
    
    private Map<Integer, List<VersamentoDocInformation>> addDocsDaRitentare(Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare, JPAQueryFactory queryFactory) {
    List<Tuple> versamentiRitentabili = queryFactory
                .select(QVersamento.versamento.id, QVersamento.versamento.idDoc.id, QVersamento.versamento.idArchivio.id)
                .from(QVersamento.versamento)
                .where(QVersamento.versamento.stato.eq(Versamento.StatoVersamento.ERRORE_RITENTABILE.toString()).and(
                    QVersamento.versamento.ignora.eq(false))
                )
                .fetch();
        for (Tuple versamentoRitentabile : versamentiRitentabili) {
            Integer idVersamento = versamentoRitentabile.get(QVersamento.versamento.id);
            Integer idDoc = versamentoRitentabile.get(QVersamento.versamento.idDoc.id);
            Integer idArchivio = versamentoRitentabile.get(QVersamento.versamento.idArchivio.id);
            List<VersamentoDocInformation> versamenti = versamentiDaEffettuare.get(idDoc);
            boolean versamentoDaInserire = true;
            if (versamenti == null) {
                versamenti = new ArrayList<>();
                versamentiDaEffettuare.put(idDoc, versamenti);
            } else {
                for (VersamentoDocInformation v : versamenti) {
                    if ((idArchivio == null && v.getIdArchivio() == null) || 
                        (idArchivio != null && v.getIdArchivio() != null && v.getIdArchivio().equals(idArchivio))) {
                        v.setVersamentoPrecedente(idVersamento);
                        versamentoDaInserire = false;
                        break;
                    }
                }
            }
            
            if (versamentoDaInserire) {
                VersamentoDocInformation versamentoDocInformation = buildVersamentoDocInformation(idDoc, idArchivio, SessioneVersamento.TipologiaVersamento.GIORNALIERO_DOCUMENTI);
                versamenti.add(versamentoDocInformation);
            }
        }
        
        return versamentiDaEffettuare;
    }
    
    private Map<Integer, List<VersamentoDocInformation>> addDocsDaVersareDaDocs(Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare, JPAQueryFactory queryFactory) {
        QDoc qDoc = QDoc.doc;
        BooleanExpression filter = 
            qDoc.statoVersamento.isNotNull().and(
            qDoc.statoVersamento.in(
                    Arrays.asList(
                            Versamento.StatoVersamento.VERSARE.toString(), 
                            Versamento.StatoVersamento.AGGIORNARE.toString()))
            );
        List<Integer> docsIdDaVersare = queryFactory.select(qDoc.id).from(qDoc).where(filter).fetch();
        
        for (Integer docIdDaVersare : docsIdDaVersare) {
            VersamentoDocInformation versamentoDocInformation = buildVersamentoDocInformation(docIdDaVersare, null, SessioneVersamento.TipologiaVersamento.GIORNALIERO_DOCUMENTI);
            List<VersamentoDocInformation> versamenti = versamentiDaEffettuare.get(docIdDaVersare);
            if (versamenti == null) {
                versamenti = new ArrayList<>();
                versamentiDaEffettuare.put(docIdDaVersare, versamenti);
            }
            versamenti.add(versamentoDocInformation);
        }
        return versamentiDaEffettuare;
    }
    
    private Map<Integer, List<VersamentoDocInformation>> addDocsDaVersareDaArchivi(Map<Integer, List<VersamentoDocInformation>> versamentiDaEffettuare, JPAQueryFactory queryFactory) {
        
        QArchivio qArchivio = QArchivio.archivio;
        BooleanExpression filterArchiviDaVersare = 
            qArchivio.statoVersamento.isNotNull().and(
            qArchivio.statoVersamento.in(
                    Arrays.asList(
                            Versamento.StatoVersamento.VERSARE.toString(), 
                            Versamento.StatoVersamento.AGGIORNARE.toString(), 
                            Versamento.StatoVersamento.ERRORE_RITENTABILE.toString()))
            );
        
        List<Integer> archiviIdDaVersare = queryFactory.select(qArchivio.id).from(qArchivio).where(filterArchiviDaVersare).fetch();
        for (Integer idArchivioDaVersare : archiviIdDaVersare) {
            QArchivioDoc qArchivioDoc = QArchivioDoc.archivioDoc;
            List<Integer> docsIdDaVersare = queryFactory
                    .select(qArchivioDoc.idDoc.id)
                    .from(qArchivioDoc)
                    .where(qArchivioDoc.idArchivio.id.eq(idArchivioDaVersare)).fetch();
            for (Integer docIdDaVersare : docsIdDaVersare) {
                VersamentoDocInformation versamentoDocInformation = buildVersamentoDocInformation(docIdDaVersare, idArchivioDaVersare, SessioneVersamento.TipologiaVersamento.GIORNALIERO_FASCICOLI);
                List<VersamentoDocInformation> versamentiDoc = versamentiDaEffettuare.get(docIdDaVersare);
                if (versamentiDoc == null) {
                    versamentiDoc = new ArrayList<>();
                    versamentiDaEffettuare.put(docIdDaVersare, versamentiDoc);
                }
                versamentiDoc.add(versamentoDocInformation);
//versaDoc(versatoreDocsInstance, queryFactory, docIdDaVersare, idArchivioDaVersare, SessioneVersamento.TipologiaVersamento.GIORNALIERO_FASCICOLI);
;
            }
        }
        
        return versamentiDaEffettuare;
    }
    
    private VersamentoDocInformation buildVersamentoDocInformation(Integer idDoc, Integer idArchivio, SessioneVersamento.TipologiaVersamento tipologiaVersamento) {
        VersamentoDocInformation versamentoDocInformation = new VersamentoDocInformation();
        versamentoDocInformation.setTipologiaVersamento(tipologiaVersamento);
        versamentoDocInformation.setIdDoc(idDoc);
        versamentoDocInformation.setIdArchivio(idArchivio);
        return versamentoDocInformation;
    }
    
    private VersamentoDocInformation versaDoc(VersatoreDocs versatoreDocsInstance, JPAQueryFactory queryFactory, Integer idDoc, Integer idArchivio, SessioneVersamento.TipologiaVersamento tipologiaVersamento) {
        VersamentoDocInformation versamentoDocInformation = new VersamentoDocInformation();
        versamentoDocInformation.setTipologiaVersamento(tipologiaVersamento);
        versamentoDocInformation.setIdDoc(idDoc);
        versamentoDocInformation.setIdArchivio(idArchivio);
        
        VersamentoDocInformation statoVersamentoRes;
        try {
            statoVersamentoRes = versatoreDocsInstance.versa(versamentoDocInformation);
        } catch (Throwable ex) {
            statoVersamentoRes = versamentoDocInformation;
            statoVersamentoRes.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
        }
        updateStatoDoc(queryFactory, idDoc, statoVersamentoRes);
        return statoVersamentoRes;
    }
    
    private void updateStatoDoc(JPAQueryFactory queryFactory, Integer idDoc, VersamentoDocInformation versamentoDocInformation) {       
        transactionTemplate.executeWithoutResult(a -> {
            Versamento.StatoVersamento statoVersamentoDoc = versamentoDocInformation.getStatoVersamento();
            for (VersamentoAllegatoInformation versamentoAllegatoInformation : versamentoDocInformation.getVeramentiAllegatiInformations()) {
                Allegato allegato = entityManager.find(Allegato.class, versamentoAllegatoInformation.getIdAllegato());
                Allegato.DettaglioAllegato dettaglioAllegato = allegato.getDettagli().getByKey(versamentoAllegatoInformation.getTipoDettaglioAllegato());
                dettaglioAllegato.setStatoVersamento(versamentoAllegatoInformation.getStatoVersamento());
                statoVersamentoDoc = getStatoVersamentoDocFromStatoVersamentoAllegato(statoVersamentoDoc, versamentoAllegatoInformation.getStatoVersamento());
                entityManager.persist(allegato);
            }
        
            queryFactory
                .update(QDoc.doc)
                .set(QDoc.doc.statoVersamento, statoVersamentoDoc != null ? statoVersamentoDoc.toString(): null)
                .where(QDoc.doc.id.eq(idDoc));
            });
    }
    
    private Versamento.StatoVersamento getStatoVersamentoDocFromStatoVersamentoAllegato(Versamento.StatoVersamento statoVersamentoDocAttuale, Versamento.StatoVersamento statoVersamentoAllegato) {
        if (statoVersamentoDocAttuale == null) {
            statoVersamentoDocAttuale = statoVersamentoAllegato;
        } else if (statoVersamentoAllegato == Versamento.StatoVersamento.ERRORE_RITENTABILE) {
            statoVersamentoDocAttuale = Versamento.StatoVersamento.ERRORE_RITENTABILE;
        } else if (statoVersamentoDocAttuale == Versamento.StatoVersamento.IN_CARICO) {
            if (statoVersamentoAllegato == Versamento.StatoVersamento.ERRORE) {
                statoVersamentoDocAttuale = Versamento.StatoVersamento.IN_CARICO_CON_ERRORI;
            }
        } else if (statoVersamentoAllegato == Versamento.StatoVersamento.ERRORE || statoVersamentoAllegato == Versamento.StatoVersamento.ERRORE_FORZABILE) {
            statoVersamentoDocAttuale = statoVersamentoAllegato;
        }
        
        return statoVersamentoDocAttuale;
    }
}
