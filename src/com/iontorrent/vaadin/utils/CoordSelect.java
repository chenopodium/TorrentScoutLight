/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class CoordSelect {

    private int x;
    private int y;
    Button.ClickListener listener;
    TextField tx;
    TextField ty;
    public CoordSelect(int x, int y, Button.ClickListener listener) {
        this.x = x;
        this.y = y;
        this.listener = listener;
    }

    public WellCoordinate getCoord() {
    	return new WellCoordinate(x, y);
    }
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        tx.setValue(x);
    }
    public void setY(int y) {
        ty.setValue(y);
    }
    public void addGuiElements(AbstractComponentContainer comp) {
        HorizontalLayout h = new HorizontalLayout();
        comp.addComponent(h);
        // h.addComponent(current);
        tx = new TextField();
        tx.setWidth("40px");
        // tflow.setHeight("25px");
        tx.setImmediate(true);
        tx.setDescription("The column in the chip (hit refresh to refresh the view)");
        h.addComponent(new Label("X:"));
        h.addComponent(tx);
        tx.setValue(x);
        tx.addListener(new Property.ValueChangeListener() {

            public void valueChange(ValueChangeEvent event) {
                x = getInt(tx);
            }
        });

        ty = new TextField();
        ty.setWidth("40px");
        ty.setDescription("The row on the chip (hit refresh to refresh the view)");
        // tflow.setHeight("25px");
        ty.setImmediate(true);
        ty.setValue(y);
        h.addComponent(new Label("Y:"));
        ty.addListener(new Property.ValueChangeListener() {

            public void valueChange(ValueChangeEvent event) {
                y = getInt(ty);
            }
        });
        h.addComponent(ty);

        Button ref = new Button();
        ref.setDescription("(Re)Loads the data for the entered coordinates");
        ref.setIcon(new ThemeResource("img/view-refresh-3.png"));
        h.addComponent(ref);
        ref.addListener(new Button.ClickListener() {

            public void buttonClick(Button.ClickEvent event) {
                x = getInt(tx);
                y = getInt(ty);
                listener.buttonClick(event);
            }
        });
    }

    public int getInt(TextField t) {
        if (t.getValue() == null) {
            return 0;
        }
        String v = "" + t.getValue();
        int i = 0;
        try {
            i = Integer.parseInt(v);
        } catch (Exception e) {
        }
        return i;
    }

    /** ================== LOGGING ===================== */
   private static void err(String msg, Exception ex) {
        Logger.getLogger(CoordSelect.class.getName()).log(Level.SEVERE, msg, ex);
    }

    private static void err(String msg) {
        Logger.getLogger(CoordSelect.class.getName()).log(Level.SEVERE, msg);
    }

    private static void warn(String msg) {
        Logger.getLogger(CoordSelect.class.getName()).log(Level.WARNING, msg);
    }

    private static void p(String msg) {
        ////system.out.println("CoordSelect: " + msg);
        Logger.getLogger(CoordSelect.class.getName()).log(Level.INFO, msg);
    }
}
