/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.NativeButton;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class ZoomControl {

	private int bucket;
	Button.ClickListener listener;
	Button plus;
	Button minus;
	Button pplus;
	Button mminus;

	int max;
	public ZoomControl(int bucket, Button.ClickListener listener) {
		this.bucket = bucket;
		this.listener = listener;
		max = 50;
	}

	public void setMax(int max) {
		this.max = max;
	}
	public int getBucket() {
		return bucket;
	}

	public void addGuiElements(AbstractComponentContainer h) {
	//	HorizontalLayout h = new HorizontalLayout();
		//comp.addComponent(h);
		//String W = "28px";
		pplus = new NativeButton();
		pplus.setStyleName("nopadding");
		pplus.setIcon(new ThemeResource("img/zoom-inin.png"));
		pplus.setImmediate(true);
	//	pplus.setWidth(W);
		h.addComponent(pplus);
		pplus.setDescription("Zoom in quickly (multiple steps)");
		plus = new NativeButton();
		plus.setStyleName("nopadding");
	//	plus.setWidth(W);
		plus.setIcon(new ThemeResource("img/zoom-in.png"));
		plus.setImmediate(true);
		h.addComponent(plus);

		pplus.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				bucket -=8;
				pplus.setEnabled(bucket > 1);
				plus.setEnabled(bucket > 1);
				if (bucket <= 1) plus.setDescription("Maximum zoom level reached ("+bucket+")");
				else
					pplus.setDescription("Zoom in quickly (multiple steps)");

				bucket = Math.max(1, bucket);
				listener.buttonClick(event);
			}
		});
		pplus.setEnabled(bucket > 1);
		
		plus.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				bucket--;
				pplus.setEnabled(bucket > 1);
				plus.setEnabled(bucket > 1);
				if (bucket <= 1) plus.setDescription("Maximum zoom level reached ("+bucket+")");
				else
					plus.setDescription("Click to zoom in further ("+bucket+")");

				bucket = Math.max(1, bucket);
				listener.buttonClick(event);
			}
		});
		plus.setEnabled(bucket > 1);
		if (bucket <=1) plus.setDescription("Maximum zoom level reached");
		else
			plus.setDescription("Click to zoom in further");
		minus = new NativeButton();
		minus.setStyleName("nopadding");
		minus.setIcon(new ThemeResource("img/zoom-out.png"));
		minus.setImmediate(true);
		//minus.setWidth(W);
		minus.setDescription("Click to zoom out");
		h.addComponent(minus);

		minus.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				bucket++;
				minus.setEnabled(bucket < max);
				mminus.setEnabled(bucket < max);
				bucket = Math.min(max, bucket);				
				listener.buttonClick(event);
			}
		});
		
		mminus = new NativeButton();
		mminus.setStyleName("nopadding");
	//	mminus.setWidth(W);
		mminus.setIcon(new ThemeResource("img/zoom-outout.png"));
		mminus.setImmediate(true);
		mminus.setDescription("Click to zoom out in multiple steps");
		h.addComponent(mminus);

		mminus.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				bucket+=8;
				minus.setEnabled(bucket < max);
				mminus.setEnabled(bucket < max);
				bucket = Math.min(max, bucket);		
				listener.buttonClick(event);
			}
		});

	}

	/** ================== LOGGING ===================== */
	private static void err(String msg, Exception ex) {
		Logger.getLogger(ZoomControl.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(ZoomControl.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(ZoomControl.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		// //system.out.println("ZoomControl: " + msg);
		Logger.getLogger(ZoomControl.class.getName()).log(Level.INFO, msg);
	}
}
