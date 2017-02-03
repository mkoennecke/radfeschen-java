/**
 * This is the interface any interpreter has to implement which can 
 * operate on JXP style syntax files. For more information, see file
 * JXPProcessor.java
 * 
 *  copyright: GPL
 *  
 *  Mark Koennecke, May 2007
 */
package radieschen;

public interface JXPInterpreter {
	/**
	 * get the value of variable name as a string
	 * @param name The name of the variable
	 * @return The value formatted as a string or UNKNOWN in case 
	 * of an error
	 */
	public String getVariable(String name);
	/**
	 * get the result of the evaluation of script as text
	 * @param script The script to evaluate
	 * @return The result of the script evaluation as a string
	 */
	public String getEvalResult(String script);
	/**
	 * just evaluate script. 
	 * @param script The script to evaluate
	 */
	public void evaluate(String script);
	/**
	 * set the Radieschen Directory. This is meant to
	 * help the Interpreter locate scripts in the 
	 * Radieschen database.
	 * @param dir  The directory for scripts. 
	 */
	public void setRadiDir(String dir);

}
