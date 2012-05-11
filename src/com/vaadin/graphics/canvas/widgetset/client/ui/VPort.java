package com.vaadin.graphics.canvas.widgetset.client.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.vaadin.terminal.gwt.client.UIDL;

public class VPort extends VArc {
	
	List<VConnector> inConnectors = new ArrayList<VConnector>();
	List<VConnector> outConnectors = new ArrayList<VConnector>();
	
	VConnector connectionInProgress;

	public VPort(UIDL uidl) {
		super(uidl);
	}
	
	public VPort(UIDL uidl, String id, String groupId) {
		super(uidl, id, groupId);
	}

	public VPort(double radius, VPoint centre) {
		super(radius, centre, 0, 2*Math.PI, false);
	}
	
	@Override
	protected void initHandlers(){
		MouseMoveHandler moveHandler = new MouseMoveHandler(){
			@Override
			public void onMouseMove(MouseMoveEvent event) {
				VPoint p = new VPoint(event.getClientX() - VPort.this.canvas.getAbsoluteLeft(), event.getClientY() - VPort.this.canvas.getAbsoluteTop());
				if(VPort.this.contains(p)){
					if("IOPORT".equals(VPort.this.getRole()) || "IPORT".equals(VPort.this.getRole())){
						VPort port = VPort.this;
						if(port.canvas.connectionStartPort != port){
							port.highlightConnectionEvent(event);
						}
					}
				}

				if("IOPORT".equals(VPort.this.getRole()) || "OPORT".equals(VPort.this.getRole()) && VPort.this.isSelected() && VPort.this.connectionInitiated){
					VPort port = VPort.this;
					port.updateConnectorEvent(event);
				}
			}
		};
		
		canvas.addMouseEventHandler(moveHandler, MouseMoveEvent.getType());
		
		MouseDownHandler downHandler = new MouseDownHandler(){
			
			@Override
			public void onMouseDown(MouseDownEvent event){
				VPoint p = new VPoint(event.getClientX() - VPort.this.canvas.getAbsoluteLeft(), event.getClientY() - VPort.this.canvas.getAbsoluteTop());
				if(VPort.this.contains(p)){
					if("IOPORT".equals(VPort.this.getRole()) || "OPORT".equals(VPort.this.getRole())){
						VPort.this.initiateConnectionEvent(event);
					}
					
					if("IOPORT".equals(VPort.this.getRole()) || "IPORT".equals(VPort.this.getRole())){
						
					}
				}
			}
		};
		
		canvas.addMouseEventHandler(downHandler, MouseDownEvent.getType());
		
		MouseUpHandler upHandler = new MouseUpHandler() {
			
			@Override
			public void onMouseUp(MouseUpEvent event) {
				VPoint p = new VPoint(event.getClientX() - VPort.this.canvas.getAbsoluteLeft(), event.getClientY() - VPort.this.canvas.getAbsoluteTop());
				
				if("IOPORT".equals(VPort.this.getRole()) || "OPORT".equals(VPort.this.getRole())){
					VPort port = VPort.this;
					if(port.canvas.connectionEndPort == null && port.canvas.connectionStartPort == port){
						List<VUIElement> elements = port.canvas.elementsUnderPoint(p);
						for(VUIElement element: elements){
							if(element.getRole().equals("IPORT") || (element.getRole().equals("IOPORT") && element != port)){
								return;
							}
						}
						port.abortConnectionEvent(event);
					}
				}
				
				if("IOPORT".equals(VPort.this.getRole()) || "IPORT".equals(VPort.this.getRole())){
					VPort port = VPort.this;
					if(port.canvas.connectionStartPort != null && port.contains(p)){
						port.canvas.connectionEndPort = port;
						port.finalizeConnectionEvent(event);
					}
				}
			}
		};
		
		canvas.addMouseEventHandler(upHandler, MouseUpEvent.getType());
		
		MouseOutHandler outHandler = new MouseOutHandler() {
			
			@Override
			public void onMouseOut(MouseOutEvent event) {
				if("IOPORT".equals(VPort.this.getRole()) || "OPORT".equals(VPort.this.getRole())){
					if(VPort.this.canvas.connectionStartPort == VPort.this){
						VPort.this.setSelected(true);
					}
				}
				
				if("IOPORT".equals(VPort.this.getRole()) || "IPORT".equals(VPort.this.getRole())){
					if(VPort.this.canvas.connectionStartPort != null){
						VPort.this.setSelected(false);
						VPort.this.canvas.connectionEndPort = null;
					}
				}
			}
		};
		
		canvas.addMouseEventHandler(outHandler, MouseOutEvent.getType());
	}

	protected void initiateConnectionEvent(MouseDownEvent event) {
		this.setSelected(true);
		VPoint start = getCenter();
		VPoint end = new VPoint(event.getClientX() - 
				this.canvas.getAbsoluteLeft(), event.getClientY() - this.canvas.getAbsoluteTop());
		
		VConnector connector = new VConnector(start, end);
		connector.setColor(this.getColor());
		connector.setBorderWidth(this.getBorderWidth());
		connector.setHighlightedColor(this.getHighlightedColor());
		connector.setFromPort(this);
		this.canvas.addChild(connector);
		this.connectionInProgress = connector;
		this.canvas.connectionStartPort = this;
		this.connectionInitiated = true;
	}
	
	protected void highlightConnectionEvent(MouseMoveEvent event) {
		for(VConnector connector: outConnectors){
			connector.setHighlighted(true);
		}
	}
	
	protected void lowlightConnectionEvent(MouseMoveEvent event){
		for(VConnector connector: outConnectors){
			connector.setHighlighted(false);
		}
	}
	
	protected void finalizeConnectionEvent(MouseUpEvent event) {
		this.connectionInProgress = this.canvas.connectionStartPort.connectionInProgress;

		this.canvas.connectionStartPort.outConnectors.add(this.connectionInProgress);
		this.connectionInProgress.setToPort(this);
		this.inConnectors.add(this.connectionInProgress);
		
		this.canvas.connectionStartPort.connectionInProgress = null;
		this.canvas.connectionStartPort.connectionInitiated = false;
		this.canvas.connectionStartPort = null;
		this.canvas.connectionEndPort = null;
		this.connectionInProgress = null;
	}
	
	protected void updateConnectorEvent(MouseMoveEvent event) {
		this.connectionInProgress.setEnd(new VPoint(event.getX(), event.getY()));
	}
	
	protected void abortConnectionEvent(MouseUpEvent event) {
		this.canvas.removeChild(this.connectionInProgress);
		this.canvas.connectionStartPort = null;
		this.connectionInitiated = false;
		this.connectionInProgress = null;
	}
	
	@Override
	protected void processMoveEvent(MouseMoveEvent event) {
		super.processMoveEvent(event);
		for(VConnector connector: inConnectors){
			connector.setEnd(this.getCenter());
		}
		
		for(VConnector connector: outConnectors){
			connector.setStart(this.getCenter());
		}
	}
	
	@Override
	public void moveBy(VPoint delta){
		super.moveBy(delta);
		for(VConnector connector: inConnectors){
			connector.setEnd(this.getCenter());
		}
		
		for(VConnector connector: outConnectors){
			connector.setStart(this.getCenter());
		}
	}

	List<VConnector> getOutConnectors(){
		return this.outConnectors;
	}
	
	List<VConnector> getInConnectors(){
		return this.inConnectors;
	}
}
