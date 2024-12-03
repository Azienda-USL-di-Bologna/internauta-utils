package org.xhtmlrenderer.pdf;

import java.io.InputStream;


public class NullUserAgent extends ITextUserAgent {

    public NullUserAgent(float dotsPerPoint, int dotsPerPixel) {
        super(new ITextOutputDevice(dotsPerPoint), dotsPerPixel);
    }

    @Override
    public String resolveURI(String uri) {
        return null;
    }

    @Override
    protected InputStream resolveAndOpenStream(String filepath) {
        return null;
    }
}
