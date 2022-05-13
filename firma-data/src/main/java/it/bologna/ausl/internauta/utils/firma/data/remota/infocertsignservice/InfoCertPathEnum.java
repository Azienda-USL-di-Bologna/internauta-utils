package it.bologna.ausl.internauta.utils.firma.data.remota.infocertsignservice;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public enum InfoCertPathEnum {
    FIRMA_CADES("/[CONTEXT]/sign/cades/[ALIAS]"),
    FIRMA_CADES_MARCA_TEMPORALE("/[CONTEXT]/sign/cades-t/[ALIAS]"),
    FIRMA_PADES("/[CONTEXT]/sign/pades/[ALIAS]"),
    FIRMA_PADES_MARCA_TEMPORALE("/[CONTEXT]/sign/pades-t/[ALIAS]"),
    FIRMA_XADES_ENVELOPED("/[CONTEXT]/sign/xades-enveloped/[ALIAS]"),
    FIRMA_XADES_ENVELOPING("/[CONTEXT]/sign/xades-enveloping/[ALIAS]"),
    FIRMA_XADES_DETACHED("/[CONTEXT]/sign/xades-detached/[ALIAS]"),
    FIRMA_XADES_ENVELOPED_MARCA_TEMPORALE("/[CONTEXT]/sign/xades-t-enveloped/[ALIAS]"),
    FIRMA_XADES_ENVELOPING_MARCA_TEMPORALE("/[CONTEXT]/sign/xades-t-enveloping/[ALIAS]"),
    FIRMA_XADES_DETACHED_MARCA_TEMPORALE("/[CONTEXT]/sign/xades-t-detached/[ALIAS]"),
    RICHIESTA_OPT("/[CONTEXT]/request-otp/[ALIAS]"),
    VERIFICA_FIRMA_MARCA("/[CONTEXT]"),
    MARCA_TSD("/[CONTEXT]/tsd"),
    MARCA_M7M("/[CONTEXT]/m7m"),
    MARCA_TSR("/[CONTEXT]/tsr"),
    DOWNLOAD_FILE("/[CONTEXT]/get-signed-file/[ALIAS]"),
    RICHIESTA_CERTIFICATO("/remote/alias/[ALIAS]"),
    ESTRAZIONE_DOCUMENTO_ORIGINALE_FIRMATO("/[CONTEXT]/extract"),
    VERIFICA_ESTRAZIONE_DOCUMENTO_ORIGINALE_FIRMATO("/[CONTEXT]/verifyExtractor");

    public final String path;

    private InfoCertPathEnum(String path) {
        this.path = path;
    }
    
    public String getPath(String context, String alias) {
        return path.replace("[CONTEXT]", context).replace("[ALIAS]", alias);
    }
    
}
