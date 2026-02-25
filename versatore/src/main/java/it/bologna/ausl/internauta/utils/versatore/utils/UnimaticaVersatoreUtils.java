package it.bologna.ausl.internauta.utils.versatore.utils;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 * @author boria
 */
public class UnimaticaVersatoreUtils {

    //toglie l'estensione a un nome file in formato stringa
    public static String removeExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            return fileName; // Nessun punto, quindi niente estensione
        }
        return fileName.substring(0, lastDot);
    }

    //TODO tolgo?
    public static String sha256(InputStream inputStream) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192]; // buffer ragionevole per grandi file
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
        byte[] hashBytes = digest.digest();

        // Converti in HEX
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
    metodo per convertire le date da usarenell'xml
    @param zdt
    @return
    @throws DatatypeConfigurationException
     */
    public static XMLGregorianCalendar toXMLGregorianDate(ZonedDateTime zdt)
        throws DatatypeConfigurationException {

        LocalDate localDate = zdt.toLocalDate();

        return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
            localDate.getYear(),
            localDate.getMonthValue(),
            localDate.getDayOfMonth(),
            DatatypeConstants.FIELD_UNDEFINED
        );
    }

}
