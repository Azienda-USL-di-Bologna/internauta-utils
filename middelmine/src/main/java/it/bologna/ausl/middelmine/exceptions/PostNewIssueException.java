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
public class PostNewIssueException extends MiddleMineException {
    
    public PostNewIssueException(String message) {
        super(message);
    }
    
    public PostNewIssueException(Exception exception) {
        super(exception);
    }
    
    public PostNewIssueException(String message, Exception ex) {
        super(message, ex);
    }
    
}
