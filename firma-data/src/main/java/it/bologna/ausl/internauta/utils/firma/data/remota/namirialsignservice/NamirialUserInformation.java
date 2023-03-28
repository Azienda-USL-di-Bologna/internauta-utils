package it.bologna.ausl.internauta.utils.firma.data.remota.namirialsignservice;

import it.bologna.ausl.internauta.utils.firma.data.remota.infocertsignservice.*;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class NamirialUserInformation extends InfocertUserInformation {
    
    public NamirialUserInformation(final String alias, final String pin, final String token, final ModalitaFirma modalitaFirma, final Boolean useSavedCredential) {
        super(alias, pin, token, modalitaFirma, useSavedCredential);
    }
}
