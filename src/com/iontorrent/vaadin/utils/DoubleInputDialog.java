package com.iontorrent.vaadin.utils;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class DoubleInputDialog extends Window {
    Recipient r;
    TextField tf = new TextField();
    Window parent;
    
    public DoubleInputDialog(final Window parent, String question, Recipient recipient) {
    	this(parent, question, recipient, null);
    }
    public DoubleInputDialog(final Window par, String question, Recipient recipient, String name) {
    	if (par.getParent() != null) parent = par.getParent();
        else parent = par;
        r = recipient;
        setCaption(question);
        setModal(true);
        getLayout().setSizeUndefined();
        if (name != null) tf.setValue(name);
        addComponent(tf);
        
        final Window dialog = this;
        addComponent(new Button("Ok", new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
            	double val = 0;
            	if (tf != null) {
            		try {
            			val = Double.parseDouble(tf.toString());
            		} catch (Exception e) {}
            	}
                r.gotInput(val);
                parent.removeWindow(dialog);
            }
        }));
        parent.addWindow(this);
    }

    public interface Recipient {
        public void gotInput(double input);
    }
}