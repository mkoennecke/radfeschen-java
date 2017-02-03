/**
 * This is a renderer which renders the wiki text  into Markdown format for exchange 
 * with other wiki systems through pandoc. This conversion is never complete.
 *  
 *  copyright: GPL
 *  
 *  Mark Koennecke, October 2014
 */

package radieschen.wiki;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import radieschen.RadiDatabase;

public class MarkdownRenderer extends BaseWikiRenderer {
	private String targetDir, dbDir;
	boolean export;
	private int listDepth;
	protected Stack toConvert;
	protected  HashSet converted;
	
	public MarkdownRenderer() throws IOException {
		super("/radieschen/wiki/markdown.prop");
		export = false;
		listDepth = -1;
		toConvert = new Stack();
		converted = new HashSet();
	}

	protected void processLink(String txt) {
		String url, name;
		
		txt.trim();
		int length = txt.length();
		txt = txt.substring(1,length-1);

		int idx = txt.indexOf('|');
		if(idx  > 0){
			url = txt.substring(0,idx);
			name = txt.substring(idx+1,txt.length());
		} else {
			url = txt;
			name = txt;
		}
		if(!url.startsWith("http") && !url.startsWith("ftp") &&!url.startsWith("file")){
			result.append('[' + name.trim() +"](" + url + ".md)");
			if(!converted.contains(name) && !toConvert.contains(name)){
				toConvert.push(name);
			}
		} else {
			result.append("[" + name.trim() + "](" + url +")");
		}
	}

	protected void processImage(String txt) {
		result.append("![](" + txt + ")");
		if(export){
			tryCopyLocalFile(dbDir, targetDir,txt.substring(2,txt.length()-2));
		}
	}

	protected void processPreformatted(String txt) {
			String lines[] = txt.split("\n");
			for(int i = 0; i < lines.length; i++){
				result.append("    " + lines[i] + "\n");
			}
	}
	/**
	 * export a wiki page and its linked pages into targetdir in Markdown format.
	 * TODO: make this work recusively.
	 * @param page The start page
	 * @param targetDir The target directory
	 * @param db The database to get wiki data from.
	 * @throws IOException if there is an IO problem
	 */
	public void export(String page, String targetDir, RadiDatabase db, 
			String cssFile) throws IOException{
		this.dbDir = db.getDatabase();
		this.targetDir = targetDir;
		export = true;
		
		toConvert.clear();
		converted.clear();
		
		if(page.indexOf(".radi") < 0){
			page += ".radi";
		}
		
		toConvert.push(page);
		
		while(!toConvert.empty()){
			String currentPage = (String)toConvert.pop();
			converted.add(currentPage);
			if(currentPage.indexOf(".radi") <0){
				currentPage += ".radi";
			}
			String pageData = db.getWikiSource(currentPage);
			String renderedPage = render(new StateMachineTokenFilter(new 
					JFlexWikiLexer(new StringReader(pageData))));

			String filename = targetDir + File.separatorChar + 
					currentPage.replaceFirst(".radi",".md");
			PrintWriter out = new PrintWriter(new FileWriter(filename));
			out.println(renderedPage.toString());
			out.close();
			out = null;
		}
		
		export = false;
	}
	protected void processDefList(String txt) {
		String def = txt.substring(1, txt.length()-1);
		result.append("* **" + def + "** ");
	}

	protected void processStartHeader(String txt) {
		result.append("\n");
		super.processStartHeader(txt);
		result.append(" ");
	}

	protected boolean tokenHandled(WikiToken tok, TokenProducer input) throws IOException {
		int type = tok.getType();
		if(type == WikiToken.STARTQUOTE){
			result.append("\n");
			tok = input.nextToken();
			do {
				if(tok.getType() == WikiToken.NL || tok.getText().indexOf('\n') >= 0){
					result.append("\n>");
				} else {
					result.append(tok.getText());
				}
				tok = input.nextToken();
			}while(tok.getType() != WikiToken.ENDQUOTE);
			return true;
		}
		if(type == WikiToken.ULLISTOPEN || type == WikiToken.OLLISTOPEN){
			listDepth++;
		}
		if(type == WikiToken.OLLISTCLOSE || type == WikiToken.ULLISTCLOSE){
			listDepth--;
		}
		if(type == WikiToken.ULLIST || type == WikiToken.OLLIST){
			for(int i = 0; i < listDepth*4; i++){
				result.append(' ');
			}
		}
		if(type == WikiToken.TABLEOPEN){
			processTable(input);
			return true;
		}
		return super.tokenHandled(tok, input);
	}

	private void processTable(TokenProducer input) throws IOException{
		List<List<String>> columnList = new ArrayList<List<String>>();
		List<String> column = new ArrayList<String>();
		StringBuffer elementData = new StringBuffer();
		WikiToken tok;
		int tableIndex = 0;

		do{
			tok = input.nextToken();
			if(tok.getType() == WikiToken.TABLESEP){
				column.add(elementData.toString().trim());
				column = new ArrayList<String>();
				columnList.add(column);
				elementData = new StringBuffer();
				continue;
			} else if(tok.getType() == WikiToken.TABLEEND){
				column.add(elementData.toString().trim());
				elementData = new StringBuffer();
				break;
			} else if(tok.getType() == WikiToken.TABLEROW){
				column = new ArrayList<String>();
				columnList.add(column);
				elementData = new StringBuffer();
				continue;
			}else if(tok.getType() == WikiToken.NL){
				continue;
			}else {
				elementData.append(tok.getText());
			}
		}while(tok.getType() != WikiToken.TABLEEND);

		do{
			tok = input.nextToken();
			if(tok.getType() == WikiToken.TABLEROW){
				tableIndex = 0;
				continue;
			} else if(tok.getType() == WikiToken.TABLESEP){
				column = columnList.get(tableIndex);
				column.add(elementData.toString().trim());
				tableIndex++;
				elementData = new StringBuffer();
				continue;
			} else if(tok.getType() == WikiToken.TABLEEND){
				column = columnList.get(tableIndex);
				column.add(elementData.toString().trim());
				elementData = new StringBuffer();
				tableIndex = 0;
				continue;
			}else if(tok.getType() == WikiToken.NL){
				continue;
			}else if(tok.getType() == WikiToken.TABLECLOSE){
				break;
			} else {
				elementData.append(tok.getText().trim());
			}
		}while(tok.getType() != WikiToken.TABLECLOSE);


		formatTable(columnList);
	}

	private void formatTable(List<List<String>> columnList) {
		int length[] = new int[columnList.size()];
		List<String>column;
		int idx, colAdd = 3, colCount = 0;
		String interCol = "   ", format;
		
		for(idx = 0; idx < columnList.size(); idx++){
			column = columnList.get(idx);
			Iterator<String> it = column.iterator();
			while(it.hasNext()){
				String txt = it.next();
				if(txt.length() > length[idx]){
					length[idx] = txt.length();
				}
			}
		}
		
		// format dash line
		result.append("\n");
		for(idx = 0; idx < length.length; idx++){
			for(int i = 0 ; i < length[idx]+ colAdd; i++){
				result.append('-');
			}
			result.append(interCol);
		}
		result.append('\n');
		
		// format data
		column = columnList.get(0);
		colCount = column.size();
		for(int colIdx = 0; colIdx < colCount; colIdx++){
			for(idx = 0; idx < columnList.size(); idx++){
				column = columnList.get(idx);
				colCount = column.size();
				String data = column.get(colIdx);
				format = "%" + Integer.toString(length[idx] + colAdd) + "s";
				result.append(String.format(format, data));
				result.append(interCol);
			}
			result.append('\n');
		}
		
		// format final dash line
		for(idx = 0; idx < length.length; idx++){
			for(int i = 0 ; i < length[idx]+ colAdd; i++){
				result.append('-');
			}
			result.append(interCol);
		}
		result.append("\n\n");
	}	
}
