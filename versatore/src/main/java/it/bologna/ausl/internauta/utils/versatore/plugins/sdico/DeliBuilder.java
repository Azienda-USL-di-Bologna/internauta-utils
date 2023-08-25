/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.sdico;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author boria
 */
public class DeliBuilder {

    private static final Logger log = LoggerFactory.getLogger(DeliBuilder.class);

    private VersamentoBuilder versamentoBuilder;

    public DeliBuilder() {
        versamentoBuilder = new VersamentoBuilder();
    }

    public VersamentoBuilder build() {

        versamentoBuilder.setDocType("83");

        versamentoBuilder.addSinglemetadataByParams(true, "id_ente_versatore", Arrays.asList("azero"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(true, "idTipoDoc", Arrays.asList("83"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(true, "idClassifica", Arrays.asList("3111"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(true, "classificazioneArchivistica", Arrays.asList("0101003"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "descrizione_classificazione", Arrays.asList("Delibera"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "tempo_di_conservazione", Arrays.asList("illimitato"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "oggettodeldocumento", Arrays.asList("Delibera n. DL/2023/0000011"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "repertorio", Arrays.asList("DL - Delibere dell'Ente"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "numero_documento", Arrays.asList("0000011"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "data_di_registrazione", Arrays.asList(""), "DATA");
        versamentoBuilder.addSinglemetadataByParams(false, "numeroProtocollo", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "dataRegistrazioneProtocollo", Arrays.asList(""), "DATA");
        versamentoBuilder.addSinglemetadataByParams(false, "tipologia_di_flusso", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "ufficioProduttore", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "amministrazioneTitolareDelProcedimento", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "responasbileProcedimento", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "aooDiRiferimento", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "idFascicolo", Arrays.asList(""), "MULTIPLO");
        versamentoBuilder.addSinglemetadataByParams(false, "registro", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "serie", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "idSistemaVersante", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "numeroAltraRegistrazione", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "dataAltraRegistrazione", Arrays.asList(""), "DATA");
        versamentoBuilder.addSinglemetadataByParams(false, "applicativoProduzione", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "note1", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "note2", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "annotazione", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "firmato_digitalmente", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "marcatura_temporale", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "idDocumentoOriginale", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "numero_allegati", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "livelloRiservatezza", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "riservato", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "esistenzaOriginaleAnalogico", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "oggettoProcedimento", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "materiaargomentostruttura_procedimento", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "conformita_copie_immagine_su_supporto_informatico", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "sigillato_elettronicamente", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "cfTitolareFirma", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "prodotto_software", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "prodotto_software_nome_prodotto", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "prodotto_software_versione_prodotto", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "prodotto_software_produttore", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "tipo_registro", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "codice_registro", Arrays.asList(""), "TESTO");

        //qui finisce la dete
        versamentoBuilder.addSinglemetadataByParams(false, "data_proposta", Arrays.asList(""), "DATA");
        versamentoBuilder.addSinglemetadataByParams(false, "numero_proposta", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "data_esecutiva", Arrays.asList(""), "DATA");
        versamentoBuilder.addSinglemetadataByParams(false, "natura_documento", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "data_pubblicazione", Arrays.asList(""), "DATA");
        versamentoBuilder.addSinglemetadataByParams(false, "autore_pubblicazione", Arrays.asList(""), "TESTO");

        //qui ricomincia
        versamentoBuilder.addFileByParams("Documento_di_prova_test_1.pdf", "application/pdf", "FE6330A16F73E08BA0BCDF0249F6A815CE88840B044F0C6415090F2B35255FC0");

        log.info(versamentoBuilder.toString());
        
        return versamentoBuilder;

    }

}
