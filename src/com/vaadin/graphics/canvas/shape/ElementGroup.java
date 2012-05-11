package com.vaadin.graphics.canvas.shape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.graphics.event.MouseEvent;
import com.vaadin.graphics.event.MouseEvent.Type;
import com.vaadin.graphics.event.listener.MouseEventListener;

public abstract class ElementGroup extends UIElement {

	List<UIElement> elements;
	private List<String> elementIds = new ArrayList<String>();
	String mainElementId;
	List<Point> relativePositions;
	private Map<MouseEvent.Type, List<MouseEventListener>> listeners = new HashMap<MouseEvent.Type, List<MouseEventListener>>();

	Point center;
	
	public ElementGroup(){
		elements = new ArrayList<UIElement>();
		relativePositions = new ArrayList<Point>();
	}
	
	@Override
	public void moveTo(Point p) {
		center = p;
		for(int i=0; i<elements.size(); i++){
			UIElement element = elements.get(i);
			element.moveTo(Point.add(center, relativePositions.get(i)));
		}
	}

	@Override
	public Point getCenter() {
		return center;
	}

	@Override
	public boolean contains(Point p) {
		for(UIElement element : elements){
			if(element.contains(p))
				return true;
		}
		return false;
	}

	@Override
	public void addListener(MouseEventListener listener, Type eventType) {
		List<MouseEventListener> listenerList;
		if(!this.listeners.containsKey(eventType)){
			listenerList = new ArrayList<MouseEventListener>();
			this.listeners.put(eventType, listenerList);
		}else{
			listenerList = this.listeners.get(eventType);
		}
		listenerList.add(listener);

	}

	@Override
	public void fireMouseEvent(MouseEvent event) {
		Type type = event.getType();
		
		List<MouseEventListener> listernerList = this.listeners.get(type);
		for(MouseEventListener listener : listernerList){
			listener.onMouseEvent(event);
		}

	}
	
	public void add(Point p){
		for(UIElement element : elements){
			element.add(p);
		}
	}
	
	public void addElement(UIElement element, Point p){
		element.add(p);
		element.add(center);
		element.setGroupId(getId());
		elements.add(element);
		relativePositions.add(p);
		elementIds.add(element.getId());
	}
	
	public String[] getElements(){
		return elementIds.toArray(new String[]{});
	}
	
	public void setFillColor(String fillColor){
		if(fillColor != null){
			super.setFillColor(fillColor);
			for(UIElement element : elements){
				element.setFillColor(fillColor);
			}
		}
	}
	
	public void setColor(String color){
		if(color != null){
			super.setColor(color);
			for(UIElement element : elements){
				element.setColor(color);
			}
		}
	}
	
	
	/**
	 * @param selectedColor the selectedColor to set
	 */
	public void setSelectedColor(String selectedColor) {
		if(selectedColor != null){
			super.setSelectedColor(selectedColor);
			for(UIElement element : elements){
				element.setSelectedColor(selectedColor);
			}
		}
	}

	/**
	 * @param selectedFillColor the selectedFillColor to set
	 */
	public void setSelectedFillColor(String selectedFillColor) {
		if(selectedFillColor != null){
			super.setSelectedFillColor(selectedFillColor);
			for(UIElement element : elements){
				element.setSelectedFillColor(selectedFillColor);
			}
		}
	}

	/**
	 * @param highlighted the highlighted to set
	 */
	public void setHighlighted(boolean highlighted) {
		super.setHighlighted(highlighted);
		for(UIElement element : elements){
			element.setHighlighted(highlighted);
		}
	}
	
	public void setSelected(boolean selected) {
		super.setSelected(selected);
		for(UIElement element : elements){
			element.setSelected(selected);
		}
	}
	
	public void setPressed(boolean pressed) {
		super.setPressed(pressed);
		for(UIElement element : elements){
			element.setPressed(pressed);
		}
	}
	
	@Override
	public Map<String, Object> getDrawInstructions() {
		Map<String, Object> drawInstructions = new HashMap<String, Object>();
		for(UIElement element : elements){
			drawInstructions.putAll(element.getDrawInstructions());
		}
		
		drawInstructions.put("elementid", getId());
		drawInstructions.put("groupid", getGroupId());
		drawInstructions.put(getPrefix() + "elementlist", getElements());
		drawInstructions.put(getPrefix() + "elementtype", "group");
		if(mainElementId != null){
			drawInstructions.put(getPrefix() + "mainelementid", mainElementId);
		}
		return drawInstructions;
	}
	
}
