package com.vaadin.graphics.canvas.shape;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Cross extends Polygon {

	
	
	public Cross(int x, int y, int width, int size_of_arm) {
		this(x, y, width, size_of_arm, java.awt.Color.ORANGE, 1);
	}
	
	public Cross(int x, int y, int width, int size_of_arm, java.awt.Color col, int nr) {
		super(null);
		int d = width;
		int s = size_of_arm;
		
		Point[] points = new Point[] { new Point(x, y), new Point(x + d, y), new Point(x + d, y + s), new Point(x + d + s, y + s), new Point(x + d + s, y + d + s), new Point(x + d, y + d + s), new Point(x + d, y + s + d + s), new Point(x, y + s + d + s), 
				new Point(x, y + d + s), new Point(x - s, y + d + s), new Point(x - s, y + s), new Point(x, y + s), new Point(x, y) };
		super.setVertices(points);
		setBorderWidth(2);
		//this.setDescription("Cross "+nr);
		setColor("#000000");
		
		String hexStr = Integer.toHexString( col.getRGB() );
		
		String scolor = hexStr;
		if (hexStr.length()>6) hexStr = hexStr.substring(hexStr.length()-6);
				
		if (!hexStr.startsWith("#")) scolor="#"+hexStr;
		if (scolor.endsWith("x")) scolor = scolor.substring(0, scolor.length()-1);
		//p("Got color: "+hexStr+", scolor="+scolor);
		setFillColor(scolor);
		this.setId("Cross "+nr);
		setSelectedFillColor("yellow");
	}
	private static void p(String msg) {
		//system.out.println("Cross: " + msg);
		Logger.getLogger(Cross.class.getName()).log(Level.INFO, msg);
	}
}
