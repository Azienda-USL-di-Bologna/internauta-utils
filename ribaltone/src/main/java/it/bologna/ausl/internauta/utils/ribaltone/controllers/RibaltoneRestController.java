/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.controllers;

import it.bologna.ausl.internauta.utils.ribaltone.Ribaltone;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Top
 */
@RestController
@RequestMapping(value = "${ribaltone.mapping.url}")
public class RibaltoneRestController {
    
    @RequestMapping(value = "/getSourceData", method = RequestMethod.GET)
    public void getSourceData()
    {
        
    }
    
    @RequestMapping(value = "/cleanSourceData", method = RequestMethod.GET)
    public void cleanSourceData(){
        
    }
    
    @RequestMapping(value = "/checkSourceData", method = RequestMethod.GET)
    public Map<Ribaltone.checkMapKey,Object> checkSourceData(){
        
        Map<String,String> risultatoStr = checkStrutture();
        Map<String,String> risultatoApp = checkAppartenenti();
        Map<String,String> risultatoTra = checkTrasformazioni();
        
        return generateReport(risultatoStr, risultatoApp, risultatoTra);
    }
    
    
    
    
    @RequestMapping(value = "/ribalta", method = RequestMethod.GET)
    public void ribalta(){
       
        
    }
}
