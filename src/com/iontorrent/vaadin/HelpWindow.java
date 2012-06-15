/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.vaadin.utils.WindowOpener;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Button;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class HelpWindow extends WindowOpener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private TSVaadin app;
	
	private TextField txtTimeout;
	private Label lblTimeout;
//	private Button keep;
	//TabSheet tabsheet;

	public HelpWindow(TSVaadin app, Window main, String description, int x, int y) {
		super("Help", main, description, x, y, 600, 650);
		this.app = app;
	}

	@Override
	public void windowOpened(final Window mywindow) {
		createGui();
	}

	private void createGui() {

		VerticalLayout v = new VerticalLayout();
		HorizontalLayout h = new HorizontalLayout();
		h.addComponent(new Label("Session Timeout (minutes)"));
		txtTimeout = new TextField();
		txtTimeout.setValue(""+app.getTimeout());
		txtTimeout.addListener(new Property.ValueChangeListener() {

			public void valueChange(ValueChangeEvent event) {
				// addChart(chartTab);
				int min = 60;
				try  {
					min = Integer.parseInt(""+txtTimeout.getValue());
				}
				catch (Exception e) {}
				app.setTimeout(min);
				reopen();
			}
		});
		h.addComponent(txtTimeout);
//		keep = new Button("Keep alive ( it is "+app.isKeepAlive()+")");
//		keep.addListener(new Button.ClickListener() {
//			@Override
//			public void buttonClick(ClickEvent event) {
//				app.setKeepAlive(!app.isKeepAlive());
//				reopen();
//			}
//
//		});
//		h.addComponent(keep);
		v.setSizeFull();
		mywindow.addComponent(v);
		Accordion accordion = new Accordion();
		accordion.setSizeFull();
		Panel panel = new Panel();
		panel.setWidth("540px");
		panel.setHeight("540px");
		panel.addComponent(accordion);
		// Trim its layout to allow the Accordion take all space.
		panel.getLayout().setSizeFull();
		panel.getLayout().setMargin(false);
		
		String url = app.getURL().toString();
		p("help url: "+url);
	//	: App url: http://127.0.0.1:8080/TSL/
		if (!app.getServer().equalsIgnoreCase("localhost") && url.indexOf("127.0.0.1") > -1) {
			url = url.replace("127.0.0.1:8080", app.getServer());
			url = url.replace("127.0.0.1", app.getServer());
		}
		if (!url.endsWith("TSL/")) url = url+"TSL/";
		p("help url is now: "+url);
		Link link = new Link("PDF document", new ExternalResource(url+"VAADIN/help.pdf"));
		link.setTargetName("_blank");
        v.addComponent(link);
        v.addComponent(h);
		v.addComponent(panel);
		
		accordion.addTab(new Label(getTroubleShootingMsg(), Label.CONTENT_XHTML),"Trouble Shooting/FAQ", null);
		accordion.addTab(new Label(app.getHelpInfo(), Label.CONTENT_XHTML), "General", null);
		
		
		for (final WindowOpener win : app.getMyWindows()) {			
			accordion.addTab(new Label(win.getHelpMessage(), Label.CONTENT_XHTML), win.getName(), null);			
		}
	}
	private String getTroubleShootingMsg() {
		String msg = "";
		msg += "<h2>Common Problems</h2>";
		msg += "<h3>Disappearing Menu Bar</h3>";
		msg += "<p>Use the mouse wheel to <b>zoom all the way out</b> until youy see the menu bar again. Then zoom back in.<br>" +
				"This sometimes happens when a window that is further down is moved up. You can also resolve this by making your browser window very large</p>";
		msg += "<h3>Wait symbol/Slow response</h3>";
		msg += "<p>Try to use the closest location to run TSL. If you are running it far away (in terms of network), it takes longer to send signals back and forth to and from the server, resulting in slow response</p>";
		msg += "<h2>Frequently Asked Questions</h2>";
		msg += "<h3>How to view my own custom dataset</h3>";
		msg += "<p>Select Open Experiment/Pick folders. You just need to select the raw and the results folder in most cases. It will try to figure out the other parts.</p>";
		
		
		return msg;
	}
	private static void p(String msg) {
		//system.out.println("HelpWindow: " + msg);
		Logger.getLogger(HelpWindow.class.getName()).log(Level.INFO, msg);
	}

	@Override
	public String getHelpMessage() {
		return null;
	}

}
