package it.bologna.ausl.internauta.utils.versatore.plugins.sdico;

import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.SdicoFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

/**
 *
 * @author Andrea
 */
public class Metadati {
    private String  metadati;
    private ArrayList<SdicoFile> files;


    public String getMetadati() {
        return metadati;
    }

    public void setMetadati(String metadati) {
        this.metadati = metadati;
    }

    public ArrayList<SdicoFile> getFiles() {
        if (files == null) {
            files = new ArrayList();
        }
        return files;
    }

    public void setFiles(ArrayList<SdicoFile> files) {
        this.files = files;
    }

    public void addFile(SdicoFile f) {
        getFiles().add(f);
    }

    public ArrayList<NameValuePair> getFormValues() throws UnsupportedEncodingException {
        ArrayList<NameValuePair> res = new ArrayList();
        res.add(new BasicNameValuePair("metadati.xml", metadati));
        return res;

    }
}
