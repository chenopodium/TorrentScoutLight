package com.iontorrent.vaadin.utils;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class InputDialog extends Window {
    Recipient r;
    TextField tf = new TextField();
    Window parent;

    public InputDialog(final Window parent, String question, Recipient recipient) {
    	this(parent, question, recipient, null);
    }
    public InputDialog(final Window par, String question, Recipient recipient, String name) {
        r = recipient;
        if (par.getParent() != null) parent = par.getParent();
        else parent = par;
        setCaption(question);
        setModal(false);
        getLayout().setSizeUndefined();
        if (name != null) tf.setValue(name);
        addComponent(tf);
        
        final Window dialog = this;
        dialog.focus();
        addComponent(new Button("Ok", new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
                r.gotInput(tf.toString());
                parent.removeWindow(dialog);
            }
        }));
        parent.addWindow(this);
    }

    public interface Recipient {
        public void gotInput(String input);
    }
}