/**
 * The View Manager translates what's in the data model (GenBankFile) into visible and 
 * processable information on the screen. Most of the really good magic happens in 
 * GenBankFile, but a lot of the grunt stuff happens here. That's a good thing. Really!
 *
 */

/*
 *
 *  GenBankExplorer 
 *  Copyright (C) 2007 Gaurav Vaidya
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *  
 */

package com.ggvaidya.TaxonDNA.GenBankExplorer;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.prefs.*;

import javax.swing.*;		// "Come, thou Tortoise, when?"
import javax.swing.event.*;
import javax.swing.tree.*;

import com.ggvaidya.TaxonDNA.Common.*;
import com.ggvaidya.TaxonDNA.DNA.*;
import com.ggvaidya.TaxonDNA.DNA.formats.*;
import com.ggvaidya.TaxonDNA.UI.*;

public class ViewManager implements TreeModel {
	private GenBankExplorer explorer = 	null;			// the GenBankExplorer object

	// Internal information
	private Vector	treeListeners =		new Vector();

	// UI objects
	private JPanel		panel =		null;			// the 'view' itself
	private JTree		tree =		new JTree(this);	// the tree
	private JTextArea	ta_file =	new JTextArea();	// the text area for file information
	private JTextArea	ta_selected =	new JTextArea();	// the text area for selection information

	// Data objects
	private GenBankFile	genBankFile =	null;			// the currently loaded file

	/**
	 * Constructor. Sets up the UI (on the dialog object, which isn't madeVisible just yet)
	 * and 
	 */
	public ViewManager(GenBankExplorer explorer) {
		// set up the GenBankExplorer
		this.explorer = explorer;
		createUI();
	}

	/**
	 * Create the UI we will use for interacting with the user.
	 */
	public void createUI() {
		panel = new JPanel();
		panel.setLayout(new BorderLayout());

		// okay, boys
		// lessgo!
		// 
		// we'll 'create' UI objects first, then create the intricate set of split panes
		// which lay them out.
		//
		ta_file.setEditable(false);
		ta_selected.setEditable(false);

		// layout time!
		JSplitPane p_textareas = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ta_file, ta_selected);
		p_textareas.setResizeWeight(0.5);	// half way (by default)
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), p_textareas);
		split.setResizeWeight(0);		// bit more right by default

		split.setPreferredSize(new Dimension(400, 500));

		panel.add(split);
	}

	/**
	 * Clear the currently loaded file.
	 */
	public void clear() {
		if(genBankFile != null) {
			genBankFile = null;
		}
	}

	/**
	 * Loads the new file, after clearing the previous one.
	 */
	public void loadFile(File f) {
		clear();

		try {
			genBankFile = new GenBankFile(f, null);
		} catch(IOException e) {
			displayIOExceptionWhileWriting(e, f);
			return;
		} catch(FormatException e) {
			displayException("Error while reading file '" + f + "'", "The file '" + f + "' could be read as a GenBank file. Are you sure it is a properly formatted GenBank file?\nThe following error occured while trying to read this file: " + e.getMessage());
			return;
		} catch(DelayAbortedException e) {
			return;
		}

		// update the entire tree
		fireTreeEvent(new TreeModelEvent(tree, new TreePath(getRoot())));
	}

	/**
	 * Returns the panel to anyone interested.
	 */
	public JPanel getPanel() {
		return panel;
	}

// 	ERROR HANDLING AND DISPLAY CODE
//
	public void displayIOExceptionWhileWriting(IOException e, File f) {
		new MessageBox(
			explorer.getFrame(),
			"Error while writing file '" + f + "'!",
			"The following error was encountered while writing to file " + f + ": " + e.getMessage() + "\n\nPlease ensure that you have the permissions to write to this file, that the disk is not full, and that the file is not write-protected.",
			MessageBox.MB_ERROR).go();
	}

	public void displayIOExceptionWhileReading(IOException e, File f) {
		new MessageBox(
			explorer.getFrame(),
			"Error while reading file '" + f + "'!",
			"The following error was encountered while trying to read from file " + f + ": " + e.getMessage() + "\n\nPlease ensure that the file exists, and that you have the permissions to read from it.",
			MessageBox.MB_ERROR).go();
	}

	public void displayException(String title, String message) {
		new MessageBox(
			explorer.getFrame(),
			title,
			message,
			MessageBox.MB_ERROR).go();
	}
	
// 	DATA MODEL INTERFACE CODE
//

// 	TREE MODEL CODE
//

	public void addTreeModelListener(TreeModelListener l) {
		treeListeners.add(l);
	}
	public void removeTreeModelListener(TreeModelListener l) {
		treeListeners.remove(l);
	}
	/**
	 * Fires a tree event at all listening TreeModelListeners. 
	 * Warning: this will ONLY fire the event as a treeStructureChanged(TreeModelEvent).
	 * If you need a less powerful event to be fired, err ... update this code?
	 */
	private void fireTreeEvent(TreeModelEvent e) {
		Iterator i = treeListeners.iterator();
		while(i.hasNext()) {
			TreeModelListener l = (TreeModelListener) i.next();

			l.treeStructureChanged(e);
		}
	}

	public Object getChild(Object parent, int index) {
		if(parent.equals(getRoot())) {
			return genBankFile.getLocus(index);
		} else 
			return null;
	}
	public int getChildCount(Object parent) {
		if(genBankFile == null)
			return 0;

		if(parent.equals(getRoot()))
			return genBankFile.getLocusCount();

		return 0;
	}
	public int getIndexOfChild(Object parent, Object child) {
		return -1;
	}
	public Object getRoot() {
		if(genBankFile != null)
			return new String(genBankFile.getFile().getAbsolutePath());
		else
			return new String("No file loaded");
	}
	public boolean isLeaf(Object node) {
		if(node.equals(getRoot()))
			return false;
		else
			return true;
	}
	public void valueForPathChanged(TreePath path, Object newValue) {
		// hmmm!	
	}
}