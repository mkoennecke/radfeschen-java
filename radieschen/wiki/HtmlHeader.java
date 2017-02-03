/**
 * This is little helper class which builds a HTML header for Radieschen.
 */
package radieschen.wiki;

public class HtmlHeader {
	final static private  String html3doc = 
		"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">\n";
	final static private String xhtmldoc = 
			"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"" +
			" \"http://www.w3c.org/TR/xhtml1/DTD/xhtml1-strict.dtd\" >\n";
	
	/**
	 * make a HTML3.2 header
	 * @param title The page title
	 * @param base The directory to use as base
	 * @param css The css filename for styles
	 * @return A nice header till the body tag
	 */
	public static String makeHtml3Header(String title, String base, String css){
		StringBuffer stb = new StringBuffer();
		stb.append(html3doc);
		stb.append("<html><head>");
		stb.append("<title>");
		stb.append(title);
		stb.append("</title>\n");
		stb.append("<base href=\"file://");
		stb.append(base);
		stb.append("/\">\n");
		if(css != null){
			stb.append("<LINK rel=\"stylesheet\" type=\"text/css\" href=\"");
			stb.append(css);
			stb.append("\">");
		}
		stb.append("</head><body BGCOLOR=\"#FFFFFF\" >\n");
		return stb.toString();
	}
	/**
	 * make a XHTML header
	 * @param title The page title
	 * @param base The directory to use as base
	 * @param css The css filename for styles
	 * @return A nice header till the body tag
	 */
	public static String makeXHtmlHeader(String title, String base, String css){
		StringBuffer stb = new StringBuffer();
		stb.append(xhtmldoc);
		stb.append("<html><head>");
		stb.append("<title>");
		stb.append(title);
		stb.append("</title>\n");
		stb.append("<base href=\"file://");
		stb.append(base);
		stb.append("/\"/>\n");
		if(css != null){
			stb.append("<LINK rel=\"stylesheet\" type=\"text/css\" href=\"");
			stb.append(css);
			stb.append("\"/>");
		}
		stb.append("</head><body BGCOLOR=\"#FFFFFF\">\n");
		return stb.toString();
	}
}
