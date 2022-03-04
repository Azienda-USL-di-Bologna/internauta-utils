package it.bologna.ausl.internauta.utils.firma.remota.data;

/**
 *
 * @author gdm
 * 
 * Questa classe descrive il file da firmare
 */
public class FirmaRemotaFile {
    public static final String PDF_MIMETYPE = "application/pdf";
    public static final String P7M_MIMETYPE = "application/pkcs7-mime";
    
    public static enum FormatiFirma{PDF, P7M};
    public static enum OutputType{URL, UUID};
    
    // in
    private String fileId; // guid del file per identificarlo
    //private String name;  // non sappiamo se ci servirà
    private String url; // url per scaricare il file da firmare
    private String mimeType; // mime Type del file da firmare
    private FormatiFirma formatoFirma; //formato firma da ottenere 
    private SignAppearance signAppearance; // se popolato indica che la firma è visibile definendo pagina e posizione. Valido solo se formato firma è pdf. page;width;heigth;paddingWidth;paddingHeigth
    private OutputType outputType = OutputType.UUID; // indica se l'output del file firmato sarà direttamente l'uuid su mongodownload oppure il link per poterlo scaricare (con il downloader)
    
    private String codiceAzienda; //azienda proprietaria del file. Probabilmente serve solo per le applicazioni INDE, poi vedremo
    
    // out
    private String uuidFirmato; // uuid di mongo download del file firmato, l'idea e usarlo solo per le applicazioni INDE e usare url in tutti gli altri casi (anche quando lo useremo in pico/dete/deli internauta
    private String urlFirmato; // uuid di mongo download del file firmato

    
    public FirmaRemotaFile() {
    }

    public FirmaRemotaFile(String fileId, String url, String mimeType, FormatiFirma formatoFirma, String uuidFirmato) {
        this.fileId = fileId;
        this.url = url;
        this.mimeType = mimeType;
        this.formatoFirma = formatoFirma;
        this.uuidFirmato = uuidFirmato;
    }
    
    public FirmaRemotaFile(String fileId, String url, String mimeType, FormatiFirma formatoFirma) {
        this.fileId = fileId;
        this.url = url;
        this.mimeType = mimeType;
        this.formatoFirma = formatoFirma;
    }

    public FirmaRemotaFile(String fileId, String url, String mimeType, FormatiFirma formatoFirma, SignAppearance signAppearance) {
        this.fileId = fileId;
        this.url = url;
        this.mimeType = mimeType;
        this.formatoFirma = formatoFirma;
        this.signAppearance = signAppearance;
    }

    public FirmaRemotaFile(String fileId, String url, String mimeType, FormatiFirma formatoFirma, SignAppearance signAppearance, String uuidFirmato) {
        this.fileId = fileId;
        this.url = url;
        this.mimeType = mimeType;
        this.formatoFirma = formatoFirma;
        this.signAppearance = signAppearance;
        this.uuidFirmato = uuidFirmato;
    }
    
    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public FormatiFirma getFormatoFirma() {
        return formatoFirma;
    }

    public void setFormatoFirma(FormatiFirma formatoFirma) {
        this.formatoFirma = formatoFirma;
    }

    public String getUuidFirmato() {
        return uuidFirmato;
    }

    public void setUuidFirmato(String uuidFirmato) {
        this.uuidFirmato = uuidFirmato;
    }

    public String getUrlFirmato() {
        return urlFirmato;
    }

    public void setUrlFirmato(String urlFirmato) {
        this.urlFirmato = urlFirmato;
    }

    public OutputType getOutputType() {
        return outputType;
    }

    public void setOutputType(OutputType outputType) {
        this.outputType = outputType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public SignAppearance getSignAppearance() {
        return signAppearance;
    }

    public void setSignAppearance(SignAppearance signAppearance) {
        this.signAppearance = signAppearance;
    }

    public String getCodiceAzienda() {
        return codiceAzienda;
    }

    public void setCodiceAzienda(String codiceAzienda) {
        this.codiceAzienda = codiceAzienda;
    }
}
