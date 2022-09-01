package it.bologna.ausl.internauta.utils.jpa.natiquery;

import java.sql.Connection;  
import java.sql.PreparedStatement;  
import java.sql.SQLException;  
   
/** 
 * @author Adam Crume for JavaWorld.com, original code 
 * @author Wanja Gayk for brixomatic.wordpress.com, reduced code extract 
 * @see http://www.javaworld.com/article/2077706/core-java/named-parameters-for-preparedstatement.html?page=2 
 */ 
public class PreparedStatementFactory {  
   
  public static PreparedStatement create(final Connection connection, final String queryStringWithNamedParameters) throws SQLException {  
       final String parsedQuery = parse(queryStringWithNamedParameters);  
       return connection.prepareStatement(parsedQuery);  
  }  
   
  private static String parse(final String query) {  
       // I was originally using regular expressions, but they didn't work well for ignoring  
            // parameter-like strings inside quotes.  
       final int length = query.length();  
       final StringBuffer parsedQuery = new StringBuffer(length);  
       boolean inSingleQuote = false;  
       boolean inDoubleQuote = false;  
        
       for (int i = 0; i < length; ++i) {  
            char c = query.charAt(i);  
            if (inSingleQuote) {  
                 if (c == '\'') {  
                      inSingleQuote = false;  
                 }  
            } else if (inDoubleQuote) {  
                 if (c == '"') {  
                      inDoubleQuote = false;  
                 }  
            } else {  
                 if (c == '\'') {  
                      inSingleQuote = true;  
                 } else if (c == '"') {  
                      inDoubleQuote = true;  
                 } else if (c == ':' && i + 1 < length && Character.isJavaIdentifierStart(query.charAt(i + 1))) {  
                      int j = i + 2;  
                      while (j < length && Character.isJavaIdentifierPart(query.charAt(j))) {  
                           ++j;  
                      }  
                      final String name = query.substring(i + 1, j);  
                      c = '?'; // replace the parameter with a question mark  
                      i += name.length(); // skip past the end of the parameter  
                 }  
            }  
            parsedQuery.append(c);  
       }  
       return parsedQuery.toString();  
  }  
   
}