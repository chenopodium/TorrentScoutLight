package com.vaadin.graphics.canvas.widgetset.client.ui;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.Context2d.TextAlign;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.vaadin.terminal.gwt.client.UIDL;

public class VText extends VUIElement {
	
	private String text;
	private VPoint point;
	private double maxWidth;
	private String alignment;
	
	public VText(UIDL uidl) {
		this.setId(uidl.getStringAttribute("elementid"));
		this.setGroupId(uidl.getStringAttribute("groupid"));
		update(uidl);
	}
	
	public VText(UIDL uidl, VCanvas canvas) {
		this.setId(uidl.getStringAttribute("elementid"));
		this.setGroupId(uidl.getStringAttribute("groupid"));
		this.canvas = canvas;
		update(uidl);
	}

	public VText(UIDL uidl, String id, String groupId) {
		this.setId(id);
		this.setGroupId(groupId);
		update(uidl);
	}

	@Override
	protected void processMoveEvent(MouseMoveEvent event) {
		double deltaX = event.getClientX() - this.getMouseDownPoint().getX();
		double deltaY = event.getClientY() - this.getMouseDownPoint().getY();
		this.point.add(deltaX, deltaY);
		setChanged(true);
	}

	@Override
	public void moveTo(VPoint p) {
		this.point = p;
		setChanged(true);
	}

	@Override
	public VPoint getCenter() {
		return null;
	}

	@Override
	public void draw(Context2d context) {
		super.draw(context);
		context.beginPath();
		
		if(alignment != null && alignment.length() > 0){
			if(alignment.equals("LEFT")){
				context.setTextAlign(TextAlign.LEFT);
			}else if(alignment.equals("RIGHT")){
				context.setTextAlign(TextAlign.RIGHT);
			}else if(alignment.equals("START")){
				context.setTextAlign(TextAlign.START);
			}else if(alignment.equals("END")){
				context.setTextAlign(TextAlign.END);
			}else if(alignment.equals("CENTER")){
				context.setTextAlign(TextAlign.CENTER);
			}
		}
		
		if(maxWidth > 0){
			context.strokeText(text, point.getX(), point.getY(), maxWidth);
		}else{
			context.strokeText(text, point.getX(), point.getY());
		}
		context.closePath();
		
		if(getFillColor() != null && getFillColor().length() > 0){
			if(maxWidth > 0){
				context.fillText(text, point.getX(), point.getY(), maxWidth);
			}else{
				context.fillText(text, point.getX(), point.getY());
			}
		}
		
		context.restore();
		setChanged(true);
	}

	@Override
	public boolean contains(VPoint p) {
		return false;
	}

	@Override
	public void update(UIDL uidl) {
		super.update(uidl);
		String prefix = getPrefix();
		
		String text = uidl.getStringAttribute(prefix + "text");
		alignment = uidl.getStringAttribute(prefix + "alignment");
		double x = uidl.getDoubleAttribute(prefix + "x");
		double y = uidl.getDoubleAttribute(prefix + "y");
		double maxWidth = uidl.getDoubleAttribute(prefix + "maxwidth");
		
		
		setPoint(new VPoint(x, y));
		setText(text);
		setMaxWidth(maxWidth);
		setChanged(true);
	}

	public void setMaxWidth(double maxWidth) {
		this.maxWidth = maxWidth;
		setChanged(true);
	}

	public void setPoint(VPoint point) {
		this.point = point;
		setChanged(true);
	}

	public void setText(String text) {
		this.text = text;
		setChanged(true);
	}
	
	public String getText() {
		return this.text;
	}

	@Override
	public void moveBy(VPoint delta) {
		this.point = VPoint.add(this.point, delta);
		setChanged(true);
	}

}
