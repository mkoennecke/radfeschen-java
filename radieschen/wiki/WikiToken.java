/*
 * Class encapsulating a Wiki Token
 *  
 * Created on Mar 10, 2006
 *
 * Author: Mark.Koennecke@psi.ch
 * License: GPL
 */  
package radieschen.wiki;

public class WikiToken {
	public static final int STARTHEADER =  4; 
	public static final int ENDHEADER   =  5; 
	public static final int STARTCHAR   =  6; 
	public static final int CHAR        =  7; 
	public static final int PAREND      =  8; 
	public static final int LINK        =  9; 
	public static final int STARTSUPER  =  10; 
	public static final int ENDSUPER    =  11; 
	public static final int STARTSUB    =  12; 
	public static final int ENDSUB      =  13; 
	public static final int STARTBOLD   =  14; 
	public static final int ENDBOLD     =  15; 
	public static final int STARTITAL   =  16; 
	public static final int ENDITAL     =  17; 
	public static final int NL          =  18; 
	public static final int ESCAPE      =  19; 
	public static final int PRE         =  20; 
	public static final int ULLIST      =  21; 
	public static final int OLLIST      =  22; 
	public static final int DEFLIST     =  23; 
	public static final int IMAGE       =  24;
	public static final int TABLEROW    =  25;
	public static final int TABLESEP    =  26;
	public static final int TABLEEND    =  28;
	public static final int RULE        =  29;
	public static final int STRONGRULE  =  30;
	public static final int LINEBREAK   =  31;
	public static final int STARTCENTER =  32;
	public static final int ENDCENTER   =  33;
	public static final int STARTQUOTE  =  34;
	public static final int ENDQUOTE    =  35;
	public static final int STARTCOLOUR =  36;
	public static final int ENDCOLOUR   =  37;
	public static final int MULTISEP    =  38; // multicolumn table separator
	public static final int MULTIROW    =  39; // multicolumn table row start
	public static final int SYMBOL      =  40;
	
	/**
	 * additional implicit tokens
	 */
	public static final int PAROPEN      = 100;
	public static final int TABLECLOSE   = 101;
	public static final int TABLEOPEN    = 102;
	public static final int DEFITEMCLOSE = 103;
	public static final int DEFDATACLOSE = 113; // out of order
	public static final int DEFLISTCLOSE = 104;
	public static final int DEFLISTOPEN  = 105;
	public static final int ULITEMCLOSE  = 106;
	public static final int ULLISTOPEN   = 107;
	public static final int ULLISTCLOSE  = 108;
	public static final int OLITEMCLOSE  = 109;
	public static final int OLLISTOPEN   = 110;
	public static final int OLLISTCLOSE  = 111;
	public static final int IGNORE       = 112;
	public static final int EOF          = 113;

	private String text;
	private int column, line, type;
	
	public WikiToken(int type, String txt, int l, int col){
		text = txt;
		this.type = type;
		line = l;
		column = col;
	}
	public WikiToken(int type){
		this.type = type;
		text = "";
		line = 0;
		column = 0;
	}
	
	public int getColumn() {
		return column;
	}

	public void setColumn(int column) {
		this.column = column;
	}

	public int getLine() {
		return line;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}
