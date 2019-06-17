package it.bologna.ausl.rubrica.maven.resources;


import javax.xml.bind.annotation.XmlRootElement;




@XmlRootElement
public class RubricaResource {
	private String descrizione;
	private String idStruttura;
	private String nome;
	private String tipo;
	private int id;
	
	

	public String getDescrizione() {
		return descrizione;
	}

	public void setDescrizione(String descrizione) {
		this.descrizione = descrizione;
	}

	public String getIdStruttura() {
		return idStruttura;
	}

	public void setIdStruttura(String idStruttura) {
		this.idStruttura = idStruttura;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
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

	public RubricaResource()
	{
		
	}
	
}
