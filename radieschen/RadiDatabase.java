/*
 * Created on Feb 22, 2006
 *
 * This class is responsible for getting radieschen files from and to the repository
 * 
 * Modifed to use my own Wiki Renderer, March 2006
 * 
 * Author: Mark.Koennecke@psi.ch
 * License: GPL
 */  
package radieschen;

import java.io.*;
import java.net.URL;
import java.util.Vector;
import radieschen.wiki.*;
import java.util.Arrays;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import java.util.regex.*;

public class RadiDatabase {
	protected String projectDir;
	protected HtmlWikiRenderer renderer;
	protected JXPProcessor jxp;
	protected JXPInterpreter inti;
	public final static int ARTICLE    = 0; 
	public final static int BOOK       = 1; 
	public final static int PROSPER    = 2; 
	public final static int BEAMER     = 3; 
	
	/**
	 * default constructor
	 */
	public RadiDatabase() throws IOException{
		projectDir = System.getProperty("user.home");
		if(projectDir == null){
			projectDir = "C:\\radidir";
		} else {
			projectDir += File.separatorChar + "radidir";
		}
        setDatabase(projectDir);
		renderer = new HtmlWikiRenderer();
	}
	/**
	 * constructor with project dir as an argument
	 */
	public RadiDatabase(String database) throws IOException{
		renderer = new HtmlWikiRenderer();
		setDatabase(database);
	}
	/**
	 * set a new database
	 * @param database The name of the database to set 
	 */
	public void setDatabase(String database){
		File f = new File(database);
		if(!f.exists()){
			f.mkdir();
		}
		projectDir = database;
		checkAndCopyTemplates();
		inti = new BeanshellInterpreter(database);
		jxp = new JXPProcessor(inti);
	}
	/**
	 * get the database identifier
	 * @return The databse identifier
	 */
	public String getDatabase(){
		return projectDir;
	}
	/**
	 * get the wiki file as source text after processing 
	 * for JXP tags.
	 * @param name The name of the file to get
	 * @return The content of the file or empty data
	 * @throws IOException
	 */
	public String getWikiSource(String name){
		File f = new File(getWikiFilename(name));
		
		try {
			String rawFile = loadFileData(f);
			return jxp.processJXP(rawFile);
		}catch(IOException Eva){
			return "= New File =\n";
		}
	}
	/**
	 * get the wiki file as source text
	 * @param name The name of the file to get
	 * @return The content of the file or empty data
	 * @throws IOException
	 */
	public String getWikiEditSource(String name){
		File f = new File(getWikiFilename(name));
		
		try {
			return loadFileData(f);
		}catch(IOException Eva){
			return "= New File =\n";
		}
	}
	/**
	 * return a wiki file content rendered as html
	 * @param name The name of the file to get 
	 * @return The content of the file name rendered as html
	 */
	public String getWikiHtml(String name) {
		String htmlContent = "<h1>New File</h1>";
		String source = getWikiSource(name);
		try{
//			HtmlWikiTokenFilter wtf = new HtmlWikiTokenFilter(new JFlexWikiLexer(new StringReader(source)));
			StateMachineTokenFilter wtf = new StateMachineTokenFilter(new JFlexWikiLexer(new StringReader(source)));
			htmlContent = renderer.render(wtf);
		}catch(IOException eva){
			System.out.println("This should not happen: IOException " + eva.getMessage());
			eva.printStackTrace();
		}
		StringBuffer htmlText = new StringBuffer();
		htmlText.append(htmlContent);
		return htmlText.toString();
	}
	/**
	 * save  new wiki content to file  
	 * @param file The file to save to
	 * @param content The new content
	 * @throws IOException When things go wrong
	 */
	public void saveWikiData(String file, String content)throws IOException{
		PrintWriter out = new PrintWriter(new 
				FileWriter(getWikiFilename(file)));
		out.println(content);
		out.close();
	}
	/**
	 * delete a wiki file
	 * @param name The file to delete
	 */
	public void deleteWikiFile(String name){
		File f = new File(getWikiFilename(name));
		f.delete();
	}
	/**
	 * get the list of all Ziki files
	 * @return A vector containg all radieschen files
	 */
	public Vector getWikiFileList(){
		Vector v = new Vector();
		int i;
		
		File f = new File(projectDir);
		String dirList[] = f.list();
		Arrays.sort(dirList);
		if(dirList != null){
			for(i = 0; i < dirList.length; i++ ){
				if(dirList[i].indexOf(".radi") > 0){
					v.add(dirList[i]);
				}
			}
		} else {
			v.add("Start.radi");
		}
		return v;
	}
	/**
	 * get the filename of the wiki file
	 * @param name The wiki name of the entry
	 * @return The full filename for the wiki file
	 */
	public String getWikiFilename(String name){
		StringBuffer stb = new StringBuffer();
		stb.append(projectDir);
		stb.append(File.separatorChar);
		stb.append(name);
		if(stb.lastIndexOf(".radi") < 0){
			stb.append(".radi");
		}
		return stb.toString();
	}
	/**
	 * check if a given wiki file exists
	 * @param name The name of the file to check for
	 * @return true or false
	 */
	public boolean radiExists(String name){
		String fname = projectDir + File.separatorChar + name;
		File f = new File(fname);
		return f.exists();
	}
	/**
	 * exports the page decribed by radiFile with all linked pages into targetDir
	 * @param targetDir The directory to write created files to
	 * @param radiFile The start page for exporting
	 * @throws IOException when things go wrong
	 */
	public void exportHtml(String targetDir, String radiFile, 
			String cssFile) throws IOException{
		HtmlExportWikiRenderer h = new HtmlExportWikiRenderer();
		h.export(radiFile,targetDir,this,cssFile);
	}
	/**
	 * exports the page decribed by radiFile with all linked pages into tergetDir 
	 * formatted for the S5 WWW-browser presentation system
	 * @param targetDir The directory to write created files to
	 * @param radiFile The start page for exporting
	 * @throws IOException when things go wrong
	 */
	public void exportS5(String targetDir, String radiFile, 
			String cssFile) throws IOException{
		S5ExportWikiRenderer h = new S5ExportWikiRenderer();
		h.export(radiFile,targetDir,this);
	}
	/**
	 * exports the page decribed by radiFile with all linked pages into targetDir
	 * as original source.
	 * @param targetDir The directory to write created files to
	 * @param radiFile The start page for exporting
	 * @throws IOException when things go wrong
	 */
	public void exportRadi(String targetDir, String radiFile, 
			String cssFile) throws IOException{
		RadiExportWikiRenderer h = new RadiExportWikiRenderer();
		h.export(radiFile,targetDir,this,cssFile);
	}
	/**
	 * exports the page decribed by radiFile with all linked pages into tergetDir
	 * @param targetDir The directory to write created files to
	 * @param radiFile The start page for exporting
	 * @throws IOException when things go wrong
	 */
	public void exportXHtml(String targetDir, String radiFile, 
			String cssFile) throws IOException{
		XHtmlExportWikiRenderer h = new XHtmlExportWikiRenderer();
		h.export(radiFile,targetDir,this,cssFile);
	}
	/**
	 * exports the page decribed by radiFile with all linked pages into tergetDir
	 * @param targetDir The directory to write created files to
	 * @param radiFile The start page for exporting
	 * @throws IOException when things go wrong
	 */
	public void exportMarkdown(String targetDir, String radiFile, 
			String cssFile) throws IOException{
		MarkdownRenderer h = new MarkdownRenderer();
		h.export(radiFile,targetDir,this,cssFile);
	}
	/**
	 * export to various latex formats
	 * @param targetDir The target directory for exporting
	 * @param radiFile The start file for the export
	 * @param format The format to export. Must be one of the 
	 *  known formats of LatexWikiRenderer
	 * @throws IOException if things go wrong
	 */
	public void exportLatex(String targetDir,String radiFile, int format)
		throws IOException{
		if(format == PROSPER){
			ProsperWikiRenderer h = new ProsperWikiRenderer();
			h.export(radiFile,targetDir,this,null);
		} else if(format == BEAMER){
			BeamerWikiRenderer h = new BeamerWikiRenderer();
			h.export(radiFile,targetDir,this,null);
		}else {
			LatexWikiRenderer h = new LatexWikiRenderer(format);
			h.export(radiFile,targetDir,this,null);
		}
	}
	/**
	 * search the database for the pattern specified
	 * @param pattern The pattern to search for
	 * @return the text of a pga eto render with the serah results.
	 */
	public String searchDatabase(String pattern){
		StringBuffer result = new StringBuffer();
        int i;
		
		result.append(HtmlHeader.makeHtml3Header("SearchResults",projectDir,null));
		result.append("<h1>");
		result.append("Your Search Results For: ");
		result.append(pattern);
		result.append("</h1>\n");
		
		Pattern p = Pattern.compile(pattern,Pattern.CASE_INSENSITIVE);
		
		File d = new File(projectDir);
		String radiNames[] = d.list(new SuffixFileFilter(".radi"));
		for(i = 0; i < radiNames.length; i++){
			try{
				File f = new File(projectDir + File.separatorChar + radiNames[i]);
				FileReader r = new FileReader(f);
				String fileTxt = IOUtils.toString(r);
				Matcher m = p.matcher(fileTxt);
				if(m.find() ){
					result.append("<h3><a href=\"radi://");
					result.append(radiNames[i]);
					result.append("\">");
					result.append(radiNames[i]);
					result.append("</a></h3>\n");
				}
			}catch(Exception eva){}
		}
		
		result.append("</body></html>");
		return result.toString();
	}
	/**
	 * Make sure that all necessary template files live
	 *  in the database directory
	 */
	private void checkAndCopyTemplates()  {
		String templates[] = {"radi.css", "prosper.template",
			"s5.template", "beamer.template", "psi_logo_white.png"};
		int i;
		File f;
		
		for(i = 0; i < templates.length; i++){
			f = new File(projectDir + File.separatorChar + templates[i]);
			if(!f.exists()){
				try{
					URL ulli = 
						this.getClass().getResource("/radieschen/" + templates[i]);
					FileUtils.copyURLToFile(ulli,f);
				}catch(IOException eva){
					System.out.println("Failed to copy template: " + templates[i] +
							" into database directory");
					eva.printStackTrace();
				}
			}
		}
	}
	/**
	 * actually read the file
	 * @param f The file to read
	 * @throws IOException
	 */
	private String loadFileData(File f) throws IOException{
		String line;
		StringBuffer data;

		data = new StringBuffer();

		BufferedReader in = new BufferedReader(new FileReader(f));
 		while((line = in.readLine()) != null){
			data.append(line);
			data.append('\n');
		}
		return data.toString();
	}
}
