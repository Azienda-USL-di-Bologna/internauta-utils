/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.plugin.gru;

/**
 *
 * @author Top
 */
public class QueryRecuperoDati {

        private String appartenenti;
        private String strutture;
        private String responsabili;
        private String trasformazioni;
        private String anagrafiche;
        private Integer progressivoUltimaTrasformazione;

        public QueryRecuperoDati(String appartenenti, String strutture, String responsabili, String trasformazioni,String anagrafiche, Integer progressivoUltimaTrasformazione) {
            this.appartenenti = appartenenti;
            this.strutture = strutture;
            this.responsabili = responsabili;
            this.trasformazioni = trasformazioni;
            this.anagrafiche = anagrafiche;
            this.progressivoUltimaTrasformazione = progressivoUltimaTrasformazione;
        }

        public QueryRecuperoDati() {
        }

        public String getQueryAppartenenti() {
            return appartenenti;
        }

        public void setQueryAppartenenti(String appartenenti) {
            this.appartenenti = appartenenti;
        }

        public String getQueryStrutture() {
            return strutture;
        }

        public void setQueryStrutture(String strutture) {
            this.strutture = strutture;
        }

        public String getQueryResponsabili() {
            return responsabili;
        }

        public void setQueryResponsabili(String responsabili) {
            this.responsabili = responsabili;
        }

        public String getQueryTrasformazioni() {
            return trasformazioni;
        }

        public void setQueryTrasformazioni(String trasformazioni) {
            this.trasformazioni = trasformazioni;
        }

        public String getQueryAnagrafiche() {
            return anagrafiche;
        }

        public void setQueryAnagrafiche(String anagrafiche) {
            this.anagrafiche = anagrafiche;
        }

        public Integer getProgressivoUltimaTrasformazione() {
            return progressivoUltimaTrasformazione;
        }

        public void setProgressivoUltimaTrasformazione(Integer progressivoUltimaTrasformazione) {
            this.progressivoUltimaTrasformazione = progressivoUltimaTrasformazione;
        }  
    }
