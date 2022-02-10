/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.estrattore;

import it.bologna.ausl.estrattore.exception.ExtractorException;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Top
 */
public class Main {
    public static void main(String[] args) throws ExtractorException, IOException{
        
    File folder = new File("prova");
    File file = new File("ccc.eml");
    ExtractorCreator zip = new ExtractorCreator(file);
    zip.extractAll(folder);//        ExtractorCreator<InputStream> toMerge = new ArrayList();
//        for (File file : folder.listFiles()) {
//            if (file.isFile()) {
////                EmlExtractor eml = new EmlExtractor(file);
////                eml.extract(folder);
//                ZipExtractor zip = new ZipExtractor(file);
//                zip.extract(folder);
//            }
//        }
    }
}
