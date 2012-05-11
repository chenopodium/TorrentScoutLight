package com.iontorrent.vaadin.utils;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

public class FlowRangeDialog extends Window {
    Recipient r; 
    TextField tmin;
    TextField tmax ;

    public FlowRangeDialog(final Window parent, String title, String question, int min, int max, Recipient recipient) {
        r = recipient;
        tmin = new TextField();
        tmax = new TextField();
        setCaption(title);
        setModal(true);
        getLayout().setSizeUndefined();
        GridLayout h = new GridLayout(2,2);
        addComponent(new Label(question));
        h.addComponent(new Label("Starting flow (0 based):"));
        h.addComponent(tmin);
        tmin.setWidth("100px");
        tmax.setWidth("100px");
        tmin.setValue(min);
        tmax.setValue(max);
        h.addComponent(new Label("Ending flow (0 = all flows):"));
        h.addComponent(tmax);
        addComponent(h);
        final Window dialog = this;
        dialog.setWidth("400px");
        addComponent(new Button("Ok", new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
            	int min = 0;
            	int max = 0;
            	try {
            		if (tmin.getValue()!= null) min = Integer.parseInt(""+tmin.getValue());
            	
            		if (tmax.getValue()!= null) max = Integer.parseInt(""+tmax.getValue());
            	}
            	catch (Exception e) {}
                r.gotInput(min, max);
                parent.removeWindow(dialog);
            }
        }));
        parent.addWindow(this);
    }

    public interface Recipient {
        public void gotInput(int start, int end);
    }
}