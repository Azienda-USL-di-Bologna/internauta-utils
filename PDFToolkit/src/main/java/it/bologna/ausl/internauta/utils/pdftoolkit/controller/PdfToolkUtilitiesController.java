package it.bologna.ausl.internauta.utils.pdftoolkit.controller;

import it.bologna.ausl.internauta.utils.pdftoolkit.exceptions.PdfToolkitHttpException;
import it.bologna.ausl.internauta.utils.pdftoolkit.openpdf.OpenPdfPdfUtils;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${pdftoolkit.mapping.url.root}/utilities")
public class PdfToolkUtilitiesController {
    private static final Logger log = LoggerFactory.getLogger(PdfToolkUtilitiesController.class);
    
    @RequestMapping(value = "checkPdf", method = RequestMethod.POST, consumes = "application/pdf", produces = "text/plain")
    public ResponseEntity<String> checkPdf(
            @RequestBody InputStreamResource file) throws IOException, PdfToolkitHttpException {
        if (file != null) {
            return ResponseEntity.ok(OpenPdfPdfUtils.checkPdf(file.getInputStream()).toString());
        } else {
            String error = "non è stato passato nessun file";
            log.error(error);
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
