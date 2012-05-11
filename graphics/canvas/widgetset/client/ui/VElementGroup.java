package com.vaadin.graphics.canvas.widgetset.client.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.vaadin.terminal.gwt.client.UIDL;

public class VElementGroup extends VUIElement {

	private String[] elementList;
	
	private Map <String, VUIElement> elements;
	
	private String mainElementId;
	VPoint center;

	public VElementGroup(){
		super();
		elements = new HashMap<String, VUIElement>();
	}
	
	public VElementGroup(UIDL uidl, VCanvas canvas){
		elements = new HashMap<String, VUIElement>();
		this.setId(uidl.getStringAttribute("elementid"));
		this.setGroupId(uidl.getStringAttribute("groupId"));
		this.canvas = canvas;
		this.update(uidl);
	}
	
	public VElementGroup(UIDL uidl, String id, String groupId, VCanvas canvas) {
		elements = new HashMap<String, VUIElement>();
		this.setId(id);
		this.setGroupId(groupId);
		this.canvas = canvas;
		this.update(uidl);
	}

	@Override
	public void moveTo(VPoint p) {
		setChanged(true);
	}

	@Override
	public VPoint getCenter() {
		return center;
	}

	@Override
	public void draw(Context2d context) {
		for(String elementId : elementList){
			VUIElement elem = elements.get(elementId);
			elem.draw(context);
		}
		setChanged(false);
	}

	@Override
	public boolean contains(VPoint p) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	protected void initHandlers(){

		MouseMoveHandler moveHandler = new MouseMoveHandler(){

			@Override
			public void onMouseMove(MouseMoveEvent event) {
				VElementGroup.this.processMoveEvent(event);
			}
			
		};
		
		canvas.addMouseEventHandler(moveHandler, MouseMoveEvent.getType());
		
		MouseDownHandler downHandler = new MouseDownHandler() {
			
			@Override
			public void onMouseDown(MouseDownEvent event) {
				VElementGroup.this.processMouseDownEvent(event);
			}
		};
		
		canvas.addMouseEventHandler(downHandler, MouseDownEvent.getType());
		
		MouseUpHandler upHandler = new MouseUpHandler() {
			
			@Override
			public void onMouseUp(MouseUpEvent event) {
				VElementGroup.this.processMouseUpEvent(event);
			}
		};
		
		canvas.addMouseEventHandler(upHandler, MouseUpEvent.getType());
		
		MouseOverHandler overHandler = new MouseOverHandler() {
			
			@Override
			public void onMouseOver(MouseOverEvent event) {
				VElementGroup.this.processMouseOverEvent(event);
			}
		};
		
		canvas.addMouseEventHandler(overHandler, MouseOverEvent.getType());
		
		MouseOutHandler outHandler = new MouseOutHandler() {
			
			@Override
			public void onMouseOut(MouseOutEvent event) {
				VElementGroup.this.processMouseOutEvent(event);
			}
		};
		
		canvas.addMouseEventHandler(outHandler, MouseOutEvent.getType());
	
	}
	
	@Override
	public List<VUIElement> elementsUnderPoint(VPoint p){
		List<VUIElement> list = new ArrayList<VUIElement>();
		for(String elementId: elementList){
			VUIElement element = elements.get(elementId);
			if(element.contains(p)){
				list.add(element);
			}
		}
		
		return list;
	}
	
	@Override
	public void setHighlighted(boolean highlighted) {
		super.setHighlighted(highlighted);
		
		for(String elementId : elementList){
			elements.get(elementId).setHighlighted(highlighted);
		}
		setChanged(true);
	}
	
	@Override
	public void setSelected(boolean highlighted) {
		super.setSelected(highlighted);
		
		for(String elementId : elementList){
			elements.get(elementId).setSelected(highlighted);
		}
		setChanged(true);
	}

	@Override
	public void update(UIDL uidl) {
		this.elementList = uidl.getStringArrayAttribute(getPrefix() + "elementlist");
		this.mainElementId = uidl.getStringAttribute(getPrefix() + "mainelementid");
		this.setRole(uidl.getStringAttribute(getPrefix() + "role"));
		
		for(String elementId : elementList){
			if(elements.get(elementId) != null){
				VUIElement elem = elements.get(elementId);
				elem.update(uidl);
			}else{
				VUIElement elem = VUIElement.createFromUIDL(uidl, elementId, getId(), this.canvas);
				elements.put(elementId, elem);
			}
		}
		this.center = elements.get(mainElementId).getCenter();
		setChanged(true);
	}

	public void processMouseOutEvent(MouseOutEvent event) {
//		VPoint p = new VPoint(event.getClientX() - this.canvas.getAbsoluteLeft(), event.getClientY() - this.canvas.getAbsoluteTop());
//		VUIElement mainElement = this.elements.get(this.mainElementId);
//		if(mainElement.contains(p)){
			this.setHighlighted(false);
			this.setMouseOutPoint(new VPoint(event.getClientX(), event.getClientY()));
//		}
		setChanged(true);
		
	}

	public void processMouseOverEvent(MouseOverEvent event) {
//		VPoint p = new VPoint(event.getClientX() - this.canvas.getAbsoluteLeft(), event.getClientY() - this.canvas.getAbsoluteTop());
//		VUIElement mainElement = this.elements.get(this.mainElementId);
//		if(mainElement.contains(p)){
			this.setHighlighted(true);
			this.setMouseOverPoint(new VPoint(event.getClientX(), event.getClientY()));
//		}
		setChanged(true);
	}

	public void processMouseUpEvent(MouseUpEvent event) {
//		VPoint p = new VPoint(event.getClientX() - this.canvas.getAbsoluteLeft(), event.getClientY() - this.canvas.getAbsoluteTop());
//		VUIElement mainElement = this.elements.get(this.mainElementId);
//		if(mainElement.contains(p)){
			this.setSelected(false);
			this.setMouseUpPoint(new VPoint(event.getClientX(), event.getClientY()));
//		}
		setChanged(true);
	}

	public void processMouseDownEvent(MouseDownEvent event) {
		VPoint p = new VPoint(event.getClientX() - this.canvas.getAbsoluteLeft(), event.getClientY() - this.canvas.getAbsoluteTop());
		VUIElement mainElement = this.elements.get(this.mainElementId);
		if(mainElement.contains(p)){
			this.setSelected(true);
			this.setMouseDownPoint(new VPoint(event.getClientX(), event.getClientY()));
		}
		setChanged(true);
	}
	
	public void moveBy(VPoint delta){
		for(String elementId : elementList){
			VUIElement elem = elements.get(elementId);
			elem.moveBy(delta);
		}
		setChanged(true);
	}
	
	@Override
	protected void processMoveEvent(MouseMoveEvent event) {
//		VUIElement mainElement = this.elements.get(this.mainElementId);
//		VPoint p = new VPoint(event.getClientX() - mainElement.canvas.getAbsoluteLeft(), event.getClientY() 
//				- mainElement.canvas.getAbsoluteTop());
//		if(mainElement.contains(p)){
			if(this.isSelected()){
				double deltaX = event.getClientX() - this.getMouseDownPoint().getX();
				double deltaY = event.getClientY() - this.getMouseDownPoint().getY();
				VPoint delta = new VPoint(deltaX, deltaY);
				moveBy(delta);
			}
			this.mouseDownPoint = new VPoint(event.getClientX(), event.getClientY());
//		}
		setChanged(true);
	}
	
	public String getSelectedColor() {
		return super.getSelectedColor();
	}

	public void setSelectedColor(String selectedColor) {
		super.setSelectedColor(selectedColor);
		for(String elementId : elementList){
			VUIElement elem = elements.get(elementId);
			elem.setSelectedColor(selectedColor);
		}
		setChanged(true);
	}

	public String getSelectedFillColor() {
		return super.getSelectedFillColor();
	}

	public void setSelectedFillColor(String selectedFillColor) {
		super.setSelectedFillColor(selectedFillColor);
		for(String elementId : elementList){
			VUIElement elem = elements.get(elementId);
			elem.setSelectedFillColor(selectedFillColor);
		}
		setChanged(true);
	}

	public String getHighlightedColor() {
		return super.getHighlightedColor();
	}

	public void setHighlightedColor(String highlightedColor) {
		super.setHighlightedFillColor(highlightedColor);
		for(String elementId : elementList){
			VUIElement elem = elements.get(elementId);
			elem.setHighlightedColor(highlightedColor);
		}
		setChanged(true);
	}

	public String getHighlightedFillColor() {
		return super.getHighlightedFillColor();
	}

	public void setHighlightedFillColor(String highlightedFillColor) {
		super.setHighlightedFillColor(highlightedFillColor);
		for(String elementId : elementList){
			VUIElement elem = elements.get(elementId);
			elem.setHighlightedFillColor(highlightedFillColor);
		}
		setChanged(true);
	}

}
