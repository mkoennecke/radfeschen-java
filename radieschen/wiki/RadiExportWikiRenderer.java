/**
 * This class helps in exporting part or radi system to another directory. 
 * It does run a html rendering in order to get access to the linked pages 
 * and images which need to be copied too.
 * 
 * Mark Koennecke, October 2006
 * 
 */
package radieschen.wiki;
import radieschen.RadiDatabase;
import java.io.*;

public class RadiExportWikiRenderer extends HtmlExportWikiRenderer{
	
	/*
	 * a constructor: to satisfy Java
	 */
	public RadiExportWikiRenderer() throws IOException {
		super();
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
			if(currentPage == page){
				filename = targetDir + File.separatorChar + "Start.radi";
			} else {
				filename = targetDir + File.separatorChar + currentPage;
			}
			PrintWriter out = new PrintWriter(new FileWriter(filename));
			out.println(pageData);
			out.close();
			out = null;
		}
	}
}
