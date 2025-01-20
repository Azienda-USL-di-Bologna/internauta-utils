/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.basedata.datachecker;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.userreport.UserReport;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.userreport.UserReportStrutture;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import java.util.List;

/**
 *
 * @author Top
 */
public class OperationStrutture extends Operation<DatiDaImportareStruttura>{

    public OperationStrutture(Azione azione, DatiDaImportareStruttura entitaCoinvolta) {
        super(azione, entitaCoinvolta);
    }

    @Override
    public void esegui() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    
    
}
