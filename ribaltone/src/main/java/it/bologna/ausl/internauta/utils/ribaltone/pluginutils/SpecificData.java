/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.pluginutils;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.gru.GruSpecificData;

/**
 *
 * @author Top
 */

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "classz")
@JsonSubTypes({
    @JsonSubTypes.Type(value = GruSpecificData.class, name = "GruSpecificData"),
})
public abstract class SpecificData {
    
}
