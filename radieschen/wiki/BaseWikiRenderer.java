/*
 * Created on Mar 14, 2006
 *
 * This is a base class for a wiki token renderer. It uses a property file in order
 * to output appropriate tags for tokens coming in.
 * 
 * Author: Mark.Koennecke@psi.ch
 * License: GPL
 */  
package radieschen.wiki;
import java.util.Properties;
import java.net.URL;
import java.io.*;
import org.apache.commons.io.IOUtils;

public class BaseWikiRenderer {
	protected Properties tagMap;
	protected StringBuffer result;
	protected Properties tokenToName;
	
	/*
	 * constructor
	 * @param in The TokenProducer for getting the tokens. There is a strong dependency
	 * here. The tokenProducer has to deliver the right tokens.
	 * @param tagpath The path to the property file with the tag mappings 
	 */
	public BaseWikiRenderer(String tagpath) throws IOException{
		tagMap = new Properties();
		URL ulli = BaseWikiRenderer.class.getResource(tagpath);
		if(ulli == null){
			throw new IOException("Tag property file not found");
		}
		tagMap.load(ulli.openStream());
		tokenToName = new Properties();
		initTokenToName();
	}
	/**
	 * The main entry point: the render function
	 * @param input The TokneProducer from which to render things
	 * @return A rendered String
	 */
	public  String render(TokenProducer input) throws IOException{
		WikiToken tok;
		String tagName, tagValue;
		int length;

		result = null;
		result = new StringBuffer();

		tok = input.nextToken();
		while(tok != null){
			//System.out.println("Rendering token: " + tok.getType() + ", text = " + tok.getText());
			if(tokenHandled(tok, input)){
				tok = input.nextToken();
				continue;
			}
			switch(tok.getType()){
			case WikiToken.CHAR:
			case WikiToken.STARTCHAR:
			case WikiToken.NL:	
				processChar(tok.getText());
				break;
			case WikiToken.IGNORE:
				tok = input.nextToken();
				continue;
			case WikiToken.DEFLIST:
				processDefList(tok.getText());
				break;
			case WikiToken.STARTHEADER:
				processStartHeader(tok.getText());
				break;
			case WikiToken.ENDHEADER:
				processEndHeader(tok.getText());
				break;
			case WikiToken.ESCAPE:
				length = tok.getText().length();
				processChar(tok.getText().substring(length-1,length));
				break;
			case WikiToken.IMAGE:
				processImage(tok.getText());
				break;
			case WikiToken.LINK:
				processLink(tok.getText());
				break;
			case WikiToken.PRE:
				processPreformatted(tok.getText());
				break;
			case WikiToken.STARTCOLOUR:
				processColour(tok.getText());
				break;
			case WikiToken.MULTIROW:
				processMultiStart(tok.getText());
				break;
			case WikiToken.MULTISEP:
				processMultiSep(tok.getText());
				break;
			case WikiToken.SYMBOL:
				processSymbol(tok.getText());
				break;
			default:
				tagName = (String)tokenToName.get(new Integer(tok.getType()));
				if(tagName == null){
					throw new IOException("Bad token maping");
				}
				tagValue = tagMap.getProperty(tagName);
				if(tagValue != null){
					result.append(tagValue);
				}
				break;
			}
			tok = input.nextToken();
		}
		return result.toString();
	}
	
	/**
	 * This allows derived classes to listen into the token handling
	 * @param tok The current token being processed
	 * @param input The input Tokenstream
	 * @return true when the method has handled the token (and may be others), false when normal 
	 * processing shall still take place.
	 */
	protected boolean tokenHandled(WikiToken tok, TokenProducer input) throws IOException{
		return false;
	}
	/**
	 * process a symbol
	 * @param txt The text of the symbol
	 */
	protected void processSymbol(String txt){
		result.append(txt);
	}
	/**
	 * replace the column count and add the appropriate value.
	 * @param tag
	 * @param text
	 */
	private void insertMulti(String tag, String text){
		int col = text.length() - 1;
		result.append(tag.replaceFirst("%d", Integer.toString(col)));
	}
	/**
	 * process a table entry spanning multicolumns, being the first column
	 * @param text The token text, needed to count the |
	 */
	protected void processMultiStart(String text)throws IOException{
		String tagName, tagValue;
		
		tagName = (String)tokenToName.get(new Integer(WikiToken.MULTIROW));
		if(tagName == null){
			throw new IOException("Bad token maping");
		}
		tagValue = tagMap.getProperty(tagName);
		if(tagValue != null){
			insertMulti(tagValue,text);
		}
	}
	/**
	 * process a table entry spanning multicolumns
	 * @param text The token text, needed to count the |
	 */
	protected void processMultiSep(String text)throws IOException{
		String tagName, tagValue;
	
		tagName = (String)tokenToName.get(new Integer(WikiToken.MULTISEP));
		if(tagName == null){
			throw new IOException("Bad token maping");
		}
		tagValue = tagMap.getProperty(tagName);
		if(tagValue != null){
			insertMulti(tagValue,text);
		}
	}
	/**
	 * process link data. This has to be overriden by special implementations
	 * @param linkData
	 */
	protected void processLink(String linkData){
		result.append(linkData);
	}
	/**
	 * processImage processes an image tag. This is different for each implementation
	 * and has to be overriden by sub classes
	 * @param txt The image tag data
	 */
	protected void processImage(String txt){
		result.append(txt);
	}
	/**
	 * process colour markup for text
	 * @param txt The colour markup
	 */
	protected void processColour(String txt){
		/* do nothing */
	}
	/**
	 * a helper for colour processing: actually extracts the colour info
	 * @param txt The colour markup text
	 * @return The colour value 
	 */
	protected  String extractColour(String tok){
		return tok.substring(2,tok.length()-1);
	}
	/**
	 * process preformatted data
	 * @param txt The preformatted text
	 */
	protected void processPreformatted(String txt){
		int length, i;
		String t;
		
		result.append(tagMap.getProperty("startpre"));
		length = txt.length();
		for(i = 0; i  < length-1; i++){
			processChar(txt.substring(i,i+1));
		}
		result.append(tagMap.getProperty("endpre"));
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
			tagName = "endheader1";
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
			System.out.println("Header depth  " + depth + " out of range");
			return;
		}
		result.append(tagMap.getProperty(tagName));
	}
	/**
	 * start of header processing: count the =
	 * @param txt The header tag
	 */
	protected void processStartHeader(String txt){
		int depth = txt.trim().length();
		String tagName;
		switch(depth){
		case 1:
			tagName = "startheader1";
			break;
		case 2:
			tagName = "startheader2";
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
		result.append(tagMap.getProperty(tagName));
	}
	/**
	 * process a deflist item: item::data
	 * @param txt The item to process
	 */
	protected void processDefList(String txt){
		int idx;
		
		result.append(tagMap.getProperty("defitem"));
		idx = txt.indexOf(":");
		result.append(txt.substring(1,idx));
		result.append('\n');
		result.append(tagMap.getProperty("defitemclose"));
		result.append(tagMap.getProperty("defdata"));
	}
	/**
	 * process a single character
	 * @param txt The character to process
	 */
	protected void processChar(String txt){
		result.append(txt);
	}
	/**
	 * This is a support function for copying images etc from the database
	 * directory to the target directory of a possible export.
	 * @param dbdir The database directory
	 * @param targetDir The target directory
	 * @param file The file to copy.
	 */
	protected void tryCopyLocalFile(String dbdir, String targetDir, String file){
		if(file.startsWith("http") && file.startsWith("ftp")){
			return;
		}
		try{
			File in = new File(dbdir + File.separatorChar + file);
			File out = new File(targetDir + File.separatorChar + file);
			FileWriter fw = new FileWriter(out);
			IOUtils.copy(new FileReader(in), fw);
			fw.close();
		}catch(Exception eva){}
	}
	/**
	 * this method initializes a map which maps integer token codes to key names
	 * in the properties file
	 */
	private void initTokenToName(){
		tokenToName.put(new Integer(WikiToken.ENDBOLD),"endbold");
		tokenToName.put(new Integer(WikiToken.ENDITAL),"endital");
		tokenToName.put(new Integer(WikiToken.ENDSUB),"endsub");
		tokenToName.put(new Integer(WikiToken.ENDSUPER),"endsuper");
		tokenToName.put(new Integer(WikiToken.OLITEMCLOSE),"endolitem");
		tokenToName.put(new Integer(WikiToken.OLLIST),"startolitem");
		tokenToName.put(new Integer(WikiToken.OLLISTCLOSE),"endol");
		tokenToName.put(new Integer(WikiToken.OLLISTOPEN),"startol");
		tokenToName.put(new Integer(WikiToken.PAREND),"parend");
		tokenToName.put(new Integer(WikiToken.PAROPEN),"parstart");
		tokenToName.put(new Integer(WikiToken.STARTBOLD),"startbold");
		tokenToName.put(new Integer(WikiToken.STARTITAL),"startital");
		tokenToName.put(new Integer(WikiToken.STARTSUB),"startsub");
		tokenToName.put(new Integer(WikiToken.STARTSUPER),"startsuper");
		tokenToName.put(new Integer(WikiToken.TABLECLOSE),"endtable");
		tokenToName.put(new Integer(WikiToken.TABLEOPEN),"tablestart");
		tokenToName.put(new Integer(WikiToken.TABLEROW),"tablerowstart");
		tokenToName.put(new Integer(WikiToken.TABLEEND),"tablerowend");
		tokenToName.put(new Integer(WikiToken.TABLESEP),"tablesep");
		tokenToName.put(new Integer(WikiToken.ULITEMCLOSE),"endulitem");
		tokenToName.put(new Integer(WikiToken.ULLIST),"startulitem");
		tokenToName.put(new Integer(WikiToken.ULLISTCLOSE),"endul");
		tokenToName.put(new Integer(WikiToken.ULLISTOPEN),"startul");
		tokenToName.put(new Integer(WikiToken.DEFLISTCLOSE),"enddef");
		tokenToName.put(new Integer(WikiToken.DEFLISTOPEN),"startdef");
		tokenToName.put(new Integer(WikiToken.DEFITEMCLOSE),"defitemclose");
		tokenToName.put(new Integer(WikiToken.DEFDATACLOSE),"defdataclose");
		tokenToName.put(new Integer(WikiToken.DEFLIST),"defdata");
		tokenToName.put(new Integer(WikiToken.RULE),"rule");
		tokenToName.put(new Integer(WikiToken.STRONGRULE),"strongrule");
		tokenToName.put(new Integer(WikiToken.LINEBREAK),"linebreak");
		tokenToName.put(new Integer(WikiToken.STARTCENTER),"startcenter");
		tokenToName.put(new Integer(WikiToken.ENDCENTER),"endcenter");
		tokenToName.put(new Integer(WikiToken.STARTQUOTE),"startquote");
		tokenToName.put(new Integer(WikiToken.ENDQUOTE),"endquote");
		tokenToName.put(new Integer(WikiToken.ENDCOLOUR),"endcolour");
		tokenToName.put(new Integer(WikiToken.MULTIROW),"tablerowmultistart");
		tokenToName.put(new Integer(WikiToken.MULTISEP),"tablemultisep");
		
	}
}
