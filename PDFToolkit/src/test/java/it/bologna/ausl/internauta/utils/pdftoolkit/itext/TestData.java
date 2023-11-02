package it.bologna.ausl.internauta.utils.pdftoolkit.itext;

import org.junit.jupiter.api.TestInstance;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * @author ferri
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestData {
    private Boolean aBoolean;
    private Integer aInteger;
    private Float aFloat;
    private Double aDouble;
    private String aString;
    private List<String> aStringList;
    private byte aByte;
    private byte[] aByteArray;
    private URI aUri;
    private Path aPath;
    private String aBlankImageInBase64;
    private ZonedDateTime aZonedDateTime;
    private Map<String, Object> aObjectMap;

    public static Map<String, Object> getTestInitialized() {
        TestData testDataAndFunctions = new TestData();
        testDataAndFunctions.setaBoolean(true);
        testDataAndFunctions.setaInteger(110);
        testDataAndFunctions.setaFloat(2220.3399F);
        testDataAndFunctions.setaDouble(220.0091D);
        testDataAndFunctions.setaString("TEST");
        List<String> stringListTest = new ArrayList<>();
        stringListTest.add("TEST-list0");
        stringListTest.add("");
        stringListTest.add("   ");
        stringListTest.add("\"\t\\\n");
        testDataAndFunctions.setaStringList(stringListTest);
        testDataAndFunctions.setaPath(Paths.get("./"));
        testDataAndFunctions.setaUri(URI.create("https://example.com/ftp/test.xml"));
        testDataAndFunctions.setaZonedDateTime(ZonedDateTime.of(2023, 10, 20,
                15, 12, 0, 1234567, ZoneId.of("Europe/Rome")));
        byte aByte = 42;
        testDataAndFunctions.setaByte(aByte);
        byte[] byteArray = {1, 2};
        testDataAndFunctions.setaByteArray(byteArray);

        testDataAndFunctions.setaBlankImageInBase64(Base64.getEncoder().encodeToString("".getBytes()));

        Map<String, Object> objectMapTest = new HashMap<String, Object>();
        objectMapTest.put("testString", new String("test.com"));
        testDataAndFunctions.setaObjectMap(objectMapTest);

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("testDataAndFunctions", testDataAndFunctions);

        return dataModel;
    }

    public Boolean getaBoolean() {
        return aBoolean;
    }

    public void setaBoolean(Boolean aBoolean) {
        this.aBoolean = aBoolean;
    }

    public Integer getaInteger() {
        return aInteger;
    }

    public void setaInteger(Integer aInteger) {
        this.aInteger = aInteger;
    }

    public Float getaFloat() {
        return aFloat;
    }

    public void setaFloat(Float aFloat) {
        this.aFloat = aFloat;
    }

    public Double getaDouble() {
        return aDouble;
    }

    public void setaDouble(Double aDouble) {
        this.aDouble = aDouble;
    }

    public String getaString() {
        return aString;
    }

    public void setaString(String aString) {
        this.aString = aString;
    }

    public List<String> getaStringList() {
        return aStringList;
    }

    public void setaStringList(List<String> aStringList) {
        this.aStringList = aStringList;
    }

    public byte getaByte() {
        return aByte;
    }

    public void setaByte(byte aByte) {
        this.aByte = aByte;
    }

    public byte[] getaByteArray() {
        return aByteArray;
    }

    public void setaByteArray(byte[] aByteArray) {
        this.aByteArray = aByteArray;
    }

    public URI getaUri() {
        return aUri;
    }

    public void setaUri(URI aUri) {
        this.aUri = aUri;
    }

    public Path getaPath() {
        return aPath;
    }

    public void setaPath(Path aPath) {
        this.aPath = aPath;
    }

    public String getaBlankImageInBase64() {
        return aBlankImageInBase64;
    }

    public void setaBlankImageInBase64(String aBlankImageInBase64) {
        this.aBlankImageInBase64 = aBlankImageInBase64;
    }

    public ZonedDateTime getaZonedDateTime() {
        return aZonedDateTime;
    }

    public void setaZonedDateTime(ZonedDateTime aZonedDateTime) {
        this.aZonedDateTime = aZonedDateTime;
    }

    public Map<String, Object> getaObjectMap() {
        return aObjectMap;
    }

    public void setaObjectMap(Map<String, Object> aObjectMap) {
        this.aObjectMap = aObjectMap;
    }
}