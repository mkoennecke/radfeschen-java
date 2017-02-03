/*
 * Created on Mar 15, 2006
 * This adds HTML link handling to the BaseWikiRenderer. Moreover this shall 
 * add HTML character code handling
 * 
 * Author: Mark.Koennecke@psi.ch
 * License: GPL
 */  
package radieschen.wiki;
import java.io.IOException;
import java.util.HashMap;

public class HtmlWikiRenderer extends BaseWikiRenderer{
	protected HashMap charMap;
	/**
	 * constructor: starts with tag library for HTML
	 * @throws IOException when tag library cannot be opened
	 */
	public HtmlWikiRenderer() throws IOException{
		super("/radieschen/wiki/html.prop");
		initCharMap();
	}
	/**
	 * initialize ourselves with a special taglib.
	 * @param taglib The tag library to use
	 * @throws IOException when things go wrong.
	 */
	public HtmlWikiRenderer(String taglib) throws IOException {
		super(taglib);
		initCharMap();
	}
	
	/**
	 * treat an image: generate an img tag
	 * @param txt The value of the tag
	 */
	protected void processImage(String txt){
		int length;
		result.append("<img src=\"");
		length= txt.length();
		result.append(txt.substring(2,length-2));
		result.append("\">");
	}
	/**
	 * treat a link
	 * @param txt The text of the link
	 */
	protected void processLink(String txt){
		int length, idx;
		String name, url;
		
		txt = txt.trim();
		length = txt.length();
		txt = txt.substring(1,length-1);
		
		/* 
		 * if a | is there, the text after | is the name for the link
		 * else linkname and URL are identical
		 */
		idx = txt.indexOf('|');
		if(idx  > 0){
			url = txt.substring(0,idx);
			name = txt.substring(idx+1,txt.length());
		} else {
			url = txt;
			name = txt;
		}
		
		url = url.trim();
		
		/*
		 * anchor tags
		 */
		if(url.startsWith("#")){
			result.append("<a name=\"");
			result.append(url);
			result.append("\"></a>"); 
			return;
		}
		
		/*
		 * tags with http and ftp are external links, else wiki link
		 */
		if(!url.startsWith("http") && !url.startsWith("ftp") &&!url.startsWith("file")){
			url = formatWikiLink(url);
		}
		result.append("<a href=\"");
		result.append(url);
		result.append("\">");
		result.append(name);
		result.append("</a>");
	}
	/**
	 * format a wiki internal link. Overload this for other uses other then Radieschen
	 * @param url The internal wiki link
	 * @return A URL formatted to represent an internal wiki link
	 */
	protected String formatWikiLink(String url){
		int idx;
		idx = url.indexOf("#");
		if(idx < 0){
			return "radi://" + url + ".radi";
		} else {
			return "radi://" + url.substring(0,idx) + ".radi" +
				url.substring(idx,url.length());
		}
	}
	/**
	 * process a character, thereby applying HTML encoding
	 * @param txt The char to process
	 */
	protected void processChar(String txt){
		String enc = (String)charMap.get(txt);
		if(enc != null){
			result.append(enc);
		} else {
			result.append(txt);
		}
	}
	/**
	 * process colour markup for text
	 * @param txt The colour markup
	 */
	protected void processColour(String txt){
		String col = extractColour(txt);
		result.append("<font color=\"");
		result.append(col);
		result.append("\">");
	}
	/**
	 * initialize the character map
	 */
	private void initCharMap(){
		charMap = new HashMap();
		charMap.put("<","&lt;");
		charMap.put(">","&gt;");
		charMap.put("\\","&quot;");
	}
}
