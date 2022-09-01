package it.bologna.ausl.internauta.utils.firma.data.jnj;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

/**
 *
 * @author gdm
 */
public class SignParamsComponent {
    
    @JsonIgnore
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EndSign {
        public static enum EndSignResults {
            ALL_SIGNED, PARTIALLY_SIGNED, ERROR, ABORT
        }
        
        private String callBackUrl;
        private Map<String, Object> endSignParams;
        private List<SignDocument> signedFileList;
        private EndSignResults endSignResult;

        public EndSign() {
        }
        
        public String getCallBackUrl() {
            return callBackUrl;
        }

        public void setCallBackUrl(String callBackUrl) {
            this.callBackUrl = callBackUrl;
        }

        public Map<String, Object> getEndSignParams() {
            return endSignParams;
        }

        public void setEndSignParams(Map<String, Object> endSignParams) {
            this.endSignParams = endSignParams;
        }

        public List<SignDocument> getSignedFileList() {
            return signedFileList;
        }

        public void setSignedFileList(List<SignDocument> signedFileList) {
            this.signedFileList = signedFileList;
        }

        public EndSignResults getEndSignResult() {
            return endSignResult;
        }

        public void setEndSignResult(EndSignResults endSignResult) {
            this.endSignResult = endSignResult;
        }
        
        public static EndSign parse(String str) throws JsonProcessingException {
            return getObjectMapper().readValue(str, EndSign.class);
        }
        
         public String toJsonString() throws JsonProcessingException {
            return SignParamsComponent.getObjectMapper().writeValueAsString(this);
        }
    
        public byte[] toJsonByte() throws JsonProcessingException {
            return SignParamsComponent.getObjectMapper().writeValueAsBytes(this);
        }
    } 
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SignDocument {
        public static enum SignTypes {CADES, PADES, XADES}
        public static enum Sources {URI, FILE_SYSTEM, BASE_64}
         public static enum SignDocumentResults {
            SIGNED, ERROR, SKIPPED
        }
        private String file;
	private Sources source;
        private String name;
	private String type;
        private String id;
        private String uploaderResult; // risultato tornato dalla chiamata a signedFileUploaderUrl
        private String mimeType;
        private SignTypes signType;
        private SignFileAttributes signAttributes;
        private SignDocumentResults signDocumentResult;
        private Map<String, Object> additionalData;

        public SignDocument() {
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public Sources getSource() {
            return source;
        }

        public void setSource(Sources source) {
            this.source = source;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUploaderResult() {
            return uploaderResult;
        }

        public void setUploaderResult(String uploaderResult) {
            this.uploaderResult = uploaderResult;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public SignTypes getSignType() {
            return signType;
        }

        public void setSignType(SignTypes signType) {
            this.signType = signType;
        }

        public SignFileAttributes getSignAttributes() {
            return signAttributes;
        }

        public void setSignAttributes(SignFileAttributes signAttributes) {
            this.signAttributes = signAttributes;
        }

        public SignDocumentResults getSignDocumentResult() {
            return signDocumentResult;
        }

        public void setSignDocumentResult(SignDocumentResults signDocumentResult) {
            this.signDocumentResult = signDocumentResult;
        }

        public Map<String, Object> getAdditionalData() {
            return additionalData;
        }

        public void setAdditionalData(Map<String, Object> additionalData) {
            this.additionalData = additionalData;
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SignFileAttributes {
        private Boolean visible;
        private String textTemplate;
        private SignFileAttributesPosition position;

        public SignFileAttributes() {
        }

        public Boolean getVisible() {
            return visible;
        }

        public void setVisible(Boolean visible) {
            this.visible = visible;
        }

        public String getTextTemplate() {
            return textTemplate;
        }

        public void setTextTemplate(String textTemplate) {
            this.textTemplate = textTemplate;
        }

        public SignFileAttributesPosition getPosition() {
            return position;
        }

        public void setPosition(SignFileAttributesPosition position) {
            this.position = position;
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SignFileAttributesPosition {
        public static enum AlignmentVerticalPositions {TOP, BOTTOM, MIDDLE, NONE}
        public static enum AlignmentHorizontalPositions {LEFT, RIGHT, CENTER, NONE}
        private AlignmentVerticalPositions alignmentVertical;
        private AlignmentHorizontalPositions alignmentHorizontal;
        private Integer fieldOriginX;
        private Integer fieldOriginY;
        private Integer fieldWidth;
        private Integer fieldHeight;
        private Integer page;  // from 1 to n..., 0 not allowed, -1 is the last page

        public SignFileAttributesPosition() {
        }

        public AlignmentVerticalPositions getAlignmentVertical() {
            return alignmentVertical;
        }

        public void setAlignmentVertical(AlignmentVerticalPositions alignmentVertical) {
            this.alignmentVertical = alignmentVertical;
        }

        public AlignmentHorizontalPositions getAlignmentHorizontal() {
            return alignmentHorizontal;
        }

        public void setAlignmentHorizontal(AlignmentHorizontalPositions alignmentHorizontal) {
            this.alignmentHorizontal = alignmentHorizontal;
        }

        public Integer getFieldOriginX() {
            return fieldOriginX;
        }

        public void setFieldOriginX(Integer fieldOriginX) {
            this.fieldOriginX = fieldOriginX;
        }

        public Integer getFieldOriginY() {
            return fieldOriginY;
        }

        public void setFieldOriginY(Integer fieldOriginY) {
            this.fieldOriginY = fieldOriginY;
        }

        public Integer getFieldWidth() {
            return fieldWidth;
        }

        public void setFieldWidth(Integer fieldWidth) {
            this.fieldWidth = fieldWidth;
        }

        public Integer getFieldHeight() {
            return fieldHeight;
        }

        public void setFieldHeight(Integer fieldHeight) {
            this.fieldHeight = fieldHeight;
        }

        public Integer getPage() {
            return page;
        }

        public void setPage(Integer page) {
            this.page = page;
        }
    }
    
    
}
