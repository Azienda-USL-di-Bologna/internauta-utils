/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.utils.service;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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

    @Bean
    public ConversionService conversionService() {
        ApplicationConversionService conversionService = new ApplicationConversionService();
        conversionService.addConverter(
            String.class,
            LocalDateTime.class,
            (Converter) source -> {
                return LocalDateTime.parse((String) source, DateTimeFormatter.ISO_LOCAL_TIME);
            });

        conversionService.addConverter(
            String.class,
            ZonedDateTime.class,
            (Converter) source -> {
                try {
                    return ZonedDateTime.parse((String) source, DateTimeFormatter.ISO_ZONED_DATE_TIME);
                } catch (Exception e) {
                    //return LocalDateTime.parse((String) source, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")).atZone(java.time.ZoneId.of("Europe/Rome"));
                }
                try {
                    return LocalDateTime.parse((String) source, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")).atZone(java.time.ZoneId.of("Europe/Rome"));
                } catch (Exception e) {
                    //return LocalDateTime.parse((String) source, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")).atZone(java.time.ZoneId.of("Europe/Rome"));
                }
                try {
                    // String format = ((Timestamp) o).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    Instant toInstant = new SimpleDateFormat("dd/MM/yy").parse(source.toString()).toInstant();
                    return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
                } catch (ParseException e) {
                    //non Ã¨ stato parsato
                }
                try {
                    Instant toInstant = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(source.toString()).toInstant();
                    return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
                } catch (ParseException e) {
                    //non Ã¨ stato parsato
                }
                try {
                    Instant toInstant = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(source.toString()).toInstant();
                    return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
                } catch (ParseException e) {
                    //non Ã¨ stato parsato
                }

                try {

                    String time = ((Timestamp) source).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    Instant toInstant = new SimpleDateFormat("dd/MM/yyyy").parse(time).toInstant();
                    return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
                } catch (ParseException e) {
                    //non Ã¨ stato parsato
                }
                try {
                    String time = ((Timestamp) source).toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    Instant toInstant = new SimpleDateFormat("dd/MM/yyyy").parse(time).toInstant();
                    return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
                } catch (ParseException e) {
                }
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    sdf.setLenient(false);
                    Instant toInstant = sdf.parse(source.toString()).toInstant();
                    return ZonedDateTime.ofInstant(toInstant, ZoneId.systemDefault());
                } catch (ParseException e) {
                    // non è stato parsato
                }

                return null;
            });
        return conversionService;

    }
}
