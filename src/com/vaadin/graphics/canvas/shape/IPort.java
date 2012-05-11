package com.vaadin.graphics.canvas.shape;

public class IPort extends Port {

	public IPort(double radius, Point centre) {
		super(radius, centre);
		this.setRole(ElementRole.IPORT);
	}

}
