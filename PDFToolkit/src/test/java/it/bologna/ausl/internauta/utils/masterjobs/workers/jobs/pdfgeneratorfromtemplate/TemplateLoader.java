/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate;

import it.bologna.ausl.internauta.utils.pdftoolkit.openpdf.OpenPdfPdfUtils;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author gdm
 */
public class TemplateLoader {
    private static String minIODBDriver = "org.postgresql.Driver";
    private static String minIODBUrl = "jdbc:postgresql://nuc.nextsw.it:5432/minirepo?stringtype=unspecified";
    private static String minIODBUsername = "minirepo";
    private static String minIODBPassword = "siamofreschi";
    private static Integer minIOPoolSize = 5;
        
    public static void main(String[] args) throws MinIOWrapperException, IOException {
        //uploadFile();
        checkPdf();
    }
    
    public static void checkPdf() throws FileNotFoundException, IOException {
        try(FileInputStream fis = new FileInputStream("password3.pdf");) {
            OpenPdfPdfUtils.OpenedPdfStatus pdfOpenable = OpenPdfPdfUtils.checkPdf(fis);
            System.out.println(pdfOpenable);
        }
    }
    
    public static void merge() throws FileNotFoundException, IOException {
        ArrayList files = new ArrayList();
        files.add(new File("password3.pdf"));
        files.add(new File("okA_2.pdf"));
        files.add(new File("okA.pdf"));
        try(FileInputStream fis = new FileInputStream("password2.pdf");
                FileInputStream icc = new FileInputStream("AdobeRGB1998.icc")) {
            byte[] mergePdfOpenPdf = OpenPdfPdfUtils.mergePdfOpenPdf(files, icc, "ciao");
            IOUtils.write(mergePdfOpenPdf, new FileOutputStream("merge.pdf"));
        }
    }
    
    public static void uploadFile() throws MinIOWrapperException, IOException {
        MinIOWrapper wrapper = new MinIOWrapper(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, minIOPoolSize);
        wrapper.putWithBucket(new File("AvCp_dataset.xhtml"), "pdftoolkit",
                "/resources/reporter/internauta/templates/AvCp_dataset.xhtml", 
                "AvCp_dataset.xhtml", null, true, "next-babel-veneto-stg-100");
    }
    
}
