/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.raw;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.rawdataaccess.transformation.DataTransformation;
import com.iontorrent.rawdataaccess.transformation.TransformFactory;
import com.iontorrent.utils.system.Parameter;
import com.vaadin.data.Property;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Select;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class TransOptions implements Property.ValueChangeListener {

    ArrayList<DataTransformation> alltrans;
    DataTransformation trans;
    Label lblDesc;
    TextField[] txtParam = new TextField[4];
    Label[] lblParam = new Label[4];
    CheckBox box;
    TextArea area;

    public TransOptions() {
        alltrans = TransformFactory.getTransformations();
        DataTransformation[] data = new DataTransformation[alltrans.size()];
        int i = 0;
        for (DataTransformation t : alltrans) {
            data[i++] = t;
        }
        trans = data[0];
    }

    public void createGui(AbstractLayout l) {
        // left: selection of trans
        HorizontalLayout h = new HorizontalLayout();
       
        Select sel = new Select("Transformations");
        for (DataTransformation data : alltrans) {
            sel.addItem(data);
            sel.setItemCaption(data, data.getName());
        }
        h.addComponent(sel);
        sel.setImmediate(true);
        sel.addListener(this);
        l.addComponent(h);
        VerticalLayout v = new VerticalLayout();
        h.addComponent(v);
        // add description etc
        box = new CheckBox("Enabled");
        area = new TextArea("Current setting");
        v.addComponent(box);
        lblDesc = new Label();
        v.addComponent(lblDesc);
        for (int i = 0; i < txtParam.length; i++) {
            HorizontalLayout row = new HorizontalLayout();
            txtParam[i] = new TextField();
            lblParam[i] = new Label();
            row.addComponent(lblParam[i]);
            row.addComponent(txtParam[i]);
            v.addComponent(row);
        }
        v.addComponent(area);
        area.setWidth("300px");
        area.setHeight("300px");
        updateRight(trans);
    }

    public void updateRight(DataTransformation trans) {

        String s = trans.getDescription();     
        lblDesc.setValue(s);

        Parameter par[] = trans.getParams();
        for (TextField txt : txtParam) {
            txt.setValue("");
        }

        box.setValue(trans.isEnabled());
        for (int i = 0; i < txtParam.length; i++) {
            update(lblParam[i], txtParam[i], par, i);
        }
        s = trans.toLongString();

        area.setValue(s);


    }

    private void update(Label lbl, TextField txt, Parameter[] pars, int which) {
        boolean en = false;
        if (pars != null) {
            en = pars.length > which;
        }
        lbl.setEnabled(en);
        txt.setEnabled(en);
        lbl.setVisible(en);
        txt.setVisible(en);
        if (en) {
            Parameter par = pars[which];
            if (par != null) {
                if (par.getValue() != null) {
                    txt.setValue(par.getValue());
                }
                lbl.setValue(par.getName());
                txt.setDescription(par.getDescription());
            }
        }
    }

    public void getParameters() {
        if (trans == null) {
            return;
        }
        Parameter par[] = trans.getParams();
        if (par != null) {
            for (int i = 0; i < par.length; i++) {
                par[i].setValue("" + txtParam[i].getValue());
            }
            trans.setParams(par);
        }
    }

    public void valueChange(Property.ValueChangeEvent event) {
        // The event.getProperty() returns the Item ID (IID) 
        // of the currently selected item in the component.
        Property id = event.getProperty();
        trans = (DataTransformation) id.getValue();
        updateRight(trans);
    }

   
    private static void err(String msg, Exception ex) {
        Logger.getLogger(TransOptions.class.getName()).log(Level.SEVERE, msg, ex);
    }

    private static void err(String msg) {
        Logger.getLogger(TransOptions.class.getName()).log(Level.SEVERE, msg);
    }

    private static void warn(String msg) {
        Logger.getLogger(TransOptions.class.getName()).log(Level.WARNING, msg);
    }

    private static void p(String msg) {
        ////system.out.println("TransOptions: " + msg);
        Logger.getLogger(TransOptions.class.getName()).log(Level.INFO, msg);
    }
}
