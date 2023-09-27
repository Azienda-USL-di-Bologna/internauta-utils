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

    public static String buildIdFascicolo(Archivio archivio) {
        //TODO anche il numero del fascicolo va formattato a 7 cifre?
        DecimalFormat df = new DecimalFormat("0000000");
        String numeroFascicolo = df.format(archivio.getNumero());
        return ("Id: " + archivio.getId() + " - n° " + numeroFascicolo + "/" + archivio.getAnno());
    }

    public static String buildNoteFascicolo(Archivio archivio, ArchivioDetail archivioDetails, EntityManager entityManager) {
        //TODO anche il numero del fascicolo va formattato a 7 cifre?
        DecimalFormat df = new DecimalFormat("0000000");
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
        String note1 = "Fascicolo di appartenenza del documento\n"
                + "Numero: " + df.format(archivioDetails.getNumero())
                + "Anno: " + archivioDetails.getAnno()
                + "Tipo: " + archivioDetails.getTipo()
                + "Responsabile: " + archivioDetails.getIdPersonaResponsabile().getDescrizione()
                + "Struttura del responsabile: " //TODO chiedere come sapere la struttura del responsabile
                + "Vicari: " + vicari
                + "Oggetto: " + archivioDetails.getOggetto()
                + "Classificazione: " //TODO capire cos'è
                + "Anni tenuta" + archivio.getAnniTenuta()
                + "Gerarchia" + gerarchia;

        return note1;
    }

}
