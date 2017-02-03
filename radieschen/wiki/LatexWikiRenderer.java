/**
 * This a class which renders to latex ouput.
 * 
 * copyright: GPL
 * 
 *  Mark Koennecke, May 2006
 */
package radieschen.wiki;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;

import radieschen.RadiDatabase;
/**
 * @author koennecke
 *
 */
public class LatexWikiRenderer extends LatexBaseWikiRenderer {
	
	/**
	 * constructor
	 * @throws IOException
	 */
	public LatexWikiRenderer(int book) throws IOException{
		super("/radieschen/wiki/latex.prop");
		if(book != RadiDatabase.ARTICLE){
			loadFormat(book);
		}
	}
	/**
	 * load the right format mapping
	 * @param format The mapping to load
	 * @throws IOException if the format cannot be loaded
	 */
	public void loadFormat(int format)throws IOException{
		String mapURL;
		
		switch(format){
		case RadiDatabase.ARTICLE:
			mapURL = "/radieschen/wiki/latex.prop";
			break;
		case RadiDatabase.BOOK:
			mapURL = "/radieschen/wiki/latexbook.prop";
			break;
		default:
			throw new IOException("Invalid latex format requested");
		}
		tagMap = new Properties();
		URL ulli = BaseWikiRenderer.class.getResource(mapURL);
		if(ulli == null){
			throw new IOException("Tag property file not found");
		}
		tagMap.load(ulli.openStream());
	}
	/**
	 * process link data. This has to be overriden by special implementations
	 * @param linkData
	 */
	protected void processLink(String linkData){
		int idx;
		String name;
		
		linkData = linkData.trim();
		if((idx = linkData.indexOf("|")) > 0){
			linkData = linkData.substring(1,idx);
		} else {
			linkData = linkData.substring(1,linkData.length() - 1);
		}
		if(!linkData.startsWith("http") && !linkData.startsWith("ftp") &&!linkData.startsWith("file")){
			idx = linkData.indexOf("#");
			if(idx > 0) {
				name = linkData.substring(0,idx);
			} else if(idx == 0){
				// TODO: think about internal links
				return;
			} else {
				name = linkData;
			}
			if(name.length() > 1){
				result.append("\\ref{");
				result.append(linkData);
				result.append("}");
				if(!converted.contains(name) && !toConvert.contains(name)){
					toConvert.addLast(name);
				}
			}
		} else {
				result.append(linkData);
		}
	}
	/**
	 * Make sure that an \label is appended to the first header in a file
	 */
	protected void processEndHeader(String txt){
		super.processEndHeader(txt);
		if(firstHeader){
			if(currentFile != null){
				result.append("\\label{");
				result.append(currentFile);
				result.append("}\n");
			}
			firstHeader = false;
		}
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
		
		toConvert.addLast(page);
		
		htmlText = null;
		htmlText = new StringBuffer();
		htmlText.append(tagMap.getProperty("dochead"));
		htmlText.append("\\usepackage[dvips]{graphicx}\n");
		htmlText.append("\n\\begin{document}\n");
		filename = targetDir + File.separatorChar + 
		page.replaceFirst(".radi",".tex");
		this.targetDir = targetDir;
		this.dbDir = db.getDatabase(); 
		while(toConvert.size() > 0){
			currentPage = (String)toConvert.getFirst();
			converted.add(currentPage);
			if(currentPage.indexOf(".radi") < 0){
				currentPage += ".radi";
			}
			pageData = db.getWikiSource(currentPage);
			firstHeader = true;
			currentFile = FilenameUtils.removeExtension(currentPage);
/*
			renderedPage = render(new HtmlWikiTokenFilter(new 
					JFlexWikiLexer(new StringReader(pageData))));
*/
			renderedPage = render(new StateMachineTokenFilter(new 
					JFlexWikiLexer(new StringReader(pageData))));
			htmlText.append(renderedPage);
			toConvert.removeFirst();
		}
		htmlText.append("\n\\end{document}\n");
		PrintWriter out = new PrintWriter(new FileWriter(filename));
		out.println(htmlText.toString());
		out.close();
		out = null;
	}
}
