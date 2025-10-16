package it.bologna.ausl.internauta.utils.ribaltone.finalChecks;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.ribaltonedati.checks.QRibaltoneValidationCheck;
import it.bologna.ausl.model.entities.ribaltonedati.checks.RibaltoneValidationCheck;
import it.bologna.ausl.model.entities.ribaltonedati.checks.RisultatiErrati;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;

/**
 *
 * @author Top
 */
public class QueryChecks {

    public static void confomalsDataChecks(RepositoryFactory repositoryFactory, String codiceAzienda) {

        List<RibaltoneValidationCheck> findCheckActiveByCodiceAzienda = repositoryFactory.getRibaltoneValidationCheckRepository().findCheckActiveByCodiceAzienda(codiceAzienda);

        for (RibaltoneValidationCheck ribaltoneValidationCheck : findCheckActiveByCodiceAzienda) {

            switch (ribaltoneValidationCheck.getRegolaDiSuccesso()) {
                case ZERO_ROWS -> {
                    String query = ribaltoneValidationCheck.getQuery();
                    query = query.replaceAll(":codiceAzienda", "'" + codiceAzienda + "'");
                    List<Map<String, Object>> res = repositoryFactory.getJdbcTemplate().queryForList(query);
                    RisultatiErrati risultatiErrati = ribaltoneValidationCheck.getRisultatiErrati();
                    if (risultatiErrati == null) {
                        risultatiErrati = new RisultatiErrati();
                        ribaltoneValidationCheck.setRisultatiErrati(risultatiErrati);
                    }
                    if (res != null && !res.isEmpty()) {
                        risultatiErrati.addRisultatiAzienda(codiceAzienda, res);
                    } else {
                        risultatiErrati.removeRisultatiAzienda(codiceAzienda);
                    }
                    repositoryFactory.getTransactionTemplate().setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    repositoryFactory.getTransactionTemplate().executeWithoutResult(action -> {
                        QRibaltoneValidationCheck qRibaltoneValidationCheck = QRibaltoneValidationCheck.ribaltoneValidationCheck;
                        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
                        jPAQueryFactory
                            .update(qRibaltoneValidationCheck)
                            .set(qRibaltoneValidationCheck.risultatiErrati, ribaltoneValidationCheck.getRisultatiErrati())
                            .where(qRibaltoneValidationCheck.id.eq(ribaltoneValidationCheck.getId()))
                            .execute();

                    });
                    if (res != null && !res.isEmpty()) {
                        throw new RibaltoneHttpException("query di controllo ha ritornato dei risultati " + query);
                    }

                }

                default ->
                    throw new AssertionError();
            }

        }
    }
}
