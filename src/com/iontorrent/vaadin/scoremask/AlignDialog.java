package com.iontorrent.vaadin.scoremask;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class AlignDialog extends Window {
    Recipient r; 
    TextField tread;
    TextField tref ;

    public AlignDialog(final Window parent, String title, String question, String seq, String ref, Recipient recipient) {
        r = recipient;
        tread = new TextField();
        tref = new TextField();
        setCaption(title);
        setModal(true);
        getLayout().setSizeUndefined();
        
        addComponent(new Label(question));
        GridLayout h = new GridLayout(2,2);
        h.addComponent(new Label("Read alignment:"));
        h.addComponent(tread);
        tread.setWidth("100px");
        
        addComponent(h);
        
        tref.setWidth("100px");
        tread.setValue(seq);
        tread.setDescription("If you want multiple possiblities for the read sequence, you can leave it empty");
        tref.setValue(ref);
        tref.setDescription("If you want multiple possiblities for the reference, you can leave it empty");
        h.addComponent(new Label("Reference alignment:"));
        h.addComponent(tref);
        addComponent(h);
        final Window dialog = this;
        dialog.setWidth("400px");
        addComponent(new Button("Ok", new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
            	String seq="";
            	String ref = "";
            	try {
            		if (tread.getValue()!= null) seq = ""+tread.getValue();            	
            		if (tref.getValue()!= null) ref = ""+tref.getValue();
            	}
            	catch (Exception e) {}
                r.gotInput(seq, ref);
                parent.removeWindow(dialog);
            }
        }));
        parent.addWindow(this);
    }

    public interface Recipient {
        public void gotInput(String seq, String ref);
    }
}