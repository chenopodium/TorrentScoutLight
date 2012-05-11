/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.vaadin.TSVaadin;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Layout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public abstract class WindowOpener extends CustomComponent implements Window.CloseListener, MenuBar.Command {

    protected Window mainwindow;  // Reference to main window
    protected Window mywindow;    // The window to be opened
 //   private Button openbutton;  // Button for opening the window
    // Button closebutton; // A button in the window
    //   Label explanation; // A descriptive text
    private int location_x, location_y;
    private String name;
    private Layout winlayout;
    private int width, height;
    private boolean isOpen;
    private TSVaadin app;
    
    public WindowOpener(String label, Window main, String description, int x, int y, int w, int h) {
        mainwindow = main;
        this.app = (TSVaadin)main.getApplication();
        this.name = label;
        this.location_x = x;
        this.location_y = y;
        this.width = w;
        this.height = h;
        Layout layout = new VerticalLayout();
        //openbutton = new Button(name, this,"openButtonClick");

        setDescription(description);
        //  explanation = new Label(description);
        //layout.addComponent(openbutton);
        // layout.addComponent(explanation);
        setCompositionRoot(layout);
    }
    public void clear(){}
    
    public abstract String getHelpMessage();
    
    public String getName() {
    	return name;
    }
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public void setDescription(String desc) {
    //    openbutton.setDescription("<h2><img src=\"/TSL/VAADIN/themes/torrentscout/img/note.png\"/>"
    //            + "Open " + name + "</h2>" + desc);
        super.setDescription(desc);
    }

    public void close() {
        isOpen = false;
        p("Closing window " + name);
        mainwindow.removeWindow(mywindow);
       // openbutton.setEnabled(true);
        
        // for debugging
        Thread threads[] = new Thread[10];
        Thread.enumerate(threads);
        for (Thread t: threads) {
        	if (t != null && !t.getName().startsWith("http") && !t.getName().startsWith("ajp") && !t.getName().startsWith("main") && !t.getName().startsWith("Cont")) { 
        		p("Got thread: "+t.toString());
        	}
        }
    }

    public void reopen() {
       // if (mywindow.i)
        if (!isOpen) {
            return;
        }
        close();
        openButtonClick(null);
    }

    public void open() {
    	app.logModule(this.name, "open");
        if (isOpen)close();        
        openButtonClick(null);        
    }

    /** Handle the clicks for the two buttons. */
    public void openButtonClick(Button.ClickEvent event) {
    	
        /* Create a new window. */
        isOpen = true;
        
        // it was open before
        if (mywindow != null) {
            location_x=mywindow.getPositionX();
            location_y=mywindow.getPositionY();
            height = (int) mywindow.getHeight();
            width = (int) mywindow.getWidth();
        }
        mywindow = new Window(name);
        mywindow.setPositionX(location_x);
        mywindow.setPositionY(location_y);
        mywindow.setHeight(height + "px");
        mywindow.setWidth(width + "px");
        /* Add the window inside the main window. */
        
        mainwindow.addWindow(mywindow);
        /* Listen for close events for the window. */
        mywindow.addListener(this);
        /* Add components in the window. */
        //   mywindow.addComponent(
        //           new Label(explanation));

        winlayout = new VerticalLayout();
        /* Allow opening only one window at a time. */
   //     openbutton.setEnabled(false);
        //  explanation.setValue("Window opened");
        mywindow.addComponent(winlayout);
        windowOpened(mywindow);
    }

    public void windowOpened(Window mywindow) {
    }

    /** Handle Close button click and close the window. */
    public void closeButtonClick(Button.ClickEvent event) {
        /* Windows are managed by the application object. */
        mainwindow.removeWindow(mywindow);
        isOpen = false;
        /* Return to initial state. */
   //     openbutton.setEnabled(true);
        //  explanation.setValue("Closed with button");
    }

    /** In case the window is closed otherwise. */
    public void windowClose(CloseEvent e) {
        /* Return to initial state. */
  //      openbutton.setEnabled(true);
    	 isOpen = false;
        //  explanation.setValue("Closed with window controls");
    }

    private static void p(String msg) {
        //system.out.println("WindowOpener: " + msg);
        Logger.getLogger(WindowOpener.class.getName()).log(Level.INFO, msg);
    }
	@Override
	public void menuSelected(MenuItem selectedItem) {
		this.open();
		
	}

  
}
