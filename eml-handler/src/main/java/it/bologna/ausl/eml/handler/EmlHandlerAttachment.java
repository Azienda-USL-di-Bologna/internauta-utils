package it.bologna.ausl.eml.handler;

/** 
 * @author andrea zucchelli
 * <br>
 * <p>
 * Questa classe descrive un attachment.<br>
 * <b>fileName</b> contiene il nome del file orignale come specificato nell'email.<br>
 * <b>filePath</b> contiene il pathc completo al file. <b>N.B.</b> il nome del file puo' differire da fileName.<br>
 * <b>mimeType</b> contiene il content type dell'attachment.
 * </p>
 */
public class EmlHandlerAttachment {
	
        private Integer id;
	private String fileName;
	private String filePath;
	private String mimeType;
        private Integer size;

        public Integer getId() {
            return id;
        }
        public void setId(Integer id) {
            this.id = id;
        }
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getMimeType() {
		return mimeType;
	}
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
        public Integer getSize() {
            return size;
        }
        public void setSize(Integer size) {
            this.size = size;
        }
	
	public String toString()
	{
		return "filename: "+fileName+" filepath: "+filePath+" mimetype: "+mimeType + " size: "+size;
	}
}
