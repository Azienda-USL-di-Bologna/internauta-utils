/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.versatore.plugins.sdico;

import it.bologna.ausl.model.entities.scripta.DocDetail;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author boria
 */
public class RgPicoBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(DeliBuilder.class);
    
    private VersamentoBuilder versamentoBuilder;
    
    private DocDetail docDetail;
    
    public RgPicoBuilder(DocDetail docDetail) {
        versamentoBuilder = new VersamentoBuilder();
        this.docDetail = docDetail;
    }
    
    public VersamentoBuilder build() {
        
        versamentoBuilder.setDocType("8001");
        
        versamentoBuilder.addSinglemetadataByParams(true, "id_ente_versatore", Arrays.asList("r_veneto"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(true, "idTipoDoc", Arrays.asList("8001"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(true, "idClassifica", Arrays.asList("3035"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(true, "classificazioneArchivistica", Arrays.asList("C.101.15.2.a1"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "oggettodeldocumento", Arrays.asList(this.docDetail.getOggetto()), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "amministrazioneTitolareDelProcedimento", Arrays.asList("r_veneto"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "aooDiRiferimento", Arrays.asList("aoogiunta"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "Codice_identificativo_del_registro", Arrays.asList("1"), "TESTO");
        versamentoBuilder.addSinglemetadataByParams(false, "Numero_progressivo_del_registro", Arrays.asList("329/I"), "TESTO");
        
        versamentoBuilder.addFileByParams("Documento_di_prova.pdf", "application/pdf", "106cdc73f652bfa0349910d7d1c9a541dc326e729e22d779c7c2a64920034e0f");
        
        log.info(versamentoBuilder.toString());
        
        return versamentoBuilder;
        
    }
    
}
