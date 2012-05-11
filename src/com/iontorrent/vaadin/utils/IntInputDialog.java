package com.iontorrent.vaadin.utils;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class IntInputDialog extends Window {
    Recipient r;
    TextField tf = new TextField();

    public IntInputDialog(final Window parent, String question, Recipient recipient) {
    	this(parent, question, null, recipient, null, 0);
    }
    public IntInputDialog(final Window parent, String title, String question, Recipient recipient, String name) {
    	this(parent, title, question, recipient, name, 0);
    }
    public IntInputDialog(final Window parent, String title, String question, Recipient recipient, String name, int width) {
        r = recipient;
        setCaption(title);
        setModal(true);
        if (width <= 0) getLayout().setSizeUndefined();
        else this.setWidth(width+"px");
        if (name != null) tf.setValue(name);
        if (question != null) {
        	addComponent(new Label(question));
        }
        addComponent(tf);
        
        final Window dialog = this;
        addComponent(new Button("Ok", new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
            	int val = 0;
            	
            	if (tf != null) {
            		try {
            			val = Integer.parseInt(tf.toString());
            		} catch (Exception e) {}
            	}
            	parent.removeWindow(dialog);
                r.gotInput(val);
                
            }
        }));
        parent.addWindow(this);
    }

    public interface Recipient {
        public void gotInput(int input);
    }
}