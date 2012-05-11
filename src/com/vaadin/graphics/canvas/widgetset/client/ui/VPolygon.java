/**
 * 
 */
package com.vaadin.graphics.canvas.widgetset.client.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.shared.EventHandler;
import com.vaadin.terminal.gwt.client.UIDL;

/**
 * @author kapil - kapil.verma@globallogic.com
 * @author chenopodium - croth@nobilitas.com
 *
 */
public class VPolygon extends VUIElement {

	private VPoint[] vertices;

	EventHandler handler;
//	
	private Map<String, List<EventHandler>> handlers = new HashMap<String, List<EventHandler>>();
//	
	
	public VPolygon(UIDL uidl){
		this.setId(uidl.getStringAttribute("elementid"));
		this.setGroupId(uidl.getStringAttribute("groupid"));
		update(uidl);
	}
	
	public VPolygon(VPoint[] vertices){
		this.vertices = vertices;
		
	}
	
	/*public interface MouseEventListener{
		public void onMouseEvent(MouseEvent event);
	}*/
	
	public VPolygon(UIDL uidl, String id, String groupId) {
		this.setId(id);
		this.setGroupId(groupId);
		update(uidl);
	}

	/* (non-Javadoc)
	 * @see com.workflow.ivr.web.model.VUIElement#draw()
	 */
	public void draw(Context2d context) {
		super.draw(context);
		context.beginPath();
		context.moveTo(vertices[0].getX(), vertices[0].getY());
		int i=1;
		for(;i<vertices.length ; i++){
			context.lineTo(vertices[i].getX(), vertices[i].getY());
		}
		
		context.closePath();
		context.stroke();
		
		if(getFillColor().length() > 0){
			context.fill();
		}
		VPoint center = getCenter();
		if (description != null && !description.equalsIgnoreCase("null")) {
			context.fillText(description, center.getX()+5, center.getY()+5);
		}
		
		context.restore();
		setChanged(false);
	}

	/* (non-Javadoc)
	 * @see com.workflow.ivr.web.model.VUIElement#getCenterX()
	 */
	public VPoint getCenter() {
		VPoint sum = new VPoint(0, 0);
		for(int i = 0; i < vertices.length; i++){
			sum = VPoint.add(sum, vertices[i]);
		}
		return VPoint.mult(sum,(1.0/(double)vertices.length));
	}

	/* (non-Javadoc)
	 * @see com.workflow.ivr.web.model.VUIElement#moveTo(double, double)
	 */
	public void moveTo(VPoint p) {
		VPoint delta = new VPoint(p.getX() - vertices[0].getX(),p.getY() - vertices[0].getY());
		moveBy(delta);
		setChanged(true);
	}
	
	public void moveBy(VPoint delta) {
		if (this.isLocky() && delta.getY() != 0) {
			delta.setY(0);
		}
		for(int i = 0; i < vertices.length; i++){
			vertices[i] = VPoint.add(vertices[i], delta);
		}
		setChanged(true);
	}

	/* (non-Javadoc)
	 * @see com.ui.model.VUIElement#contains(double, double)
	 */
	public boolean contains(VPoint p) {
		return VUIElement.pointInPolygon(vertices, p);
	}
	/* (non-Javadoc)
	 * @see com.ui.model.VUIElement#addListener(com.vaadin.ui.Component.Listener)
	 */
//	public void addListener(MouseEventListener listener, MouseEvent.Type eventType) {
//		
//	}
	/* (non-Javadoc)
	 * @see com.ui.model.VUIElement#fireMouseEvent(com.vaadin.event.MouseEvents)
	 */
//	public void fireEvent(MouseEvent<EventHandler> event) {
//		
//	}


	/* (non-Javadoc)
	 * @see com.vaadin.graphics.canvas.widgetset.client.ui.VUIElement#update(com.google.gwt.canvas.dom.client.Context2d, com.vaadin.terminal.gwt.client.UIDL)
	 */
	@Override
	public void update(UIDL uidl) {
		super.update(uidl);
		String prefix = getPrefix();
		
		int numOfVertices = uidl.getIntAttribute(prefix + "numberofvertices");
		
		this.vertices = new VPoint[numOfVertices];
		for(int i=0; i< numOfVertices; i++){
			this.vertices[i] = new VPoint(uidl.getDoubleAttribute(prefix + "x" + i), uidl.getDoubleAttribute(prefix + "y" + i));
		}
				
		setChanged(true);
	}
	
	protected void processMoveEvent(MouseMoveEvent event){
		double deltaX = event.getClientX() - this.getMouseDownPoint().getX();
		double deltaY = event.getClientY() - this.getMouseDownPoint().getY();
		
		if (this.isLocky()) deltaY = 0;
		for(VPoint p: vertices){
			p.add(deltaX, deltaY);
		}
		setChanged(true);
	}

}
