package it.bologna.ausl.rubrica.maven.resources;



import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class EmailResource {

	
	private String descrizione;
	private String email;
	private Boolean predefinito;
	private String provenienza;
	private String tipo;
	private int id;
	private Boolean pec;
	
	



	public Boolean getPec() {
		return pec;
	}

	public void setPec(Boolean pec) {
		this.pec = pec;
	}

	public EmailResource()
	{
		
	}
	
	public String getDescrizione() {
		return descrizione;
	}

	public void setDescrizione(String descrizione) {
		this.descrizione = descrizione;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Boolean getPredefinito() {
		return predefinito;
	}

	public void setPredefinito(Boolean predefinito) {
		this.predefinito = predefinito;
	}

	public String getProvenienza() {
		return provenienza;
	}

	public void setProvenienza(String provenienza) {
		this.provenienza = provenienza;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	
	
	
}
