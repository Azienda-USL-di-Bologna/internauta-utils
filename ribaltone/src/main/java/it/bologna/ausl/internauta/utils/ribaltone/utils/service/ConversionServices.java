package it.bologna.ausl.internauta.utils.ribaltone.utils.service;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;

/**
 *
 * @author MicheleD'Onza
 */
@Configuration
public class ConversionServices {

    private static final Logger log = LoggerFactory.getLogger(ConversionServices.class);

    @Bean
    public ConversionService conversionService() {
        ApplicationConversionService conversionService = new ApplicationConversionService();
        conversionService.addConverter(
            String.class,
            LocalDateTime.class,
            (Converter) source -> {
                return LocalDateTime.parse((String) source, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            });

        conversionService.addConverter(
            String.class,
            ZonedDateTime.class,
            (Converter) source -> {
                try {
                    return ZonedDateTime.parse((String) source, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS Z"));
                } catch (Exception e) {
                    log.error("service di mido formato data non yyyy-MM-dd HH:mm:ss.SSS Z");
                }
                try {
                    return ZonedDateTime.parse((String) source, DateTimeFormatter.ISO_ZONED_DATE_TIME);
                } catch (Exception e) {
                    log.error("service di mido formato data non ISO_ZONED_DATE_TIME");
                }
                try {
                    return LocalDateTime.parse((String) source, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")).atZone(java.time.ZoneId.of("Europe/Rome"));
                } catch (Exception e) {
                    log.error("service di mido formato data non ISO_ZONED_DATE_TIME");
                }
                try {
                    // String format = ((Timestamp) o).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    Instant toInstant = new SimpleDateFormat("dd/MM/yy").parse(source.toString()).toInstant();
                    return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
                } catch (ParseException e) {
                    log.error("service di mido formato data non ISO_ZONED_DATE_TIME");
                }
                try {
                    Instant toInstant = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(source.toString()).toInstant();
                    return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
                } catch (ParseException e) {
                    log.error("service di mido formato data non ISO_ZONED_DATE_TIME");
                }
                try {
                    Instant toInstant = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(source.toString()).toInstant();
                    return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
                } catch (ParseException e) {
                    log.error("service di mido formato data non ISO_ZONED_DATE_TIME");
                }
                try {
                    String time = ((Timestamp) source).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    Instant toInstant = new SimpleDateFormat("dd/MM/yyyy").parse(time).toInstant();
                    return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
                } catch (ParseException e) {
                    log.error("service di mido formato data non dd/MM/yyyy");
                }
                try {
                    String time = ((Timestamp) source).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    Instant toInstant = new SimpleDateFormat("dd/MM/yyyy").parse(time).toInstant();
                    return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
                } catch (ParseException e) {
                    log.error("service di mido formato data non dd/MM/yyyy");
                }
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    sdf.setLenient(false);
                    Instant toInstant = sdf.parse(source.toString()).toInstant();
                    return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
                } catch (ParseException e) {
                    log.error("service di mido formato data non dd/MM/yyyy");
                }

                return null;
            });
        return conversionService;

    }
}
