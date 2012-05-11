package com.vaadin.graphics.canvas.shape;


public class Node extends ElementGroup {
	public static double connectorRadius = 5;
	
	public Node(double width, double height, Point center, String label){
		super();
		this.center = center;
		Rect rect = new Rect(new Point(- width/2, - height/2), new Point(width/2, height/2));
		IPort inPort = new IPort(Node.connectorRadius, new Point(0, 0));
		OPort oPort = new OPort(Node.connectorRadius, new Point(0, 0));
		Text text = new Text(label, new Point(0, 0));
		text.setAlignment(Text.TextAlign.LEFT);
		addElement(rect, new Point(0, 0));
		addElement(inPort, new Point(-width/2, 0));
		addElement(oPort, new Point(width/2, 0));
		addElement(text, new Point(0, 0));
		this.relativePositions.add(new Point(center));
		this.relativePositions.add(new Point(0, height/2));
		this.relativePositions.add(new Point(width, height/2));
		this.relativePositions.add(new Point(0, height/2));
		this.mainElementId = rect.getId();
	}

}
