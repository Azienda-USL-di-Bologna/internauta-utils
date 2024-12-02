/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.pdfgeneratorfromtemplate;

import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author gdm
 */
public class TemplateLoader {
    public static void main(String[] args) throws MinIOWrapperException, IOException {
        //MinIOWrapper("org.postgresql.Driver", "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified", "minirepo", "siamofreschi", 5);
        String minIODBDriver = "org.postgresql.Driver";
        String minIODBUrl = "jdbc:postgresql://nuc.nextsw.it:5432/minirepo?stringtype=unspecified";
        String minIODBUsername = "minirepo";
        String minIODBPassword = "siamofreschi";
        Integer minIOPoolSize = 5;
        MinIOWrapper wrapper = new MinIOWrapper(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, minIOPoolSize);
        wrapper.putWithBucket(new File("AvCp_dataset.xhtml"), "pdftoolkit",
                "/resources/reporter/internauta/templates/AvCp_dataset.xhtml", 
                "AvCp_dataset.xhtml", null, true, "next-babel-veneto-stg-100");
    }
   
}
