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
public class PecBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(DeliBuilder.class);
    
    private VersamentoBuilder versamentoBuilder;
    
    public PecBuilder() {
        versamentoBuilder = new VersamentoBuilder();
    }
    
    public VersamentoBuilder build() {
        
        versamentoBuilder.setDocType("8002");
        
        versamentoBuilder.addSinglemetadataByParams(true, "id_ente_versatore", Arrays.asList("r_veneto"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(true, "idTipoDoc", Arrays.asList("8002"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(true, "idClassifica", Arrays.asList("3036"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(true, "classificazioneArchivistica", Arrays.asList("C.101.15.2.a2"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "oggettodeldocumento", Arrays.asList("Oggetto della PEC"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "amministrazioneTitolareDelProcedimento", Arrays.asList("r_veneto"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "aooDiRiferimento", Arrays.asList("aoogiunta"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "CODICE_IPA_UNIVOCO_UFFICIO", Arrays.asList(""), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "DENOMINAZIONE_STRUTTURA", Arrays.asList("DIREZIONE ORGANIZZAZIONE E PERSONALE"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "numeroProtocollo", Arrays.asList("12345678"), "TESTO");
        
        versamentoBuilder.addFileByParams("Documento_di_prova.pdf", "application/pdf", "106cdc73f652bfa0349910d7d1c9a541dc326e729e22d779c7c2a64920034e0f");
        
        log.info(versamentoBuilder.toString());
        
        return versamentoBuilder;
        
    }
    
}
