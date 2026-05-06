package it.bologna.ausl.internauta.utils.versatore.utils;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.versatore.exceptions.http.VersatoreHttpException;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.tools.QSupportedFile;
import it.bologna.ausl.model.entities.tools.SupportedFile;
import jakarta.persistence.EntityManager;
import java.util.List;

/**
 * Utils usabili per il versatore trasversalemnte ai vari plugin
 * @author boria
 */
public class VersatoreCommonUtils {

    /**
     * Trona true se il file passato è convertibile in pdf
     *
    @param allegato allegato da controllare
    @param supportedFilesList lista dei mimeType supportati
    @return
    @throws VersatoreHttpException
     */
    public static boolean isConvertibile(Allegato allegato, List<SupportedFile> supportedFilesList) throws VersatoreHttpException {
        String mimeType = allegato.getDettagli().getByKey(Allegato.DettagliAllegato.TipoDettaglioAllegato.ORIGINALE).getMimeType();
        SupportedFile supportedFile = supportedFilesList.stream()
            .filter(f -> f.getMimeType().equals(mimeType))
            .findFirst()
            .orElse(null);
        if (supportedFile != null) {
            return supportedFile.getConvertibilePdf();
        } else {
            throw new VersatoreHttpException("MimeType non previsto");
        }
    }

}
