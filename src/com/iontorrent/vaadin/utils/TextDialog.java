package com.iontorrent.vaadin.utils;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.Window;

public class TextDialog extends Window {
    /**
	 * 
	 */
	
	Window parent;
	private static final long serialVersionUID = 1L;
	TextArea area; 

    public TextDialog(final Window par, String title, String data) {
    	if (par.getParent() != null) parent = par.getParent();
        else parent = par;
        area = new TextArea();
        area.setWidth("560px");
        area.setHeight("250px");
        area.setDescription("You can copy this text for later use");
        setCaption(title);
        setModal(true);
        getLayout().setSizeUndefined();               
        area.setValue(data);
        addComponent(area);
        final Window dialog = this;
        dialog.setWidth("600px");
        dialog.setHeight("370px");
        addComponent(new Button("Ok", new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {            	
                parent.removeWindow(dialog);
            }
        }));
        parent.addWindow(this);
    }

}