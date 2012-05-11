package com.vaadin.graphics.canvas.shape;

public class IOPort extends Port {

	public IOPort(double radius, Point centre) {
		super(radius, centre);
		this.setRole(ElementRole.IOPORT);
	}

}
