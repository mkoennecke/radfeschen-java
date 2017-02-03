/**
 * This is a new implementation of the Wiki Token Filter. Its purpose it to insert 
 * the tokens missing in the stream from the wiki syntax into the token stream.
 * This reimplementation strives to provide a better structure and to handle the 
 * case of nested lists properly.
 * 
 * The design here is a state machine realized via the switch statement 
 * in nextToken. State is mainatined in a state stack. Depending on the 
 * current state a state handler function is called. The state handler 
 * functions checks if the next token causes a state change which requires 
 * other tokens to be added to the token strea, If so, this is done and 
 * if needed the state stakc modified. Functions return KEEP when no state
 * change happened or CHANGE when the state stack was changed. The do
 * loop in nextToken keeps on processing until a KEEP is obtained. 
 *  
 * copyright: GPL
 * 
 * Mark Koennecke, April 2007
 */
package radieschen.wiki;

import java.util.Stack;
import java.io.IOException;
	
public class NewTokenFilter implements TokenProducer {
	private Stack tokenStack, stateStack;
    private int ulDepth, olDepth, defDepth;
    private TokenProducer input;
	/**
	 * definition of states
	 */
    protected final int PARAGRAPH = 1001;
    protected final int ULLIST    = 1002;
    protected final int OLLIST    = 1003;
    protected final int DEFLIST   = 1004;
    protected final int ULITEM    = 1005;
    protected final int DEFITEM   = 1006;
    protected final int TABLE     = 1007;
    protected final int EMPTY     = 1008;
    protected final int OLITEM    = 1009;
    /*
     * definition of return codes
     */
    protected final int KEEP = 1;
    protected final int CHANGE  = 2;
    
	public NewTokenFilter(TokenProducer i ){
		input = i;
		tokenStack = new Stack();
		stateStack = new Stack();
		stateStack.push(new Integer(EMPTY));
		defDepth = 0;
		ulDepth = 0;
		olDepth = 0;
	}

	public WikiToken nextToken() throws IOException {
		WikiToken tok;
		int returnStatus = KEEP;
		Integer state;
		
		/*
		 * if there is something on the stack: serve it first
		 */
		if(!tokenStack.empty()){
			return (WikiToken)tokenStack.pop();
		}
		
		/*
		 * get a new token from input
		 */
		tok = input.nextToken();
		
		if(tok != null && tok.getType() == WikiToken.STARTCHAR){
			System.out.println("Startchar detected");
		}
		do {
			state = (Integer)stateStack.peek();
			switch(state.intValue()){
			case EMPTY:
				returnStatus = processEmpty(tok);
				break;
			case PARAGRAPH:
				returnStatus = processParagraph(tok);
				break;
			case ULLIST:
				returnStatus = processULList(tok);
				break;
			case ULITEM:
				returnStatus = processULItem(tok);
				break;
			case OLLIST:
				returnStatus = processOLList(tok);
				break;
			case OLITEM:
				returnStatus = processOLItem(tok);
				break;
			case DEFLIST:
				returnStatus = processDefList(tok);
				break;
			case DEFITEM:
				returnStatus = processDefItem(tok);
				break;
			case TABLE:
				returnStatus = processTable(tok);
				break;
			default:
				System.out.println("Invalid state" + state);
				System.exit(1);
				break;
			}
		}while(returnStatus == CHANGE);

		return (WikiToken)tokenStack.pop();
	}
	/**
	 * process tokens when in state empty
	 * @param tok The token to process
	 * @return KEEP or POP
	 */
	protected int processEmpty(WikiToken tok){
		if(tok == null){
			return KEEP;
		}
		switch(tok.getType()){
		case WikiToken.STARTCHAR:
		case WikiToken.DEFLIST:
		case WikiToken.LINK:
		case WikiToken.IMAGE:
		case WikiToken.OLLIST:
		case WikiToken.ULLIST:
		case WikiToken.STARTBOLD:
		case WikiToken.STARTITAL:
		case WikiToken.TABLEROW:
		case WikiToken.STARTCENTER:
		case WikiToken.STARTQUOTE:
		case WikiToken.STARTCOLOUR:
		case WikiToken.MULTIROW:
				stateStack.push(new Integer(PARAGRAPH));
				tokenStack.add(new WikiToken(WikiToken.PAROPEN));
				return CHANGE;
		}
		
		return KEEP;
	}
	/**
	 * process tokens in paragraph
	 * @param tok The token to process
	 * @return KEEP or CHANGE
	 */
	protected int processParagraph(WikiToken tok){

		switch(tok.getType()){
		case WikiToken.PAREND:	
			stateStack.pop();
			return CHANGE;
		case WikiToken.STARTHEADER:
		case WikiToken.RULE:
		case WikiToken.STRONGRULE:
			tokenStack.add(new WikiToken(WikiToken.PAREND));
			stateStack.pop();
			return CHANGE;
		case WikiToken.ULLIST:
			stateStack.push(new Integer(ULLIST));
			tokenStack.push(new WikiToken(WikiToken.ULLISTOPEN));
			ulDepth++;
			return CHANGE;
		case WikiToken.OLLIST:
			stateStack.push(new Integer(OLLIST));
			tokenStack.push(new WikiToken(WikiToken.OLLISTOPEN));
			olDepth++;
			return CHANGE;
		case WikiToken.TABLEROW:
		case WikiToken.MULTIROW:
			stateStack.push(new Integer(TABLE));
			tokenStack.add(new WikiToken(WikiToken.TABLEOPEN));
			return CHANGE;
		case WikiToken.DEFLIST:
			stateStack.push(new Integer(DEFLIST));
			tokenStack.add(new WikiToken(WikiToken.DEFLISTOPEN));
			defDepth = 1;
			return CHANGE;
		}
		return KEEP;
	}
	/**
	 * process for a UL list
	 * @param tok The WikiToken to process
	 * @return KEEP or CHANGE
	 */
	protected int processULList(WikiToken tok){
		int listDepth = countListDepth(tok.getText());
		switch(tok.getType()){
		case WikiToken.ULLIST:
			if(listDepth == ulDepth){
				stateStack.push(new Integer(ULITEM));
				return KEEP;
			}
			if(listDepth < ulDepth){
				tokenStack.add(new WikiToken(WikiToken.ULLISTCLOSE));
				stateStack.pop();
				ulDepth--;
				return CHANGE;
			}
			if(listDepth > ulDepth){
				tokenStack.add(new WikiToken(WikiToken.ULLISTOPEN));
				stateStack.push(new Integer(ULLIST));
				ulDepth++;
				return CHANGE;
			}
			break;
		case WikiToken.STARTCHAR:
		case WikiToken.PAREND:
		case WikiToken.PRE:
		case WikiToken.DEFLIST:
		case WikiToken.TABLEROW:
		case WikiToken.STARTHEADER:
		case WikiToken.RULE:
		case WikiToken.STRONGRULE:
		case WikiToken.MULTIROW:	
		case WikiToken.OLLIST:
			tokenStack.add(new WikiToken(WikiToken.ULLISTCLOSE));
			stateStack.pop();
			ulDepth--;
			return CHANGE;
		}
		return KEEP;
	}
	protected int processULItem(WikiToken tok){

		switch(tok.getType()){
		case WikiToken.ULLIST:
				tokenStack.add(new WikiToken(WikiToken.ULITEMCLOSE));
				stateStack.pop();
				return CHANGE;
		case WikiToken.OLLIST:
			tokenStack.add(new WikiToken(WikiToken.OLLISTOPEN));
			olDepth++;
			stateStack.push(new Integer(OLLIST));
			return CHANGE;
		case WikiToken.TABLEROW:
		case WikiToken.MULTIROW:
		case WikiToken.STARTCHAR:
		case WikiToken.PAREND:
		case WikiToken.PRE:
		case WikiToken.DEFLIST:
		case WikiToken.STARTCENTER:
		case WikiToken.STARTQUOTE:
		case WikiToken.STARTHEADER:
		case WikiToken.RULE:
		case WikiToken.STRONGRULE:
			tokenStack.add(new WikiToken(WikiToken.ULITEMCLOSE));
			stateStack.pop();
			return CHANGE;
		
		}
		return KEEP;
	}
	/**
	 * process for a OL list
	 * @param tok The WikiToken to process
	 * @return KEEP or CHANGE
	 */
	protected int processOLList(WikiToken tok){
		int listDepth = countListDepth(tok.getText());
		switch(tok.getType()){
		case WikiToken.OLLIST:
			if(listDepth == olDepth){
				stateStack.push(new Integer(OLITEM));
				return KEEP;
			}
			if(listDepth < olDepth){
				tokenStack.add(new WikiToken(WikiToken.OLLISTCLOSE));
				stateStack.pop();
				olDepth--;
				return CHANGE;
			}
			if(listDepth > olDepth){
				tokenStack.add(new WikiToken(WikiToken.OLLISTOPEN));
				stateStack.push(new Integer(OLLIST));
				olDepth++;
				return CHANGE;
			}
			break;
		case WikiToken.STARTCHAR:
		case WikiToken.PAREND:
		case WikiToken.PRE:
		case WikiToken.DEFLIST:
		case WikiToken.TABLEROW:
		case WikiToken.STARTHEADER:
		case WikiToken.RULE:
		case WikiToken.STRONGRULE:
		case WikiToken.MULTIROW:	
		case WikiToken.ULLIST:
			tokenStack.add(new WikiToken(WikiToken.OLLISTCLOSE));
			stateStack.pop();
			olDepth--;
			return CHANGE;
		}
		return KEEP;
	}
	protected int processOLItem(WikiToken tok){

		switch(tok.getType()){
		case WikiToken.OLLIST:
				tokenStack.add(new WikiToken(WikiToken.OLITEMCLOSE));
				stateStack.pop();
				return CHANGE;
		case WikiToken.ULLIST:
			tokenStack.add(new WikiToken(WikiToken.ULLISTOPEN));
			ulDepth++;
			stateStack.push(new Integer(ULLIST));
			return CHANGE;
		case WikiToken.TABLEROW:
		case WikiToken.MULTIROW:
		case WikiToken.STARTCHAR:
		case WikiToken.PAREND:
		case WikiToken.PRE:
		case WikiToken.DEFLIST:
		case WikiToken.STARTCENTER:
		case WikiToken.STARTQUOTE:
		case WikiToken.STARTHEADER:
		case WikiToken.RULE:
		case WikiToken.STRONGRULE:
			tokenStack.add(new WikiToken(WikiToken.OLITEMCLOSE));
			stateStack.pop();
			return CHANGE;
		
		}
		return KEEP;
	}
	protected int processDefList(WikiToken tok){
		switch(tok.getType()){
		case WikiToken.DEFLIST:
			stateStack.push(new Integer(DEFITEM));
			return KEEP;
		case WikiToken.TABLEROW:
		case WikiToken.MULTIROW:
		case WikiToken.STARTCHAR:
		case WikiToken.PAREND:
		case WikiToken.PRE:
		case WikiToken.STARTCENTER:
		case WikiToken.STARTQUOTE:
		case WikiToken.STARTHEADER:
		case WikiToken.RULE:
		case WikiToken.STRONGRULE:
			tokenStack.add(new WikiToken(WikiToken.DEFLISTCLOSE));
			defDepth--;
			stateStack.pop();
			return CHANGE;
		}
		return KEEP;
	}
	protected int processDefItem(WikiToken tok){
		switch(tok.getType()){
		case WikiToken.ULLIST:
			tokenStack.add(new WikiToken(WikiToken.ULLISTOPEN));
			ulDepth++;
			stateStack.push(new Integer(ULLIST));
			return CHANGE;
		case WikiToken.OLLIST:
			tokenStack.add(new WikiToken(WikiToken.OLLISTOPEN));
			olDepth++;
			stateStack.push(new Integer(OLLIST));
			return CHANGE;
		case WikiToken.DEFLIST:
		case WikiToken.TABLEROW:
		case WikiToken.MULTIROW:
		case WikiToken.STARTCHAR:
		case WikiToken.PAREND:
		case WikiToken.PRE:
		case WikiToken.STARTCENTER:
		case WikiToken.STARTQUOTE:
		case WikiToken.STARTHEADER:
		case WikiToken.RULE:
		case WikiToken.STRONGRULE:
			tokenStack.add(new WikiToken(WikiToken.DEFDATACLOSE));
			stateStack.pop();
			return CHANGE;
		}
		return KEEP;
	}
	/**
	 * table processing
	 * @param tok
	 * @return
	 */
	protected int processTable(WikiToken tok){
		switch(tok.getType()){
		case WikiToken.STARTCHAR:
		case WikiToken.PAREND:
		case WikiToken.PRE:
		case WikiToken.DEFLIST:
		case WikiToken.STARTCENTER:
		case WikiToken.STARTQUOTE:
		case WikiToken.STARTHEADER:
		case WikiToken.RULE:
		case WikiToken.STRONGRULE:
		case WikiToken.ULLIST:
		case WikiToken.OLLIST:
			tokenStack.add(new WikiToken(WikiToken.TABLECLOSE));
			stateStack.pop();
			return CHANGE;
		}
		return KEEP;
	}
	/**
	 * count the stacking depth of the list
	 * @param text The list item text
	 * @return The stacking depth
	 */
	private int countListDepth(String text){
		return text.trim().length();
	}
}
