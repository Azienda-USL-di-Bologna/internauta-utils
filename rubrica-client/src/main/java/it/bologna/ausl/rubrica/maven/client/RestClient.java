package it.bologna.ausl.rubrica.maven.client;
import java.util.ArrayList;
import java.util.List;

import it.bologna.ausl.rubrica.maven.resources.ContattoResource;
import it.bologna.ausl.rubrica.maven.resources.ContattoSempliceResource;
import it.bologna.ausl.rubrica.maven.resources.EmailResource;
import it.bologna.ausl.rubrica.maven.resources.FullContactResource;
import it.bologna.ausl.rubrica.maven.resources.GruppoResource;
import it.bologna.ausl.rubrica.maven.resources.IndirizzoResource;
import it.bologna.ausl.rubrica.maven.resources.MemberResource;
import it.bologna.ausl.rubrica.maven.resources.RubricaResource;
import it.bologna.ausl.rubrica.maven.resources.TelefonoResource;

import javax.ws.rs.core.MediaType;

//import com.sun.jersey.api.ParamException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

public class RestClient {
	private String serverUri;
	private Client client;
	private WebResource res;
	protected String authtoken;
	private static final int QUERY_MIN_LENGTH=3;
	private static final String CONTATTO_PATH="/contatto";
	private static final String RUBRICA_PATH="/rubrica";
	private static final String EMAIL_PATH="/email";
	private static final String INDIRIZZI_PATH="/indirizzi";
	private static final String TELEFONI_PATH="/telefoni";
	private static final String GRUPPI_PATH="/gruppi";
	private static final String RUBRICA_PERSONALE_PATH="/rubrica_personale";
	private static final String AZIENDE_PATH="/aziende";
	
	public RestClient(){}	
	
	public RestClient(String serverUri,String username, String password)
	
	{ 
		JSONConfiguration.natural().build();
		ClientConfig cc = new DefaultClientConfig();
	    cc.getClasses().add(JAXBContextResolver.class);
		this.serverUri=serverUri;
		client=Client.create(cc);
		res=client.resource(serverUri);
		authtoken=res.path("/login/").queryParam("username",username).queryParam("password", password).accept(MediaType.APPLICATION_JSON).get(String.class);
		res=res.queryParam("authtoken",authtoken);
	}

	public void init(String serverUri,String username,String password)
	{ 
		JSONConfiguration.natural().build();
		ClientConfig cc = new DefaultClientConfig();
	    cc.getClasses().add(JAXBContextResolver.class);
		this.serverUri=serverUri;
		client=Client.create(cc);
		res=client.resource(serverUri);
		authtoken=res.path("/login/").queryParam("username",username).queryParam("password", password).accept(MediaType.APPLICATION_JSON).get(String.class);
		res=res.queryParam("authtoken",authtoken);
	}	
	
	public List<RubricaResource> getAddressBooks(String user)
	{
		WebResource req;
		if (user==null)
			req=res.path(RUBRICA_PATH+"/");
		else
			req=res.path(RUBRICA_PATH+"/"+user);
		return req.accept(MediaType.APPLICATION_JSON).get(new GenericType<List<RubricaResource>>(){});
	}
	
	
	
	public List<ContattoResource> getContacts (int rubricaId,String user,String dept) throws RestClientException
	 {
	  
      return getContacts(rubricaId,user,dept,null,null);
	 /* WebResource req=res.path(RUBRICA_PATH+"/"+rubricaId+"/contatti");
	  
	  if (user!=null) req=req.queryParam("utente",user);
	  if (dept!=null) req=req.queryParam("servizio",dept);
	  try{
	  return req.accept(MediaType.APPLICATION_JSON).get(new GenericType<List<ContattoResource>>(){});
	  }
	  catch ( com.sun.jersey.api.client.UniformInterfaceException e)
	  {
	   if (! e.getMessage().contains("404 Not Found")) {
	   String error=e.getResponse().getEntity(String.class);
	   throw new RestClientException(error);
	   }
	  }
	  return new ArrayList<ContattoResource>();*/
	  
	 }
	
	public List<ContattoResource> getContacts (int rubricaId,String user,String dept,Integer pagina,Integer righe) throws RestClientException
	 {
	  
	  WebResource req=res.path(RUBRICA_PATH+"/"+rubricaId+"/contatti");
	  if (pagina!=null) req=req.queryParam("pagina",String.valueOf(pagina));
	  if (righe!=null) req=req.queryParam("righe", String.valueOf(righe));
	  
	  if (user!=null) req=req.queryParam("utente",user);
	  if (dept!=null) req=req.queryParam("servizio",dept);
	  try{
	  return req.accept(MediaType.APPLICATION_JSON).get(new GenericType<List<ContattoResource>>(){});
	  }
	  catch ( com.sun.jersey.api.client.UniformInterfaceException e)
	  {
	   if (! e.getMessage().contains("404 Not Found")) {
	   String error=e.getResponse().getEntity(String.class);
	   throw new RestClientException(error);
	   }
	  }
	  return new ArrayList<ContattoResource>();
	  
	 }
	
	
	public RubricaResource getPersonalAddressBook(String user)
	{
	
		return res.path(RUBRICA_PERSONALE_PATH+"/"+user).accept(MediaType.APPLICATION_JSON).get(RubricaResource.class);
	}
	
	
	
	
	public List<FullContactResource> searchContact(String query) throws RestClientException
	{
		return searchContact(query,null,null,null,null,null,null);
	}
	
	public List<FullContactResource> searchContact(String query,String utente,String servizio) throws RestClientException
	{
		return searchContact(query,utente,servizio,null,null,null,null);
	}
	
	public List<FullContactResource> searchContact(String query,String utente,String servizio,Integer pagina,Integer righe) throws RestClientException
	{
		return searchContact(query,utente,servizio,null,null,pagina,righe);
	}
	public List<FullContactResource> searchContact(String query,String utente,String servizio,Boolean isAzienda,Integer pagina,Integer righe) throws RestClientException
	{
		return searchContact(query,utente,servizio,isAzienda,null,pagina,righe);
	}	
	public List<FullContactResource> searchContact(String query,String utente,String servizio,Boolean isAzienda) throws RestClientException
	{
		return searchContact(query,utente,servizio,isAzienda,null,null,null);
	}
	public List<FullContactResource> searchContact(String query,String utente,String servizio,Boolean isAzienda,Integer aziendaId) throws RestClientException
	{
		return searchContact(query,utente,servizio,isAzienda,aziendaId,null,null);
	}
	public List<FullContactResource> searchContact(String query,Integer pagina,Integer righe) throws RestClientException
	{
		return searchContact(query,null,null,null,null,pagina,righe);
	}
	
	public List<FullContactResource> searchContact(String query,String utente,String servizio,Boolean isAzienda,Integer aziendaId,Integer pagina,Integer righe) throws RestClientException
	{
		if (query==null) query="";
		query=query.replace("\\","");
		if (query.length() < QUERY_MIN_LENGTH && aziendaId==null) 
			throw new IllegalArgumentException("Query is too short it must be at least "+QUERY_MIN_LENGTH+"chars");
		try {
		WebResource req=res.path(CONTATTO_PATH+"/search/"+query);
		if (pagina!=null) req=req.queryParam("pagina",String.valueOf(pagina));
		if (righe!=null) req=req.queryParam("righe", String.valueOf(righe));
		if (aziendaId!=null) req=req.queryParam("aziendaid",String.valueOf(aziendaId));
		if (isAzienda!=null) req=req.queryParam("isAzienda", String.valueOf(isAzienda));
		if (utente!=null) req=req.queryParam("utente",utente);
		if (servizio!=null) req=req.queryParam("servizio", servizio);
 		return req.accept(MediaType.APPLICATION_JSON).get(new GenericType<List<FullContactResource>>(){});
		}
		catch ( com.sun.jersey.api.client.UniformInterfaceException e)
		{
			if (! e.getMessage().contains("404 Not Found")) {
				   String error=e.getResponse().getEntity(String.class);
				   throw new RestClientException(error);
				   }
			
		}
		return new ArrayList<FullContactResource>();
		
	}
	
	public List<FullContactResource> searchContactbyMail(String query, String utente,String servizio) throws RestClientException
	{

		
		try {
		WebResource req=res.path(CONTATTO_PATH+"/searchmail/"+query);
		if (utente!=null) req=req.queryParam("utente",utente);
		if (servizio!=null) req=req.queryParam("servizio", servizio);
 		return req.accept(MediaType.APPLICATION_JSON).get(new GenericType<List<FullContactResource>>(){});
		}
		catch ( com.sun.jersey.api.client.UniformInterfaceException e)
		{
			if (! e.getMessage().contains("404 Not Found")) {
				   String error=e.getResponse().getEntity(String.class);
				   throw new RestClientException(error);
				   }
			
		}
		return new ArrayList<FullContactResource>();
		
	}
	
	public List<ContattoSempliceResource> simpleSearchContact(String query) throws RestClientException
	{
		return simpleSearchContact(query,null,null);
	}
	
	public List<ContattoSempliceResource> simpleSearchContact(String query,Integer pagina,Integer righe) throws RestClientException
	{
		query=query.replace("\\","");
		if (query.length() < QUERY_MIN_LENGTH) 
			throw new IllegalArgumentException("Query is too short it must be at least "+QUERY_MIN_LENGTH+"chars");
		try {
			WebResource req=res.path(CONTATTO_PATH+"/simplesearch/"+query);
			if (pagina!=null) req=req.queryParam("pagina",String.valueOf(pagina));
			if (righe!=null) req=req.queryParam("righe", String.valueOf(righe));
			return req.accept(MediaType.APPLICATION_JSON).get(new GenericType<List<ContattoSempliceResource>>(){});
		}
		catch ( com.sun.jersey.api.client.UniformInterfaceException e)
		{
			if (! e.getMessage().contains("404 Not Found")) {
				   String error=e.getResponse().getEntity(String.class);
				   throw new RestClientException(error);
				   }
			
		}
		return new ArrayList<ContattoSempliceResource>();
		
	}
	
	
	//Contact
	public ContattoResource addContact(int rubricaId,ContattoResource c) throws RestClientException
	{
	
		try {
			return res.path(RUBRICA_PATH+"/"+rubricaId+"/contatti").accept(MediaType.APPLICATION_JSON).post(ContattoResource.class,c);
		} catch (UniformInterfaceException e) {
			String error=e.getResponse().getEntity(String.class);
			throw new RestClientException(error);
		} 
	}
	
	
	public FullContactResource getContact(int contattoId)
	{
		return res.path(CONTATTO_PATH+"/"+contattoId).accept(MediaType.APPLICATION_JSON).get(FullContactResource.class);
	}
	
	public void deleteContact(int contattoId)
	{
		
		res.path(CONTATTO_PATH+"/"+contattoId).accept(MediaType.APPLICATION_JSON).delete();
		
	}	
	
	public void updateConcact(int contattoId,ContattoResource c) throws RestClientException
	{
		try{
		res.path(CONTATTO_PATH+"/"+contattoId).accept(MediaType.APPLICATION_JSON).put(c);
	} catch (UniformInterfaceException e) {
		String error=e.getResponse().getEntity(String.class);
		throw new RestClientException(error);
	} 
	}
	
	public List<ContattoResource> getContactAziende(int contattoId) throws RestClientException
	{
		WebResource req=res.path(CONTATTO_PATH+"/"+contattoId+"/aziende");
		  try{
		  return req.accept(MediaType.APPLICATION_JSON).get(new GenericType<List<ContattoResource>>(){});
		  }
		  catch ( com.sun.jersey.api.client.UniformInterfaceException e)
		  {
		   if (! e.getMessage().contains("404 Not Found")) {
		   String error=e.getResponse().getEntity(String.class);
		   throw new RestClientException(error);
		   }
		  }
		  return new ArrayList<ContattoResource>();
		
	}
	
	//generics
	
	private String getParentPath(Object o)
	{
		if (o.getClass().equals(EmailResource.class))
			return CONTATTO_PATH;
		return null;
	}
	
	private String getChildPath(Object o)
	{
		if (o.getClass().equals(EmailResource.class))
			return "email";
		return null;
		
	}
	
	public <T> T addParent(String path,T t) throws RestClientException
	{
		try{
		return (T) res.path(path).accept(MediaType.APPLICATION_JSON).post(t.getClass(),t);
		}
		catch (UniformInterfaceException e) {
			String error=e.getResponse().getEntity(String.class);
			throw new RestClientException(error);
		} 
		
	}
	
	public <T> T add(int parentId,String ppath,String cpath,T t) throws RestClientException
	{
		try{
		return (T) res.path(ppath+"/"+parentId+"/"+cpath).accept(MediaType.APPLICATION_JSON).post(t.getClass(),t);
		}
		catch (UniformInterfaceException e) {
			String error=e.getResponse().getEntity(String.class);
			throw new RestClientException(error);
		} 
		
	}
	
	public <T> T get(int objectId,String opath, Class<T> oclass)
	{
		return (T) res.path(opath+"/"+objectId).accept(MediaType.APPLICATION_JSON).get(oclass);
	}
	
	
	public <T> void delete(int objectId,String opath)
	{
		res.path(opath+"/"+objectId).accept(MediaType.APPLICATION_JSON).delete();
	}	
	
	public <T> void deleteMember(int parentId,String ppath,String cpath,int memberId) throws RestClientException
	{
		try{
			 res.path(ppath+"/"+parentId+"/"+cpath+"/"+memberId).accept(MediaType.APPLICATION_JSON).delete();
			}
			catch (UniformInterfaceException e) {
				String error=e.getResponse().getEntity(String.class);
				throw new RestClientException(error);
			} 
	}
	
	public <T> void update(int objectId,String opath,T t) throws RestClientException
	{
		try{
		res.path(opath+"/"+objectId).accept(MediaType.APPLICATION_JSON).put(t);
		}
		catch (UniformInterfaceException e) {
			String error=e.getResponse().getEntity(String.class);
			throw new RestClientException(error);
		} 
	}
	
	//Email
	public EmailResource addEmail(int contactId,EmailResource e) throws RestClientException
	{
		return add(contactId,CONTATTO_PATH,"email",e);
	}
	
	public EmailResource getEmail(int emailId)
	{
		return get(emailId,EMAIL_PATH,EmailResource.class);
	}
	
	public void deleteEmail(int emailId)
	{
		delete(emailId,EMAIL_PATH);
	}
	
	
	public void updateEmail(int emailId,EmailResource e) throws RestClientException
	{
		update(emailId,EMAIL_PATH,e);
	}

	//Indirizzo
	public IndirizzoResource addIndirizzo(int contactId,IndirizzoResource i) throws RestClientException
	{
		return add(contactId,CONTATTO_PATH,"indirizzi",i);
	}
	
	public IndirizzoResource getIndirizzo(int indirizzoId)
	{
		return get(indirizzoId,INDIRIZZI_PATH,IndirizzoResource.class);
	}
	
	public void deleteIndirizzo(int indirizzoId)
	{
		delete(indirizzoId,INDIRIZZI_PATH);
	}
	
	public void updateIndirizzo(int indirizzoId,IndirizzoResource i) throws RestClientException
	{
		update(indirizzoId,INDIRIZZI_PATH,i);
	}		

	//Telefono
	public TelefonoResource addTelefono(int contactId,TelefonoResource t) throws RestClientException
	{
		return add(contactId,CONTATTO_PATH,"telefoni",t);
	}
	
	public TelefonoResource getTelefono(int telefonoId)
	{
		return get(telefonoId,TELEFONI_PATH,TelefonoResource.class);
	}
	
	public void deleteTelefono(int telefonoId)
	{
		delete(telefonoId,TELEFONI_PATH);
	}
	
	public void updateTelefono(int telefonoId,TelefonoResource t) throws RestClientException
	{
		update(telefonoId,TELEFONI_PATH,t);
	}
		
	//Gruppo
	public GruppoResource addGruppo(GruppoResource g) throws RestClientException
	{
		return addParent(GRUPPI_PATH,g);
	}
	
	public GruppoResource getGruppo(int gruppoId)
	{
		return get(gruppoId,GRUPPI_PATH,GruppoResource.class);
	}
	
	public void deleteGruppo(int gruppoId)
	{
		delete(gruppoId,GRUPPI_PATH);
	}
	
	public void updateGruppo(int gruppoId,GruppoResource g) throws RestClientException
	{
		update(gruppoId,GRUPPI_PATH,g);
	}
	
	public void grouppoAddContact(int gruppoId,MemberResource m) throws RestClientException
	{
		add(gruppoId,GRUPPI_PATH,"membri",m);
	}
	
	public void grouppoDeleteContact(int gruppoId,MemberResource m) throws RestClientException
	{
		deleteMember(gruppoId,GRUPPI_PATH,"membri",m.getId());
	}
	
	public List<GruppoResource> grouppoSearch(String query) throws RestClientException
	{
		if (query.length() < QUERY_MIN_LENGTH) 
			throw new IllegalArgumentException("Query is too short it must be at least "+QUERY_MIN_LENGTH+"chars");
		try {
		WebResource req=res.path(GRUPPI_PATH+"/search/"+query);
		
		return req.accept(MediaType.APPLICATION_JSON).get(new GenericType<List<GruppoResource>>(){});
		}
		catch ( com.sun.jersey.api.client.UniformInterfaceException e)
		{
			if (! e.getMessage().contains("404 Not Found")) {
				   String error=e.getResponse().getEntity(String.class);
				   throw new RestClientException(error);
				   }
			
		}
		return new ArrayList<GruppoResource>();
		
	}
	
	public List<MemberResource> gruppoGetMembers(int gruppoId) throws RestClientException
	{
		  WebResource req=res.path(GRUPPI_PATH+"/"+gruppoId+"/membri");
		  try{
		  return req.accept(MediaType.APPLICATION_JSON).get(new GenericType<List<MemberResource>>(){});
		  }
		  catch ( com.sun.jersey.api.client.UniformInterfaceException e)
		  {
		   if (! e.getMessage().contains("404 Not Found")) {
		   String error=e.getResponse().getEntity(String.class);
		   throw new RestClientException(error);
		   }
		  }
		  return new ArrayList<MemberResource>();
		  
	}
	
	public List<MemberResource> aziendaGetMembers(int aziendaId) throws RestClientException
	{
		  WebResource req=res.path(AZIENDE_PATH+"/"+aziendaId+"/membri");
		  try{
		  return req.accept(MediaType.APPLICATION_JSON).get(new GenericType<List<MemberResource>>(){});
		  }
		  catch ( com.sun.jersey.api.client.UniformInterfaceException e)
		  {
		   if (! e.getMessage().contains("404 Not Found")) {
		   String error=e.getResponse().getEntity(String.class);
		   throw new RestClientException(error);
		   }
		  }
		  return new ArrayList<MemberResource>();
		  
	}
	public void aziendaAddContact(int aziendaId,MemberResource m) throws RestClientException
	{
		add(aziendaId,AZIENDE_PATH,"membri",m);
	}
	
	public void aziendaDeleteContact(int aziendaId,MemberResource m) throws RestClientException
	{
		deleteMember(aziendaId,AZIENDE_PATH,"membri",m.getId());
	}
}

