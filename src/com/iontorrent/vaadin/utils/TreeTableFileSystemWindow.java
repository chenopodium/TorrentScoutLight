/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.vaadin.utils.FileContainer.FileItem;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class TreeTableFileSystemWindow {

    private String lastError;
    private Exception lastException;
    //   private Panel explorerPanel;
    //  private Tree tree;
    private File selected;
    private Label lselected;
    private TextField text;
    private String path;
    ArrayList<Object> pathitems;

    public TreeTableFileSystemWindow() {
    }

//    public Object addFile(File f, Object parent) throws UnsupportedOperationException, ReadOnlyException, ConversionException {
//        //  try {
//        // add new item (String) to tree
//        if (f == null) {
//            return null;
//        }
//        String curpath = f.getAbsolutePath();
//        if (curpath == null || curpath.length() < 1) {
//            curpath = "/";
//        }
//        Item it = null;
//        String returnitem = null;
//        if (curpath != null) {
//            it = tree.addItem(curpath);
//            if (it == null) {
//                return null;
//            }
//            //    
//            String name = f.getName();
//            if (name == null || name.length() < 1) {
//                name = curpath;
//            }
//            it.getItemProperty("caption").setValue(name);
//            it.getItemProperty("file").setValue(f);
//            // set parent if this item has one
//            if (parent != null) {
//                tree.setParent(curpath, parent);
//            }
//            // check if item is a directory and read access exists
//            if (f.isDirectory() && f.canRead()) {
//                // yes, childrens therefore exists
//                tree.setChildrenAllowed(curpath, true);
//                it.getItemProperty("icon").setValue(new ThemeResource("../torrentscout/img/folder.png"));
//                if (path.contains(curpath)) {
//                    returnitem = curpath;
//                }
//            } else {
//                // no, childrens therefore do not exists
//                tree.setChildrenAllowed(curpath, false);
//                if (f.isDirectory()) {
//                    it.getItemProperty("icon").setValue(new ThemeResource("../torrentscout/img/folder.png"));
//                } else {
//                    it.getItemProperty("icon").setValue(new ThemeResource("../torrentscout/img/document.png"));
//                }
//                if (f.getAbsolutePath().equals(path)) {
//                    returnitem = curpath;
//                }
//            }
//        }
//        return returnitem;
//    }
    public void open(final Window parent, final TextField origtextfield) {
        this.text = origtextfield;
        path = "" + origtextfield.getValue();
        if (path == null) {
            path = "";
        }
        File root = new File(path);
        File actualfile = new File(path);
        File parentfile = actualfile;
        File pparentfile = null;
        if (actualfile != null && actualfile.getParentFile() != null) {
            parentfile = actualfile.getParentFile();
        }
        if (parentfile != null && parentfile.getParentFile() != null) {
            pparentfile = parentfile.getParentFile();
        }
        path = root.getAbsolutePath();
        if (path == null) {
            path = "/";
        }

        p("Got path: " + path);
        while (root != null && root.getParentFile() != null) {
            root = root.getParentFile();
        }

        final Window w = new Window("Browse");
        w.setPositionX(100);
        w.setPositionY(100);
        w.setHeight("600px");
        w.setWidth("800px");
        // Component with an icon from a custom theme        
        // Main window contains heading and panel        
        // configure file structure panel
        lselected = new Label("Selected File: " + origtextfield.getValue());
        VerticalLayout v = new VerticalLayout();
        //    HorizontalLayout h = new HorizontalLayout();
        w.addComponent(v);
        v.addComponent(lselected);
        //    final TextField t = new TextField();
        //     t.setValue("/");       
        //      v.addComponent(h);
        //     h.addComponent(new Label("Add root:"));
        //     h.addComponent(t);

        // explorerPanel = new Panel();
        //  w.addComponent(explorerPanel);
        Button ok = new Button("Ok");
        ok.setIcon(new ThemeResource("img/ok.png"));


        // Handle button clicks
        ok.addListener(new Button.ClickListener() {

            public void buttonClick(Button.ClickEvent event) {
                if (selected != null) {
                    origtextfield.setValue(selected);
                }
                parent.removeWindow(w);
                w.detach();
            }
        });

        p("Creating file system container with parent " + parentfile);
        FileContainer container = new FileContainer(actualfile, 2);
        if (pparentfile != null) {
            p("adding pparentfile "+pparentfile);
            container.addRoot(pparentfile);
        }
//        if (parentfile != null) {
//            p("adding parentfile "+parentfile);
//            container.addRoot(parentfile);
//        }
        // container.

        final TreeTable treetable = new TreeTable();
        treetable.setWidth("730px");
        treetable.setHeight("450px");
        treetable.setContainerDataSource(container);
        treetable.setItemIconPropertyId("Icon");
        treetable.setVisibleColumns(new Object[]{"Name", "Size",
                    "Last Modified"});

        treetable.addListener(new ExpandListener() {

            public void nodeExpand(ExpandEvent event) {
                p("Got expand event: " + event.getItemId() + ", " + event.getItemId().getClass().getName());
                File f = (File) event.getItemId();
                if (f == null) {
                    return;
                }
//                FileContainer container = new FileContainer(f, 2);
//                 p("created container");
//                treetable.setContainerDataSource(container);
//                treetable.setItemIconPropertyId("Icon");
//                treetable.setVisibleColumns(new Object[]{"Name", "Size",
//                            "Last Modified"});
//                p("set container");
                origtextfield.setValue(f.toString());
                parent.removeWindow(w);
                w.detach();
                open(parent, text);
                //treetable.refreshCurrentPage();
            }
        });
        //    t.setImmediate(true);

//        t.addListener(new ValueChangeListener() {
//
//            public void valueChange(ValueChangeEvent event) {
//                p("Text VALUE changed in textfield: " + t.getValue());
//                parent.removeWindow(w);
//                w.detach();
//                open(parent, t);
//            }
//            
//        });
//        t.addListener(new TextChangeListener() {
//
//            public void textChange(TextChangeEvent event) {
//                p("Text TEXTchanged in textfield: " + t.getValue());
////                parent.removeWindow(w);
////                w.detach();
////                open(parent, t);
//                
//                container.addRoot(new File((String)t.getValue()));
//                treetable.setContainerDataSource(container);
//                treetable.requestRepaint();
//                //treetable.repaint
//            }
//        });
        treetable.addListener(new ItemClickListener() {

            public void itemClick(ItemClickEvent event) {
                p("Got item: " + event.getItem());
                FileItem it = (FileItem) event.getItem();
                if (it == null) {
                    return;
                }
                File sel = (File) it.getFile();
                p("Selected file: " + sel);
                setSelected(sel);
            }
        });
        w.addComponent(treetable);
        w.addComponent(ok);
        parent.addWindow(w);
    }

//    public void open1(final Window parent, final TextField tf) {
//        this.text = tf;
//        path = "" + tf.getValue();
//        if (path == null) {
//            path = "";
//        }
//        File root = new File(path);
//        path = root.getAbsolutePath();
//        if (path == null) {
//            path = "/";
//        }
//
//        p("Got path: " + path);
//        while (root != null && root.getParentFile() != null) {
//            root = root.getParentFile();
//        }
//
//        final Window w = new Window("Browse");
//        w.setPositionX(100);
//        w.setPositionY(100);
//        w.setHeight("600px");
//        w.setWidth("800px");
//        // Component with an icon from a custom theme        
//        // Main window contains heading and panel        
//        // configure file structure panel
//        lselected = new Label("Selected File: " + path);
//        HorizontalLayout h = new HorizontalLayout();
//        w.addComponent(h);
//        h.addComponent(lselected);
//        explorerPanel = new Panel();
//        w.addComponent(explorerPanel);
//        Button ok = new Button("Ok");
//        ok.setIcon(new ThemeResource("img/ok.png"));
//        w.addComponent(ok);
//
//        // Handle button clicks
//        ok.addListener(new Button.ClickListener() {
//
//            public void buttonClick(Button.ClickEvent event) {
//                if (selected != null) {
//                    text.setValue(selected);
//                }
//                parent.removeWindow(w);
//                w.detach();
//            }
//        });
//        tree = new Tree();
//        tree.setImmediate(true);
//        explorerPanel.addComponent(tree);
//        explorerPanel.setHeight("450px");
//        explorerPanel.setWidth("1000px");
//        tree.setSelectable(true);
//        tree.addContainerProperty("caption", String.class, null);
//        tree.addContainerProperty("icon", Resource.class, null);
//        tree.addContainerProperty("file", File.class, null);
//        tree.setItemIconPropertyId("icon");
//
//        //tree.seti
//        tree.setItemCaptionPropertyId("caption");
//        tree.setItemCaptionMode(AbstractSelect.ITEM_CAPTION_MODE_PROPERTY);
//        // "this" handles tree's expand event
//
//        // Get sample directory
//
//        File root1 = new File("/");
//        addFile(root1, null);
//        if (!root1.equals(root)) {
//            addFile(root1, null);
//        }
//        pathitems = new ArrayList<Object>();
//
//
//        populateNode(root.getAbsolutePath(), root.getAbsolutePath());
//        // tree.expandItem(f.getAbsolutePath());
//        tree.expandItem(path);
//        tree.expandItem(root1.getAbsolutePath());
//        tree.expandItem(root.getAbsolutePath());
//        // now expand path
//
//        for (int i = pathitems.size() - 1; i >= 0; i--) {
//            Object it = pathitems.get(i);
//            p("Expanding path item: " + it);
//            //tree.expandItemsRecursively(it);
//            tree.expandItem(it);
//        }
//        tree.addListener(this);
//        tree.addListener(new ItemClickListener() {
//
//            public void itemClick(ItemClickEvent event) {
//                p("Got item: " + event.getItem());
//                Item it = event.getItem();
//                if (it == null) {
//                    return;
//                }
//                File sel = (File) it.getItemProperty("file").getValue();
//                setSelected(sel);
//            }
//        });
//
//        parent.addWindow(w);
//    }
    /**
     * Handle tree expand event, populate expanded node's childs with new files
     * and directories.
     */
//    public void nodeExpand(ExpandEvent event) {
//        final Item i = tree.getItem(event.getItemId());
//        if (i == null) {
//            return;
//        }
//        if (!tree.hasChildren(i)) {
//            // populate tree's node which was expanded
//            populateNode(event.getItemId().toString(), event.getItemId());
//        }
//    }
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
//    private void populateNode(String file, Object parent) {
//        final File subdir = new File(file);
//        final File[] files = subdir.listFiles();
//
//        if (files == null) {
//            return;
//        }
//        Arrays.sort(files, new FileComparator());
//        for (int x = 0; x < files.length; x++) {
//            Object itid = addFile(files[x], parent);
//            if (itid != null) {
//                pathitems.add(itid);
//            }
//        }
//
//    }
    private static void err(String msg, Exception ex) {
        Logger.getLogger(TreeTableFileSystemWindow.class.getName()).log(Level.SEVERE, msg, ex);
    }

    private static void err(String msg) {
        Logger.getLogger(TreeTableFileSystemWindow.class.getName()).log(Level.SEVERE, msg);
    }

    private static void warn(String msg) {
        Logger.getLogger(TreeTableFileSystemWindow.class.getName()).log(Level.WARNING, msg);
    }

    private static void p(String msg) {
        //system.out.println("TreeTableFileSystemWindow: " + msg);
        Logger.getLogger(TreeTableFileSystemWindow.class.getName()).log(Level.INFO, msg);
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
