/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testChiara;

import com.mongodb.MongoException;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import it.bologna.ausl.mongowrapper.MongoWrapperMinIO;
import it.bologna.ausl.mongowrapper.exceptions.MongoWrapperException;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;

/**
 *
 * @author utente
 */
public class testChiara {
    public static void main(String[] args) throws UnknownHostException, MongoException, MongoWrapperException, IOException {
        String minIODBUrl = "jdbc:postgresql://babel-big-auslbo.avec.emr.it:5432/minirepo?stringtype=unspecified";
        String minIODBUsername= "minirepo";
        String  minIODBDriver= "org.postgresql.Driver";
        String mongoUri = "mongodb://argo:siamocaldi@babelmongo1,babelmongo2/prod?safe=true&replicaSet=prod0";
        String minIODBPassword = "4fHkXbSEA6D3";
        MongoWrapperMinIO m = (MongoWrapperMinIO) MongoWrapper.getWrapper(true, mongoUri, minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, "109", null);
        String put = m.put(new File("Deliberazioni0000258_2022_relata_1880_2022.pdf"), "Deliberazioni0000258_2022_relata_1880_2022.pdf", "/Relate", true);
        System.out.println(put);
    }
}
