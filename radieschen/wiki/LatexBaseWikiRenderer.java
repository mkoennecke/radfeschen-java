/**
 * This is a base class which holds all those fields and methods which 
 * are special to latex rendering classes.
 * 
 * Mark Koennecke, November 2006
 */
package radieschen.wiki;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.HashSet;

import org.apache.commons.io.FilenameUtils;

public class LatexBaseWikiRenderer extends BaseWikiRenderer{
	protected int tableDepth;
	protected boolean tableStart;
	protected HashMap charMap;
	protected HashMap symbolMap;
    protected boolean firstHeader;
	protected String targetDir, dbDir;
	protected LinkedList toConvert;
	protected HashSet converted;
	protected String currentFile;

	public LatexBaseWikiRenderer(String tagMap) throws IOException{
		super(tagMap);
		tableDepth = 0;
		tableStart = false;
		initCharMap();
		initSymbolMap();
		firstHeader = false;
		targetDir = null;
		dbDir = null;
		toConvert = new LinkedList();
		converted = new HashSet();
		currentFile = null;
	}
	/**
	 * The main entry point: the render function
	 * @param input RThe TokneProducer from which to render things
	 * @return A rendered String
	 */
	public  String render(TokenProducer input) throws IOException{
		WikiToken tok;
		String tagName;
		int length;
		boolean multiOpen = false;
		
		result = null;
		result = new StringBuffer();
		firstHeader = true;
		
		tok = input.nextToken();
		while(tok != null){
			//System.out.println("Rendering token: " + tok.getType() + ", text = " + tok.getText());
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
				break;
			case WikiToken.TABLEOPEN:
				tableStart = true;
				tableDepth = 0;
				appendTokenTag(tok.getType());
				break;
			case WikiToken.TABLEROW:
				if(tableStart){
					tableDepth++;
				}
				appendTokenTag(tok.getType());
				break;
			case WikiToken.MULTIROW:
				if(tableStart){
					tableDepth++;
				}
				processMultiStart(tok.getText());
				multiOpen = true;
				break;
			case WikiToken.TABLESEP:
				if(multiOpen){
					result.append("}");
					multiOpen = false;
				}
				if(tableStart){
					tableDepth++;
				}
				appendTokenTag(tok.getType());
				break;
			case WikiToken.MULTISEP:
				if(multiOpen){
					result.append("}");
					multiOpen = false;
				}
				if(tableStart){
					tableDepth++;
				}
				processMultiSep(tok.getText());
				multiOpen = true;
				break;
			case WikiToken.TABLEEND:
				if(multiOpen){
					result.append("}");
					multiOpen = false;
				}
				tableDepth++;
				treatTableSize();
				appendTokenTag(tok.getType());
				break;
			case WikiToken.SYMBOL:
				processSymbol(tok.getText());
				break;
			default:
				appendTokenTag(tok.getType());
				break;
			}
			tok = input.nextToken();
		}
		return result.toString();
	}
	/**
	 * processSymbol processes special symbols like umlauts etc.
	 * @txt The html code for the symbol
	 */
	protected void processSymbol(String txt){
		String enc = (String)symbolMap.get(txt);
		if(enc != null){
			result.append(enc);
		} else {
			result.append(txt);
		}
	}
	/**
	 * processImage processes an image tag. This is different for each implementation
	 * and has to be overriden by sub classes
	 * @param txt The image tag data
	 */
	protected void processImage(String txt){
		result.append("\\begin{figure}[!ht]\n");
		result.append("\\includegraphics[width=0.75\\textwidth]{");
		result.append(FilenameUtils.removeExtension(txt.substring(2,txt.length()-2)));
		result.append(".eps");
		result.append("}");
		result.append("\\end{figure}\n");
		tryCopyLocalFile(dbDir, targetDir,txt.substring(2,txt.length()-2));
	}
	
	/**
	 * append the mapped string according to the tokenType
	 * @param tokenType The type of the token to append
	 * @throws IOException when the token cannot be found
	 */
	protected void appendTokenTag(int tokenType)throws IOException{
		String tagName = (String)tokenToName.get(new Integer(tokenType));
		String tagText  = null;
		if(tagName == null){
			throw new IOException("Bad token maping");
		}
		tagText = tagMap.getProperty(tagName);
		if(tagText != null){
			result.append(tagText);
		}
	}

	/**
	 * in latex tabular, the tabular environment start must hold the size of the table.
	 * This is not known until the first complete table row has been read. This code
	 * replaces the tabular environment in result with something which matches the
	 * size of the table.Necessarily they will fail in tables which are not equally
	 * columned.
	 */
	private void treatTableSize(){
		int i;
		String tabst = "\\begin{tabular}";
		
		if(!tableStart){
			return;
		}
		
		StringBuffer tab = new StringBuffer();
		tab.append(tabst);
		tab.append("{|");
		for(i = 0; i < tableDepth; i++){
			tab.append("c|");
		}
		tab.append("}\n");
		int idx = result.lastIndexOf(tabst);
		if(idx >= 0){
			result = result.replace(idx,idx + tabst.length(),tab.toString());
		}
		tableStart = false;
	}
	/**
	 * process preformatted data
	 * @param txt The preformatted text
	 */
	protected void processPreformatted(String txt){
		
		result.append(tagMap.getProperty("startpre"));
        result.append(txt);
		result.append(tagMap.getProperty("endpre"));
	}
	/**
	 * process a deflist item: item::data
	 * @param txt The item to process
	 */
	protected void processDefList(String txt){
		int idx, i;
		String itemText;
		
		result.append(tagMap.getProperty("defitem"));
		idx = txt.indexOf(":");
		itemText = txt.substring(1,idx);
		for(i = 0; i < itemText.length(); i++){
			processChar(itemText.substring(i,i+1));
		}
		result.append('\n');
		result.append(tagMap.getProperty("defitemclose"));
		result.append(tagMap.getProperty("defdata"));
	}
	
	/**
	 * process a character, thereby applying Latex encoding
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
	 * initialize the character map
	 */
	private void initCharMap(){
		charMap = new HashMap();
		charMap.put("~","\\~{}");
		charMap.put("_","\\_");
		charMap.put("^","\\^{}");
		charMap.put("%","\\%");
		charMap.put("#","\\#");
		charMap.put("{","\\{");
		charMap.put("}","\\}");
		charMap.put("$","\\$");
	}
	/*
	 * initialize the symbol map
	 */
	private void initSymbolMap(){
		symbolMap = new HashMap();
		symbolMap.put("&uuml;","\\\"u");
		symbolMap.put("&auml;","\\\"a");
		symbolMap.put("&ouml;","\\\"o");
		symbolMap.put("&Uuml;","\\\"U");
		symbolMap.put("&Auml;","\\\"A");
		symbolMap.put("&Ouml;","\\\"O");
		symbolMap.put("&Aring;","\\AA");
		symbolMap.put("&aring;","\\aa");
	}
}

