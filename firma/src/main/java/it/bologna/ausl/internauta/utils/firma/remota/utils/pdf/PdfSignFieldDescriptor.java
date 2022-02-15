package it.bologna.ausl.internauta.utils.firma.remota.utils.pdf;

/**
 *
 * @author gdm
 */
public class PdfSignFieldDescriptor {
    private int page;
    private int lowerLeftX;
    private int lowerLeftY;
    private int upperRightX;
    private int upperRightY;
    private String signName;
    private String location;

    public PdfSignFieldDescriptor() {
    }

    public PdfSignFieldDescriptor(int page, int lowerLeftX, int lowerLeftY, int upperRightX, int upperRightY, String signName, String location) {
        this.page = page;
        this.lowerLeftX = lowerLeftX;
        this.lowerLeftY = lowerLeftY;
        this.upperRightX = upperRightX;
        this.upperRightY = upperRightY;
        this.signName = signName;
        this.location = location;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getLowerLeftX() {
        return lowerLeftX;
    }

    public void setLowerLeftX(int lowerLeftX) {
        this.lowerLeftX = lowerLeftX;
    }

    public int getLowerLeftY() {
        return lowerLeftY;
    }

    public void setLowerLeftY(int lowerLeftY) {
        this.lowerLeftY = lowerLeftY;
    }

    public int getUpperRightX() {
        return upperRightX;
    }

    public void setUpperRightX(int upperRightX) {
        this.upperRightX = upperRightX;
    }

    public int getUpperRightY() {
        return upperRightY;
    }

    public void setUpperRightY(int upperRightY) {
        this.upperRightY = upperRightY;
    }

    public String getSignName() {
        return signName;
    }

    public void setSignName(String signName) {
        this.signName = signName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
