package it.bologna.ausl.internauta.utils.firma.remota.utils.pdf;

import com.itextpdf.text.pdf.PdfReader;
import it.bologna.ausl.internauta.utils.firma.data.remota.SignAppearance;
import java.io.IOException;
import java.io.InputStream;

/** Utility per la gestione dei file pdf
 *
 * @author gdm
 */
public class PdfUtils {

/**
 * Torna un oggetto PdfSignFieldDescriptor che descrive un campo firma di un Pdf
 * @param pdfFile
 * @param signAppearance campo della classe FirmaRemotaFile
 * @return un oggetto PdfSignFieldDescriptor che descrive un campo firma di un Pdf
 * @throws IOException 
 */
public static PdfSignFieldDescriptor toPdfSignFieldDescriptor(InputStream pdfFile, SignAppearance signAppearance) throws IOException {

    PdfReader pdf = new PdfReader(pdfFile);

    String[] signPositionSplitted = signAppearance.getSignPosition().split(";");
    int page;
    if (signPositionSplitted[0].equalsIgnoreCase("n"))
        page = pdf.getNumberOfPages();
    else
        page = Integer.parseInt(signPositionSplitted[0]);

    int width = Integer.parseInt(signPositionSplitted[1]);
    int heigth = Integer.parseInt(signPositionSplitted[2]);
    int paddingWidth = Integer.parseInt(signPositionSplitted[3]);
    int paddingHeigth = Integer.parseInt(signPositionSplitted[4]);
    
    float pageWidth = pdf.getPageSize(page).getWidth();
    float pageHeight = pdf.getPageSize(page).getHeight();

    // la pagina è orientata orizzontalmente quindi inverto pageWidth con pageHeight
    if (pdf.getPageRotation(page) % 180 == 90) {
        float swap = pageHeight;
        pageHeight = pageWidth;
        pageWidth = swap;
    }

    float lowerLeftX = pageWidth - width - paddingWidth;
    float lowerLeftY = pageHeight - heigth - paddingHeigth;
    float upperRigthX = lowerLeftX + width;
    float upperRigthY = lowerLeftY + heigth;

    PdfSignFieldDescriptor pdfSignFieldDescriptor = 
            new PdfSignFieldDescriptor(page, (int)lowerLeftX, (int)lowerLeftY, (int)upperRigthX, (int)upperRigthY, signAppearance.getSignName(), null);

    return pdfSignFieldDescriptor;
}
//
//    /** Inserisce il campo firma in un file pdf
//     *
//     * @param pdfFile il file pdf in cui inserire il campo firma
//     * @throws IOException
//     * @throws DocumentException
//     */
//    public void insertIntoPdf(File pdfFile) throws IOException, DocumentException {
//        File tempFile = null;
//        FileOutputStream tempOutStream = null;
//
//        try {
//            String pdfFileName = pdfFile.getAbsolutePath();
//            PdfReader pdf = new PdfReader(pdfFileName);
//            tempFile = new File(pdfFileName.substring(0, pdfFileName.lastIndexOf(File.separator)) + "/_pdfSignFieldtemp_" + pdfFile.getName());
//            tempOutStream = new FileOutputStream(tempFile.getAbsolutePath());
//            PdfStamper stp = new PdfStamper(pdf, tempOutStream);
//
//            PdfFormField sig = PdfFormField.createSignature(stp.getWriter());
//
//            int intpage = 0;
//            if (page.equalsIgnoreCase("n"))
//                intpage = pdf.getNumberOfPages();
//            else {
//                try {
//                    intpage = Integer.parseInt(page);
//                }
//                catch (NumberFormatException ex) {
//                    throw new NumberFormatException("Numero di pagina errato.");
//                }
//            }
//
//            float pageWidth = pdf.getPageSize(intpage).getWidth();
//            float pageHeight = pdf.getPageSize(intpage).getHeight();
//
//            // la pagina è orientata orizzontalmente quindi inverto pageWidth con pageHeight
//            if (pdf.getPageRotation(intpage) % 180 == 90) {
//                float swap = pageHeight;
//                pageHeight = pageWidth;
//                pageWidth = swap;
//            }
//
//            float llx = pageWidth - width - paddingWidth;
//            float lly = pageHeight - heigth - paddingHeigth;
//            float urx = llx + width;
//            float ury = lly + heigth;
//
//            sig.setWidget(new Rectangle(llx, lly, urx, ury), null);
////            if (hiddenSignature)
////                sig.setFlags(PdfAnnotation.FLAGS_HIDDEN);
//
//            sig.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, PdfTemplate.createTemplate(stp.getWriter(), 0, 0));
//            sig.setFlags(PdfAnnotation.FLAGS_PRINT);
//
//            sig.put(PdfName.DA, new PdfString("/Arial 0 Tf 0 g"));
//
//            sig.setFieldName(signatureName);
//
////            sig.setPage(intpage);
////            stp.addAnnotation(sig, intpage);
//            stp.addAnnotation(sig, intpage);
//            pdf.close();
//            stp.close();
//
//            pdfFile.delete();
//            File pdfWithField = new File(pdfFileName.substring(0, pdfFileName.lastIndexOf(File.separator)) + "/_pdfSignFieldtemp_" + pdfFile.getName());
//            pdfWithField.renameTo(new File(pdfFileName));
//        }
//        finally {
//            tempOutStream.close();
//            tempFile.delete();
//        }
//    }
}