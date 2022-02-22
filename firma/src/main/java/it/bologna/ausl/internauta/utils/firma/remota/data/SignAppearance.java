package it.bologna.ausl.internauta.utils.firma.remota.data;

/**
 * Descrive le caratteristiche del campo firma visibile su un pdf
 * 
 * @author gdm
 */
public class SignAppearance {

    private String signPosition;
    private String signName;

    public SignAppearance() {
    }

    public SignAppearance(String signPosition, String signName) {
        this.signPosition = signPosition;
        this.signName = signName;
    }

    public String getSignPosition() {
        return signPosition;
    }

    public void setSignPosition(String signPosition) {
        this.signPosition = signPosition;
    }

    public String getSignName() {
        return signName;
    }

    public void setSignName(String signName) {
        this.signName = signName;
    }
}
