/**
 * This is a preprocessor class which can do JXP like text replacement 
 * on a file. The following syntax is supported in the template file:
 * <% script %> evalues the script and copies the result into the text 
 *              stream
 * <%=var %>   is replaced by the value of var in the interpreter
 * <%! script %> Just execute the script; do no text replacement
 * 
 * copyright: GPL
 *               
 * Mark Koennecke, May 2007              
 */
package radieschen;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;

public class JXPProcessor {
	protected JXPInterpreter interpreter;
	
	public JXPProcessor(JXPInterpreter i){
		interpreter = i;
	}
	/*
	 * process the template instructions and write the result
	 * @param in The input stream with template instructions
	 * @param out The OutputStream with the processed material
	 * @throws IOException when things go wrong
	 */
	public void process(InputStream in, OutputStream out) throws IOException {
		int start, end, current = 0;
		PrintWriter pw = new PrintWriter(out);
		String script = null;
		
		byte b[] = IOUtils.toByteArray(in);
		String data = new String(b);
		
		start = data.indexOf("<%");
		end =   data.indexOf("%>");
		while(start >= 0){
			if(end < 0){
				throw new IOException("Found start tag but no end");
			}
			pw.print(data.substring(current, start));
			script = data.substring(start,end);
			pw.print(evaluateScript(script));
			current = end + 2; // TODO: check limits
			start = data.indexOf("<%",end+2);
			end   = data.indexOf("%>", start);
		}
		pw.print(data.substring(current,data.length()));
	}
	/*
	 * process JXP tags in input
	 * @param input The input text with JXP tags
	 * @return The processed text 
	 */
	public String processJXP(String input) throws IOException{
		StringBuffer data;
		int start, end, current = 0;
		String script = null;
		
		data = new StringBuffer();
		start = input.indexOf("<%");
		end =   input.indexOf("%>");
		while(start >= 0){
			if(end > start){
				data.append(input.substring(current, start));
				script = input.substring(start+2,end);
				data.append(evaluateScript(script));
				current = end + 2;
				start = input.indexOf("<%",end+2);
				end   = input.indexOf("%>", start);
			} else {
				start = input.indexOf("<%",start+2);
				end   = input.indexOf("%>", start);
			}
		}
	    data.append(input.substring(current,input.length()));
		return data.toString();
	}
	/**
	 * actually execute the script when necessary and put the data 
	 * into the output stream
	 * @param script The script to execute
	 * @return  The result of the script evaluation 
	 */
	protected String evaluateScript(String script){
		char c;
		String result = null;
		
		c = script.charAt(0);
		if(c == '='){
			// variable execution
			script = script.substring(1,script.length()).trim();
			result = interpreter.getVariable(script);
		} else if(c == '!'){
			script = script.substring(1,script.length());
			interpreter.evaluate(script);
		} else {
			result = interpreter.getEvalResult(script);
		}
		if(result != null){
			return result;
		} else {
			return "";
		}
	}
	/*
	 * a main method, for testing
	 */
	static void main(String argv[]){
		JXPInterpreter i = null;
		
		JXPProcessor p = new JXPProcessor(i);
		if(argv.length < 1){
			System.out.println("ERROR: need filename to process as argument");
			System.exit(1);
		}
		try{
			FileInputStream fin = new FileInputStream(argv[0]);
			p.process(fin, System.out);
		}catch(Exception  eva){
			eva.printStackTrace();
		}
	    System.exit(0);
	}
}
