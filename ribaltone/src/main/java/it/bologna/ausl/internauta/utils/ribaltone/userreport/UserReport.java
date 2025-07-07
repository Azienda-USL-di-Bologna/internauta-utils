/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.userreport;

import java.io.File;

/**
 *
 * @author Top
 */
public abstract class UserReport {
    
    public static enum UserReportType {
        CSV,
        HTML
    }
    
    public abstract File getCSV();
    
    public abstract String getHTML();
    
}
