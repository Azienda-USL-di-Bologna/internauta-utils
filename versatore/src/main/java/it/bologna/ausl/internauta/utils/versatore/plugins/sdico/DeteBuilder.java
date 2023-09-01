package it.bologna.ausl.internauta.utils.versatore.plugins.sdico;

import it.bologna.ausl.model.entities.scripta.DocDetail;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Andrea
 */
public class DeteBuilder {
    
    private static final Logger log = LoggerFactory.getLogger(DeteBuilder.class);

    private VersamentoBuilder versamentoBuilder;
    
    private DocDetail docDetail;
    
    public DeteBuilder(DocDetail docDetail) {
        versamentoBuilder = new VersamentoBuilder();
        this.docDetail = docDetail;
    }
    
    public VersamentoBuilder build() {
        versamentoBuilder.setDocType("82");
        
       
        
        versamentoBuilder.addSinglemetadataByParams(true, "id_ente_versatore", Arrays.asList("azero"), "TESTO");
        versamentoBuilder.addFileByParams("Documento_di_prova.pdf", "application/pdf", "106cdc73f652bfa0349910d7d1c9a541dc326e729e22d779c7c2a64920034e0f");
       
        versamentoBuilder.addSinglemetadataByParams(true, "idTipoDoc", Arrays.asList("82"), "TESTO");
    
        versamentoBuilder.addSinglemetadataByParams(false, "descrizione_classificazione", Arrays.asList("Determinazioni"), "TESTO");

        versamentoBuilder.addSinglemetadataByParams(false, "tempo_di_conservazione", Arrays.asList("illimitato"), "TESTO");
      
        versamentoBuilder.addSinglemetadataByParams(false, "oggettodocumento", Arrays.asList(this.docDetail.getOggetto()), "TESTO");
        
        versamentoBuilder.addSinglemetadataByParams(false, "repertorio", Arrays.asList("DD - Decreti e Determinazioni"), "TESTO");
        
        versamentoBuilder.addSinglemetadataByParams(false, "numero_documento", Arrays.asList("0000001"), "TESTO");
        
        versamentoBuilder.addSinglemetadataByParams(false, "data_di_registrazione", Arrays.asList("2023-03-07T18:02:00.000"), "DATA");
        
        versamentoBuilder.addSinglemetadataByParams(false, "numeroProtocollo", Arrays.asList("123123"), "TESTO");
        
        log.info(versamentoBuilder.toString());
        
        return versamentoBuilder;
    }
    
    
        

    
}
