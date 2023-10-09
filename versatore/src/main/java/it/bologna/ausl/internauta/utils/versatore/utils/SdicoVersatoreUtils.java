/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.utils;

import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDetail;
import java.text.DecimalFormat;
import javax.persistence.EntityManager;

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
        return ("Id" + archivio.getId() + "-n" + archivio.getNumero() + "/" + archivio.getAnno());
    }

    //TODO vedere se serve
    public static String buildNoteFascicolo(Archivio archivio, ArchivioDetail archivioDetails, EntityManager entityManager) {
        Integer[] idVicari = archivioDetails.getIdVicari();
        String vicari = "";
        for (int i = 0; i < idVicari.length; i++) {
            Persona vicario = entityManager.find(Persona.class, idVicari[i]);
            vicari += vicario.getDescrizione() + ", ";
        }
        String gerarchia;
        switch (archivioDetails.getLivello()) {
            case 1: {
                gerarchia = "Fascicolo"; //TODO vedere se denominarlo così
                break;
            }
            case 2: {
                gerarchia = "Sottofascicolo";
                break;
            }
            case 3: {
                gerarchia = "Inserto";
                break;
            }
            default:
                throw new AssertionError();
        }
        vicari = vicari.substring(0, vicari.length() - 2);
        String note1 = "Fascicolo di appartenenza del documento" + "\n"
                + "Numero: " + archivioDetails.getNumero() + "\n"
                + "Anno: " + archivioDetails.getAnno() + "\n"
                + "Tipo: " + archivioDetails.getTipo() + "\n"
                + "Responsabile: " + archivioDetails.getIdPersonaResponsabile().getDescrizione() + "\n"
                + "Struttura del responsabile: " + "\n" //TODO chiedere come sapere la struttura del responsabile
                + "Vicari: " + vicari + "\n"
                + "Oggetto: " + archivioDetails.getOggetto() + "\n"
                + "Classificazione: " + "\n" //TODO capire cos'è
                + "Anni tenuta: " + archivio.getAnniTenuta() + "\n"
                + "Gerarchia: " + gerarchia;

        return note1;
    }

}
