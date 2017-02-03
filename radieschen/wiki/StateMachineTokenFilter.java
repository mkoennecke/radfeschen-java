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
	
public class StateMachineTokenFilter implements TokenProducer {
	private Stack tokenStack, stateStack;
    private int defDepth, listDepth, headerDepth;
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
    protected final int HEADER    = 1010;
    /*
     * definition of return codes
     */
    protected final int KEEP   = 1;
    protected final int CHANGE = 2;
    
	public StateMachineTokenFilter(TokenProducer i ){
		input = i;
		tokenStack = new Stack();
		stateStack = new Stack();
		stateStack.push(new Integer(EMPTY));
		defDepth = 0;
		listDepth = 0; 
		headerDepth = 0;
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
		
		if(tok != null && tok.getType() == WikiToken.STARTQUOTE){
//			System.out.println("End detected");
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
			case HEADER:
				returnStatus = processHeader(tok);
				break;
			default:
				System.out.println("Invalid state " + state);
				System.exit(1);
				break;
			}
		}while(returnStatus == CHANGE);
		tokenStack.insertElementAt(tok, 0);
		
/*
		WikiToken t = (WikiToken)tokenStack.peek();
		if(t != null){
			System.out.println("Returning token type: " + t.getType() + ", text = " +
					t.getText());
		}
*/
		return (WikiToken)tokenStack.pop();
	}
	protected int processHeader(WikiToken tok){
		if(tok == null){
			return KEEP;
		}
		switch(tok.getType()){
		case WikiToken.ENDHEADER:
			stateStack.pop();
			return KEEP;
		case WikiToken.DEFLIST:
		case WikiToken.MULTIROW:
		case WikiToken.OLLIST:
		case WikiToken.PAROPEN:
		case WikiToken.PRE:
		case WikiToken.RULE:
		case WikiToken.STRONGRULE:
		case WikiToken.STARTCENTER:
		case WikiToken.STARTCHAR:
		case WikiToken.STARTHEADER:
		case WikiToken.TABLEROW:
		case WikiToken.ULLIST:
			WikiToken plus = new WikiToken(WikiToken.ENDHEADER);
			StringBuffer stb = new StringBuffer();
			for(int i = 0; i < headerDepth; i++){
				stb.append('=');
			}
			plus.setText(stb.toString());
			tokenStack.insertElementAt(plus, 0);
			stateStack.pop();
			return KEEP;
		}
		return KEEP;
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
		case WikiToken.LINK:
		case WikiToken.IMAGE:
		case WikiToken.STARTBOLD:
		case WikiToken.STARTITAL:
		case WikiToken.STARTCOLOUR:
				stateStack.push(new Integer(PARAGRAPH));
				tokenStack.insertElementAt(new WikiToken(WikiToken.PAROPEN),0);
				return CHANGE;
		case WikiToken.ULLIST:
			stateStack.push(new Integer(ULLIST));
			tokenStack.insertElementAt(new WikiToken(WikiToken.ULLISTOPEN),0);
			listDepth++;
			return CHANGE;
		case WikiToken.OLLIST:
			stateStack.push(new Integer(OLLIST));
			tokenStack.insertElementAt(new WikiToken(WikiToken.OLLISTOPEN),0);
			listDepth++;
			return CHANGE;
		case WikiToken.TABLEROW:
		case WikiToken.MULTIROW:
			stateStack.push(new Integer(TABLE));
			tokenStack.insertElementAt(new WikiToken(WikiToken.TABLEOPEN),0);
			return CHANGE;
		case WikiToken.DEFLIST:
			stateStack.push(new Integer(DEFLIST));
			tokenStack.insertElementAt(new WikiToken(WikiToken.DEFLISTOPEN),0);
			defDepth = 1;
			return CHANGE;
		case WikiToken.STARTHEADER:
			headerDepth = countListDepth(tok.getText());
			stateStack.push(new Integer(HEADER));
			return KEEP;
		case WikiToken.PAREND:
			tok.setType(WikiToken.CHAR);
			break;
		}
		return KEEP;
	}
	/**
	 * process tokens in paragraph
	 * @param tok The token to process
	 * @return KEEP or CHANGE
	 */
	protected int processParagraph(WikiToken tok){

		if(tok == null){
			tokenStack.insertElementAt(new WikiToken(WikiToken.PAREND),0);
			stateStack.pop();
			return CHANGE;
		}
		switch(tok.getType()){
		case WikiToken.PAREND:	
			stateStack.pop();
			return KEEP;
		case WikiToken.STARTHEADER:
		case WikiToken.RULE:
		case WikiToken.STRONGRULE:
		case WikiToken.ULLIST:
		case WikiToken.OLLIST:
		case WikiToken.DEFLIST:
		case WikiToken.TABLEROW:
		case WikiToken.MULTIROW:
		case WikiToken.PRE:
		case WikiToken.STARTCENTER:
		case WikiToken.ENDCENTER:
		case WikiToken.STARTQUOTE:
		case WikiToken.ENDQUOTE:
			tokenStack.insertElementAt(new WikiToken(WikiToken.PAREND),0);
			stateStack.pop();
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
		if(tok == null){
			tokenStack.insertElementAt(new WikiToken(WikiToken.ULLISTCLOSE),0);
			stateStack.pop();
			listDepth--;
			return CHANGE;
		}
		int tokDepth = countListDepth(tok.getText());
		switch(tok.getType()){
		case WikiToken.ULLIST:
			if(tokDepth == listDepth){
				stateStack.push(new Integer(ULITEM));
				return KEEP;
			}
			if(tokDepth < listDepth){
				tokenStack.insertElementAt(new WikiToken(WikiToken.ULLISTCLOSE),0);
				stateStack.pop();
				listDepth--;
				return CHANGE;
			}
			if(tokDepth > listDepth){
				tokenStack.insertElementAt(new WikiToken(WikiToken.ULLISTOPEN),0);
				stateStack.push(new Integer(ULLIST));
				listDepth++;
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
		case WikiToken.STARTCENTER:
		case WikiToken.STARTQUOTE:
			tokenStack.insertElementAt(new WikiToken(WikiToken.ULLISTCLOSE),0);
			stateStack.pop();
			listDepth--;
			return CHANGE;
		}
		return KEEP;
	}
	protected int processULItem(WikiToken tok){

		if(tok == null){
			tokenStack.insertElementAt(new WikiToken(WikiToken.ULITEMCLOSE),0);
			stateStack.pop();
			return CHANGE;
		}
		int tokDepth = countListDepth(tok.getText());
		switch(tok.getType()){
		case WikiToken.ULLIST:
				if(tokDepth <= listDepth) {
					tokenStack.insertElementAt(new WikiToken(WikiToken.ULITEMCLOSE),0);
					stateStack.pop();
					return CHANGE;
				} else {
					tokenStack.insertElementAt(new WikiToken(WikiToken.ULLISTOPEN),0);
					stateStack.push(new Integer(ULLIST));
					listDepth++;
					return CHANGE;
				}
		case WikiToken.OLLIST:
			if(tokDepth > listDepth){
				tokenStack.insertElementAt(new WikiToken(WikiToken.OLLISTOPEN),0);
				listDepth++;
				stateStack.push(new Integer(OLLIST));
				return CHANGE;
			} else {
				tokenStack.insertElementAt(new WikiToken(WikiToken.ULITEMCLOSE),0);
				stateStack.pop();
				return CHANGE;
			}
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
			tokenStack.insertElementAt(new WikiToken(WikiToken.ULITEMCLOSE),0);
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
		if(tok == null){
			tokenStack.insertElementAt(new WikiToken(WikiToken.OLLISTCLOSE),0);
			stateStack.pop();
			listDepth--;
			return CHANGE;
		}
		int tokDepth = countListDepth(tok.getText());
		switch(tok.getType()){
		case WikiToken.OLLIST:
			if(tokDepth == listDepth){
				stateStack.push(new Integer(OLITEM));
				return KEEP;
			}
			if(tokDepth < listDepth){
				tokenStack.insertElementAt(new WikiToken(WikiToken.OLLISTCLOSE),0);
				stateStack.pop();
				listDepth--;
				return CHANGE;
			}
			if(tokDepth > listDepth){
				tokenStack.insertElementAt(new WikiToken(WikiToken.OLLISTOPEN),0);
				stateStack.push(new Integer(OLLIST));
				listDepth++;
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
		case WikiToken.STARTQUOTE:
			tokenStack.insertElementAt(new WikiToken(WikiToken.OLLISTCLOSE),0);
			stateStack.pop();
			listDepth--;
			return CHANGE;
		}
		return KEEP;
	}
	protected int processOLItem(WikiToken tok){
		if(tok == null){
			tokenStack.insertElementAt(new WikiToken(WikiToken.OLITEMCLOSE),0);
			stateStack.pop();
			return CHANGE;
		}
		int tokDepth = countListDepth(tok.getText());
		switch(tok.getType()){
		case WikiToken.OLLIST:
			if(tokDepth <= listDepth){
				tokenStack.insertElementAt(new WikiToken(WikiToken.OLITEMCLOSE),0);
				stateStack.pop();
				return CHANGE;
			} else {
				tokenStack.insertElementAt(new WikiToken(WikiToken.OLLISTOPEN),0);
				stateStack.push(new Integer(OLLIST));
				listDepth++;
				return CHANGE;
			}
		case WikiToken.ULLIST:
			if(tokDepth > listDepth){
				tokenStack.insertElementAt(new WikiToken(WikiToken.ULLISTOPEN),0);
				listDepth++;
				stateStack.push(new Integer(ULLIST));
				return CHANGE;
			} else {
				tokenStack.insertElementAt(new WikiToken(WikiToken.OLITEMCLOSE),0);
				stateStack.pop();
				return CHANGE;
			}
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
			tokenStack.insertElementAt(new WikiToken(WikiToken.OLITEMCLOSE),0);
			stateStack.pop();
			return CHANGE;
		
		}
		return KEEP;
	}
	protected int processDefList(WikiToken tok){
		if(tok == null){
			tokenStack.insertElementAt(new WikiToken(WikiToken.DEFLISTCLOSE),0);
			defDepth--;
			stateStack.pop();
			return CHANGE;
		}
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
			tokenStack.insertElementAt(new WikiToken(WikiToken.DEFLISTCLOSE),0);
			defDepth--;
			stateStack.pop();
			return CHANGE;
		}
		return KEEP;
	}
	protected int processDefItem(WikiToken tok){
		if(tok == null){
			tokenStack.insertElementAt(new WikiToken(WikiToken.DEFDATACLOSE),0);
			stateStack.pop();
			return CHANGE;
		}
		switch(tok.getType()){
		case WikiToken.ULLIST:
			tokenStack.insertElementAt(new WikiToken(WikiToken.ULLISTOPEN),0);
			listDepth++;
			stateStack.push(new Integer(ULLIST));
			return CHANGE;
		case WikiToken.OLLIST:
			tokenStack.insertElementAt(new WikiToken(WikiToken.OLLISTOPEN),0);
			listDepth++;
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
			tokenStack.insertElementAt(new WikiToken(WikiToken.DEFDATACLOSE),0);
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
		if(tok == null){
			tokenStack.insertElementAt(new WikiToken(WikiToken.TABLECLOSE),0);
			stateStack.pop();
			return CHANGE;
		}
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
			tokenStack.insertElementAt(new WikiToken(WikiToken.TABLECLOSE),0);
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
