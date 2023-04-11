package it.bologna.ausl.internauta.utils.firma.data.remota.namirialsignservice;

/**
 *
 * @author gdm
 */
public enum NamirialRestPathsEnum {
    OPEN_SESSION("/rest/sign/openSession"),
    CLOSE_SESSION("/rest/sign/closeSession"),
    GET_OTPS("/rest/enquiry/otps"),
    GET_SMS_OTP("/rest/sign/sendOtpBySMS"),
    CADES_SIGN("/rest/sign/signCades"),
    PADES_SIGN("/rest/sign/signPades"),
    XADES_SIGN("/rest/sign/signXades"),
    PKCS1_SIGN("/rest/sign/signPKCS1"),
    TIMESTAMP_APPLY("/rest/timestamps/apply"),
    VERIFY_SIGN("/rest/verify/signatures"),
    VERIFY_TIMESTAMP_TSD("/rest/verify/timestamps/tsd"),
    VERIFY_TIMESTAMP_TSR("/rest/verify/timestamps/tsr");

    private final String path;

    private NamirialRestPathsEnum(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
