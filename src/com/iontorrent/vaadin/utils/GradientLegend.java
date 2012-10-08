/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.guiutils.heatmap.GradientPanel;
import com.iontorrent.vaadin.iono.IonogramImage;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.Application;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.MouseEvents.ClickEvent;
import com.vaadin.event.MouseEvents.ClickListener;
import com.vaadin.graphics.canvas.Canvas;
import com.vaadin.graphics.canvas.shape.Cross;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class GradientLegend {

	Recipient r;
	
    private int x;
    private int y;
    Recipient listener;
    GradientPanel gradient;
    Embedded em;
    Application app;
    int height;
    int maxheight;
    int multiplier;
    
    public GradientLegend(int multiplier, GradientPanel gradient, Recipient listener, Application app, int height, int maxheight) {    
        this.listener = listener;
        this.multiplier = multiplier;
        this.gradient = gradient;
        this.app = app;
        this.height = height;
        this.maxheight = maxheight;
    }

    public WellCoordinate getCoord() {
    	return new WellCoordinate(x, y);
    }
    public void setPercent(boolean inPercent) {
    	this.gradient.setInPercent(inPercent);
    }
    public void addGuiElements(AbstractComponentContainer comp) {
        VerticalLayout v = new VerticalLayout();
        comp.addComponent(v);
        gradient.setBackground(Color.black);
        
        StreamResource.StreamSource imagesource = new GradientImage(gradient, 30, height );
        double min = gradient.getMin()/(double)multiplier;
		double max = gradient.getMax()/(double)multiplier;
        String key=this.multiplier+"_"+(int)min+ "_"+(int)max+"_"+gradient.hashCode();
        StreamResource imageresource1 = new StreamResource(imagesource, "gradient"+key+".png", app);
		imageresource1.setCacheTime(1000);
		
		Embedded em = new Embedded(null, imageresource1);
		em.setDescription("min (black): "+min+ ", max (white): "+max+", percent: "+gradient.isInPercent()+"<br>(click to change min/max)");
        v.addComponent(em);
       
        em.addListener(new ClickListener() {
			
			@Override
			public void click(ClickEvent event) {
				askForMinMax();
				
			}
		});  
        
        Canvas canvas = new Canvas();
		canvas.setHeight(maxheight-height+"px");
		canvas.setWidth("30px");
		canvas.setBackgroundColor("black");
		v.addComponent(canvas);	
    }
    private void askForMinMax() {
    	final double min = gradient.getMin()/(double)multiplier;
		final double max = gradient.getMax()/(double)multiplier;
		// needs to consider... bucket?
	
		
    	OptionsDialog options = new OptionsDialog(app.getMainWindow(), "Change Min or Max", "Change...", 
				"... The minimum value (the value for black, currently: "+min+")",
				"... The maximum value (the value for white, currently: "+max+")", 
				new OptionsDialog.Recipient() {

			@Override
			public void gotInput(final int selection) {
				if (selection < 0) return;
				// / do the search
				
				if (selection == 0) {
					// ask for flows
					DoubleInputDialog input = new DoubleInputDialog(app.getMainWindow(), "New minimum between "+min+"-"+max+"", new DoubleInputDialog.Recipient() {
						public void gotInput(double val) {
							val = val * (double)multiplier;
							gradient.setMin((int) val);
							// send event to listener
							listener.minOrMaxChanged();
						}
					}, ""+min);
					return;
				}
				
				else if (selection == 1) {
					// ask for flows
					DoubleInputDialog input = new DoubleInputDialog(app.getMainWindow(), "New maximum between "+min+"-"+max+"", new DoubleInputDialog.Recipient() {
						public void gotInput(double val) {
							val = val * (double)multiplier;
							gradient.setMax((int) val);
							// send event to listener
							listener.minOrMaxChanged();
						}
					}, ""+max);
					return;
				}
				
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

    public interface Recipient {
    	public void minOrMaxChanged();
    }
    /** ================== LOGGING ===================== */
   private static void err(String msg, Exception ex) {
        Logger.getLogger(GradientLegend.class.getName()).log(Level.SEVERE, msg, ex);
    }

    private static void err(String msg) {
        Logger.getLogger(GradientLegend.class.getName()).log(Level.SEVERE, msg);
    }

    private static void warn(String msg) {
        Logger.getLogger(GradientLegend.class.getName()).log(Level.WARNING, msg);
    }

    private static void p(String msg) {
        ////system.out.println("CoordSelect: " + msg);
        Logger.getLogger(GradientLegend.class.getName()).log(Level.INFO, msg);
    }
}
