//package it.bologna.ausl.riversamento.builder;
//
//import com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper;
//
///**
// *
// * @author spritz
// */
//public class MyNamespaceMapper extends NamespacePrefixMapper {
//
//    private static final String FOO_PREFIX = ""; // DEFAULT NAMESPACE
//    private static final String FOO_URI = "URN:PARER_VERSAMENTO:1.3";
//
//    @Override
//    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
//        if (FOO_URI.equals(namespaceUri)) {
//            return FOO_PREFIX;
//        }
//        return suggestion;
//    }
//
//    @Override
//    public String[] getPreDeclaredNamespaceUris() {
//        return new String[]{FOO_URI};
//    }
//
//}
