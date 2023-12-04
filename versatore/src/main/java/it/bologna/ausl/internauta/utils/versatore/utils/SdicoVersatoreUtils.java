/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.utils;

import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.Doc;

/**
 *
 * @author boria
 */
public class SdicoVersatoreUtils {

    /**
     * Metodo che formatta il singolo idFascicolo
     * @param archivio
     * @return 
     */
    public static String buildIdFascicolo(Archivio archivio) {
        String numero = archivio.getNumero().toString();
        if (archivio.getIdArchivioPadre() != null) {
            numero = archivio.getIdArchivioPadre().getNumero().toString() + "-" + numero;
            if (archivio.getIdArchivioPadre().getIdArchivioPadre() != null) {
                numero = archivio.getIdArchivioPadre().getIdArchivioPadre().getNumero() + "-" + numero;
            }
        }
        return (numero + "/" + archivio.getAnno() + " [id_"+ archivio.getId() + "]");
    }
    
    /**
     * Metodo che compone la stringa da inserire nell'attributo dei tracciati idFascicolo
     * @param doc
     * @param archivio
     * @return 
     */
    public static String buildIdFascicoli(Doc doc, Archivio archivio) {
        String idFascicolo = "";
        for (ArchivioDoc archivioDoc : doc.getArchiviDocList()) {
            if (archivioDoc.getDataEliminazione() == null && archivioDoc.getIdArchivio().getIdArchivioRadice().getId() == archivio.getIdArchivioRadice().getId() ) {
                idFascicolo += buildIdFascicolo(archivioDoc.getIdArchivio()) + ", ";
            }
        }
        return idFascicolo.substring(0, idFascicolo.length() - 2);
    }
    
}