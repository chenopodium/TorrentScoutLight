package com.iontorrent.vaadin.utils;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class OptionsDialog extends Window {
    Recipient r; 
    Label[] lbls;
    
    public OptionsDialog(final Window parent, String title, String question,  String a, String b, Recipient recipient) {
    	this(parent, title,question, new String[] {a, b}, 0, recipient);
    }
    public OptionsDialog(final Window parent, String title,   String question,String a, String b, String c,  Recipient recipient) {
    	this(parent, title,question, new String[] {a, b, c}, 0, recipient);
    }
    public OptionsDialog(final Window parent, String title,  String question, String a, String b, String c, String d,   Recipient recipient) {
    	this(parent, title,question, new String[] {a, b, c, d}, 0, recipient);
    }
    public OptionsDialog(final Window parent, String title, String question, final String[] options, int selected, Recipient recipient) {
        r = recipient;
        final OptionGroup group = new OptionGroup("");
        final int nr = options.length;
        lbls = new Label[nr];
        for (int i = 1; i <= nr; i++) {
        	group.addItem(i+")  "+options[i-1]);
        }
        setCaption(title);
        setModal(true);
        getLayout().setSizeUndefined();
        
        addComponent(new Label(question));
        addComponent(group);
        addComponent(new Label("  "));
       
        if (selected >-1 && selected <nr) group.setItemEnabled(options[selected], true);
        final Window dialog = this;
        dialog.setWidth("400px");
        addComponent(new Button("Ok", new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
            	String id = null;
            	parent.removeWindow(dialog);
            	if (group.getValue() != null) id = ""+group.getValue();
            	//p("Got id: "+id);
            	if (id != null) {
            		for (int i =0; i < nr; i++ ) {
            			if (id.indexOf(options[i])>1) r.gotInput(i);
            		}
            	}
            	else r.gotInput(-1);
                
            }
        }));
        parent.addWindow(this);
    }

    public interface Recipient {
        public void gotInput(int selection);
    }
}