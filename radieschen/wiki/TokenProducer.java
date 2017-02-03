/*
 * Created on Mar 13, 2006
 *
 * An interface for everything which generates a Token.
 * 
 * Author: Mark.Koennecke@psi.ch
 * License: GPL
 */  
package radieschen.wiki;
import java.io.IOException;

public interface TokenProducer {
	public WikiToken nextToken() throws IOException;                                                                                                                                      
}
