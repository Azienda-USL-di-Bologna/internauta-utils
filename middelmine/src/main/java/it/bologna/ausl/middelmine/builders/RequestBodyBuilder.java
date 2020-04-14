package it.bologna.ausl.middelmine.builders;

import it.bologna.ausl.middelmine.interfaces.BuilderInterface;
import java.io.File;
import javax.xml.crypto.OctetStreamData;
import okio.ByteString;

/**
 *
 * @author Salo
 */
public class RequestBodyBuilder implements BuilderInterface {

    public static final okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");
    public static final okhttp3.MediaType OCTET_STREAM_MEDIA_TYPE = okhttp3.MediaType.parse("application/octet-stream; charset=utf-8");

    public okhttp3.RequestBody createJSONRequestBody(String content) {
        return okhttp3.RequestBody.create(JSON, content);
    }

    public okhttp3.RequestBody createJSONRequestBody(File file) {
        return okhttp3.RequestBody.create(JSON, file);
    }

    public okhttp3.RequestBody createJSONRequestBody(byte[] content) {
        return okhttp3.RequestBody.create(JSON, content);
    }

    public okhttp3.RequestBody createOctetStreamRequestBody(byte[] content) {
        return okhttp3.RequestBody.create(OCTET_STREAM_MEDIA_TYPE, content);
    }

    @Override
    public Object build(Object... object) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
