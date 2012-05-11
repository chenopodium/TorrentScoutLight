package com.vaadin.graphics.canvas.shape;

import java.util.Map;

import com.vaadin.graphics.event.MouseEvent;
import com.vaadin.graphics.event.MouseEvent.Type;
import com.vaadin.graphics.event.listener.MouseEventListener;

public class Line extends UIElement {
	
	Point start;
	Point end;

	public Line(Point start, Point end) {
		super();
		this.start = start;
		this.end = end;
	}

	@Override
	public void moveTo(Point p) {
		// TODO Auto-generated method stub

	}

	@Override
	public Point getCenter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean contains(Point p) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addListener(MouseEventListener listener, Type eventType) {
		// TODO Auto-generated method stub

	}

	@Override
	public void fireMouseEvent(MouseEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void add(Point p) {
		// TODO Auto-generated method stub

	}
	
	public Map<String, Object> getDrawInstructions(){
		Map<String, Object> arguments = super.getDrawInstructions();
		
		arguments.put(getPrefix() + "groupid", getGroupId());
		arguments.put(getPrefix() + "elementid", getId());
		arguments.put(getPrefix() + "strokecolor", getColor());
		arguments.put(getPrefix() + "strokewidth", getBorderWidth());
		arguments.put(getPrefix() + "startx", getStart().getX());
		arguments.put(getPrefix() + "starty", getStart().getY());
		arguments.put(getPrefix() + "endx", getEnd().getX());
		arguments.put(getPrefix() + "endy", getEnd().getY());
		
		arguments.put(getPrefix() + "fillstyle", getFillColor());
		arguments.put(getPrefix() + "elementtype", "line");
		
		arguments.put(getPrefix() + "command", "draw");
		return arguments;
	}

	public Point getStart() {
		return start;
	}

	public void setStart(Point start) {
		this.start = start;
	}

	public Point getEnd() {
		return end;
	}

	public void setEnd(Point end) {
		this.end = end;
	}
}
