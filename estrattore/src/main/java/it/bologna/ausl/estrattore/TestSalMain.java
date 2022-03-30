/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.estrattore;

import it.bologna.ausl.estrattore.exception.ExtractorException;
import it.bologna.ausl.mimetypeutilities.Detector;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypeException;

/**
 *
 * @author Salo
 */
public class TestSalMain {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws ExtractorException, IOException, UnsupportedEncodingException, MimeTypeException {

        File[] filesVari = {new File("boh.eml"), new File("TestoDelMessaggio.txt"),
            new File("TestoDelMessaggio.pdf"), new File("boh.pdf"),
            new File("altro.pdf.p7m"), new File("mystery.pdf")};

        //        ExtractorCreator extractorCreator = new ExtractorCreator(file);
        //        Class<Extractor> supportedExtractorClass = extractorCreator.getSupportedExtractorClass();
        for (int i = 0; i < filesVari.length; i++) {
            File file = filesVari[i];
            String mimeTypeConFiles = Files.probeContentType(file.toPath());
            String mimeTypeConDetector = new Detector().getMimeType(file.getAbsolutePath());
            System.out.println("--------------------------------");
            System.out.println("\n");
            System.out.println("FileName\t ->\t" + file.getName());
            //System.out.println("\n");

            System.out.println("Files.probeContentType(file.toPath())\t->\t" + mimeTypeConFiles);
            //System.out.println("\n");

            System.out.println("Detector.getMimeType(file.getAbsolutePath())\t->\t" + mimeTypeConDetector);
            //System.out.println("\n");
        }

    }

}
