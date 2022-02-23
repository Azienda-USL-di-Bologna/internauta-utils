package it.bologna.ausl.internauta.utils.firma.remota.utils;

//import it.bologna.ausl.internauta.utils.authorizationutils.DownloaderTokenCreator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;

import org.springframework.stereotype.Component;

/**
 *
 * @author gdm
 */
@Component
public class FirmaRemotaUtils {

    @Value("classpath:downloader/DOWNLOADER_BABEL.crt")
    private Resource downloaderPublicCertBabelProd;

    @Value("classpath:downloader/DOWNLOADER_TEST.crt")
    private Resource downloaderPublicCertBabelTest;
    
    public static enum DonwloaderTarget {
        MinIO, Default
    }
    
//    public String uploadToUploader(URI uploaderUri, InputStream file, String filename, ContentType contentType) throws URISyntaxException, IOException {
//        /*
//        {
//        "server":"mongoDownloadConnectionString",
//        "plugin":"Mongo",
//        "force_download":true,
//        "delete_token":false,
//        "params":{
//        "path":"/tmp/test"
//        }
//         */
//        File tmpFileToUpload = null;
//        try {
//            tmpFileToUpload = File.createTempFile(getClass().getSimpleName() + "to_uploader_", ".tmp");
//            
//            RequestBody dataBody = RequestBody.create(okhttp3.MultipartBody.FORM, tmpFileToUpload);
//            MultipartBody multipartBody = new MultipartBody.Builder()
//           .addPart(MultipartBody.Part.createFormData("file", null, dataBody))
//           .build();
//            
//            DownloaderTokenCreator downloaderTokenCreator = new DownloaderTokenCreator();
//            downloaderTokenCreator.getToken(getUploaderContext(null), publicCertPath, singTokenPrivateKey, tokenEncryptionPublickey, sec, "firma-internauta");
//        }
//        finally {
//            IOUtils.closeQuietly(file);
//            if (tmpFileToUpload != null && tmpFileToUpload.exists()) {
//                tmpFileToUpload.delete();
//            }
//        }
//        JsonObject metadata = new JsonObject();
//        metadata.addProperty("server", "mongoDownloadConnectionString");
//        metadata.addProperty("plugin", "Mongo");
//        metadata.addProperty("force_download", "true");
//        metadata.addProperty("deletetoken", "false");
//        JsonObject params = new JsonObject();
//        params.addProperty("path", "/tmp/test");
//        metadata.add("params", params);
//
//        HttpEntity entity = MultipartEntityBuilder
//                .create()
//                .addTextBody("metadata", new Gson().toJson(metadata))
//                .addBinaryBody("files", file, contentType, filename)
//                .build();
//
//        HttpPost httpPost = new HttpPost(uploaderUri);
//        httpPost.setEntity(entity);
//        HttpResponse response = httpClient.execute(httpPost);
//
//        if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300) {
//            InputStream content = response.getEntity().getContent();
//            if (content != null) {
//                throw new IOException("errore nella chiamata all'URL: " + uploaderUri.toString() + "\nRESPONSE\n" + IOUtils.toString(content, "UTF-8"));
//            }
//            else {
//                throw new IOException("errore nella chiamata all'URL: " + uploaderUri.toString());
//            }
//        }
//        else { //tutto ok
//            HttpEntity result = response.getEntity();
//            JsonArray uploaderResult = (JsonArray) new JsonParser().parse(new InputStreamReader(result.getContent()));
//            String res = uploaderResult.get(0).getAsJsonObject().get("link").getAsString();
//            return res;
//        }}
//        
        
    public Map<String, Object> getUploaderContext(Map<String, Object> metadata) {        
        Map<String, Object> context = new HashMap();
        if (metadata != null) {
            Map<String, Object> minIOParams = new HashMap();
            minIOParams.put("metadata", metadata);
            context.put("params", minIOParams);
        }
        context.put("target", DonwloaderTarget.Default);
        return context;
    }  
    
}
