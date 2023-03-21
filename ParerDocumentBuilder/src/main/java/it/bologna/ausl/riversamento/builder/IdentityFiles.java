/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.riversamento.builder;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
