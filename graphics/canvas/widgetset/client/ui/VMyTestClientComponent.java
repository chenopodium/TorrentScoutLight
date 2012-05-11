package com.vaadin.graphics.canvas.widgetset.client.ui;


import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.UIDL;

/**
 * Client side widget which communicates with the server. Messages from the
 * server are shown as HTML and mouse clicks are sent to the server.
 */
public class VMyTestClientComponent extends Widget implements Paintable, ClickHandler {

	/** Set the CSS class name to allow styling. */
	public static final String CLASSNAME = "v-mytestclientcomponent";

	public static final String CLICK_EVENT_IDENTIFIER = "click";

	/** The client side widget identifier */
	protected String paintableId;

	/** Reference to the server connection object. */
	protected ApplicationConnection client;

	/**
	 * The constructor should first call super() to initialize the component and
	 * then handle any initialization relevant to Vaadin.
	 */
	public VMyTestClientComponent() {
		// TODO This example code is extending the GWT Widget class so it must set a root element.
		// Change to a proper element or remove this line if extending another widget.
		setElement(Document.get().createDivElement());
		
		// This method call of the Paintable interface sets the component
		// style name in DOM tree
		setStyleName(CLASSNAME);
		
		// Tell GWT we are interested in receiving click events
		sinkEvents(Event.ONCLICK);  
		//sinkEvents(Event.
		// Add a handler for the click events (this is similar to FocusWidget.addClickHandler())
		addDomHandler(this, ClickEvent.getType());
	}

    /** 
     * Called whenever an update is received from the server 
     */
	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
		// This call should be made first. 
		// It handles sizes, captions, tooltips, etc. automatically.
		if (client.updateComponent(this, uidl, true)) {
		    // If client.updateComponent returns true there has been no changes and we
		    // do not need to update anything.
			return;
		}

		// Save reference to server connection object to be able to send
		// user interaction later
		this.client = client;

		// Save the client side identifier (paintable id) for the widget
		paintableId = uidl.getId();

		// Process attributes/variables from the server
		// The attribute names are the same as we used in 
		// paintContent on the server-side
		int clicks = uidl.getIntAttribute("clicks");
		String message = uidl.getStringAttribute("message");
		String data = uidl.getStringAttribute("data");
		message +="<br>Data="+data;
		
		if (data != null && data.length()>0) {
			if (data.startsWith("[")) data = data.substring(1);
			if (data.endsWith("[")) data = data.substring(0, data.length()-2);
			String[] stimeseries = parseList(data, ",");
			if (stimeseries != null) {
				message += "<br>Got time series: "+stimeseries;
				for (int i = 0; i < stimeseries.length; i++) {
					try {
						int val = Integer.parseInt(stimeseries[i].trim());
						
					//	message += "<br>Element "+i+"="+val;
					//	p("Element "+i+"="+val);
					}
					catch (Exception e) {
						message += "<br>Could not parse "+stimeseries[i]+":"+e.getMessage();
					}
				}
			}
		}
		getElement().setInnerHTML("After <b>"+clicks+"</b> mouse clicks:<br>" + message);
		
	}
	public static String[] parseList(String list, String sep) {
        if (list == null) {
            return null;
        }

        list = list.trim();
        if (list.startsWith("[")) {
            list = list.substring(1);
        }
        if (list.endsWith("]")) {
            list = list.substring(0, list.length() - 1);
        }
        // out("input: "+list+", sep: "+sep);
        String[] items = list.split(sep);
        return items;
    }
	
	 private static void p(String msg) {
		 Log.debug("VMyTestClientComponent: " + msg);			
	 }

    /**
     * Called when a native click event is fired.
     * 
     * @param event
     *            the {@link ClickEvent} that was fired
     */
     public void onClick(ClickEvent event) {
		// Send a variable change to the server side component so it knows the widget has been clicked
		String button = "left click";
		// The last parameter (immediate) tells that the update should be sent to the server
		// right away
		client.updateVariable(paintableId, CLICK_EVENT_IDENTIFIER, button, true);
	}
}
