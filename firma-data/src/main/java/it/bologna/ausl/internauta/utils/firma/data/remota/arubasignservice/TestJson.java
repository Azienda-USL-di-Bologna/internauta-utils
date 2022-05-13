/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.utils.firma.data.remota.arubasignservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.firma.data.remota.arubasignservice.ArubaUserInformation.ModalitaFirma;

/**
 *
 * @author gdm
 */
public class TestJson {
    public static void main(String[] args) throws JsonProcessingException {
        ArubaUserInformation a = new ArubaUserInformation("dmrgpp83e29d851cb", "qwerty123456", "123", ModalitaFirma.OTP, "1", "frAUSLBO", false);
        ObjectMapper objectMapper = new ObjectMapper();
        String str = objectMapper.writeValueAsString(a);
        System.out.println("json:");
        System.out.println(str);
        
        ArubaUserInformation a2 = objectMapper.readValue(str, ArubaUserInformation.class);
        System.out.println("parsed:");
        System.out.println(objectMapper.writeValueAsString(a));
    }
}
