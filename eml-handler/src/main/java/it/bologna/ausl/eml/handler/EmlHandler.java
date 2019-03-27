package it.bologna.ausl.eml.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;



/**
 * @author andrea zucchelli
 * <br>
 * <p>
 * Questa classe prende in carico un file eml contentente una email.
 * Il file viene parsato, vengono estratti il contenuto testuale sia in formato testo che html,
 * le meta informazioni piu' significative e vengono estratti gli allegati.
 * Questi vengono salvati su filesystem, ogni allegao e' descritto da un oggetto di tipo {@link EmlHandlerAttachment}
 * </p>
 */


public class EmlHandler {
	
	private String rawMessage;
	private File workingDir;
	
	public EmlHandler ()
	{
		
	}
	
	public void setParameters(String message,String dir)
	{
		rawMessage=message;
		if (dir != null)
			workingDir=new File(dir);
		else
			dir=null;
	}
	
	public EmlHandlerResult handleRawEml() throws EmlHandlerException
	{
		return handleRawEml(rawMessage,workingDir);
	}
	
	public static EmlHandlerResult handleEml(String filePath)
			throws EmlHandlerException {
		FileInputStream is = null;
		MimeMessage m = null;
		EmlHandlerResult res = null;

		try {
			File in = new File(filePath);
			File dir = new File(in.getParent());

			try {
				is = new FileInputStream(filePath);
			} catch (FileNotFoundException e) {
				throw new EmlHandlerException(
						"Unable to open file " + filePath, e);
			}
			m = EmlHandlerUtils.BuildMailMessageFromInputStream(is);
			res = processEml(m, dir);
			return res;
		} finally {
			try {
				is.close();
			} catch (IOException e) {

			}
		}
	
	}
	
	
	public static EmlHandlerResult handleEml(String filePath, String workingDir)
			throws EmlHandlerException {

		FileInputStream is = null;
		MimeMessage m = null;
		EmlHandlerResult res = null;

		File dir = null;

		try {
			if (workingDir != null) {
				dir = new File(workingDir);
				if (!dir.exists() || !dir.canWrite()) {
					throw new EmlHandlerException("Working dir: '" + workingDir
							+ "' must exists and be writeable :F");
				}
			}
			try {
				is = new FileInputStream(filePath);
			} catch (FileNotFoundException e) {
				throw new EmlHandlerException(
						"Unable to open file " + filePath, e);
			}
			m = EmlHandlerUtils.BuildMailMessageFromInputStream(is);
			res = processEml(m, dir);
			return res;

		} finally {
			try {
				is.close();
			} catch (IOException e) {

			}
		}

	}
	
	
	public static EmlHandlerResult handleRawEml(String rawMessage,File working_dir) throws EmlHandlerException{
		
		MimeMessage m=null;
		EmlHandlerResult res=null;
		
		m=EmlHandlerUtils.BuildMailMessageFromString(rawMessage);
		//TODO: decidere cosa fare con il path
		res=processEml(m,working_dir);
		return res;
		
	
	}
	
	/**
	 * 
	 * @param filePath patj del file contenente il file eml da parsare e gestire
	 * @return EmlHandlerResult contenente il risultato dell'elaborazione
	 * @throws EmlHandlerException
	 */
	private static EmlHandlerResult processEml(MimeMessage m,File working_dir) throws EmlHandlerException
	{ 
		
		File dir=working_dir;
		EmlHandlerResult res=new EmlHandlerResult();
		
		try {
			res.setFrom(m.getFrom()[0]);
		} catch (MessagingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			res.setTo(m.getRecipients(Message.RecipientType.TO));
		} catch (MessagingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			res.setCc(m.getRecipients(Message.RecipientType.CC));
		} catch (MessagingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			res.setSubject(m.getSubject());
		} catch (MessagingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			res.setSendDate(m.getSentDate());
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			res.setMessageId(m.getMessageID());
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			res.setPlainText(EmlHandlerUtils.getText(m));
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			res.setHtmlText(EmlHandlerUtils.getHtml(m));
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			if (dir!=null){
				res.setAttachments(EmlHandlerUtils.getAttachments(m, dir));
			}
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
		
	}

}
