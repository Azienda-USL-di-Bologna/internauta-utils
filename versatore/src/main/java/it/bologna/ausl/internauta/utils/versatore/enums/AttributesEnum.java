package it.bologna.ausl.internauta.utils.versatore.enums;

/**
 * Enum per mappare tutti i metadati dell'oggetto da versare ad Infocert.
 * 
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public enum AttributesEnum {
    /**
     * gg-mm-aa<br>
     * Metadato mantenuto indipendentemente dalle Linee Guida.<br>
     * <b>Required</b>
     */
    DATA_DOCUMENTO("data_documento_dt"),
    
    /**
     * documentID o identificativo SdI per le fatture o ID SAP o ID del sistema 
     * di gestione documentale o anche nome file.<br>
     * <b>Required</b>
     */
    IDENTIFICATIVO_DOCUMENTO("iddocid_s"),
    
    /**
     * Hash del documento calcolato dal produttore del PdV.<br>
     * <b>Required</b>
     */
    IMPRONTA("iddocimp_s"),
    
    /**
     * Algoritmo applicato, default: SHA-256.<br>
     * <b>Required</b>
     */
    ALGORITMO("iddocalg_s"),
    
    /**
     * Valori ammessi:<br>
     *  a) creazione tramite l’utilizzo di strumenti software,<br>
     *  b) acquisizione per via telematica o della copia per immagine,<br>
     *  c) transazioni o processi informatici o moduli o formulari resi disponibili all’utente<br>
     *  d) generazione da registrazioni o banca dati<br>
     *  Può essere un default.<br>
     *  Si può indicare proprio solo la lettera. es. ‘a’ per fattura elettronica o ‘b’ per fattura cartacea scansionata.<br>
     *  Per Babel il valore di default è 'b'.<br>
     * <b>Required</b>
     */
    MODALITA_DI_FORMAZIONE("modform_s"),
    
    /**
     * Fatture attive, passive, notifiche, libro giornale, registro.<br>
     * Può essere un default.<br>
     * <b>Required</b>
     */
    TIPOLOGIA_DOCUMENTALE("tipdoc_s"),
    
    /**
     * In uscita o in entrata (per le fatture rispetto al ciclo attivo o passivo), interno (per i libri contabili).<br>
     * Può essere un default.<br>
     * Si può indicare proprio solo la lettera U, E, I.<br>
     * <b>Required</b>
     */
    TIPOLOGIA_DI_FLUSSO("datregflusso_s"),
    
    /**
     * Valori ammessi:<br>
     * • Nessuno<br>
     * • Protocollo Ordinario/ Protocollo Emergenza<br>
     * • Repertorio/Registro<br>
     * Può essere un default.<br>
     * <b>Required</b>
     */
    TIPO_REGISTRO("datregtipreg_s"),
    
    /**
     * Data/ora di registrazione del documento. Può comprendere anche l’ora.<br>
     * <b>Required</b>
     */
    DATA_REGISTRAZIONE("datregdata_dt"),
    
    /**
     * Numero di registrazione del documento.<br>
     * Numero di protocollo, ID SDI, numero documento fornitore, numero concatenato mese/anno.<br>
     * <b>Required</b>
     */
    NUMERO_DOCUMENTO("datregnum_s"),
    
    /**
     * Codice identificativo del registro in cui il documento viene registrato.<br>
     * Nel caso in cui il tipo registro sia protocollo ordinario/protocollo emergenza, o Repertorio/Registro.<br>
     * Può essere un default.<br>
     * <i>Optional</i>
     */
    CODICE_REGISTRO("datregid_s"),
    
    /**
     * Testo libero. Es. fattura attiva.<br>
     * <b>Required</b>
     */
    OGGETTO("ogg_s"),
    
    /**
     * mittente.
     */
    RUOLO_N("soggru_x_s"),
    /**
     * Valori per tipologia: 
     * • PU: PAI
     * • PE: PAI-PAE (per enti), PF (per persone fisiche), PG (per persone giuridiche)
     * • GIRI-INTERNI: PAI
     */
    TIPO_SOGGETTO_N("soggtip_x_s"),
    /**
     * Se TIPO_SOGGETTO è PG o PAI inseriamo la denominazione.
     * Per il Protocolli in Uscita sarà Denominazione Ente (A0 o AOUIVR) - Codice iPA di A0 o AOUIVR (PAI).
     */
    DENOMINAZIONE_N("denominazione_x_s"), 
    /**
     * Se TIPO_SOGGETTO è PF è obbligatorio.
     */
    COGNOME_N("cognome_x_s"),
    /**
     * Se TIPO_SOGGETTO è PF è obbligatorio.
     */
    NOME_N("nome_x_s"),
        
    /**
     * Valore predefinito "PROTOCOLLO".
     */
    NATURA("natura_docu_s"),
    
    /**
     * Catena delle persone coinvolte nel Parere di un protocollo.
     */
    PARERE("parere_s"),
    
    /**
     * Si indicano tutti i firmatari del documento, se presenti più firmatari verranno separati da virgola.
     */
    FIRMATARIO("firmatario_s"),

    /**
     * O Partita Iva.<br>
     * <i>Optional</i>
     */
    CODICE_FISCALE("codicefiscale_s"),
    
    /**
     * Se presenti.<br>
     * <i>Optional</i>
     */
    INDIRIZZI_DIGITALI_DI_RIFERIMENTO("indirizzidigit_s"),
    
    /**
     * Valori ammessi: 0, 1, 2 …<br>
     * Es. allegato a un contratto, considerato come un secondo file, versato separatamente, 
     * con un suo set di metadati e collegato al ‘contratto padre’ mediante questo metadato.<br>
     * Non rientrano gli allegati ‘embedded’ nei documenti.<br>
     * <b>Required</b>
     */
    ALLEGATI_NUMERO("alleg_i"),
    
    /**
     * Da indicare per ogni allegato se Numero allegati > 0<br>
     * <i>Optional</i>
     */
    ID_DOC_INDICE_ALLEGATI("allegiddoc_s"),
    
    /**
     * Testo libero.<br>
     * <i>Optional</i>
     */
    DESCRIZIONE_ALLEGATI("allegdesc_s"),
    
    /**
     * Campo testuale che si riferisce alle classificazioni del protocollo. 
     * Espresso in formato [CODICE] NOME (ad esempio [01-01-01] nome del titolo). 
     * Se presenti più classificazioni saranno concatenate tra loro e separate da carattere ','
     */
    CLASSIFICAZIONE("classificazione_s"),
    
    /**
     * Codifica del documento secondo il Piano di classificazione utilizzato (obbligatorio per le PA).<br>
     * <i>Optional</i>
     */
    INDICE_DI_CLASSIFICAZIONE("indclass_s"),
    
    /**
     * Descrizione per esteso dell’Indice di classificazione indicato (obbligatorio per le PA).<br>
     * <i>Optional</i>
     */
    DESCRIZIONE_CLASSIFICAZIONE("descrclass_s"),
    
    /**
     * URI del Piano di classificazione pubblicato.<br>
     * <i>Optional</i>
     */
    PIANO_CLASSIFICAZIONE("pianoclass_s"),
    
    /**
     * Boolean true/false in relazione alla visibilità nei Sistemi di Gestione Documentale di formazione e gestione.<br>
     * (Non comporta cifrature automatiche, che sono da richiedere nella prima pagina della Scheda).<br>
     * <b>Required</b>
     */
    RISERVATO("riservato_b"),
    
    /**
     * Mimetype es. Application/XML<br>
     * vedi Allegato 2 Linee Guida.<br>
     * <b>Required</b>
     */
    IDENTIFICATIVO_DEL_FORMATO("formid_s"),
    
    /**
     * Se disponibile e da validare soprattutto se si conservano formati proprietari.<br>
     * vedi Allegato 2 Linee Guida.<br>
     * <i>Optional</i>
     */
    PRODOTTO_SOFTWARE_NOME("formnom_s"),
    
    /**
     * Se disponibile e da validare soprattutto se si conservano formati proprietari.<br>
     * vedi Allegato 2 Linee Guida.<br>
     * <i>Optional</i>
     */
    PRODOTTO_SOFTWARE_VERSIONE("formvers_s"),
    
    /**
     * Se disponibile e da validare soprattutto se si conservano formati proprietari.<br>
     * vedi Allegato 2 Linee Guida.<br>
     * <i>Optional</i>
     */
    PRODOTTO_SOFTWARE_PRODUTTORE("formprod_s"),
    
    /**
     * Verifica fatta dal produttore del PdV sulla presenza/assenza.<br>
     * Obbligatorio nel solo caso di modalità di formazione doc = a/b.<br>
     * boolean true/false<br>
     * <i>Optional</i>
     */
    VERIFICA_FIRMA_DIGITALE("firm_b"),
    
    /**
     * Verifica fatta dal produttore del PdV sulla presenza/assenza.<br>
     * Obbligatorio nel solo caso di modalità di formazione doc = a/b.<br>
     * boolean true/false<br>
     * <i>Optional</i>
     */
    VERIFICA_MARCA_TEMPORALE("marc_b"),
    
    /**
     * Verifica fatta dal produttore del PdV sulla presenza/assenza.<br>
     * Obbligatorio nel solo caso di modalità di formazione doc = a/b.<br>
     * boolean true/false<br>
     * <i>Optional</i>
     */
    VERIFICA_SIGILLO("sig_b"),
    
    /**
     * Verifica fatta dal produttore del PdV sulla presenza/assenza.<br>
     * Obbligatorio nel solo caso di modalità di formazione doc = a/b.<br>
     * boolean true/false<br>
     * <i>Optional</i>
     */
    VERIFICA_CONFORMITA_COPIE("cop_b"),
    
    /**
     * Identificativo del fascicolo o della serie.<br>
     * es. 2.3.1.2020 indica il primo fascicolo dell’anno 2020 nel titolo 2 classe 3 del titolario di classificazione.<br>
     * <i>Optional</i>
     */
    ID_AGGREGAZIONE("idagg_s"),
    
    /**
     * Campo testuale che si riferisce ai fascicoli contenenti il documento. 
     * Espresso in formato [NUMERO/ANNO FASCICOLO] NOME FASCICOLO. 
     * Se presenti più fascicoli saranno concatenati tra loro separati dal carattere ','
     */
    FASCICOLO("fascicolo_s"),
    
    /**
     * Identificativo univoco e persistente del documento principale, da popolare nel versamento 
     * di un documento allegato al documento principale, per creare un vincolo tra i due.<br>
     * <i>Optional</i>
     */
    IDENTIFICATIVO_DOCUMENTO_PRINCIPALE("iddocprinc_s"),
    
    /**
     * Nome alfanumerico del documento/file così come riconosciuto all’esterno.<br>
     * <b>Required</b>
     */
    NOME_FILE("nome_file_s"),
    
    /**
     * 1, 2, 3, può essere per es. un default: 1.<br>
     * <b>Required</b>
     */
    VERSIONE_DEL_DOCUMENTO("vers_i"),
    
    /**
     * Nel caso di versione > 1 (rettifica)<br>
     * Valori ammessi:<br>
     * • annullamento<br>
     * • rettifica<br>
     * • integrazione<br>
     * • annotazione<br>
     * Metadato volto a tracciare la presenza di operazioni di modifica effettuate sul documento.<br>
     * <i>Optional</i>
     */
    TRACCIATURA_MODIFICHE_TIPO("modiftipo_s"),
    
    /**
     * Nel caso di versione > 1 (rettifica)<br>
     * Come da ruolo = Operatore definito nel metadato Soggetti.<br>
     * <i>Optional</i>
     */
    SOGGETTO_AUTORE_DELLA_MODIFICA("modifaut_s"),
    
    /**
     * Nel caso di versione > 1 (rettifica)<br>
     * Data e ora della modifica.<br>
     * <i>Optional</i>
     */
    TRACCIATURA_MODIFICHE_DATA("modifdata_dt"),
    
    /**
     * Nel caso di versione > 1 (rettifica)<br>
     * Identificativo versione precedente.<br>
     * <i>Optional</i>
     */
    TRACCIATURA_MODIFICHE_ID_DOC_VERSIONE_PRECEDENTE("modifidprec_s"),
    
    /**
     * Es. 10 anni<br>
     * (non comporta scarti automatici).<br>
     * <i>Optional</i>
     */
    TEMPO_DI_CONSERVAZIONE("tempcons_s"),
    
    NOTE("note_s"),
    
    /**
     * Segnatura del protocollo.<br>
     * <b>Required<b> Per la sola classe DOCUMENTO PROTOCOLLATO. 
     * In tal caso vanno aggiunti obbligatoriamente anche i metadati relativi alla classificazione.
     */
    SEGNATURA("segnatura_s");

    private final String attributo;       

    private AttributesEnum(String s) {
        attributo = s;
    }
    
    public boolean equalsName(String otherName) {
        // (otherName == null) check is not needed because name.equals(null) returns false 
        return attributo.equals(otherName);
    }

    @Override
    public String toString() {
       return this.attributo;
    }
}