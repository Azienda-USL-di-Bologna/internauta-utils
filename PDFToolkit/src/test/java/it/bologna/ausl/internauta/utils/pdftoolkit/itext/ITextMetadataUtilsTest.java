package it.bologna.ausl.internauta.utils.pdftoolkit.itext;

import com.itextpdf.xmp.options.SerializeOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;

import static it.bologna.ausl.internauta.utils.pdftoolkit.itext.ITextMetadataUtils.getSerializeOptions;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author ferri
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ITextMetadataUtilsTest {
    @Test
    void getSerializeOptionsTest() {
        SerializeOptions serializeOptions = getSerializeOptions();
        assertAll(
                () -> assertTrue(serializeOptions.getSort()),
                () -> assertTrue(serializeOptions.getReadOnlyPacket()),
                () -> assertTrue(serializeOptions.getUseCompactFormat()),
                () -> assertTrue(serializeOptions.getExactPacketLength()),
                () -> assertFalse(serializeOptions.getOmitPacketWrapper()),
                () -> assertFalse(serializeOptions.getEncodeUTF16BE()),
                () -> assertFalse(serializeOptions.getEncodeUTF16LE()),
                () -> assertTrue(serializeOptions.getReadOnlyPacket()),
                () -> assertFalse(serializeOptions.getOmitVersionAttribute()),
                () -> assertFalse(serializeOptions.getOmitXmpMetaElement()),
                () -> assertFalse(serializeOptions.getIncludeThumbnailPad()),
                () -> assertFalse(serializeOptions.getUseCanonicalFormat()),
                () -> assertEquals(2048, serializeOptions.getPadding()),
                () -> assertEquals(8800, serializeOptions.getOptions()),
                () -> assertEquals("\n", serializeOptions.getNewline()),
                () -> assertEquals(StandardCharsets.UTF_8.name(), serializeOptions.getEncoding()),
                () -> assertEquals(0, serializeOptions.getBaseIndent())
        );
    }
}