package it.bologna.ausl.rubrica.maven.resources;


import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class TelefonoResource {

	
	private String descrizione;
	private String ntel;
	private String tipo;
	private Boolean predefinito;
	private Boolean fax;
	private String provenienza;
	
	
	
	
	
	
	
	public String getProvenienza() {
		return provenienza;
	}

	public void setProvenienza(String provenienza) {
		this.provenienza = provenienza;
	}

	public Boolean getFax() {
		return fax;
	}

	public void setFax(Boolean fax) {
		this.fax = fax;
	}

	public Boolean getPredefinito() {
		return predefinito;
	}

	public void setPredefinito(Boolean predefinito) {
		this.predefinito = predefinito;
	}

	private int id;

	public TelefonoResource()
	{
		
	}
	
	public String getDescrizione() {
		return descrizione;
	}

	public void setDescrizione(String descrizione) {
		this.descrizione = descrizione;
	}

	public String getNtel() {
		return ntel;
	}

	public void setNtel(String ntel) {
		this.ntel = ntel;
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
