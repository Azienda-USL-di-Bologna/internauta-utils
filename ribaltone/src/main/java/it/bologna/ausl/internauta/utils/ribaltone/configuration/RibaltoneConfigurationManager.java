/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.configuration;

import java.util.Map;
import org.sql2o.Sql2o;

/**
 *
 * @author Top
 */
public abstract class RibaltoneConfigurationManager {
    
    public abstract Sql2o getConnection(Map<String, String> configuration);
    
    
}
