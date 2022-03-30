/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.estrattore;

import it.bologna.ausl.eml.handler.EmlHandler;
import it.bologna.ausl.eml.handler.EmlHandlerException;
import it.bologna.ausl.eml.handler.EmlHandlerResult;
import it.bologna.ausl.estrattore.exception.ExtractorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 *
 * @author Top
 */
public class Main {

    public static void main(String[] args) throws ExtractorException, IOException, EmlHandlerException {

        File folder = new File("prova");
        File file = new File("boh.eml");
        String actual = new String(Files.readAllBytes(file.toPath()));
        EmlHandler emlHandler = new EmlHandler();
        emlHandler.setParameters(actual, null);
        EmlHandlerResult res = emlHandler.handleRawEml();
        System.out.println("res: " + res);
        System.out.println("res: " + res.getMessageId());
        ExtractorCreator extractorCreator = new ExtractorCreator(file);
        extractorCreator.extractAll(folder);
        for (File outFile : folder.listFiles()) {
            System.out.println(outFile.getName());
        }
//    ExtractorCreator zip = new ExtractorCreator(file);
//    zip.extractAll(folder);//        ExtractorCreator<InputStream> toMerge = new ArrayList();
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
