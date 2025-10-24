package it.bologna.ausl.internauta.utils.sendintegration;

import it.bologna.ausl.internauta.service.send_integration.controller.ErogatoreApiDelegate;
import it.bologna.ausl.internauta.utils.send_integration.model.Lotto;
import it.bologna.ausl.internauta.utils.send_integration.model.LottoBase;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author gdm
 */
public class ErogatoreApiDelegateImpl implements ErogatoreApiDelegate {

    @Override
    public ResponseEntity<LottoBase> elaboraLotto(Lotto lotto) {
        return ErogatoreApiDelegate.super.elaboraLotto(lotto); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }
    
}
