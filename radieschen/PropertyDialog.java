/*
 * Created March, 31, 2006
 *
 * Author: Mark.Koennecke@psi.ch
 * License: GPL
 */  
package radieschen;

import javax.swing.JDialog;
import org.swixml.SwingEngine;
import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.util.Properties;
import java.util.Enumeration;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

public class PropertyDialog extends JDialog {
	private SwingEngine swix;
	private Properties p;
	private JTable tab;
	
	public AbstractAction propertyAction = new AbstractAction(){
		public void actionPerformed(ActionEvent e){
			PropertyDialog te = (PropertyDialog)swix.getClient();
			te.doCommand(e.getActionCommand());
		}
	};

	/**
	 * ann inner class to use as the table model
	 */
	private class PropertyTableModel extends AbstractTableModel {
		private Properties p;
		
		PropertyTableModel(Properties p){
			String key;
			this.p = new Properties();
			Enumeration e = p.propertyNames();
			while(e.hasMoreElements()){
				key = (String)e.nextElement();
				this.p.setProperty(key,p.getProperty(key));
			}
		}
		public int getRowCount(){
			return p.size();
		}
		public int getColumnCount(){
			return 2;
		}
		public Object getValueAt(int row, int col){
			String key = locateKey(row);
			if(key != null){
				if(col == 0){
					return key;
				} else {
					return p.get(key);
				}
			} else {
				return null;
			}
		}
		public void setValueAt(Object val,int row, int col){
			String key = (String)getValueAt(row,0);
			p.setProperty(key,(String)val);
		}
		public boolean isCellEditable(int row, int col){
			if(col == 1){
				return true;
			} else {
				return false;
			}
		}
		public String getColumnName(int col){
			if(col == 0){
				return "Key";
			} else {
				return "Value";
			}
		}
		private String locateKey(int idx){
			int i = 0;
			Enumeration e = p.propertyNames();
			String result = (String)e.nextElement();
			while(i < idx){
				result = (String)e.nextElement();
				i++;
			}
			return result;
		}
	}
	/**
	 * constructor
	 * @param f The papa frame
	 */
	public PropertyDialog(Frame f, Properties p){
		super(f,true);
		this.p = p;
		swix = new SwingEngine(this);
		try{
		    swix.insert("radieschen/property.xml",
					 this);
		}catch(Exception e){
		    System.out.println(e.getMessage());
		    e.printStackTrace();
		}
		tab = (JTable)swix.find("proptable");
		PropertyTableModel tm = new PropertyTableModel(p);
		tab.setModel(tm);
		setSize(getPreferredSize());
	}
	/**
	 * command processing ....
	 * @param command The command to handle
	 */
	private void doCommand(String command){
		int idx = 0, i;
		String key, val; 
		
		if(command.compareTo("Cancel") == 0){
			setVisible(false);
		} else if(command.compareTo("OK") == 0){
			for(i = 0; i < tab.getRowCount(); i++){
				key = (String)tab.getValueAt(i,0);
				val = (String)tab.getValueAt(i,1);
				if(key != null && val != null){
					p.setProperty(key,val);
				}
			}
			setVisible(false);
		}
	}
	/**
	 * center the dialog
	 * @param parent The parent
	 */
	private void centre(Component parent) {
		pack();
		
		Point p = parent.getLocation();
		Dimension d = parent.getSize();
		Dimension s = getSize();
		
		p.translate((d.width - s.width) / 2, (d.height -
				s.height) / 2);
		setLocation(p);
	}
	/**
	 * overloaded setVisble
	 */
	public void setVisible(boolean t){
		centre(swix.getAppFrame());
		super.setVisible(t);
	}	
}
