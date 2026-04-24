package it.bologna.ausl.internauta.utils.versatore.utils;

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

    /**
    metodo per convertire le date da usare nell'xml
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
