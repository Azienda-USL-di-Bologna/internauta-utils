/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.utils.service;

import java.time.LocalDateTime;
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
                    return LocalDateTime.parse((String) source, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")).atZone(java.time.ZoneId.of("Europe/Rome"));
                }
            });
        return conversionService;

    }
}
