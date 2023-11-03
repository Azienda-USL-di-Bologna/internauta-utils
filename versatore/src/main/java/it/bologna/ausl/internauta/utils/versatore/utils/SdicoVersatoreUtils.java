/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.utils;

import it.bologna.ausl.model.entities.scripta.Archivio;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author boria
 */
public class SdicoVersatoreUtils {

    /**
     * Metodo che compone la stringa da inserire nell'attributo dei tracciati idFascicolo
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
    
    public static List<String> buildListaIdFascicolo(Archivio archivio) {
        List<String> listaFascicoli = new ArrayList<>();
        listaFascicoli.add(buildIdFascicolo(archivio));
        if (archivio.getIdArchivioPadre() != null) {
            listaFascicoli.add(buildIdFascicolo(archivio.getIdArchivioPadre()));
            if (archivio.getIdArchivioPadre().getIdArchivioPadre() != null) {
                listaFascicoli.add(buildIdFascicolo(archivio.getIdArchivioPadre().getIdArchivioPadre()));
            }
        }
        return listaFascicoli;
    }
    
}