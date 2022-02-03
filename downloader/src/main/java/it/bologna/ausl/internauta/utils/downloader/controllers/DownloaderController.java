package it.bologna.ausl.internauta.utils.downloader.controllers;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author gdm
 */
@RestController
@RequestMapping(value = "${downloader.mapping.url}")
public class DownloaderController {
        @RequestMapping(value = "/download", method = RequestMethod.GET)
        public void download(HttpServletRequest request,
                @RequestParam(required = true) String token) {
            
            System.out.println("download");
    }
}
