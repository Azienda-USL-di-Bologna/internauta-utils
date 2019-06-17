package it.bologna.ausl.rubrica.maven.resources;



import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class GruppoResource {
	
	private String descrizione;
	private int id;
	
	public GruppoResource(){}


	public String getDescrizione() {
		return descrizione;
	}

	public void setDescrizione(String descrizione) {
		this.descrizione = descrizione;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}
