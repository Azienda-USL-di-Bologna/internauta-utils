package it.bologna.ausl.rubrica.maven.resources;



import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IndirizzoResource {
	
	
	private String civico;
	private String nazione;
	private String descrizione;
	private String comune;
	private String provincia;
	private String tipo;
	private Boolean predefinito;
	private String via;
	private String cap;
	private int id;
	private String provenienza;


	public String getProvenienza() {
		return provenienza;
	}

	public void setProvenienza(String provenienza) {
		this.provenienza = provenienza;
	}

	public IndirizzoResource()
	{
		
	}
	
	public String getCivico() {
		return civico;
	}

	public void setCivico(String civico) {
		this.civico = civico;
	}

	public String getNazione() {
		return nazione;
	}

	public void setNazione(String nazione) {
		this.nazione = nazione;
	}

	public String getDescrizione() {
		return descrizione;
	}

	public void setDescrizione(String descrizione) {
		this.descrizione = descrizione;
	}

	public String getComune() {
		return comune;
	}

	public void setComune(String comune) {
		this.comune = comune;
	}

	public String getProvincia() {
		return provincia;
	}

	public void setProvincia(String provincia) {
		this.provincia = provincia;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public Boolean getPredefinito() {
		return predefinito;
	}

	public void setPredefinito(Boolean predefinito) {
		this.predefinito = predefinito;
	}

	public String getVia() {
		return via;
	}

	public void setVia(String via) {
		this.via = via;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCap() {
		return cap;
	}

	public void setCap(String cap) {
		this.cap = cap;
	}

}
