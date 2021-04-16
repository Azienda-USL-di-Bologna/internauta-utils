/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.mongowrapper.test;

import com.mongodb.MongoException;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import it.bologna.ausl.mongowrapper.MongoWrapperMinIO;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 *
 * @author gdm
 */
public class TestNotReleaseConnectionPool {
    private static String minIODBUrl = "jdbc:postgresql://gdml.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified";
    //private static String minIODBUrl = "jdbc:postgresql://arena.internal.ausl.bologna.it:5432/minirepo?stringtype=unspecified";
    private static String mongoUrl = "mongodb://argo:siamofreschi@babelmongotest01-auslbo.avec.emr.it/doc?safe=true";
    //private static String mongoUrl = "mongodb://argo:siamofreschi@arena/arena?safe=true";
    public static void main(String[] args) throws UnknownHostException, MongoException, MongoWrapperException {
        test();
    }
    
    public static void test() throws UnknownHostException, MongoException, MongoWrapperException {
        boolean stop = false;
        MongoWrapperMinIO wrapper =  (MongoWrapperMinIO) MongoWrapper.getWrapper(true, mongoUrl, "org.postgresql.Driver", minIODBUrl, "minirepo", "siamofreschi", "105t", null);
        while (!stop) {
            wrapper.delDirFiles("/Procton/Documenti/2021-722");
            System.out.println("scrivere \"true\" per fermare il ciclo");
            Scanner scanner = new Scanner(System.in);
            try {
                stop = scanner.nextBoolean();
            } catch (Exception e) {
            }
            System.out.println("stop: " + stop);
        }
    }
}
