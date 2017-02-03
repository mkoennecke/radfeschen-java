/*
 * Created on Mar 17, 2006
 * This is for exporting wiki pages in HTML format. Linked wiki pages are put into
 * a stack if they have not been converted already. This shall ensure that all pages
 * linked to the start wiki page are converted too. 
 *  
 * Author: Mark.Koennecke@psi.ch
 * License: GPL
 */  
package radieschen.wiki;
import java.io.*;

import radieschen.RadiDatabase;
import java.util.HashSet;
import java.util.Stack;

public class HtmlExportWikiRenderer extends HtmlWikiRenderer{
	protected Stack toConvert;
	protected  HashSet converted;
	protected String targetDir, dbDir;
	
	/**
	 * a constructor
	 * @throws IOException when the tag library cannot be openend
	 */
	public HtmlExportWikiRenderer() throws IOException {
		super();
		toConvert = new Stack();
		converted = new HashSet();
		targetDir = null;
		dbDir = null;
	}
	/**
	 * initialize ourselves with a special taglib.
	 * @param taglib The tag library to use
	 * @throws IOException when things go wrong.
	 */
	public HtmlExportWikiRenderer(String taglib) throws IOException {
		super(taglib);
		toConvert = new Stack();
		converted = new HashSet();
		targetDir = null;
		dbDir = null;
	}
	/**
	 * format a wiki link. Take care of keeping track of linked pages, too
	 * @param the wiki url to treat
	 * @return The new name of the wiki page
	 */
	protected String formatWikiLink(String url){
		int idx;
		String name = null, anchor = null;
		StringBuffer result;
		
		idx = url.indexOf("#");
		if(idx > 0) {
			name = url.substring(0,idx);
			anchor = url.substring(idx,url.length());
		} else {
			name = url;
		}
		
		if(!converted.contains(name) && !toConvert.contains(name)){
			toConvert.push(name);
		}
		result = new StringBuffer();
		result.append(name);
		result.append(".html");
		if(anchor != null){
			result.append(anchor);
		}
		return result.toString();
	}
	/**
	 * export a wiki page and its linked pages into targetdir
	 * @param page The start page
	 * @param targetDir The target directory
	 * @param db The database to get wiki data from.
	 * @throws IOException if there is an IO problem
	 */
	public void export(String page, String targetDir, RadiDatabase db, 
			String cssFile) throws IOException{
		String renderedPage;
		String pageData;
		String currentPage;
		StringBuffer htmlText;
		String filename;
		
		toConvert.clear();
		converted.clear();
		
		toConvert.push(page);
		
		this.targetDir = targetDir;
		this.dbDir = db.getDatabase(); 
		while(!toConvert.empty()){
			currentPage = (String)toConvert.pop();
			converted.add(currentPage);
			if(currentPage.indexOf(".radi") < 0){
				currentPage += ".radi";
			}
			pageData = db.getWikiSource(currentPage);
/*
			renderedPage = render(new HtmlWikiTokenFilter(new 
						JFlexWikiLexer(new StringReader(pageData))));
*/						
			renderedPage = render(new StateMachineTokenFilter(new 
					JFlexWikiLexer(new StringReader(pageData))));
			htmlText = null;
			htmlText = new StringBuffer();
			htmlText.append(HtmlHeader.makeHtml3Header(currentPage,targetDir,cssFile));
			htmlText.append(renderedPage);
			htmlText.append("</body></html>\n");
			
			filename = targetDir + File.separatorChar + 
				currentPage.replaceFirst(".radi",".html");
			PrintWriter out = new PrintWriter(new FileWriter(filename));
			out.println(htmlText.toString());
			out.close();
			out = null;
		}
	}
	/**
	 * treat an image: generate an img tag
	 * @param txt The value of the tag
	 */
	protected void processImage(String txt){
		super.processImage(txt);
		tryCopyLocalFile(dbDir, targetDir,txt.substring(2,txt.length()-2));
	}	
}
