/*
 * Created on Feb 20, 2006
 * Main Class for the ZiKi Personal Wiki. ZiKi makes use of two other great
 * open source components: swixml for generating the UI and Textile4J for converting
 * WiKi markup into html. My contribution is just these few minor classes to pack those
 * Giants products together.
 * 
 * Author: Mark.Koennecke@psi.ch
 * License: GPL
 * 
 * Major update, November 2006, added Undo, Redo, Multi column table elements,
 * export to prosper and WWW presentation format
 * Mark Koennecke  
 */  
package radieschen;

import org.swixml.SwingEngine;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.text.DefaultEditorKit;

import java.io.*;
import java.util.Stack;
import java.net.URL;
import java.util.Properties;
import java.util.Vector;
import java.util.Enumeration;

import org.apache.commons.io.FileUtils;

import radieschen.wiki.*;

import javax.swing.undo.UndoManager;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CannotRedoException;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.UndoableEditEvent;

public class RadiMain implements HyperlinkListener,
	ListSelectionListener {
	protected SwingEngine swix;
	protected String currentFile;
	protected boolean editing;
	protected JEditorPane eddi;
	protected JList radiList;
	protected RadiDatabase db;
	protected Stack urlStack;
	protected String currentUrl;
	protected Properties radiProperty;
	protected String cssFile;
	protected String oldExportDir;
	/**
	 * all the Undoe, Redo stuff copied from the Notepad demo application
	 * coming with the Java SDK
	 */
	protected UndoManager undo;
	public UndoAction undoAction;
	public RedoAction redoAction;
	protected UndoHandler undoHandler;
	
    class UndoHandler implements UndoableEditListener {

    	/**
    	 * Messaged when the Document has created an edit, the edit is
    	 * added to <code>undo</code>, an instance of UndoManager.
    	 */
            public void undoableEditHappened(UndoableEditEvent e) {
    	    undo.addEdit(e.getEdit());
    	    undoAction.update();
    	    redoAction.update();
    	}
        }
	
    class UndoAction extends AbstractAction {
    	public UndoAction() {
    	    super("Undo");
    	    setEnabled(false);
    	}

    	public void actionPerformed(ActionEvent e) {
    	    try {
    		undo.undo();
    	    } catch (CannotUndoException ex) {
    		System.out.println("Unable to undo: " + ex);
    		ex.printStackTrace();
    	    }
    	    update();
    	    redoAction.update();
    	}

    	protected void update() {
    	    if(undo.canUndo()) {
    	    	setEnabled(true);
    	    	putValue(Action.NAME, undo.getUndoPresentationName());
    	    } else {
    	    	setEnabled(false);
    	    	putValue(Action.NAME, "Undo");
    	    }
    	}
        }

        class RedoAction extends AbstractAction {
    	public RedoAction() {
    	    super("Redo");
    	    setEnabled(false);
    	}

    	public void actionPerformed(ActionEvent e) {
    	    try {
    		undo.redo();
    	    } catch (CannotRedoException ex) {
    		System.out.println("Unable to redo: " + ex);
    		ex.printStackTrace();
    	    }
    	    update();
    	    undoAction.update();
    	}

    	protected void update() {
    	    if(undo.canRedo()) {
    		setEnabled(true);
    		putValue(Action.NAME, undo.getRedoPresentationName());
    	    }
    	    else {
    		setEnabled(false);
    		putValue(Action.NAME, "Redo");
    	    }
    	}
        }
	
	public Action menuAction = new AbstractAction() {
		public void actionPerformed(ActionEvent e){
			Action a;
			String command = e.getActionCommand();
			//System.out.println("Doing command: " + command);
			RadiMain me = (RadiMain)swix.getClient();
			me.doCommand(command);
		}
	};
	public Action cutAction, copyAction, pasteAction;

	/**
	 * constructor
	 */
	public RadiMain() throws Exception{
        cutAction = new DefaultEditorKit.CutAction();
        copyAction = new DefaultEditorKit.CopyAction();
        pasteAction = new DefaultEditorKit.PasteAction();
        undo = new UndoManager();
        undoHandler = new UndoHandler();
        undoAction = new UndoAction();
        redoAction = new RedoAction();
		swix = new SwingEngine(this);
		Container c = swix.render("radieschen/radieschen.xml");
		editing = false;
		urlStack = new Stack();
		radiProperty = new Properties();
		cssFile =  "radi.css";
		oldExportDir = null;
		
		/* 
		 * configure UI elements
		 */
		eddi = (JEditorPane)swix.find("eddi");
		eddi.addHyperlinkListener(this);

		radiList = (JList)swix.find("zikilist");
		radiList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		radiList.addListSelectionListener(this);

		/*
		 * start directory...
		 */
		currentFile = "Start.radi";
		setInitialRadiDir();
		syncExternalBrowser();
		Frame f = swix.getAppFrame();
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				try{
					doSave();
				}catch(Exception eva){}
			}
		}
		);
		c.setVisible(true);
	}
	/**
	 * make the name of the Radi property file
	 * @return The name of the Radi property file.
	 */
	private String makeRadiFile(){
		String radiFile;
		
		radiFile = System.getProperty("user.home");
		if(radiFile == null){
			radiFile = "C:\\";
		}
		radiFile += File.separatorChar + ".radieschen";
		return radiFile;
	}
	/**
	 * try to find the last saved radidir, if not look for default, 
	 * if not create. 
	 * @throws IOException
	 */
	private void setInitialRadiDir() throws IOException{
		String RadiFile = makeRadiFile();
		try{
			radiProperty.load(new FileInputStream(RadiFile));
		}catch(Exception eva){}
		if(radiProperty.getProperty("editor") == null){
			radiProperty.setProperty("editor","undefined");
		}
		if(radiProperty.getProperty("browser") == null){
			radiProperty.setProperty("browser","undefined");
		}
		if(radiProperty.getProperty("externalbrowser") == null){
			radiProperty.setProperty("externalbrowser","false");
		}
		String projectDir = radiProperty.getProperty("radidir");
		if(projectDir != null){
			db = new RadiDatabase(projectDir);
		} else {
			db = new RadiDatabase();
		}
		setCurrentDirectory(db.getDatabase());
	}
	/**
	 * doCommand handles all menu commands
	 * @param command The command to handle
	 */
	private void doCommand(String command){
		
		//System.out.println(command);
		try{
			if(command.compareTo("Exit") == 0){
				try{
					doSave();
				}catch(Exception erika){}
				System.exit(0);
				return;
			} else if(command.compareTo("Edit") == 0){
				doEdit();
				return;
			} else if(command.compareTo("Save") == 0){
				doSave();
				return;
			} else if(command.compareTo("Show Formatting Help") == 0){
				loadHelp("/radieschen/quick.html");
				return;
			} else if(command.compareTo("General Help") == 0){
				loadHelp("/radieschen/RadiHelp.html");
				return;
			} else if(command.compareTo("About") == 0){
				loadHelp("/radieschen/RadiAbout.html");
				return;
			} else if(command.compareTo("Back") == 0){
				backCommand();
				return;
			} else if(command.compareTo("Home") == 0){
				if(editing){
					doSave();
				}
				loadRadiFile("Start.radi");
				return;
			} else if(command.compareTo("Open Radidir") == 0){
				openRadiDir();
				return;
			} else if(command.compareTo("Delete Selected Page") == 0){
				deleteSelected();
				return;
			} else if(command.compareTo("Show HTML") == 0){
				showHtml();
				return;
			} else if(command.compareTo("Export RADI") == 0){
				exportRadi();
				return;
			} else if(command.compareTo("Export Markdown") == 0){
				exportMarkdown();
				return;
			} else if(command.compareTo("Export HTML") == 0){
				exportHtml();
				return;
			} else if(command.compareTo("Export XHTML") == 0){
				exportXHtml();
				return;
			} else if(command.compareTo("Export S5") == 0){
				exportS5();
				return;
			} else if(command.compareTo("Export Latex Article") == 0){
				exportLatex(RadiDatabase.ARTICLE);
				return;
			} else if(command.compareTo("Export Latex Book") == 0){
				exportLatex(RadiDatabase.BOOK);
				return;
			} else if(command.compareTo("Export Prosper") == 0){
				exportLatex(RadiDatabase.PROSPER);
				return;
			} else if(command.compareTo("Export Beamer") == 0){
				exportLatex(RadiDatabase.BEAMER);
				return;
			} else if(command.compareTo("Reload") ==0){
				if(!editing){
					loadRadiFile(currentFile);
					loadRadiFileList();
				} else {
					doEdit();
				}
				return;
			} else if(command.compareTo("Abort Edit") == 0){
				abortEdit();
				return;
			} else if(command.compareTo("Edit External") == 0){
				editExternally();
				return;
			} else if(command.compareTo("Edit Properties") == 0){
				editProperties();
				return;
			} else if(command.compareTo("Delete Line") == 0){
				deleteLine();
				return;
			} else if(command.compareTo("Go") == 0){
				search();
				return;
			} else if(command.compareTo("External Pages in External Browser") == 0){
				toggleExternalBrowser();
				return;
			} else if(command.compareTo("Import Media") == 0){
				importMedia();
				return;
			}
		}catch(Exception eva){
			JOptionPane.showMessageDialog(swix.getAppFrame(),
					"Exception: " + eva.getMessage() + " occurred","Radi Exception",
					JOptionPane.ERROR_MESSAGE);
			eva.printStackTrace();
			System.out.println("Exception " + eva.getMessage() + " Occured while accessing "
					+ currentFile);
			eva.printStackTrace();
		}
	}
	/**
	 * change the state of the external browser flag.
	 */
	private void toggleExternalBrowser(){
		JCheckBoxMenuItem jb = (JCheckBoxMenuItem)swix.find("exBox"); 
		if(jb.getState()){
			radiProperty.setProperty("externalbrowser", "true");
		} else {
			radiProperty.setProperty("externalbrowser", "false");
		}
		saveRadiDirProperty();
	}
	/**
	 * make the state of the external browser checkbox  match the state of
	 * the property
	 */
	private void syncExternalBrowser(){
		String state = radiProperty.getProperty("externalbrowser");
		JCheckBoxMenuItem jb = (JCheckBoxMenuItem)swix.find("exBox"); 
		if(state != null && state.indexOf("true") >= 0){
			jb.setState(true);
		} else {
			jb.setState(false);
		}
	}
	/**
	 * delete the current line
	 */
	private void deleteLine(){
		int start, end;
		char c;
		
		if(editing){
			JEditorPane eddi = (JEditorPane)swix.find("eddi");
			int pos = eddi.getCaretPosition();
			String text = eddi.getText();
			/*
			 * find start
			 */
			for(start = pos; start >= 0; start--){
				c = text.charAt(start);
				if(c == '\n' || c == '\r'){
					break;
				}
			}
			start++;
			for(end = pos; pos < text.length(); end++){
				c = text.charAt(end);
				if(c == '\n' || c == '\r'){
					break;
				}
			}
			eddi.select(start,end+1);
			eddi.cut();
		}
	}
	/**
	 * edit the properties of this
	 */
	private void editProperties(){
		PropertyDialog pd = new PropertyDialog(swix.getAppFrame(),radiProperty);
		pd.setVisible(true);
		saveRadiDirProperty();
		syncExternalBrowser();
	}
	/**
	 * showHtml shows the HTML as rendered by Textile. This is useful for debugging the
	 * renderer and the markup
	 */
	private void showHtml() throws IOException{
		if(editing){
			doSave();
		}
		urlStack.push(currentUrl);
		String content = eddi.getText();
		eddi.setEditorKit(null);
		eddi.setContentType("text/plain");
		eddi.setEditable(false);
		eddi.setText(content);
	}
	/**
	 * handle deletion of a Radi File
	 */
	private void deleteSelected() throws IOException{
		String RadiFile = (String)radiList.getSelectedValue();
		if(RadiFile != null){
			int result = JOptionPane.showConfirmDialog(swix.getAppFrame(),
					"Do you really want to delete: " + RadiFile + " ?",
					"Confirm Deletion",JOptionPane.YES_NO_OPTION);
			if(result == JOptionPane.YES_OPTION){
				if(RadiFile.compareTo(currentFile) == 0){
					loadRadiFile("Start.radi");
				}
				db.deleteWikiFile(RadiFile);
				loadRadiFileList();
			}
		}
	}
	/**
	 * openRadiDir throws a directory serach dialog  and on success sets the
	 * working directory for the Radi to the new value
	 */
	private void openRadiDir()throws IOException{
		JFileChooser jfc = new JFileChooser();
		String savedRadi;
		
		savedRadi = db.getDatabase();
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		String olddir = System.getProperty("user.home");
		if(olddir == null){
			olddir = "C:\\";
		}
		if(olddir != null){
			jfc.setCurrentDirectory(new File(olddir));
		}
		int retval = jfc.showOpenDialog(swix.getAppFrame());
		if(retval == JFileChooser.APPROVE_OPTION){
			File d = jfc.getSelectedFile();
			if(d.isDirectory()){
				try{
					setCurrentDirectory(d.getAbsolutePath());
					saveRadiDirProperty();
				}catch(Exception eva){
					setCurrentDirectory(savedRadi);
					saveRadiDirProperty();
				}
			}
		}		
	}
	/**
	 * openExportDir throws a directory serach dialog  and on success returns the name
	 * of an export directory.
	 * @return null on failure or the directory name
	 */
	private String openExportDir()throws IOException{
		JFileChooser jfc = new JFileChooser();
		String olddir;
		
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if(oldExportDir != null){
			olddir = oldExportDir;
		} else {
			olddir = System.getProperty("user.home");
			if(olddir == null){
				olddir = "C:\\";
			}
		}
		if(olddir != null){
			jfc.setCurrentDirectory(new File(olddir));
		}
		int retval = jfc.showOpenDialog(swix.getAppFrame());
		if(retval == JFileChooser.APPROVE_OPTION){
			File d = jfc.getSelectedFile();
			if(d.isDirectory()){
				oldExportDir = d.getAbsolutePath();
				return d.getAbsolutePath();
			}
		}
		return null;
	}
	/**
	 * export the current page as source
	 */
	private void exportRadi() throws IOException{
		String targetDir = openExportDir();
		if(targetDir != null){
			db.exportRadi(targetDir,currentFile,cssFile);
			checkCSS(targetDir);
		}
	}
	/**
	 * export the current page as source
	 */
	private void exportMarkdown() throws IOException{
		String targetDir = openExportDir();
		if(targetDir != null){
			db.exportMarkdown(targetDir,currentFile,cssFile);
			checkCSS(targetDir);
		}
	}
	/**
	 * export the current page as HTML
	 */
	private void exportHtml() throws IOException{
		String targetDir = openExportDir();
		if(targetDir != null){
			db.exportHtml(targetDir,currentFile,cssFile);
			checkCSS(targetDir);
		}
	}
	/**
	 * export the current page as XHTML
	 */
	private void exportXHtml() throws IOException{
		String targetDir = openExportDir();
		if(targetDir != null){
			db.exportXHtml(targetDir,currentFile,cssFile);
			checkCSS(targetDir);
		}
	}
	/**
	 * export the current page formatted for the S5 presentation system
	 */
	private void exportS5() throws IOException{
		String targetDir = openExportDir();
		if(targetDir != null){
			db.exportS5(targetDir,currentFile,cssFile);
		}
	}
	/**
	 * export the current page as latex
	 * @param format RadiDatabase.BOOK, ARTICLE or PROSPER for the 
	 * various output formats
	 */
	private void exportLatex(int format) throws IOException{
		String targetDir = openExportDir();
		if(targetDir != null){
			db.exportLatex(targetDir,currentFile,format);
		}
	}
	/**
	 * doSave saves the currently edited file and displays it
	 */
	private void doSave() throws IOException{
		if(!editing){
			return;
		}
		String content = eddi.getText();
		db.saveWikiData(currentFile,content);
		editing = false;
		eddi.setEditable(false);
		loadRadiFile(currentFile);
		loadRadiFileList();
	}
	/**
	 * abort editing, not saving changes
	 * @throws IOException
	 */
	private void abortEdit() throws IOException{
		if(!editing){
			return;
		}
		editing = false;
		eddi.setEditable(false);
		loadRadiFile(currentFile);
		loadRadiFileList();
	}
	/**
	 * switch into editing mode
	 */
	private void doEdit() throws Exception{
		String content;
		
		if(editing){
			return;
		}
		
		content = db.getWikiEditSource(currentFile);
		eddi.setContentType("text/plain");
//		eddi.setFont(eddi.getFont().deriveFont(20));
		eddi.setEditable(true);
		eddi.setText(content);
		eddi.getDocument().addUndoableEditListener(undoHandler);
		resetUndoManager();
		editing = true;
		eddi.setCaretPosition(0);
		eddi.requestFocus();
	}
	/**
	 * edit a file externally
	 * @throws Exception when things go badly
	 */
	private void editExternally() throws Exception{
		String edFile = db.getWikiFilename(currentFile);
		String exEditor = radiProperty.getProperty("editor");
		if(exEditor == null || exEditor.compareTo("undefined") == 0){
			throw new Exception("No external editor configured");
		}
		String command = exEditor + " " + edFile;
		Runtime.getRuntime().exec(command);
	}
	/**
	 * loads the named Radi file or replaces it through empty data when not there.
	 * @param filename The filename to load
	 */
	private void loadRadiFile(String filename) throws IOException{
		String content, anchor;
		int idx;
		
		idx = filename.indexOf("#");
		if(idx < 0){
			currentFile = filename;
			anchor = null;
		} else {
			currentFile = filename.substring(0,idx);
			anchor = filename.substring(idx,filename.length());
		}
			
		String htmlContent = db.getWikiHtml(currentFile);
		StringBuffer htmlText = new StringBuffer();
		htmlText.append(HtmlHeader.makeHtml3Header(currentFile,db.getDatabase(),cssFile));
		htmlText.append(htmlContent);
		htmlText.append("</body></html>\n");
		eddi.setContentType("text/html");
		eddi.setEditable(false);
		eddi.setText(htmlText.toString());
		eddi.requestFocus();
//		System.out.println(htmlText.toString());
		if(anchor != null){
			eddi.scrollToReference(anchor);
		} else {
			eddi.setCaretPosition(0);
		}
		currentUrl = "radi://" + currentFile;
		setStatus(currentFile);
	}
	/**
	 * load the list of all wiki files
	 * @throws IOException
	 */
	private void  loadRadiFileList() throws IOException{
		Vector v = db.getWikiFileList();
	    radiList.setListData(v);
		radiList.revalidate();
	}
	/**
	 * search the Radieschen database ...
	 */
	private void search(){
		JTextField jt = (JTextField)swix.find("searchdata");
		String pattern = jt.getText();
		if(pattern != null && pattern.length() > 0){
			String htmlResult = db.searchDatabase(pattern);
			urlStack.push(currentUrl);
			eddi.setEditorKit(null);
			eddi.setEditable(false);
			eddi.setContentType("text/html");
			eddi.setText(htmlResult);
			eddi.revalidate();
			eddi.requestFocus();
			setStatus("Search Results for " + pattern);
		}
	}
	/**
	 * loadHelp loads help on formatting....
	 */
	private void loadHelp(String helpFile)throws Exception{
		URL ulli = this.getClass().getResource(helpFile);
		if(editing){
			doSave();
		}
		urlStack.push(currentUrl);
		eddi.setEditorKit(null);
		eddi.setEditable(false);
		eddi.setContentType("text/html");
		eddi.setPage(ulli);
		eddi.revalidate();
		eddi.requestFocus();
		setStatus("Internal Help File: " + helpFile);
	}
	/**
	 * load data from an URL into the EditorPane
	 * @param ulli The URL to load
	 * @throws Exception when things go wrong...
	 */
	private void loadURL(URL ulli)throws Exception{
		if(editing){
			doSave();
		}
		eddi.setPage(ulli);
		eddi.setEditable(false);
		eddi.revalidate();
		eddi.requestFocus();
		setStatus(ulli.toString());
	}
	/**
	 * goes one page back in the list of URL's travelled so far
	 */
	private void backCommand() throws Exception{
		if(editing){
			return;
		}
		if(!urlStack.empty()){
			String urltext = (String)urlStack.pop();
			int idx = urltext.indexOf("radi://");
			if(idx < 0){
				loadURL(new URL(urltext));
				currentUrl = urltext;
			} else {
				String RadiFile = urltext.substring(7,urltext.length());
				loadRadiFile(RadiFile);
			}
		}
	}
	/**
	 * if the URL is of type file://, find out if the file can ne found.
	 * If not, try to find in our database directory and augment the 
	 * URL accordingly in order to display. 
	 * @param urltext The input URL
	 * @return The input URL or an augmented URL if our tests 
	 * succeed
	 */
	private String locateFileURL(String urltext){
		String result = urltext;
		if(urltext.startsWith("file://")){
			String file = urltext.substring(7,urltext.length());
			File f = new File(file);
			if(f.exists()){
				return urltext;
			} else {
				file = db.getDatabase() + File.separatorChar + file;
				f = new File(file);
				if(f.exists()){
					return "file://" + file;
				}
			}
		}
		return result;
	}
	/**
	 * load URL's which do not belong to the Radieschen system
	 * @param urltext
	 */
	private void loadNonRadi(String urltext)throws Exception{
	    boolean externalBrowser = false;
	    
	    urltext = locateFileURL(urltext);
		String ex = radiProperty.getProperty("externalbrowser");
		if(ex != null && ex.compareTo("true") == 0){
			externalBrowser = true;
		}
		if(externalBrowser){
			String command = radiProperty.getProperty("browser");
			if(command.compareTo("undefined") == 0) {
				throw new Exception("No external browser defined");
			}
			urlStack.pop();
			command += " " + urltext;
			Runtime.getRuntime().exec(command);
		} else {
			try{
				loadURL(new URL(urltext));
				currentUrl = urltext;
			}catch(Exception edith){
				eddi.setContentType("text/plain");
				StringBuffer stb = new StringBuffer();
				stb.append("EXCEPTION occured loading ");
				stb.append(urltext);
				stb.append('\n');
				stb.append("Exception message: ");
				stb.append(edith.getMessage());
				stb.append('\n');
				eddi.setText(stb.toString());
			}
		}
	}
	/**
	 * listening to link events in EditorPane
	 */
	public void hyperlinkUpdate(HyperlinkEvent e){
		
		if(e.getEventType() ==  HyperlinkEvent.EventType.ACTIVATED){
			String urltext = e.getDescription();
			urlStack.push(currentUrl);
			int idx = urltext.indexOf("radi://");
			try{
				if(idx < 0) {
					loadNonRadi(urltext);
				} else {
					String RadiFile = urltext.substring(7,urltext.length());
					loadRadiFile(RadiFile);
				}
			}catch(Exception eva){
				System.out.println("Exception " + eva.getMessage() + " Occured while accessing "
						+ currentFile);
				eva.printStackTrace();
			}
		}
	}
	/**
	 * valueChanged for listening to ListSelectionEvents
	 */
	public void valueChanged(ListSelectionEvent e){
		if(editing){
			try{
				doSave();
			}catch(Exception edith){}
		}

		String savedRadi = currentUrl;
		String RadiFile = (String)radiList.getSelectedValue();
		if(RadiFile != null){
			/*
			 * prevent multi click madness
			 */
			int idx = currentUrl.indexOf("radi://");
			if(idx >= 0){
				String oldFile = currentUrl.substring(7,currentUrl.length());
				if(oldFile.compareTo(RadiFile) == 0){
					return;
				}
			}
			try{
				loadRadiFile(RadiFile);
				urlStack.push(savedRadi);
			}catch(IOException eva){
				try{
					loadRadiFile(savedRadi);
				}catch(Exception erika){}
			}
		}
	}
	/**
	 * check for the existence of the CSS style file for Radieschen. If it does
	 * not exist: copy it! 
	 * @param dirname The name of the directory to check
	 * @throws IOException
	 */
	private void checkCSS(String dirname) throws IOException {
		File f = new File(dirname + File.separatorChar + "radi.css");
		if(!f.exists()){
			URL ulli = this.getClass().getResource("/radieschen/radi.css");
			FileUtils.copyURLToFile(ulli,f);
		}
	}
	/**
	 * does everything that is necessary to set the Radi to a new directory 
	 * @param dirName The new directory to use
	 */
	private void setCurrentDirectory(String dirName)throws IOException{
		if(editing){
			doSave();
		}
		urlStack.clear();
		db.setDatabase(dirName);
		loadRadiFile("Start.radi");
		loadRadiFileList();
	}
	private void saveRadiDirProperty(){
		String RadiFile = makeRadiFile();
		radiProperty.setProperty("radidir",db.getDatabase());
		try{
			radiProperty.store(new FileOutputStream(RadiFile),"Radi");
		}catch(Exception eva){}
	}
	/**
	 * import a media file into the database directory 
	 * @throws IOException
	 */
	private void importMedia() throws IOException {
		JFileChooser jfc = new JFileChooser();
        String radiDir;
		
		radiDir = db.getDatabase();
		String olddir = System.getProperty("user.home");
		if(olddir == null){
			olddir = "C:\\";
		}
		if(olddir != null){
			jfc.setCurrentDirectory(new File(olddir));
		}
		int retval = jfc.showOpenDialog(swix.getAppFrame());
		if(retval == JFileChooser.APPROVE_OPTION){
			File d = jfc.getSelectedFile();
			File out = new File(radiDir + File.separatorChar + d.getName());
			FileUtils.copyFile(d,out);
		}
	}
	/**
	 * set the status label text
	 * @param url The URL of the item to display
	 */
	private void setStatus(String url){
		JLabel stat = (JLabel)swix.find("status");
		stat.setText("Displaying: " +url);
	}
    /**
     * Resets the undo manager.
     */
    protected void resetUndoManager() {
    	undo.discardAllEdits();
    	undoAction.update();
    	redoAction.update();
    }
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try{
			RadiMain me = new RadiMain();
		} catch(Exception eva){
			System.out.println("Exception: " + eva.getMessage() + " ocurred");
			eva.printStackTrace();
		}
	}
}
