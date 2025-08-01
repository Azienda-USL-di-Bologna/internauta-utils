package it.bologna.ausl.internauta.utils.bdm.workflows.utils;

import it.bologna.ausl.internauta.utils.bdm.core.exceptions.ProcessWorkFlowException;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author gdm
 */
public class UtilityFunctions {
    public static String sendHttpMessage(String targetUrl, String username, String password, Map<String, String> parameters, String method) throws MalformedURLException, ProtocolException, IOException, ProcessWorkFlowException {

        //System.out.println("connessione...");		
        String parametersToSend = "";
        if (parameters != null) {
            Set<Map.Entry<String, String>> entrySet = parameters.entrySet();
            Iterator<Map.Entry<String, String>> iterator = entrySet.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> param = iterator.next();
                String paramName = param.getKey();
                String paramValue = param.getValue();
                parametersToSend += paramName + "=" + paramValue;
                if (iterator.hasNext()) {
                    parametersToSend += "&";
                }
            }
            parametersToSend = parametersToSend.replace(" ", "%20");
        }
        URL url = new URL(targetUrl);
        method = method.toUpperCase();
        if (method.equals("GET") || method.equals("DELETE")) {
            if (parametersToSend.length() > 0) {
                targetUrl += "?" + parametersToSend;
            }
            url = new URL(targetUrl);
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        if (username != null && !username.equals("")) {
            String userpassword;
            if (password != null) {
                userpassword = username + ":" + password;
            } else {
                userpassword = "restuser";
            }
            String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encodedAuthorization);
        }

        if (method.equals("POST")) {
            connection.setDoOutput(true);
        }
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("charset", "utf-8");
        if (method.equals("POST")) {
            connection.setRequestProperty("Content-Length", "" + Integer.toString(parametersToSend.getBytes().length));
        }
        connection.setUseCaches(false);

        if (method.equals("POST")) {
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(parametersToSend);
            wr.flush();
            wr.close();
        }

        try (InputStream resultStream = connection.getInputStream()) {
            int responseCode = connection.getResponseCode();
            //System.out.println("risposta: " + responseCode + " - " + connection.getResponseMessage());

            String responseCodeToString = String.valueOf(responseCode);

            String resultString = null;
            if (resultStream != null) 
                resultString = inputStreamToString(resultStream);
            if (!responseCodeToString.substring(0, responseCodeToString.length() - 1).equals("20")) {
                String error = inputStreamToString(connection.getErrorStream());
    //            System.out.println("error: " + error);
                throw new ProcessWorkFlowException(responseCode + " - responde error fron url \"" + url + "\":\n" + error);
            } 
            connection.disconnect();
            return resultString;
        }

    }
    
    public static String sendHttpMessage(String targetUrl, String username, String password, Map<String, byte[]> parameters, String method, String contentType) throws MalformedURLException, IOException, ProcessWorkFlowException {
//        parameters.entrySet().stream().forEach(entry -> {System.out.println(entry + " - " + new String(entry.getValue()));});
        if (contentType == null || contentType.equals(""))
            contentType = "application/x-www-form-urlencoded";
//        System.out.println("connessione...");		
        String textParameters = "";
        byte[] byteParameters = null;
        int contentLength = 0;
        if (parameters != null) {   
            if (contentType.equals("application/x-www-form-urlencoded")) {
                Set<Map.Entry<String, byte[]>> entrySet = parameters.entrySet();
                Iterator<Map.Entry<String, byte[]>> iterator = entrySet.iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, byte[]> param = iterator.next();
                    String paramName = param.getKey();
                    String paramValue = new String(param.getValue());
                    textParameters += paramName + "=" + paramValue;
                    if (iterator.hasNext()) {
                        textParameters += "&";
                    }
                }
                textParameters = textParameters.replace(" ", "%20");
                contentLength = textParameters.getBytes().length;
            }
            else {
                byteParameters = parameters.values().iterator().next();
                contentLength = byteParameters.length;
            }
        }
        URL url = new URL(targetUrl);
        method = method.toUpperCase();
        if (method.equals("GET") || method.equals("DELETE")) {
            if (textParameters.length() > 0) {
                targetUrl += "?" + textParameters;
            }
            url = new URL(targetUrl);
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        if (username != null && !username.equals("")) {
            String userpassword;
            if (password != null) {
                userpassword = username + ":" + password;
            } else {
                userpassword = "restuser";
            }
            String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encodedAuthorization);
        }

        if (method.equals("POST")) {
            connection.setDoOutput(true);
        }
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", contentType);
        if (contentType.equals("application/x-www-form-urlencoded")) {
            connection.setRequestProperty("charset", "utf-8");
        }
        if (method.equals("POST")) {
            connection.setRequestProperty("Content-Length", "" + Integer.toString(contentLength));
        }
        connection.setUseCaches(false);

        if (method.equals("POST")) {
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            if (!textParameters.equals("")) {
                wr.writeBytes(textParameters);
                wr.flush();
                wr.close();
            }
            else {
                wr.write(byteParameters);
            }
        }

        int responseCode = connection.getResponseCode();
        String responseCodeToString = String.valueOf(responseCode);
        if (!responseCodeToString.substring(0, responseCodeToString.length() - 1).equals("20")) {
            String error = inputStreamToString(connection.getErrorStream());
//            System.out.println("error: " + error);
            throw new ProcessWorkFlowException(responseCode + " - responde error fron url \"" + url + "\":\n" + error);
        }
        
        try (InputStream resultStream = connection.getInputStream()) {
    //        System.out.println("risposta: " + responseCode + " - " + connection.getResponseMessage());

            String resultString = null;
            if (resultStream != null) 
                resultString = inputStreamToString(resultStream);

            connection.disconnect();
            return resultString;
        }
    }
    /**
     * Converte un InputStream in una stringa
     *
     * @param is l'InputStream da convertire
     * @return L'inputStream convertito in stringa
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public static String inputStreamToString(InputStream is) throws UnsupportedEncodingException, IOException {
        Writer stringWriter = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                stringWriter.write(buffer, 0, n);
            }
        } finally {
        }
        return stringWriter.toString();
    }
}
