package it.bologna.ausl.internauta.utils.sendintegration;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.model.entities.sendintegration.DocumentoLottoEntity;
import it.bologna.ausl.model.entities.sendintegration.QDocumentoLottoEntity;
import jakarta.persistence.EntityManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import org.openpdf.text.pdf.PdfReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class SendIntegrationUtils {
    private static final Logger log = LoggerFactory.getLogger(SendIntegrationUtils.class);
    
    public static String getSha256Base64Encoded(File file) throws IOException, NoSuchAlgorithmException {
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] sha256 = digest.digest(fileBytes);

        String base64Sha256 = Base64.getEncoder().encodeToString(sha256);

        return base64Sha256;
    }
    
    public static String getSha256Base64EncodedFromSha256Hex(String sha256Hex) {
        byte[] bytes = HexFormat.of().parseHex(sha256Hex);
        String base64Sha256 = Base64.getEncoder().encodeToString(bytes);
        return base64Sha256;
    }
    
    public static boolean isPdf(File file) {
        try (PdfReader reader = new PdfReader(file.getAbsolutePath())) {
            reader.close();
            return true;
        } catch (IOException ex) {
            log.error("errore nella lettura del file pdf", ex);
            return false;
        }
    }
    
      
    public static long updateDocumentiLotto(String paId, String lottoId, DocumentoLottoEntity.DocumentiLottoStatus status, EntityManager entityManager) {
        QDocumentoLottoEntity qDocumentoLottoEntity = QDocumentoLottoEntity.documentoLottoEntity;
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        long updatedRows = queryFactory
            .update(qDocumentoLottoEntity)
            .set(qDocumentoLottoEntity.status, status)
            .where(
                qDocumentoLottoEntity.paId.eq(paId).and(
                qDocumentoLottoEntity.lottoId.eq(lottoId))
            )
            .execute();
        return updatedRows;
    }
}
