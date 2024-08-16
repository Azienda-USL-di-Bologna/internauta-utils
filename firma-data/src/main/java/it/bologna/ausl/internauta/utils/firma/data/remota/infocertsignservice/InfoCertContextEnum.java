package it.bologna.ausl.internauta.utils.firma.data.remota.infocertsignservice;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public enum InfoCertContextEnum {
    REMOTE("remote"),
    AUTO("auto"),
    VERIFY("verify"),
    TIMESTAMP("timestamp"),
    LOCAL("local"),
    GRAPHO("grapho");
        
    public final String context;
    
    private InfoCertContextEnum(String context) {
        this.context = context;
    }    
}
