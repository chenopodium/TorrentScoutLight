package com.vaadin.graphics.canvas.shape;

import java.util.Map;

import com.vaadin.graphics.event.MouseEvent;
import com.vaadin.graphics.event.MouseEvent.Type;
import com.vaadin.graphics.event.listener.MouseEventListener;

public class Text extends UIElement {
	public static enum TextAlign{CENTER, END, START, LEFT, RIGHT};
	
	private TextAlign alignment;
	private String text;
	private Point point;
	private double maxWidth;

	public Text(String text, Point point, double maxWidth) {
		super();
		this.text = text;
		this.point = point;
		this.maxWidth = maxWidth;
		this.alignment = TextAlign.CENTER;
	}
	
	public Text(String text, Point point) {
		super();
		this.text = text;
		this.point = point;
		this.maxWidth = 0;
		this.alignment = TextAlign.CENTER;
	}

	public Point getPoint() {
		return point;
	}

	public void setPoint(Point point) {
		this.point = point;
	}

	public double getMaxWidth() {
		return maxWidth;
	}

	public void setMaxWidth(double maxWidth) {
		this.maxWidth = maxWidth;
	}

	@Override
	public void moveTo(Point p) {
		this.point = p;
	}
	
	public void moveBy(Point delta) {
		this.point = Point.add(point, delta);
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
		this.moveBy(p);
	}

	/**
	 * @return the text
	 */
	public String getText() {
		return text;
	}

	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		this.text = text;
	}
	
	public Map<String, Object> getDrawInstructions(){
		Map<String, Object> arguments = super.getDrawInstructions();
		arguments.put(getPrefix() + "groupid", getGroupId());
		arguments.put(getPrefix() + "elementid", getId());
		arguments.put(getPrefix() + "strokecolor", getColor());
		arguments.put(getPrefix() + "strokewidth", getBorderWidth());
		arguments.put(getPrefix() + "text", text);
		arguments.put(getPrefix() + "x", point.getX());
		arguments.put(getPrefix() + "y", point.getY());
		arguments.put(getPrefix() + "maxwidth", maxWidth);
		arguments.put(getPrefix() + "alignment", getAlignment());
		arguments.put(getPrefix() + "elementtype", "text");
		
		arguments.put(getPrefix() + "command", "draw");
		return arguments;
	}

	/**
	 * @return the alignment
	 */
	public TextAlign getAlignment() {
		return alignment;
	}

	/**
	 * @param alignment the alignment to set
	 */
	public void setAlignment(TextAlign alignment) {
		this.alignment = alignment;
	}

}
