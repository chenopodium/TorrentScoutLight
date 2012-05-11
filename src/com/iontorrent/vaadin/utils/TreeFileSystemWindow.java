/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.threads.Task;
import com.iontorrent.threads.TaskListener;
import com.iontorrent.utils.ProgressListener;
import com.vaadin.data.Item;
import com.vaadin.data.Property.ConversionException;
import com.vaadin.data.Property.ReadOnlyException;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.terminal.Resource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.Window;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class TreeFileSystemWindow implements ExpandListener, ProgressListener {

    private String lastError;
    private Exception lastException;
    private Panel explorerPanel;
    private Tree tree;
    private File selected;
    private Label lselected;
    private TextField text;
    private String path;
    ArrayList<Object> pathitems;
    ArrayList<File> parents;
    ProgressIndicator indicator;
    WorkThread t;
    public TreeFileSystemWindow() {
    }

    public void open(final Window parent, final TextField tf) {
        this.text = tf;
        path = "" + tf.getValue();
        if (path == null) {
            path = "";
        }
        File root = new File(path);
        File actualfile = new File(path);
      parents = new ArrayList<File>();
       // parents.add(actualfile);
        path = root.getAbsolutePath();
        if (path == null) {
            path = "/";
        }

        p("Got path: " + path);
        while (root != null && root.getParentFile() != null) {
            root = root.getParentFile();
            parents.add(root);
        }


        final Window w = new Window("Browse");
        w.setPositionX(100);
        w.setPositionY(100);
        w.setHeight("600px");
        w.setWidth("1000px");
        
        indicator = new ProgressIndicator(new Float(0.0));
		indicator.setHeight("40px");
		indicator.setDescription("Getting paths...");
		//indicator.setCaption("Processing folders progress");
		indicator.setPollingInterval(1000);
	
        // Component with an icon from a custom theme        
        // Main window contains heading and panel        
        // configure file structure panel
        lselected = new Label("Selected File: " + path);
        HorizontalLayout h = new HorizontalLayout();
        w.addComponent(h);
        h.addComponent(lselected);
        h.addComponent(indicator);
        explorerPanel = new Panel();
        w.addComponent(explorerPanel);
        Button ok = new Button("Ok");
        ok.setIcon(new ThemeResource("img/ok.png"));
        w.addComponent(ok);
    	
        // Handle button clicks
        ok.addListener(new Button.ClickListener() {

            public void buttonClick(Button.ClickEvent event) {
                if (selected != null) {
                    text.setValue(selected);
                }
                parent.removeWindow(w);
                w.detach();
            }
        });
        tree = new Tree();
        tree.setImmediate(true);
        explorerPanel.addComponent(tree);
        explorerPanel.setHeight("450px");
        explorerPanel.setWidth("950px");
        tree.setSelectable(true);
        tree.addContainerProperty("caption", String.class, null);
        tree.addContainerProperty("icon", Resource.class, null);
        tree.addContainerProperty("file", File.class, null);
        tree.setItemIconPropertyId("icon");

        //tree.seti
        tree.setItemCaptionPropertyId("caption");
        tree.setItemCaptionMode(AbstractSelect.ITEM_CAPTION_MODE_PROPERTY);
        // "this" handles tree's expand event



        Object oparent = null;
        int nr = parents.size();
        for (int i = 0; i < nr; i++) {
            File f = parents.get(nr - i - 1);
            p("Adding file: "+f);
            oparent = addFile(f, oparent);
            
        }
        
        t = new WorkThread(new TaskListener() {

			@Override
			public void taskDone(Task task) {
				indicator.setValue(1.0);
				indicator.setVisible(false);
				
			}
        	
        }, this);
        t.execute();
        
        File root1 = new File("/");
        if (!root1.equals(root)) {
            addFile(root1, null);
            p("adding root1: " + root1);
            //if (populateNode(root1.getAbsolutePath(), root1.getAbsolutePath(), 0);
        }
       // tree.expandItem(actualfile);
        // tree.expandItem(root1.getAbsolutePath());
        //  tree.expandItem(root.getAbsolutePath());
        // now expand path

        tree.addListener(this);
        tree.addListener(new ItemClickListener() {

            public void itemClick(ItemClickEvent event) {
                p("Got item: " + event.getItem());
                Item it = event.getItem();
                if (it == null) {
                    return;
                }
                File sel = (File) it.getItemProperty("file").getValue();
                setSelected(sel);
            }
        });

        parent.addWindow(w);
    }

 // Another thread to do some work
 	class WorkThread extends Task {
 		boolean has;
 		public WorkThread(TaskListener list, ProgressListener prog) {
 			super(list); 
 			super.setProglistener(prog);
 		}		
 		@Override
 		public boolean isSuccess() {
 			// TODO Auto-generated method stub
 			return has;
 		}
 		@Override
 		protected Void doInBackground() {
 			 int nr = parents.size();
 			 double inc = 100.0/nr;
 			  for (int i = 0; i < nr; i++) {
 		            File f = parents.get(nr - i - 1);
 		            p("Populating node file: "+f);
 		           if (f.toString().length()>2) populateNode(f.getAbsolutePath(), f.getAbsolutePath(), 1);
 		            this.setProgress((int)(i*inc));
 		            indicator.setDescription("processing "+f);
 		           // tree.expandItem(f.getAbsolutePath());
 		        }
 			return null;			
 			
 		}
 		
 	}
 	
	public void setProgressValue(int p) {
		if (indicator != null) indicator.setValue(((double) p / 100.0d));
		// progress.setValue("Creating composite image: " + p + "%");
	}

	public void setMessage(String msg) {

	}

	public void stop() {}
    /**
     * Handle tree expand event, populate expanded node's childs with new files
     * and directories.
     */
    public void nodeExpand(ExpandEvent event) {
        final Item i = tree.getItem(event.getItemId());
        if (i == null) {
            return;
        }
        if (!tree.hasChildren(i)) {
            // populate tree's node which was expanded
            populateNode(event.getItemId().toString(), event.getItemId(), 0);
        }
    }

    public Object addFile(File f, Object parent) throws UnsupportedOperationException, ReadOnlyException, ConversionException {
        //  try {
        // add new item (String) to tree
        if (f == null) {
            return null;
        }
        String curpath = f.getAbsolutePath();
        if (curpath == null || curpath.length() < 1) {
            curpath = "/";
        }
        Item it = null;
        String returnitem = null;
        if (curpath != null) {
            it = tree.addItem(curpath);
            if (it == null) {
                return null;
            }
            returnitem = curpath;
            //    
            String name = f.getName();
            if (name == null || name.length() < 1) {
                name = curpath;
            }
            it.getItemProperty("caption").setValue(name);
            it.getItemProperty("file").setValue(f);
            // set parent if this item has one
            if (parent != null) {
                tree.setParent(curpath, parent);
            }
            // check if item is a directory and read access exists
            if (f.isDirectory() && f.canRead()) {
                // yes, childrens therefore exists
                tree.setChildrenAllowed(curpath, true);
                it.getItemProperty("icon").setValue(new ThemeResource("../torrentscout/img/folder.png"));
               
            } else {
                // no, childrens therefore do not exists
                tree.setChildrenAllowed(curpath, false);
                if (f.isDirectory()) {
                    it.getItemProperty("icon").setValue(new ThemeResource("../torrentscout/img/folder.png"));
                } else {
                    it.getItemProperty("icon").setValue(new ThemeResource("../torrentscout/img/document.png"));
                }
                
            }
        }
        return returnitem;
    }

    /**
     * Populates files to tree as items. In this example items are of String
     * type that consist of file path. New items are added to tree and item's
     * parent and children properties are updated.
     * 
     * @param file
     *            path which contents are added to tree
     * @param parent
     *            for added nodes, if null then new nodes are added to root node
     */
    private void populateNode(String file, Object parent, int curlevel) {
        final File subdir = new File(file);
        final File[] files = subdir.listFiles();

        if (files == null) {
            return;
        }
        Arrays.sort(files, new FileComparator());
        for (int x = 0; x < files.length; x++) {
            Object itid = addFile(files[x], parent);
            if (itid != null) {
                //        pathitems.add(itid);
                if (curlevel > 0) {
                    populateNode(files[x].toString(), itid, curlevel - 1);
                }
            }
        }

    }

    private static void err(String msg, Exception ex) {
        Logger.getLogger(TreeFileSystemWindow.class.getName()).log(Level.SEVERE, msg, ex);
    }

    private static void err(String msg) {
        Logger.getLogger(TreeFileSystemWindow.class.getName()).log(Level.SEVERE, msg);
    }

    private static void warn(String msg) {
        Logger.getLogger(TreeFileSystemWindow.class.getName()).log(Level.WARNING, msg);
    }

    private static void p(String msg) {
        //system.out.println("TreeFileSystemWindow: " + msg);
        Logger.getLogger(TreeFileSystemWindow.class.getName()).log(Level.INFO, msg);
    }

    /**
     * @return the selected
     */
    public File getSelected() {
        return selected;
    }

    /**
     * @param selected the selected to set
     */
    public void setSelected(File selected) {
        this.selected = selected;
        lselected.setValue("Selected: " + selected);
        text.setValue(selected.toString());
    }
}
