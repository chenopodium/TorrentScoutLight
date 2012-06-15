package com.iontorrent.vaadin.utils;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class RangeDialog extends Window {
    Recipient r; 
    TextField tmin;
    TextField tmax ;
    Window parent;
    public RangeDialog(final Window par, String title, String question, double min, double max, Recipient recipient) {
        r = recipient;
        
        if (par.getParent() != null) parent = par.getParent();
        else parent = par;
        tmin = new TextField();
        tmax = new TextField();
        setCaption(title);
        setModal(true);
        getLayout().setSizeUndefined();
        HorizontalLayout h = new HorizontalLayout();
        addComponent(new Label(question));
        h.addComponent(new Label("Min:"));
        h.addComponent(tmin);
        tmin.setWidth("100px");
        tmax.setWidth("100px");
        tmin.setValue(min);
        tmax.setValue(max);
        h.addComponent(new Label("Max:"));
        h.addComponent(tmax);
        addComponent(h);
        final Window dialog = this;
        dialog.focus();
        dialog.setWidth("400px");
        addComponent(new Button("Ok", new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
            	double min = 0;
            	double max = 0;
            	try {
            		if (tmin.getValue()!= null) min = Double.parseDouble(""+tmin.getValue());
            	
            		if (tmax.getValue()!= null) max = Double.parseDouble(""+tmax.getValue());
            	}
            	catch (Exception e) {}
                r.gotInput(min, max);
                parent.removeWindow(dialog);
            }
        }));
        parent.addWindow(this);
    }

    public interface Recipient {
        public void gotInput(double min, double max);
    }
}