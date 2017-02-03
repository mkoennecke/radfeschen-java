/**
 * This is an exporter for radi pages into the S5 WWW-browser presentation system.
 * 
 * copyright: GPL
 * 
 * Mark Koennecke, November 2006
 */
package radieschen.wiki;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.LinkedList;
import org.apache.commons.io.IOUtils;

import radieschen.RadiDatabase;

public class S5ExportWikiRenderer extends HtmlExportWikiRenderer {
	protected boolean slideOpen;
	protected String template;
	protected LinkedList toConvert;
	private static final String contentID = "!!CONTENT!!";
	
	/**
	 * a constructor
	 * @throws IOException
	 */
	public S5ExportWikiRenderer()throws IOException {
		super("/radieschen/wiki/s5.prop");
		slideOpen = false;
		toConvert = new LinkedList();
	}
	/**
	 * export a wiki page and its linked pages into targetdir
	 * @param page The start page
	 * @param targetDir The target directory
	 * @param db The database to get wiki data from.
	 * @throws IOException if there is an IO problem
	 */
	public void export(String page, String targetDir, RadiDatabase db) throws IOException{
		String renderedPage;
		String pageData;
		String currentPage;
		StringBuffer htmlText;
		String filename, templateFile;
		
		toConvert.clear();
		converted.clear();
		toConvert.addLast(page);
		this.targetDir = targetDir;
		this.dbDir = db.getDatabase(); 
		filename = targetDir + File.separatorChar + 
			page.replaceFirst(".radi",".html");
		templateFile = dbDir + File.separatorChar + "s5.template";
		template = IOUtils.toString(new FileInputStream(templateFile));
		
		htmlText = null;
		htmlText = new StringBuffer();
		while(toConvert.size() > 0){
			currentPage = (String)toConvert.getFirst();
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
			htmlText.append(renderedPage);
			if(slideOpen){
				htmlText.append(tagMap.getProperty("endslide"));
				slideOpen = false;
			} 
			toConvert.removeFirst();
		}
		if(slideOpen){
			htmlText.append(tagMap.getProperty("endslide"));
			slideOpen = false;
		} 
        int idx = template.indexOf(contentID);
        StringBuffer pres = new StringBuffer(template.substring(0,idx));
        pres.append(htmlText);
        pres.append(template.substring(idx +contentID.length(), template.length()));
		PrintWriter out = new PrintWriter(new FileWriter(filename));
		out.println(pres.toString());
		out.close();
		out = null;
	}
	/**
	 * start of header processing: count the =
	 * This does, together with processEndHeader two things:
	 * - Header level 1 is considred the title and collected as a new result.
	 *   This is then used to replace all occurrences of !!TITLE!! in the 
	 *   template
	 * - Header level 2 is considered the title of the slide. If a slide had
	 *   already been opened the code for closing it is emitted.
	 * - All other headers are not supported for prosper output 
	 * @param txt The header tag
	 */
	protected void processStartHeader(String txt){
		int depth = txt.trim().length();
		String tagName = null;
		
		switch(depth){
		case 1:
			result = new StringBuffer();
			break;
		case 2:
			if(slideOpen){
				result.append(tagMap.getProperty("endslide"));
				slideOpen = false;
			} 
			tagName = "startheader2";
			slideOpen = true;
			break;
		case 3:
			tagName = "startheader3";
			break;
		case 4:
			tagName = "startheader4";
			break;
		case 5:
			tagName = "startheader5";
			break;
		default:
			System.out.println("Header depth  " + depth + " out of range");
			return;
		}
		if(tagName != null){
			result.append(tagMap.getProperty(tagName));
		}
	}
	/**
	 * end of header processing: count the =
	 * @param txt The header tag
	 */
	protected void processEndHeader(String txt){
		int depth = txt.trim().length();
		String tagName = null;
		switch(depth){
		case 1:
			template = template.replaceAll("!!TITLE!!", result.toString());
			result = new StringBuffer();
			break;
		case 2:
			tagName = "endheader2";
			break;
		case 3:
			tagName = "endheader3";
			break;
		case 4:
			tagName = "endheader4";
			break;
		case 5:
			tagName = "endheader5";
			break;
		default:
			System.out.println("Header depth  " + depth + " not supported");
			return;
		}
		if(tagName != null){
			result.append(tagMap.getProperty(tagName));
		}
	}
}
