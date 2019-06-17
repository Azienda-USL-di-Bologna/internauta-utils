package it.bologna.ausl.rubrica.maven.resources;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MemberResource {
	private int id;
	public MemberResource(){}
	public MemberResource(int id)
	{
		this.id=id;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	

}
