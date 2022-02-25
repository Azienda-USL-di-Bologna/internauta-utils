package it.bologna.ausl.internauta.utils.firma.remota.data;

/**
 * Descrive le caratteristiche del campo firma visibile su un pdf
 * 
 * @author gdm
 */
public class SignAppearance {

    // formato: page;width;heigth;paddingWidth;paddingHeigth Es. 2;200;300;10;15
    // per indicare l'ultima pagina inserire n. Es. n;200;300;10;15 
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
