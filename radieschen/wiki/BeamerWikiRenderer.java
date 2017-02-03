/**
 * This is a Wiki Renderer which generates output for the beamer
 * presentation package
 * 
 *  Mark Koennecke, January 2009
 */
package radieschen.wiki;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.FileInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FilenameUtils;

import radieschen.RadiDatabase;

public class BeamerWikiRenderer extends LatexBaseWikiRenderer {
	protected boolean slideOpen;
	protected String template;
	private static final String contentID = "!!CONTENT!!";
	/**
	 * constructor
	 * @throws IOException
	 */
	public BeamerWikiRenderer()throws IOException{
		super("/radieschen/wiki/beamer.prop");
		slideOpen = false;
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
		String filename, templateFile;
		
		toConvert.clear();
		converted.clear();
		toConvert.addLast(page);
		this.targetDir = targetDir;
		this.dbDir = db.getDatabase(); 
		filename = targetDir + File.separatorChar + 
			page.replaceFirst(".radi",".tex");
		templateFile = dbDir + File.separatorChar + "beamer.template";
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
			firstHeader = true;
			currentFile = FilenameUtils.removeExtension(currentPage);
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
		String tagName;
		switch(depth){
		case 1:
			result = new StringBuffer();
			break;
		case 2:
			if(slideOpen){
				result.append(tagMap.getProperty("endslide"));
				slideOpen = false;
			} 
			result.append(tagMap.getProperty("startheader2"));
			slideOpen = true;
			break;
		default:
			System.out.println("Header depth  " + depth + " not supported");
			return;
		}
	}
	/**
	 * end of header processing: count the =
	 * @param txt The header tag
	 */
	protected void processEndHeader(String txt){
		int depth = txt.trim().length();
		String tagName;
		switch(depth){
		case 1:
			template = template.replaceAll("!!TITLE!!", result.toString());
			result = new StringBuffer();
			break;
		case 2:
			tagName = "endheader2";
			result.append(tagMap.getProperty(tagName));
			break;
		default:
			System.out.println("Header depth  " + depth + " not supported");
			return;
		}
	}
	/**
	 * processImage processes an image tag. This is different for each implementation
	 * and has to be overriden by sub classes
	 * @param txt The image tag data
	 */
	protected void processImage(String txt){
		result.append("\\begin{figure}[!ht]\n");
		result.append("\\resizebox{7cm}{5cm}{\\includegraphics[width=0.75\\textwidth]{");
		String imfile = txt.substring(2,txt.length()-2);
		result.append(imfile);
		result.append("}}");
		result.append("\\end{figure}\n");
		tryCopyLocalFile(dbDir, targetDir,imfile);
	}

}
