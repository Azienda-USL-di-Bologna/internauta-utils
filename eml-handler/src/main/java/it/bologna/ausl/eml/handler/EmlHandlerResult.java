package it.bologna.ausl.eml.handler;

import java.io.Serializable;
import java.util.Date;
import javax.mail.*;

/**
 * @author andrea zucchelli
 * <br>
 * <p>
 * Questa classe rappresenta il risultato dell'elaborazione effettuata da
 * EmlHandler.
 * </p>
 */
public class EmlHandlerResult implements Serializable {

    private String plainText;
    private String htmlText;
    private String htmlTextImgEmbedded;
    private String subject;
    private String[] to;
    private String from;
    private String[] cc;
    private String messageId;
    private Date sendDate;
    private EmlHandlerAttachment[] attachments;
    private Integer realAttachmentNumber;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String[] getTo() {
        return to;
    }

    private String[] address2string(Address[] a) {
        if (a == null) {
            return null;
        }
        String[] tmp = new String[a.length];
        for (int i = 0; i < a.length; i++) {
            tmp[i] = a[i].toString();
        }
        return tmp;
    }

    public void setTo(Address[] to) {

        this.to = address2string(to);
    }

    public void setTo(String[] to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setFrom(Address from) {
        this.from = from.toString();
    }

    public String[] getCc() {
        return cc;
    }

    public void setCc(Address[] cc) {
        this.cc = address2string(cc);
    }

    public void setCc(String[] cc) {
        this.cc = cc;
    }

    public EmlHandlerAttachment[] getAttachments() {
        return attachments;
    }

    public void setAttachments(EmlHandlerAttachment[] attachments) {
        this.attachments = attachments;
    }

    public Date getSendDate() {
        return sendDate;
    }

    public void setSendDate(Date sendDate) {
        this.sendDate = sendDate;
    }

    public String getPlainText() {
        return plainText;
    }

    public void setPlainText(String plainText) {
        this.plainText = plainText;
    }

    public String getHtmlText() {
        return htmlText;
    }

    public void setHtmlText(String htmlText) {
        this.htmlText = htmlText;
    }

    public String getHtmlTextImgEmbedded() {
        return htmlTextImgEmbedded;
    }

    public void setHtmlTextImgEmbedded(String htmlTextImgEmbedded) {
        this.htmlTextImgEmbedded = htmlTextImgEmbedded;
    }

    public Integer getRealAttachmentNumber() {
        return realAttachmentNumber;
    }

    public void setRealAttachmentNumber(Integer realAttachmentNumber) {
        this.realAttachmentNumber = realAttachmentNumber;
    }
}
