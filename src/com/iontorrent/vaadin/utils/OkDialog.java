package com.iontorrent.vaadin.utils;

import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Window;

public class OkDialog extends Window {
   Recipient recip;
   Label lbl;
   Window parent;

   public OkDialog(final Window par, String title, String question, Recipient recipient) {
    	recip = recipient;
    	if (par.getParent() != null) parent = par.getParent();
        else parent = par;
        setCaption(title);
        setModal(true);
        setWidth("400px");
       
        lbl = new Label(question, Label.CONTENT_XHTML);
        addComponent(lbl);
        final Window dialog = this;
        HorizontalLayout h = new  HorizontalLayout();
        addComponent(h);
        h.addComponent(new Button("Ok", new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
            	recip.gotInput("OK");
                parent.removeWindow(dialog);
            }
        }));
        h.addComponent(new Button("Cancel", new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
            	recip.gotInput("CANCEL");
                parent.removeWindow(dialog);
            }
        }));
        dialog.focus();
        parent.addWindow(this);
    }
   
   public OkDialog(final Window par, String title, AbstractLayout content, Recipient recipient) {
   	recip = recipient;
   	if (par.getParent() != null) parent = par.getParent();
    else parent = par;
       setCaption(title);
       setModal(true);
       setWidth("400px");
             
       addComponent(content);
       final Window dialog = this;
       HorizontalLayout h = new  HorizontalLayout();
       addComponent(h);
       h.addComponent(new Button("Ok", new Button.ClickListener() {
           public void buttonClick(ClickEvent event) {
           	recip.gotInput("OK");
               parent.removeWindow(dialog);
           }
       }));
       h.addComponent(new Button("Cancel", new Button.ClickListener() {
           public void buttonClick(ClickEvent event) {
           	recip.gotInput("CANCEL");
               parent.removeWindow(dialog);
           }
       }));
       dialog.focus();
       parent.addWindow(this);
   }

    public interface Recipient {
        public void gotInput(String input);
    }
}