package it.bologna.ausl.internauta.utils.downloader.utilities;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * Classe per le funzioni di utilities
 * 
 * @author gdm
 */
public class DownloaderUtilities {

    /**
     * Calcola l'hash di un file passato tramite bytes
     * @param file i bytes sui quali calcolare l'hash
     * @param algorithm l'algoritmo di hasing da usare (es SHA-256, MD5, ecc.)
     * @return una stringa che rappresenta l'hash del un file in esadecimale
     * @throws IOException
     * @throws NoSuchAlgorithmException 
     */
    public static String getHashFromBytes(byte[] file, String algorithm) throws IOException, NoSuchAlgorithmException {
 
        MessageDigest mdigest = MessageDigest.getInstance(algorithm);
        
        // read the data from file and update that data in the message digest
        mdigest.update(file);
 
        // store the bytes returned by the digest() method
        byte[] hashBytes = mdigest.digest();
 
        // this array of bytes has bytes in decimal format so we need to convert it into hexadecimal format
        // for this we create an object of StringBuilder since it allows us to update the string i.e. its mutable
        StringBuilder sb = new StringBuilder();
       
        Formatter fmt = new Formatter();
        // loop through the bytes array
        for (int i = 0; i < hashBytes.length; i++) {
           
            // the following line converts the decimal into hexadecimal format and appends that to the StringBuilder object
            //sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
            fmt.format("%02X", hashBytes[i]);
        }
 
        // finally we return the complete hash
        return fmt.toString();
    }
}
