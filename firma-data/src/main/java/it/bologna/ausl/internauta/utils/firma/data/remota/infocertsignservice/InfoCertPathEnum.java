package it.bologna.ausl.internauta.utils.firma.data.remota.infocertsignservice;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public enum InfoCertPathEnum {
    FIRMA_CADES("/SignEngineWeb/rest/sign/signCades"),
    FIRMA_PADES("/SignEngineWeb/rest/sign/signPades"),
    FIRMA_XADES("/SignEngineWeb/rest/sign/signXades"),
    FIRMA_PKCS1("/SignEngineWeb/rest/sign/signXades"),
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
