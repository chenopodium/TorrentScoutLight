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
 * @author chenopodium - croth@nobilitas.com
 *
 */
public class Polygon extends UIElement {

	private Point[] vertices;
	
	MouseEventListener listener;
	
	private Map<MouseEvent.Type, List<MouseEventListener>> listeners = new HashMap<MouseEvent.Type, List<MouseEventListener>>();
	
	public Polygon(Point[] vertices){
		
		this.setVertices(vertices);
		this.setSelected(false);
		this.setBorderWidth(-1);
		this.setFillColor("");
		this.setColor("");
		
		listener = new MouseEventListener() {
			
			Point downPoint;
			Point upPoint;
			
			public void onMouseEvent(MouseEvent event) {
				Polygon source = (Polygon)event.getSource();
				
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
						
						for(Point vertex : Polygon.this.getVertices()){
							vertex.add(delta);
						}
						
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
		
		arguments.put(getPrefix() + "elementid", getId());
		arguments.put(getPrefix() + "groupid", getGroupId());
		arguments.put(getPrefix() + "strokecolor", getColor());
		arguments.put(getPrefix() + "strokewidth", getBorderWidth());
		arguments.put(getPrefix() + "numberofvertices", getVertices().length);
		
		for(int i=0; i< getVertices().length; i++){
			Point p = getVertices()[i];
			arguments.put(getPrefix() + "x" + i, p.getX());
			arguments.put(getPrefix() + "y" + i, p.getY());
		}
		
		arguments.put(getPrefix() + "fillstyle", getFillColor());
		arguments.put(getPrefix() + "elementtype", "polygon");
		
		arguments.put(getPrefix() + "command", "draw");
		
		return arguments;
		
	}


	/* (non-Javadoc)
	 * @see com.workflow.ivr.web.model.UIElement#getCenterX()
	 */
	public Point getCenter() {
		Point sum = new Point(0, 0);
		for(int i = 0; i < getVertices().length; i++){
			sum = Point.add(sum, getVertices()[i]);
		}
		return Point.mult(sum, (double)(1.0/getVertices().length));
	}

	/* (non-Javadoc)
	 * @see com.workflow.ivr.web.model.UIElement#moveTo(double, double)
	 */
	public void moveTo(Point p) {
		
	}
	
	public void add(Point p){
		for (Point vertex : getVertices()){
			vertex.add(p);
		}
	}

	/* (non-Javadoc)
	 * @see com.ui.model.UIElement#contains(double, double)
	 */
	public boolean contains(Point p) {
		return UIElement.pointInPolygon(getVertices(), p);
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

	private Point[] getVertices() {
		return vertices;
	}

	protected void setVertices(Point[] vertices) {
		this.vertices = vertices;
	}

}
