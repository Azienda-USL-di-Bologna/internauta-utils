package it.bologna.ausl.riversamento.sender;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

/**
 *
 * @author andrea
 */
public class Pacco {

    private String versione = "1.3";
    private String loginName, password, xmlsip;
    private ArrayList<PaccoFile> files;

    public String getVersione() {
        return versione;
    }

    public void setVersione(String versione) {
        this.versione = versione;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getXmlsip() {
        return xmlsip;
    }

    public void setXmlsip(String xmlsip) {
        this.xmlsip = xmlsip;
    }

    public ArrayList<PaccoFile> getFiles() {
        if (files == null) {
            files = new ArrayList();
        }
        return files;
    }

    public void setFiles(ArrayList<PaccoFile> files) {
        this.files = files;
    }

    public void addFile(PaccoFile f) {
        getFiles().add(f);
    }

    public ArrayList<NameValuePair> getFormValues() throws UnsupportedEncodingException {
        ArrayList<NameValuePair> res = new ArrayList();

        res.add(new BasicNameValuePair("VERSIONE", versione));
        res.add(new BasicNameValuePair("LOGINNAME", loginName));
        res.add(new BasicNameValuePair("PASSWORD", password));
        res.add(new BasicNameValuePair("XMLSIP", xmlsip));
        return res;

    }
}
