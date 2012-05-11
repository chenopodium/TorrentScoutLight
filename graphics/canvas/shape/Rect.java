/**
 * 
 */
package com.vaadin.graphics.canvas.shape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.graphics.event.MouseEvent;
import com.vaadin.graphics.event.MouseEvent.Type;
import com.vaadin.graphics.event.listener.MouseEventListener;

/**
 * @author kapil - kapil.verma@globallogic.com
 *
 */
public class Rect extends UIElement {

	private Point start;

	private Point end;
	
	MouseEventListener listener;
	
	private Map<MouseEvent.Type, List<MouseEventListener>> listeners = new HashMap<MouseEvent.Type, List<MouseEventListener>>();
	
	
	public Rect(Point start, Point end){
		
		this.start = start;
		this.end = end;
		this.setSelected(false);
		this.setBorderWidth(-1);
		this.setFillColor("");
		this.setColor("");
		
		listener = new MouseEventListener() {
			
			Point downPoint;
			Point upPoint;
			
			public void onMouseEvent(MouseEvent event) {
				Rect source = (Rect)event.getSource();
				
				if(event.getType() == MouseEvent.Type.DOWN){
					downPoint = event.getPoint();
					source.setPressed(true);
				}else if(event.getType() == MouseEvent.Type.UP){
					upPoint = event.getPoint();
					source.setSelected(true);
					source.setPressed(false);
				}else if(event.getType() == MouseEvent.Type.MOVE){
					if(source.isPressed()){
						Point p = event.getPoint();
						
						Point delta = Point.sub(p, downPoint);
						
						source.start.add(delta);
						
						source.end.add(delta);
						
						downPoint = p;
//						source.draw();
					}
				}else{
					System.err.println("Unknown event type: " + event.getType());
				}
			}

		};
		
		List<MouseEventListener> upListeners = new ArrayList<MouseEventListener>();
		upListeners.add(listener);
		listeners.put(Type.UP, upListeners);
		
		List<MouseEventListener> downListeners = new ArrayList<MouseEventListener>();
		downListeners.add(listener);
		listeners.put(Type.DOWN, downListeners);
		
		List<MouseEventListener> moveListeners = new ArrayList<MouseEventListener>();
		moveListeners.add(listener);
		listeners.put(Type.MOVE, moveListeners);
		
	}
	
	/* (non-Javadoc)
	 * @see com.workflow.ivr.web.model.UIElement#draw()
	 */
	public Map<String, Object> getDrawInstructions() {

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
		arguments.put(getPrefix() + "elementtype", "rect");
		
		arguments.put(getPrefix() + "command", "draw");
		
		return arguments;
		
	}

	/* (non-Javadoc)
	 * @see com.workflow.ivr.web.model.UIElement#getNext()
	 */
	/* (non-Javadoc)
	 * @see com.workflow.ivr.web.model.UIElement#getCenterX()
	 */
	public Point getCenter() {
		return Point.mult(Point.add(start, end), 0.5);
	}

	/* (non-Javadoc)
	 * @see com.workflow.ivr.web.model.UIElement#moveTo(double, double)
	 */
	public void moveTo(Point p) {
		
	}
	
	public void add(Point p){
		start.add(p);
		end.add(p);
	}
	
	/* (non-Javadoc)
	 * @see com.ui.model.UIElement#contains(double, double)
	 */
	public boolean contains(Point p) {
		return start.getX() <= p.getX() && p.getX() <= end.getX() && start.getY() <= p.getY() && p.getY() <= end.getY();
	}
	/* (non-Javadoc)
	 * @see com.ui.model.UIElement#addListener(com.vaadin.ui.Component.Listener)
	 */
	public void addListener(MouseEventListener listener, MouseEvent.Type eventType) {
		
	}
	/* (non-Javadoc)
	 * @see com.ui.model.UIElement#fireMouseEvent(com.vaadin.event.MouseEvents)
	 */
	public void fireMouseEvent(MouseEvent event) {
		Type type = event.getType();
		
		List<MouseEventListener> listernerList = this.listeners.get(type);
		for(MouseEventListener listener : listernerList){
			listener.onMouseEvent(event);
		}
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
