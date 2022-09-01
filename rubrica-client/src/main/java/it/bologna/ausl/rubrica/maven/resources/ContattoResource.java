package it.bologna.ausl.rubrica.maven.resources;



import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class ContattoResource {
	
	public String getCfpiva() {
		return cfpiva;
	}

	public void setCfpiva(String cfpiva) {
		this.cfpiva = cfpiva;
	}

	public String getCognomeRagione() {
		return cognomeRagione;
	}

	public void setCognomeRagione(String cognomeRagione) {
		this.cognomeRagione = cognomeRagione;
	}

	public String getDescrizione() {
		return descrizione;
	}

	public void setDescrizione(String descrizione) {
		this.descrizione = descrizione;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public Boolean getPrivato() {
		return privato;
	}

	public void setPrivato(Boolean privato) {
		this.privato = privato;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public Boolean getPropaga() {
		return propaga;
	}

	public void setPropaga(Boolean propaga) {
		this.propaga = propaga;
	}

	private String cfpiva;
	private String cognomeRagione;
	private String descrizione;
	private String nome;
	private Boolean privato;
	private String tipo;
	private Boolean propaga;
	private Integer azienda;
	private Integer riferito;
	private int id;
	private String provenienza;
	
	public String getProvenienza() {
		return provenienza;
	}

	public void setProvenienza(String provenienza) {
		this.provenienza = provenienza;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}


   public ContattoResource()
   {
	   
   }

  

  
   public Integer getAzienda() {
	return azienda;
}

public void setAzienda(Integer azienda) {
	this.azienda = azienda;
}

public Integer getRiferito() {
	return riferito;
}

public void setRiferito(Integer riferito) {
	this.riferito = riferito;
}


   
}
