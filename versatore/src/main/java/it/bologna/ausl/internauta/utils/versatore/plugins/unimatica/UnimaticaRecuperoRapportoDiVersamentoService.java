package it.bologna.ausl.internauta.utils.versatore.plugins.unimatica;

import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.versatore.exceptions.RecuperoRapportoDiVersamentoPluginException;
import it.bologna.ausl.internauta.utils.versatore.plugins.RecuperoRapportoDiVersamento;
import it.bologna.ausl.model.entities.versatore.RapportoDiVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author boria
 */
@Component
@Scope("prototype")
public class UnimaticaRecuperoRapportoDiVersamentoService extends RecuperoRapportoDiVersamento {

    private static final Logger log = LoggerFactory.getLogger(UnimaticaRecuperoRapportoDiVersamentoService.class);

    private static final String UNIMATICA_VERSATORE_SERVICE = "UnimaticaVersatoreService";
    private static final String PDVUUID = "pdvUuid";
    private static final String UNIMATICA_SERVIZIO_RECUPERO_RAPPORTO_DI_VERSAMENTO_URI = "UnimaticaServizioRecuperoRapportoDiVersamentoURI";
    private static final String OK = "OK";
    private static final String KO = "KO";
    private static final String OK_PARZIALE = "OK_PARZIALE";

    private String unimaticaServizioRecuperoRapportoDiVersamentoURI;

    @Autowired
    VersatoreHttpClientConfiguration versatoreHttpClientConfiguration;

    @Override
    public void init(VersatoreConfiguration versatoreConfiguration) {
        super.init(versatoreConfiguration);
        Map<String, Object> versatoreConfigurationMap = this.versatoreConfiguration.getParams();
        Map<String, Object> unimaticaServiceConfiguration = (Map<String, Object>) versatoreConfigurationMap.get(UNIMATICA_VERSATORE_SERVICE);
        unimaticaServizioRecuperoRapportoDiVersamentoURI = unimaticaServiceConfiguration.get(UNIMATICA_SERVIZIO_RECUPERO_RAPPORTO_DI_VERSAMENTO_URI).toString();
        log.info("Unimatica servizio recupero rapporto di versamento URI: {}", unimaticaServizioRecuperoRapportoDiVersamentoURI);
    }

    @Override
    public RapportoDiVersamento recuperaRapportiDiVersamentoImpl(Versamento versamento, Map<String, Object> params) throws RecuperoRapportoDiVersamentoPluginException {

        //prendo il pdvuuid del versamento
        log.info("Inizio il recupero del rapporto del versamento id " + versamento.getId());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(versamento.getRapporto());
        String pdvUuid = root.get(PDVUUID).asString();
        if (!StringUtils.hasText(pdvUuid)) {
            log.error("Il rapporto di presa in carico non contiene il pdvUuid del versamento");
            throw new RecuperoRapportoDiVersamentoPluginException("Il rapporto di presa in carico non contiene il pdvUuid del versamento");
        }
        log.info("Recupero il rapporto del pacchetto di versamento con pdvUuid " + pdvUuid);

        RapportoDiVersamento rapportoDiVersamento = new RapportoDiVersamento();

        //credenziali autorizzazione
        String username = (String) params.get("username");
        String password = (String) params.get("password");

        // richiesta
        log.info("Costruisco la request");

        String url = unimaticaServizioRecuperoRapportoDiVersamentoURI + "/" + pdvUuid;

        Request request = new Request.Builder()
            .url(url)
            .get()
            .header("Authorization", Credentials.basic(username, password)) // Basic Auth
            .build();

        //preparo il client per la connessione a Unimatica
        OkHttpClient okHttpClient = versatoreHttpClientConfiguration.getHttpClientManager().getOkHttpClient();

        //effettuo la chiamata
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                log.info("Message: " + response.message());
                String resBodyString = response.body().string();
                log.info("Body: " + resBodyString);
                String responseXmlEstratto = extractXml(resBodyString);
                rapportoDiVersamento.setIdVersamento(versamento);
                rapportoDiVersamento.setRapporto(responseXmlEstratto);
                rapportoDiVersamento.setStato(RapportoDiVersamento.StatoRapportoDiVersamento.RECUPERATO);
                String esito = getEsito(responseXmlEstratto);
                if (esito.equals(OK) || esito.equals(OK_PARZIALE)) {
                    rapportoDiVersamento.setInConservazione(Boolean.TRUE);
                } else if (esito.equals(KO)) {
                    rapportoDiVersamento.setInConservazione(Boolean.FALSE);
                } else {
                    rapportoDiVersamento.setStato(RapportoDiVersamento.StatoRapportoDiVersamento.ERRORE_PLUG_IN);
                    rapportoDiVersamento.setDaRitentare(Boolean.TRUE);
                }
            } else {
                log.error("ERROR: message = " + response.message());
                String resBodyString = response.body().string();
                log.error("Body: " + resBodyString);
                log.error(response.toString());
                rapportoDiVersamento.setIdVersamento(versamento);
                rapportoDiVersamento.setRapporto(resBodyString);
                rapportoDiVersamento.setStato(RapportoDiVersamento.StatoRapportoDiVersamento.ERRORE_CONSERVATORE);
                rapportoDiVersamento.setDaRitentare(Boolean.TRUE);
            }
        } catch (Throwable ex) {
            log.error("Errore nella chiamata al servizio di recupero rapporto di versamento", ex);
            rapportoDiVersamento.setIdVersamento(versamento);
            rapportoDiVersamento.setRapporto("Errore nella chiamata al servizio di recupero rapporto di versamento");
            rapportoDiVersamento.setStato(RapportoDiVersamento.StatoRapportoDiVersamento.ERRORE_PLUG_IN);
            rapportoDiVersamento.setDaRitentare(Boolean.TRUE);
        }

        return rapportoDiVersamento;

    }

    /**
    Uso questa funzione per estrarre l'xml di risposta che ci serve dalla response sporca che da unimatica
    @param string
    @return
     */
    public static String extractXml(String string) {
        Pattern p = Pattern.compile("<RapportoDiVersamento>.*?</RapportoDiVersamento>", Pattern.DOTALL);
        Matcher m = p.matcher(string);
        return m.find() ? m.group() : null;
    }

    /**
    Metodo per estrarre dal xml di risposta l'esito. Prendo solo l'esito che è al primo livello del xml, tralasciando quelli annidati nei tag figli
    @param xml
    @return
    @throws Exception
     */
    public static String getEsito(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));

        Element root = doc.getDocumentElement();

        NodeList children = root.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE
                && node.getNodeName().equals("Esito")) {

                return node.getTextContent();
            }
        }

        return null;
    }

}
