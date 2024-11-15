/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.riversamento.builder;

import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author utente
 */

@XmlRootElement(name = "identityFiles")
@XmlAccessorType (XmlAccessType.FIELD)
public class IdentityFiles {
    
    @XmlElement(name = "identityFile")
    private List<IdentityFile> identityFiles = null;
 
    public List<IdentityFile> getidentityFiles() {
        return identityFiles;
    }
 
    public void setidentityFiles(List<IdentityFile> identityFiles) {
        this.identityFiles = identityFiles;
    }
    
}
