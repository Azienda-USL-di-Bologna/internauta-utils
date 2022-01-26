package it.bologna.ausl.internauta.utils.firma.remota.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.stereotype.Component;

/**
 *
 * @author spritz
 */
@Component
public class FirmaRemotaUtils {

    public String uploadToUploader(URI uploaderUri, InputStream file, String filename, ContentType contentType) throws URISyntaxException, IOException {
        /*    
        {
        "server":"mongoDownloadConnectionString",
        "plugin":"Mongo",
        "force_download":true,
        "delete_token":false,
        "params":{
            "path":"/tmp/test"
        }
         */

        JsonObject metadata = new JsonObject();
        metadata.addProperty("server", "mongoDownloadConnectionString");
        metadata.addProperty("plugin", "Mongo");
        metadata.addProperty("force_download", "true");
        metadata.addProperty("deletetoken", "false");
        JsonObject params = new JsonObject();
        params.addProperty("path", "/tmp/test");
        metadata.add("params", params);

        HttpEntity entity = MultipartEntityBuilder
                .create()
                .addTextBody("metadata", new Gson().toJson(metadata))
                .addBinaryBody("files", file, contentType, filename)
                .build();

        HttpPost httpPost = new HttpPost(uploaderUri);
        httpPost.setEntity(entity);
        HttpResponse response = httpClient.execute(httpPost);

        if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300) {
            InputStream content = response.getEntity().getContent();
            if (content != null) {
                throw new IOException("errore nella chiamata all'URL: " + uploaderUri.toString() + "\nRESPONSE\n" + IOUtils.toString(content, "UTF-8"));
            }
            else {
                throw new IOException("errore nella chiamata all'URL: " + uploaderUri.toString());
            }
        }
        else { //tutto ok
            HttpEntity result = response.getEntity();
            JsonArray uploaderResult = (JsonArray) new JsonParser().parse(new InputStreamReader(result.getContent()));
            String res = uploaderResult.get(0).getAsJsonObject().get("link").getAsString();
            return res;
        }
        
        
        
        
        
    }
}
