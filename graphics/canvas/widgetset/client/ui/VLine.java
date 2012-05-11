package com.vaadin.graphics.canvas.widgetset.client.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.shared.EventHandler;
import com.vaadin.terminal.gwt.client.UIDL;

//@author chenopodium - croth@nobilitas.com
public class VLine extends VUIElement {
	
	private VPoint start;

	private VPoint end;
	
	EventHandler handler;

	private Map<String, List<EventHandler>> handlers = new HashMap<String, List<EventHandler>>();
	
	public VLine(UIDL uidl){
		this.setId(uidl.getStringAttribute("elementid"));
		this.setGroupId(uidl.getStringAttribute("groupid"));
		update(uidl);
	}
	
	public VLine(UIDL uidl, String id, String groupId){
		this.setId(id);
		this.setGroupId(groupId);
		update(uidl);
	}
	
	public VLine(VPoint start, VPoint end){
		this.start = start;
		this.end = end;
	}
	
	@Override
	public void draw(Context2d context) {
		super.draw(context);
		context.beginPath();
		context.moveTo(start.getX(), start.getY());
		context.lineTo(end.getX(), end.getY());
		if (description != null) context.fillText(description, (start.getX()+end.getX())/2, (start.getY()+end.getY())/2);
		context.stroke();
				
		context.restore();
		
		setChanged(false);
	}

	@Override
	protected void processMoveEvent(MouseMoveEvent event) {
		double deltaX = event.getClientX() - this.getMouseDownPoint().getX();
		double deltaY = event.getClientY() - this.getMouseDownPoint().getY();
		
		this.start.add(deltaX, deltaY);
		this.end.add(deltaX, deltaY);
		setChanged(true);
	}

	@Override
	public void moveTo(VPoint p) {
		VPoint delta = VPoint.sub(p, getCenter());
		moveBy(delta);
		setChanged(true);
	}

	@Override
	public VPoint getCenter() {
		return VPoint.mult(VPoint.add(start, end), 0.5);
	}

	@Override
	public boolean contains(VPoint p) {
		double squaredDistanceFromLineBorder = this.getBorderWidth() * this.getBorderWidth() / 4;
		double squaredDistanceFromSegment = VUIElement.squaredDistanceFromLine(p, this.start, this.end, true);
		
		return squaredDistanceFromSegment <= squaredDistanceFromLineBorder;
	}
	

	@Override
	public void update(UIDL uidl) {
		super.update(uidl);
		String prefix = getPrefix();
		
		double startX = uidl.getDoubleAttribute(prefix + "startx");
		double startY = uidl.getDoubleAttribute(prefix + "starty");
		double endX = uidl.getDoubleAttribute(prefix + "endx");
		double endY = uidl.getDoubleAttribute(prefix + "endy");
		
		
		setStart(new VPoint(startX, startY));
		setEnd(new VPoint(endX, endY));
		setChanged(true);
	}
	

	@Override
	public void moveBy(VPoint delta) {
		start = VPoint.add(start, delta);
		end = VPoint.add(end, delta);
		setChanged(true);
	}
	
	public VPoint getStart() {
		return start;
	}

	public void setStart(VPoint start) {
		this.start = start;
		setChanged(true);
	}

	public VPoint getEnd() {
		return end;
	}

	public void setEnd(VPoint end) {
		this.end = end;
		setChanged(true);
	}

}
