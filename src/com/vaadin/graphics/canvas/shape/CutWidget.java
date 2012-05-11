package com.vaadin.graphics.canvas.shape;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CutWidget extends Polygon {

	
	
	public CutWidget(int x, int y, int width, int height) {
		this(x, y, width, height, java.awt.Color.GRAY, 1);
	}
	
	public CutWidget(int x, int y, int width, int height, java.awt.Color col, int nr) {
		super(null);
		this.setLocky(true);
		int s = 2;
		int m = height/2;
		int d = width/2;
		int h1 = y-m-d;
		int h2 = y-m+d;
		int x1 = x-d;
		int x2 = x+d;
		int xa = x-s/2;
		int xb = x+s/2;
		Point[] points = new Point[] { 
				new Point(xa, y), new Point(xb, y), 
				new Point(xb, h1), new Point(x2, h1), 
				new Point(x2, h2), new Point(xb, h2), 
				new Point(xb, y-height), new Point(xa, y-height), 
				new Point(xa, h2), new Point(x1, h2), 
				new Point(x1, h1), new Point(xa, h1), 
				new Point(xa, y) };
		super.setVertices(points);
		setBorderWidth(2);
		this.setDescription("Widget "+nr);
		setColor("#000000");
		String hexStr = Integer.toHexString( col.getRGB() );
		
		String scolor = hexStr;
		if (hexStr.length()>6) hexStr = hexStr.substring(hexStr.length()-6);
				
		if (!hexStr.startsWith("#")) scolor="#"+hexStr;
		if (scolor.endsWith("x")) scolor = scolor.substring(0, scolor.length()-1);
		//p("Got color: "+hexStr+", scolor="+scolor);
		setFillColor(scolor);
		this.setId("Widget "+nr);
		setSelectedFillColor("yellow");
	}
	private static void p(String msg) {
		System.out.println("CutWidget: " + msg);
		Logger.getLogger(CutWidget.class.getName()).log(Level.INFO, msg);
	}
}
