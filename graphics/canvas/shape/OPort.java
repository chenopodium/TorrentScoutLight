package com.vaadin.graphics.canvas.shape;

public class OPort extends Port {

	public OPort(double radius, Point centre) {
		super(radius, centre);
		this.setRole(ElementRole.OPORT);
	}

}
