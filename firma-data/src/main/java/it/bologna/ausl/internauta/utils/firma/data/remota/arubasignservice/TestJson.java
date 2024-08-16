/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.internauta.utils.firma.data.remota.arubasignservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParams;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParamsComponent;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParamsComponent.EndSign;
import it.bologna.ausl.internauta.utils.firma.data.remota.arubasignservice.ArubaUserInformation.ModalitaFirma;

/**
 *
 * @author gdm
 */
public class TestJson {
    
    private static String signParamsString = "{\"signResult\":\"ALL_SIGNED\",\"callBackUrl\":\"http:\\/\\/localhost:8082\\/bds_tools\\/EndSignManager\",\"signedFileList\":[{\"uploaderResult\":\"0194f5e5-4585-4170-b7d8-e31ec28800ec\",\"file\":\"https:\\/\\/gdml.internal.ausl.bologna.it\\/bds_tools\\/Downloader?token=74287608-468A-8FF1-731B-BBA8A598D95B&deletetoken=false\",\"name\":\"2022-902_Allegato1\",\"signType\":\"PADES\",\"signAttributes\":{\"visible\":false,\"textTemplate\":\"[COMMONNAME]\"},\"source\":\"URI\",\"id\":\"E158FF93-C3BC-EAA6-80AB-4517D11884B2\",\"mimeType\":\"application\\/pdf\",\"type\":\"AllegatoPicoNuovoPU\"}],\"endSignParams\":{\"ResultChannel\":\"sign_response_2E83B686-3EFB-41A6-A676-8AE708E99189\"}}";
    
    public static void main1(String[] args) throws JsonProcessingException {
        ArubaUserInformation a = new ArubaUserInformation("dmrgpp83e29d851cb", "qwerty123456", "123", ModalitaFirma.OTP, "1", "frAUSLBO", false);
        ObjectMapper objectMapper = new ObjectMapper();
        String str = objectMapper.writeValueAsString(a);
        System.out.println("json:");
        System.out.println(str);
        
        ArubaUserInformation a2 = objectMapper.readValue(str, ArubaUserInformation.class);
        System.out.println("parsed:");
        System.out.println(objectMapper.writeValueAsString(a));
    }
    
    public static void main(String[] args) throws JsonProcessingException {
        SignParamsComponent.EndSign parse = EndSign.parse(signParamsString);
        System.out.println(parse.toJsonString());
    }
}
