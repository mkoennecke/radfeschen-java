/*
 * Created on Mar 15, 2006
 *
 * Author: Mark.Koennecke@psi.ch
 * License: GPL
 */  
package radieschen.wiki.test;
import radieschen.wiki.*;
import java.io.FileInputStream;

public class RenderTest {
	public static void main(String argv[]){
		try{
			BaseWikiRenderer bwr = new HtmlWikiRenderer();
//			HtmlWikiTokenFilter f = new HtmlWikiTokenFilter(new JFlexWikiLexer(new FileInputStream(argv[0])));
			StateMachineTokenFilter f = new StateMachineTokenFilter(new JFlexWikiLexer(new FileInputStream(argv[0])));
			String result = bwr.render(f);
			System.out.println(result);
		}catch(Exception eva){
			System.out.println(eva.getMessage());
			eva.printStackTrace();
		}
	}
}
