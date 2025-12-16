/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.common.utils;

import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import jakarta.activation.DataHandler;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

/**
 *
 * @author utente
 */
/**
 *
 * @author Salo 
 */
@Component
public class SimpleMailSenderUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMailSenderUtility.class);

    //per parametri pubblici?
    

    public Boolean sendMail(
            String fromName, String subject, List<String> to, String body,
            List<String> cc, List<String> bcc, List<MultipartFile> attachments, List<String> replyTo, AziendaParametriJson.MailParams mailParams,
            boolean htmlBody) throws IOException {

        

        if (mailParams != null) {
            String smtpServer = mailParams.getMailServerSmtpUrl();
            Integer port = mailParams.getMailServerSmtpPort();

            String username = mailParams.getUsername();
            String password = mailParams.getPassword();

            Properties prop = System.getProperties();
            prop.put("mail.smtp.host", smtpServer);                                 //optional, defined in SMTPTransport
            prop.put("mail.smtp.timeout", 3000);
            prop.put("mail.smtp.connectiontimeout", 3000);
            if (!StringUtils.hasLength(username)) {
                prop.put("mail.smtp.auth", "false");
            } else {
                prop.put("mail.smtp.auth", "true");
            }
            
            if (mailParams.getSslAuth() != null && mailParams.getSslAuth()) {
                prop.put("mail.smtp.ssl.enable", "true");
            }

            if (port != null && port != -1) {
                prop.put("mail.smtp.port", port.toString());                        // default port 25
            } else {
                prop.put("mail.smtp.port", "25");
            }
            
            Session session = null;
            
            if (StringUtils.hasLength(username)) {
                // Get the Session object and pass username and password
                    session = Session.getInstance(prop, new jakarta.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
            } else {
                session = Session.getInstance(prop, null);
            }
            
            Message msg = new MimeMessage(session);

            try {
                String mailFrom = mailParams.getMailFrom();
                // from
                msg.setFrom(new InternetAddress(mailFrom, fromName));

                if (replyTo != null) {
                    String addressesReplyTo = "";
                    for (String toElement : replyTo) {
                        addressesReplyTo += toElement + ",";
                    }
                    addressesReplyTo = addressesReplyTo.substring(0, addressesReplyTo.length() - 1);
                    msg.setReplyTo(InternetAddress.parse(addressesReplyTo, false));
                }

                // inserisco lista TO
                if (to != null && !to.isEmpty()) {
                    String addressesTo = "";
                    for (String toElement : to) {
                        addressesTo += toElement + ",";
                    }
                    addressesTo = addressesTo.substring(0, addressesTo.length() - 1);
                    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(addressesTo, false));
                }

                //inserico lista CC e BCC
                if (cc != null && !cc.isEmpty()) {
                    String addressesCC = "";
                    for (String ccElement : cc) {
                        addressesCC += ccElement + ",";
                    }
                    if (bcc != null && !bcc.isEmpty()) {
                        for (String bccElement : bcc) {
                            addressesCC += bccElement + ",";
                        }
                    }
                    addressesCC = addressesCC.substring(0, addressesCC.length() - 1);
                    msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(addressesCC, false));
                }

                //            if (bcc!=null && !bcc.isEmpty()){
                //                for (String bccElement : bcc) {
                //                    msg.setRecipients(Message.RecipientType.CC,
                //                        InternetAddress.parse(bccElement, false));
                //                    }
                //            }
                // subject
                //            LocalDateTime today = LocalDateTime.now();
                //            ZoneId id = ZoneId.of("Europe/Rome");
                //            ZonedDateTime zonedDateTime = ZonedDateTime.of(today, id);      //That's how you add timezone to date
                //            String formattedDateTime = DateTimeFormatter
                //                            .ofPattern("dd/MM/yyyy - HH:mm")
                //                            .format(zonedDateTime);             //  esempio 11/03/2020 - 10.44
                msg.setSubject(subject);
                // content
                if (attachments != null && !attachments.isEmpty()) {
                    Multipart multipart = new MimeMultipart();

                    // Body
                    MimeBodyPart messageBodyPart = new MimeBodyPart();
                    if (htmlBody)
                        messageBodyPart.setContent(body, "text/html; charset=UTF-8");
                    else
                        messageBodyPart.setText(body);
                    multipart.addBodyPart(messageBodyPart);

                    // Allegati
                    MimeBodyPart attachmentPart;
                    for (MultipartFile attachment : attachments) {
                        attachmentPart = new MimeBodyPart();
                        byte[] fileBytes = attachment.getBytes();
                        String attachmentName = attachment.getOriginalFilename();
                        ByteArrayDataSource source
                                = new ByteArrayDataSource(fileBytes, attachment.getContentType());
                        attachmentPart.setDataHandler(new DataHandler(source));
                        attachmentPart.setFileName(attachmentName);
                        multipart.addBodyPart(attachmentPart);
                    }
                    msg.setContent(multipart);
                } else {
                    if (htmlBody)
                        msg.setContent(body, "text/html; charset=UTF-8");
                    else
                        msg.setText(body);
                }

                // msg.setContent(body, "text/html; charset=utf-8");
                msg.setSentDate(new Date());
                // connect
                try ( // Get SMTPTransport
//                        SMTPTransport t = (SMTPTransport) session.getTransport("smtp")) {
                    Transport t = session.getTransport("smtp")) {
                    // connect
                    t.connect(smtpServer, username, password);
                    // send
                    t.sendMessage(msg, msg.getAllRecipients());
                    LOGGER.info("Invio mail");
//                    System.out.println("Response: " + t.getLastServerResponse());
                }
            } catch (MessagingException e) {
                e.printStackTrace();
                LOGGER.info("Invio mail fallito", e);
                return false;
            }
            return true;

        }
        return false;
    }

}

