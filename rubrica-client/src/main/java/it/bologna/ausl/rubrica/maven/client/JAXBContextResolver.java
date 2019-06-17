package it.bologna.ausl.rubrica.maven.client;

import it.bologna.ausl.rubrica.maven.resources.FullContactResource;
import it.bologna.ausl.rubrica.maven.resources.ContattoResource;
import it.bologna.ausl.rubrica.maven.resources.TelefonoResource;
import it.bologna.ausl.rubrica.maven.resources.IndirizzoResource;
import it.bologna.ausl.rubrica.maven.resources.RubricaResource;
import it.bologna.ausl.rubrica.maven.resources.GruppoResource;
import it.bologna.ausl.rubrica.maven.resources.EmailResource;
import it.bologna.ausl.rubrica.maven.resources.IdListResource;
import it.bologna.ausl.rubrica.maven.resources.MemberResource;
import it.bologna.ausl.rubrica.maven.resources.ContattoSempliceResource;

import java.util.Set;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;

@Provider 
public class JAXBContextResolver implements ContextResolver<JAXBContext> {

private JAXBContext context;

public JAXBContextResolver() throws Exception {

//AnnotatedClassScanner classScanner = new AnnotatedClassScanner(XmlRootElement.class, XmlType.class);
//Set<Class<?>> classes = classScanner.scan(new String[]{"it.bologna.ausl.rubrica"});
	Class<?>[] classes ={
			ContattoSempliceResource.class,
			ContattoResource.class,
			EmailResource.class,
			FullContactResource.class,
			IndirizzoResource.class,
			RubricaResource.class,
			TelefonoResource.class,
			IdListResource.class,
			GruppoResource.class,
			MemberResource.class
	};
	
	
this.context = new JSONJAXBContext(JSONConfiguration.natural().build(),
classes);
}

public JAXBContext getContext(Class<?> objectType) {

return context;
}
}
