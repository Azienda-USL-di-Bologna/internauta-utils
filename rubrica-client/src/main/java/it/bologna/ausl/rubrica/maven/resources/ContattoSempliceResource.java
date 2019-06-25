package it.bologna.ausl.rubrica.maven.resources;



import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class ContattoSempliceResource {
	private ContattoResource contatto;
    private RubricaResource rubrica;
	private List<String> pec;
	private List<String> emails;
	private List<String> addresses;
	private List<String> tels;
	private List<String> fax;
	
	
	public ContattoSempliceResource()
	{}

	public ContattoResource getContatto() {
		return contatto;
	}

	public void setContatto(ContattoResource contatto) {
		this.contatto = contatto;
	}

	public RubricaResource getRubrica() {
		return rubrica;
	}

	public void setRubrica(RubricaResource rubrica) {
		this.rubrica = rubrica;
	}
	@XmlElement(name="pec")
	public List<String> getPec() {
		return pec;
	}

	public void setPec(List<String> pec) {
		this.pec = pec;
	}
	@XmlElement(name="email")
	public List<String> getEmails() {
		return emails;
	}

	public void setEmails(List<String> emails) {
		this.emails = emails;
	}
	@XmlElement(name="indirizzo")
	public List<String> getAddresses() {
		return addresses;
	}

	public void setAddresses(List<String> addresses) {
		this.addresses = addresses;
	}
	@XmlElement(name="telefono")
	public List<String> getTels() {
		return tels;
	}

	public void setTels(List<String> tels) {
		this.tels = tels;
	}
	@XmlElement(name="fax")
	public List<String> getFax() {
		return fax;
	}

	public void setFax(List<String> fax) {
		this.fax = fax;
	}

}
