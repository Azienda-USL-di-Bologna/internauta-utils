package it.bologna.ausl.rubrica.maven.resources;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IdListResource {
	private List<Integer> list;
	
	public IdListResource(){
		
	}

	public IdListResource(List<Integer> l)
	{
		list=l;
	}
	
	public List<Integer> getList() {
		return list;
	}

	public void setList(List<Integer> list) {
		this.list = list;
	}
	
	
	
	

}
