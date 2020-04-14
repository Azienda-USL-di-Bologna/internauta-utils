/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.bologna.ausl.middelmine.exceptions;

/**
 *
 * @author Salo
 */
public class UploadNewAttachmentError extends MiddleMineException {

    public UploadNewAttachmentError(String message) {
        super(message);
    }

    public UploadNewAttachmentError(Exception exception) {
        super(exception);
    }

    public UploadNewAttachmentError(String message, Exception exception) {
        super(message, exception);
    }

}
