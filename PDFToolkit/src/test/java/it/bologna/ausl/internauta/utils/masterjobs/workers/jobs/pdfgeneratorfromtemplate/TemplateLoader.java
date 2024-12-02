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
import java.io.IOException;

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
        try(FileInputStream fis = new FileInputStream("protetto_da_password.pdf")) {
            boolean pdfOpenable = OpenPdfPdfUtils.isPdfOpenable(fis);
            System.out.println(pdfOpenable);
        }
    }
    
    public static void uploadFile() throws MinIOWrapperException, IOException {
        MinIOWrapper wrapper = new MinIOWrapper(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, minIOPoolSize);
        wrapper.putWithBucket(new File("AvCp_dataset.xhtml"), "pdftoolkit",
                "/resources/reporter/internauta/templates/AvCp_dataset.xhtml", 
                "AvCp_dataset.xhtml", null, true, "next-babel-veneto-stg-100");
    }
    
}
