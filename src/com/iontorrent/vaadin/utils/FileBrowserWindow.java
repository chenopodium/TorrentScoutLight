/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.utils;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.utils.ErrorHandler;
import com.iontorrent.utils.io.FileTools;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.terminal.Resource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.Notification;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
@SuppressWarnings("serial")
public class FileBrowserWindow extends WindowOpener {

	private static final int MAX_FILES = 300;
	Recipient recipient;

	static final String[] IGNORE = { "init.d", "etc", "lib","opt", "sys", "bin", "boot", "lib64", "windows", "Windows", "system", "system32", "System32", "Boot",
		"lock", "log", "backups", "spool"};
	
	public interface Recipient {
		public void fileSelected(File file);

		public boolean allowInList(File file, boolean toSave);
	}

	String[] exts;
	boolean onlyDir;
	Table table;
	HorizontalLayout hor;
	VerticalLayout v;
	HorizontalLayout bottom;
	TextField txt;
	
	DecimalFormat dec = new DecimalFormat("#.#");
	File justcreated;
	File rootfile;
	File originalfile;
	File dir;
	String title;
	String description;
	Button create;
	public static final int SAVE = 0;
	public static final int OPEN = 1;

	int mode = SAVE;

	public FileBrowserWindow(String title, String description, Recipient recipien, Window main, File rootfile, String extensions) {
		this(title, description,recipien, OPEN, main, rootfile, extensions, 100, 100);

	}

	public FileBrowserWindow(String title,String description, Recipient recipient, int mode, Window main, File rootfile, String extensions) {
		this(title, description,recipient, mode, main, rootfile, extensions, 100, 100);

	}

	public FileBrowserWindow(String title,String description, Recipient recipient, int mode, Window main, File rootfile, String extensions, int x, int y) {
		super("File dialog", main, "File browser", x, y, 800, 600);
		this.recipient = recipient;
		this.title = title;
		this.description = description;
		if (extensions != null && extensions.length()>0) {
			exts = extensions.split(",");
			if (exts != null) {
				for (int i = 0; i < exts.length; i++) {
					String e = exts[i].trim();
					if (e.equals("dir")) {
						onlyDir = true;						
					}
					else {
						if (!e.startsWith(".")) e = "."+e;
					}
					exts[i] = e;
				}
			}
		}
		this.rootfile = rootfile;
		this.originalfile = rootfile;
		this.mode = mode;
	}

	@Override
	public void windowOpened(Window mywindow) {
		p("Open file browser window on " + rootfile);

		if (rootfile == null) rootfile = new File(".");
		mywindow.setCaption(title);
		if (description == null) {
			
			description = "Pick a ";
			if (onlyFolders()) description += " folder";
			else description +=" file";
			description+= " to ";
			if (mode == SAVE) description += "save";
			else description += "open";
			if (exts != null && !onlyFolders()) description += " with extension "+Arrays.toString(exts);
		}
		Label lbl = new Label(description);
		mywindow.addComponent(lbl);
		
		hor = new HorizontalLayout();
		mywindow.addComponent(hor);
		
		txt = new TextField();
		txt.setValue(rootfile.toString());
		txt.setWidth("200px");
		txt.setDescription("The current file name (you can enter it if you like)");
		txt.setImmediate(true);

		hor.addComponent(txt);

		
		bottom = new HorizontalLayout();

		Button openorsave = new Button("Open");
		if (mode == SAVE) {
			openorsave.setCaption("Save");
			openorsave.setDescription("Click to use this file to save");
		}
		else openorsave.setDescription("Click to open this file");
		bottom.addComponent(openorsave);
		openorsave.setImmediate(true);
		openorsave.addListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				// TODO Auto-generated method stub
				if (txt.getValue() != null) {
					final File f = new File(""+txt.getValue());
					// check file extension
					boolean ok = true;
					if (f.isDirectory()) {
						if (onlyFolders()) ok = true;
						else {
							showMessage("File", "You have to pick a file");
							return;			
						}									
					}
					else if (onlyFolders()) {
						ok = false;
						showMessage("Folders", "You have to pick a folder");
						return;
					}
					if (ok && !f.isDirectory() && !isFileOk(f)) {
						String s="Pick a file that is ";
						if (mode == SAVE) s += "writable";
						else s +="readable";					
						if (exts != null ) {
							s += " and has one of the extensions "+Arrays.toString(exts);
						}
						showMessage("Invalid file", s);
						ok = false;
					}
					if (ok) {
						p("Ok");
						if (mode == SAVE && (f.exists() && (justcreated == null || !f.equals(justcreated)))) {
							
							OkDialog okdialog = new OkDialog(appwindow, "Overwrite?", "The file "+f+" already exists. Overwrite it?", new OkDialog.Recipient() {
								@Override
								public void gotInput(String name) {
									if (!name.equalsIgnoreCase("OK")) return;
									recipient.fileSelected(f);
								}
							});
						}
						else recipient.fileSelected(f);
						close();
					}
					
				}
				
			}
		});

		Button cancel = new Button("Cancel");
		cancel.setDescription("Cancel");
		bottom.addComponent(cancel);
		cancel.setImmediate(true);
		cancel.addListener(new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				// TODO Auto-generated method stub
				recipient.fileSelected(null);
				close();
			}
		});

		

		if (!onlyFolders() && mode == SAVE) {
			create = new Button("New file");
			create.setIcon(new ThemeResource("img/document-new-3.png"));
			create.setDescription("Create a new empty file");
			create.addListener(new Button.ClickListener() {
				@Override
				public void buttonClick(ClickEvent event) {
					dir = rootfile;
					if (!rootfile.isDirectory()) dir = rootfile.getParentFile();
					if (!dir.canWrite()) {
						showMessage("No write permission", "Can't create a new file in this folder");
						return;
					}
					String name = "mynewfile";
					if (exts != null && !onlyFolders()) {
						String ext = exts[0].trim();						
						name = name +ext;					
					}
					InputDialog input = new InputDialog(appwindow, "Name of file new file: ", new InputDialog.Recipient() {
						@Override
						public void gotInput(String name) {
							if (name == null || name.length() < 1) {
								return;
							}
							if (name.indexOf("/")>-1 || name.indexOf("\\")>-1) {
								showLongMessage("Just a name", "Just enter a name (no path)");
								return;
							}
							String mydir = FileTools.addSlashOrBackslash(dir);
							File f =new File(mydir+name);
							if (!isExtensionOk(f)) {
								showLongMessage("Extension?", "The extension doesn't look right of "+f);
								return;
							}
							boolean ok = false;
							try {
								ok = f.createNewFile();
								
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (!ok) {
								showMessage("Error", "Couldn't create the file "+f);
							}
							else {
								rootfile = f;
								justcreated = f;
								txt.setValue(f.getAbsolutePath());
								addTable();
							}
						}
					}, name);
				}
	
			});
			hor.addComponent(create);
		}
		
		Button re = new Button();
		re.setDescription("Refresh table");
		re.setIcon(new ThemeResource("img/view-refresh-3.png"));
		hor.addComponent(re);
		re.addListener(new Button.ClickListener() {
			public void buttonClick(Button.ClickEvent event) {
				if (txt.getValue() != null) {
					rootfile = new File(""+txt.getValue());
				}
				addTable();
			}
		});
		
		Button help = new Button();
		help.setDescription("Click me to get information on this window");
		help.setIcon(new ThemeResource("img/help-hint.png"));
		hor.addComponent(help);
		help.addListener(new Button.ClickListener() {
			public void buttonClick(Button.ClickEvent event) {
				showLongMessage("Help", getHelpMessage());
			}
		});
//		final Button export = new Button();
//		export.setIcon(new ThemeResource("img/export.png"));
//		export.setDescription("Store table in .csv file");
//		export.addListener(new Button.ClickListener() {
//			@Override
//			public void buttonClick(ClickEvent event) {
//				doExportAction();
//			}
//
//		});
//		hor.addComponent(export);
		
		addTable();

	}

	private void addTable() {
		String w = "750px";
		String h = "430px";
		if (table != null) {
			mywindow.removeComponent(table);
			mywindow.removeComponent(bottom);
			w = table.getWidth() + "px";
			h = table.getHeight() + "px";
		}

		table = new Table();

		table.setWidth(w);
		table.setHeight(h);

		table.addGeneratedColumn(" ", imageColumnGenerator);
		table.addGeneratedColumn("Name", nameColumnGenerator);
		//table.addContainerProperty("Name", String.class, null);
		// table.setItemIconPropertyId("Icon");

		table.addContainerProperty("Type", String.class, null);
		table.addContainerProperty("Size", String.class, null);
		table.addContainerProperty("Modified", Date.class, null);
		table.addContainerProperty("File", File.class, null);
		table.addContainerProperty("Folder", Boolean.class, null);		
		// table.addContainerProperty("Image", Resource.class, null);		
		// table.setItemIconPropertyId("Image");
		table.addStyleName("welltable");

		p("Got root folder:" + rootfile);
		File files[] = null;
		int row = 1;
		if (rootfile != null) {
			dir = rootfile;
			if (dir.isFile() || !dir.exists()) dir = dir.getParentFile();
			if (this.isFileOkToList(dir)) {
				files = dir.listFiles();			
				if (dir.getParentFile() != null) {
					p("Adding parent: "+dir.getParentFile());
					row = addRow(row, dir.getParentFile(), "..");
				}
				else p(dir+" has no parent file");
				if (!dir.canWrite() && create != null) {
					create.setEnabled(false);
					create.setDescription("You have no write permission in the folder "+dir);
				}
				else if (create != null){
					create.setEnabled(true);
					create.setDescription("Click to create a new empty file to save");
				}
			} else if (create != null){
				create.setEnabled(false);
				create.setDescription("You have no write permission in the folder "+dir);
			}
		}
		if (files != null && files.length > 0) {
			// sort files by directories first and then by name
			
			if (files.length> MAX_FILES) {
				p("Found "+files.length+" files - sorting by date");
				showLongMessage("Too many files", "Too many files to show, listing the newest "+MAX_FILES+" files.<br>You can enter a folder in the text field");
				Arrays.sort(files, new FileDateComparator());
				files = Arrays.copyOfRange(files, 0,MAX_FILES );
			}
			Arrays.sort(files, new FileComparator());
			for (int f = 0; f < files.length && row < MAX_FILES; f++) {
			//	p("Trying to add file " + files[f]);
				row = addRow(row, files[f], null);
			}
			
			

		} else {
			showMessage("No files", "I found no files in " + rootfile);
		}
		table.setColumnWidth(" ", 16);
		table.setColumnWidth("Folder",0);
		table.setColumnWidth("File", 0);
		table.setColumnCollapsingAllowed(true);
		table.setColumnCollapsed("File", true);
		table.setColumnCollapsed("Folder", true);
		table.setSortAscending(false);
		table.setSortContainerPropertyId("Folder");
		// Allow selecting items from the table.
		table.setSelectable(true);

		// Send changes in selection immediately to server.
		table.setImmediate(true);

		// Handle selection change.
		table.addListener(new Property.ValueChangeListener() {

			public void valueChange(ValueChangeEvent event) {
				Object id = table.getValue();
				if (id == null) {
					return;
				}
				Item obj = table.getItem(id);
				File f = ((File) obj.getItemProperty("File").getValue());
				rootfile = f;
				txt.setValue(""+f);
				if (f.isDirectory()) addTable();
				// app.reopenAlign();
			}
		});
		mywindow.addComponent(table);
		 mywindow.addComponent(bottom);
	}
	private class FileSorter implements Comparator {

		@Override
		public int compare(Object o1, Object o2) {
			File f1 = (File)o1;
			File f2 = (File)o2;
			return (f1.isDirectory()+f1.getName()).compareTo(f2.isDirectory()+f2.getName());
		}
		
	}
	private final ColumnGenerator imageColumnGenerator = new ColumnGenerator() {
		public Component generateCell(final Table source, final Object itemId, final Object columnId) {

			final Item item = source.getItem(itemId);
			File f = (File) item.getItemProperty("File").getValue();
			Resource r = null;
			if (f.isDirectory()) r = new ThemeResource("img/folder.png");
			else {
				if (isFileOk(f) && exts!=null) {
					r = new ThemeResource("img/ok.png");
				}
				else r = new ThemeResource("img/document.png");
			}
			Embedded embedded = new Embedded("", r);
			return embedded;
		}
		
	};
	public boolean isFileOk(File f) {
		if (!recipient.allowInList(f, mode == SAVE)) return false;
		if (!isExtensionOk(f)) return false;
		return true;
	}

	private boolean isExtensionOk(File f) {
		if (exts != null && exts.length>0) {			
			String name = f.getName();
			int dot = name.lastIndexOf(".");
			if (dot > -1) {
				String ext = name.substring(dot);
				boolean ok = false;
				for (String e: exts) {
					if (e.equalsIgnoreCase(ext)) {
						ok = true;
						break;
					}
				}
				return ok;
			}
			else return false;
		}
		return true;
	}
	public boolean onlyFolders() {
		return onlyDir;
	}
	private final ColumnGenerator nameColumnGenerator = new ColumnGenerator() {
		public Component generateCell(final Table source, final Object itemId, final Object columnId) {

			final Item item = source.getItem(itemId);
			File f = (File) item.getItemProperty("File").getValue();
			String name = f.getName();		
			if (f.isDirectory()) {
				if (dir.getParentFile() != null && dir.getParentFile().equals(f)) {
					name = "..";
				}
			}
			else if (!isFileOk(f)) {
				name = "<font color='999999'>"+f.getName()+"</font>";									
			}
			Label lbl = new Label(name, Label.CONTENT_XHTML);
			return lbl;
		}
	};

	private void doExportAction() {
		// copy data to excel
		if (table == null || table.getItemIds() == null) {
			showMessage("No data", "Found no data in table to export");
			return;
		}
		DataUtils.export(table, appwindow);

	}

	public String getHelpMessage() {
		String msg = "<ul>";
		msg += "<li>Click on the table header to sort the files</li>";
		msg += "<li>Click on .. to go to the parent folder</li>";
		msg += "<li>Click on a file to select it</li>";
		if (mode == OPEN) {
			msg += "<li>Select Open to pick the file and close this dialog</li>";
		}else msg += "<li>Select Save to save to this file and close this dialog</li>";
		
		msg += "<li>Select Cancel to abort</li>";
		
		msg += "</ul>";
		return msg;
	}

	private int addRow(int row, File f, String display) {
		if (!isFileOkToList(f)) return row;
		Date d = new Date(f.lastModified());
		String type = "";
		String name = f.getName();
		if (display != null) name = display;
		long len = f.length() / 1000;
		String unit = " kB";
		if (len > 1000) {
			len = len / 1000;
			unit = " MB";
		}
		if (len > 1000) {
			len = len / 1000;
			unit = " GB";
		}

		if (f.isDirectory()) type = "folder";
		else {
			type = "?";
			int dot = name.indexOf(".");
			if (dot > -1) {
				type = name.substring(dot + 1);
			}
		}

		Object[] rowdata = new Object[5];
		int i = 0;
	//	rowdata[i++] = new String(name);
		rowdata[i++] = new String(type);
		rowdata[i++] = new String(len + unit);
		rowdata[i++] = d;
		rowdata[i++] = f;
		rowdata[i++] = new Boolean(f.isDirectory());
		//rowdata[i++] = r;
	//	p("Adding file " + Arrays.toString(rowdata));
		table.addItem(rowdata, new Integer(row));

		row++;
		return row;
	}

	private boolean isFileOkToList(File f) {
		if (f == null) return false;
		if (f.isHidden() || !f.canRead()) {
			//p("Hidden or !readable: "+f.isHidden()+"/"+f.canRead());
			return false;
		}
		
		if (mode == SAVE && !f.canWrite()) {
			//p("!writable");
			return false;
		}
	
		if (!recipient.allowInList(f, mode == SAVE)) {
			//p("File "+f+" not allowed");
			return false;
		}
		String dir = f.getAbsolutePath();
		for (String bad: IGNORE) {
			//p("Comparing "+bad+" with "+ dir);
			if (dir.indexOf("/"+bad+"/")>-1 || dir.startsWith(bad+"/") || dir.endsWith("/"+bad)) return false;
			if (dir.indexOf("\\"+bad+"\\")>-1 || dir.startsWith(bad+"\\") || dir.endsWith("\\"+bad)) return false;			
		}
		return true;
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(FileBrowserWindow.class.getName()).log(Level.SEVERE, msg + ErrorHandler.getString(ex));
	}

	private static void err(String msg) {
		Logger.getLogger(FileBrowserWindow.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(FileBrowserWindow.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		//system.out.println("FileBrowserWindow: " + msg);
		Logger.getLogger(FileBrowserWindow.class.getName()).log(Level.INFO, msg);
	}

	public File getRootfile() {
		return rootfile;
	}

	public void setRootfile(File rootfile) {
		this.rootfile = rootfile;
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public void showLongMessage(String title, String msg) {
		p(msg);
		Notification not = new Notification(title, "<br>" + msg, Window.Notification.TYPE_HUMANIZED_MESSAGE);
		not.setDelayMsec(5000);
		appwindow.showNotification(not);
	}

	public void showTopMessage(String title, String msg) {
		showTopMessage(title, msg, false);
	}

	public void showTopMessage(String title, String msg, boolean islong) {
		p(msg);
		Notification n = new Notification(title, "<br>" + msg, Window.Notification.TYPE_HUMANIZED_MESSAGE);
		n.setPosition(Window.Notification.POSITION_TOP_LEFT);

		appwindow.showNotification(n);
	}

	public void showTopRightMessage(String title, String msg) {
		p(msg);
		Notification n = new Notification(title, "<br>" + msg, Window.Notification.TYPE_HUMANIZED_MESSAGE);
		n.setPosition(Window.Notification.POSITION_TOP_RIGHT);
		appwindow.showNotification(n);
	}

	public void showError(WindowOpener win, String msg) {
		p(msg);
		appwindow.showNotification(win.getName(), "<br>" + msg, Window.Notification.TYPE_ERROR_MESSAGE);
	}

	public void showMessage(WindowOpener win, String msg) {
		p(msg);
		appwindow.showNotification(win.getName(), "<br>" + msg, Window.Notification.TYPE_HUMANIZED_MESSAGE);
	}

	public void showMessage(String title, String msg) {
		p(msg);
		
		appwindow.showNotification(title, "<br>" + msg, Window.Notification.TYPE_HUMANIZED_MESSAGE);
	}
}
