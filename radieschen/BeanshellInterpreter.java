/**
 * This is an Interpreter interfacer for Radieschens JXP 
 * interface and the beanshell scripting language.
 * 
 *  copyright: GL
 *  
 *  Mark Koennecke, May 2007
 */
package radieschen;
import bsh.Interpreter;
import bsh.EvalError;

public class BeanshellInterpreter implements JXPInterpreter {
	protected Interpreter i;
	
	public BeanshellInterpreter(String dir){
		i = new Interpreter();
		setRadiDir(dir);
	}
	
	public void evaluate(String script) {
		try{
			Object o = i.eval(script);
		}catch(EvalError eva){
			System.out.println(eva.getErrorText());
			System.out.println("File: " + eva.getErrorSourceFile() 
					+ " line: " + eva.getErrorLineNumber());
		}
	}

	public String getEvalResult(String script) {
		try{
			Object o = i.eval(script);
			if(o != null){
				return o.toString();
			} else {
				return "";
			}
		}catch(EvalError eva){
			return eva.getErrorText() + "File: " + 
				eva.getErrorSourceFile() 
					+ " line: " + eva.getErrorLineNumber();
		}
	}

	public String getVariable(String name) {
		try{
			Object o = i.get(name);
			return o.toString();
		}catch(EvalError eva){
			return eva.getErrorText() + "File: " + 
				eva.getErrorSourceFile() 
					+ " line: " + eva.getErrorLineNumber();
		}
	}
	/**
	 * set the Radieschen Directory. This is meant to
	 * help the Interpreter locate scripts in the 
	 * Radieschen database.
	 * @param dir  The directory for scripts. 
	 */
	public void setRadiDir(String dir){
		i = null;
		i = new Interpreter();
		try{
			i.eval("addClassPath(\"" + dir + "\");");
		}catch(Exception eva){}
	}
}
