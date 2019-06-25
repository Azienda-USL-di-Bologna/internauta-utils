package it.bologna.ausl.rubrica.maven.resources;


import java.util.List;




import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class FullContactResource {
	private ContattoResource contatto;

	private List<EmailResource> emails;

	private List<IndirizzoResource> addresses;
	
	private RubricaResource rubrica;
	
	private List<TelefonoResource> tels;
	public FullContactResource()
	{
		
	}
	


	public ContattoResource getContatto() {
		return contatto;
	}

	public void setContatto(ContattoResource contatto) {
		this.contatto = contatto;
	}

	@XmlElement(name="email")
	public List<EmailResource> getEmails() {
		return emails;
	}

	public void setEmails(List<EmailResource> emails) {
		this.emails = emails;
	}

	@XmlElement(name="indirizzo")
	public List<IndirizzoResource> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<IndirizzoResource> addresses) {
		this.addresses = addresses;
	}

	@XmlElement(name="telefono")
	public List<TelefonoResource> getTels() {
		return tels;
	}

	public void setTels(List<TelefonoResource> tels) {
		this.tels = tels;
	}
	
	@XmlElement(name="rubrica")
	public RubricaResource getRubrica() {
		return rubrica;
	}

	public void setRubrica(RubricaResource r) {
		this.rubrica = r;
	}
}
