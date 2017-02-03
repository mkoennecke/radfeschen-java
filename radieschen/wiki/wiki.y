/**
  * JFlex input file for a Lexer for parsing Wiki markup into a token stream
  * 
  * Mark Koennecke, March 2006
  */
package radieschen.wiki;

%%
%class JFlexWikiLexer
%public
%implements TokenProducer
%unicode
%line
%column
%function nextToken
%type WikiToken

%{
	private boolean superOpen, subOpen, boldOpen, italOpen, centerOpen, quoteOpen, colourOpen;
	private StringBuffer preformat;
	
	private WikiToken makeToken(int type){
		return new WikiToken(type, yytext(), yyline, yycolumn);
	}
%} 

%init{
	superOpen = false;
	subOpen = false;
	boldOpen = false;
	italOpen = false;
	centerOpen = false;
	quoteOpen = false;
	colourOpen = false;
%init}

LF = \r|\n|\r\n
WS = {LF} | [ \t\f]

COLOR = "black" | "silver" | "gray" | "white" | "maroon" | "red" | "purple" | "aqua" | "fuchsia" | "green" | "lime" | "olive" | "yellow" | "navy" | "blue" | "teal" 

%state PREFORMATTED

%%
<YYINITIAL> {
/* headers */
^=+{WS} { return makeToken(WikiToken.STARTHEADER);}

=+$ { return makeToken(WikiToken.ENDHEADER);}

/* lists */
^\*+{WS} { return makeToken(WikiToken.ULLIST);}
^#+{WS} {return makeToken(WikiToken.OLLIST); }
^;~:    {return makeToken(WikiToken.DEFLIST); }

/* tables */
^\|\| {return makeToken(WikiToken.TABLEROW); }
^\|\|\|+ {return makeToken(WikiToken.MULTIROW); }
\|\| {return makeToken(WikiToken.TABLESEP); }
\|\|\|+ {return makeToken(WikiToken.MULTISEP); }
\|\|$ {return makeToken(WikiToken.TABLEEND); }

/*  preformatted text */
"\{\{\{" {
           preformat = new StringBuffer();
           yybegin(PREFORMATTED);
           }

/* horizontal rules */
---- {return makeToken(WikiToken.RULE); }
----+ {return makeToken(WikiToken.STRONGRULE); }

/* center */
^:: {
		if(centerOpen){
			centerOpen = false;
			return makeToken(WikiToken.ENDCENTER);
		}  else {
			centerOpen = true;
			return makeToken(WikiToken.STARTCENTER);
		}
    }

/* quote */
^-- {
		if(quoteOpen){
			quoteOpen = false;
			return makeToken(WikiToken.ENDQUOTE);
		}  else {
			quoteOpen = true;
			return makeToken(WikiToken.STARTQUOTE);
		}
    }

/* super script */
\^  { 
		if(superOpen){
			superOpen = false;
			return makeToken(WikiToken.ENDSUPER);
		} else {
			superOpen = true;
			return makeToken(WikiToken.STARTSUPER);
		}
  }

/* sub script */
__  { 
		if(subOpen){
			subOpen = false;
			return makeToken(WikiToken.ENDSUB);
		} else {
			subOpen = true;
			return makeToken(WikiToken.STARTSUB);
		}
 	}

/* bold */
'' | "**" { 
		if(boldOpen){
			boldOpen = false;
			return makeToken(WikiToken.ENDBOLD);
		} else {
			boldOpen = true;
			return makeToken(WikiToken.STARTBOLD);
		}
 	}
 	
/* coloured text */
\~\~{COLOR}: {
		colourOpen = true;
		return makeToken(WikiToken.STARTCOLOUR);
	   }

/* italics */
\~\~  { 
	    if(colourOpen){
	    	colourOpen = false;
	    	return makeToken(WikiToken.ENDCOLOUR);
	    }
		if(italOpen){
			italOpen = false;
			return makeToken(WikiToken.ENDITAL);
		} else {
			italOpen = true;
			return makeToken(WikiToken.STARTITAL);
		}
 	}
 
/* links */
\[\[~\]\] {return makeToken(WikiToken.IMAGE);}
\[~\] 	{ return makeToken(WikiToken.LINK);}

/* empty line: paragraph end */
^{LF}  { return makeToken(WikiToken.PAREND);}

/* forced linebreak */
%%% {return makeToken(WikiToken.LINEBREAK); }

/* escape */
\\. {return makeToken(WikiToken.ESCAPE);}

/* word at start of line */
^[:jletterdigit:] { return makeToken(WikiToken.STARTCHAR);}

{LF} {return makeToken(WikiToken.NL);}

&[:jletterdigit:]+; {
	return makeToken(WikiToken.SYMBOL);
}

/* normal word */
.  { return makeToken(WikiToken.CHAR);}

}

<PREFORMATTED> {

"\}\}\}" {
			yybegin(YYINITIAL);
			return new WikiToken(WikiToken.PRE, preformat.toString(),
				 yyline, yycolumn);
         }        
.|\n  { preformat.append(yytext()); }         
}
