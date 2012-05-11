package com.vaadin.graphics.canvas.shape;

import java.util.Map;


public abstract class Port extends Arc {

	public Port(double radius, Point centre) {
		super(radius, centre, 0, 2*Math.PI, false);
	}
	
	@Override
	public Map<String, Object> getDrawInstructions(){
		Map<String, Object> args = super.getDrawInstructions();
		args.put(getPrefix() + "elementtype", "port");
		return args;
	}
}
