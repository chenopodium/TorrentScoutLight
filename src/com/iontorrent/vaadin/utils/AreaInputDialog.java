package com.iontorrent.vaadin.utils;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class AreaInputDialog extends Window {
    Recipient r;
    TextArea tf = new TextArea();
    Window parent;

    public AreaInputDialog(final Window parent, String question, Recipient recipient) {
    	this(parent, question, recipient, null);
    }
    public AreaInputDialog(final Window par, String question, Recipient recipient, String name) {
        r = recipient;
        if (par.getParent() != null) parent = par.getParent();
        else parent = par;
        setCaption(question);
        setModal(true);
        getLayout().setSizeUndefined();
        if (name != null) tf.setValue(name);
        tf.setWidth("300px");
        tf.setRows(3);
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