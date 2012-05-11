/**
 * 
 */
package com.vaadin.graphics.canvas.widgetset.client.ui;

import java.util.ArrayList;
import java.util.List;

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
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HasHandlers;
import com.vaadin.terminal.gwt.client.UIDL;

/**
 * @author kapil - kapil.verma@globallogic.com
 * @author chenopodium - croth@nobilitas.com
 *
 */
abstract class VUIElement implements HasHandlers{
	
	private String role;
	protected VCanvas canvas;

	private String id;
	protected String groupId = "";
	private boolean locky;
	
	private VUIElement next;
	private VUIElement prev;
	VPoint mouseDownPoint;
	VPoint mouseUpPoint;
	VPoint mouseOverPoint;
	VPoint mouseOutPoint;
	
	private boolean selected = false;
	private boolean highlighted = false;
	private boolean changed = true;
	private boolean pressed;
	private String color = "";
	private String fillColor = "";
	private String selectedColor = "";
	private String selectedFillColor = "";
	private String highlightedColor = "";
	private String highlightedFillColor = "";
	private int borderWidth = -1;
	
	protected String description;
	private HandlerManager handlerManager;
	protected boolean connectionInitiated;
	
	public String toString() {
		return getClass().getName()+": selected="+selected+", center="+this.getCenter();
	}
	public static VUIElement createFromUIDL(UIDL uidl, VCanvas canvas){
		VUIElement ele = null;
		
		String id = uidl.getStringAttribute("elementid");
		
		String elementType = uidl.getStringAttribute(id + ".elementtype");
		if(elementType.equals("rect")){
			ele = new VRect(uidl);
		}else if(elementType.equals("polygon")){
			ele = new VPolygon(uidl);
		}else if(elementType.equals("arc")){
			ele = new VArc(uidl);
		}else if(elementType.equals("port")){
			ele = new VPort(uidl);
		}else if(elementType.equals("group")){
			ele = new VElementGroup(uidl, canvas);
		}else if(elementType.equals("line")){
			ele = new VLine(uidl);
		}else if(elementType.equals("text")){
			ele = new VText(uidl, canvas);
		}
		
		ele.canvas = canvas;
		ele.initHandlers();
		
		return ele;
	}
	
	public static VUIElement createFromUIDL(UIDL uidl, String id, String groupId, VCanvas canvas){
		VUIElement ele = null;
		
		String elementType = uidl.getStringAttribute(id + ".elementtype");
		if(elementType.equals("rect")){
			ele = new VRect(uidl, id, groupId);
		}else if(elementType.equals("polygon")){
			ele = new VPolygon(uidl, id, groupId);
		}else if(elementType.equals("arc")){
			ele = new VArc(uidl, id, groupId);
		}else if(elementType.equals("port")){
			ele = new VPort(uidl, id, groupId);
		}else if(elementType.equals("text")){
			ele = new VText(uidl, id, groupId);
		}else if(elementType.equals("line")){
			ele = new VLine(uidl, id, groupId);
		}else if(elementType.equals("connector")){
			ele = new VConnector(uidl, id, groupId);
		}else if(elementType.equals("group")){
			ele = new VElementGroup(uidl, id, groupId, canvas);
		}
		
		ele.canvas = canvas;
		
		if(ele.groupId == null || ele.groupId.length() == 0 || elementType.equals("port")){
			ele.initHandlers();
		}
		
		return ele;
	}
	
	protected void initHandlers(){
		MouseMoveHandler moveHandler = new MouseMoveHandler(){

			@Override
			public void onMouseMove(MouseMoveEvent event) {
//				VPoint p = new VPoint(event.getClientX() - VUIElement.this.canvas.getAbsoluteLeft(), event.getClientY() - VUIElement.this.canvas.getAbsoluteTop());
//				if(VUIElement.this.contains(p)){
					if(VUIElement.this.isSelected()){
						VUIElement.this.processMoveEvent(event);
					}
					VUIElement.this.mouseDownPoint = new VPoint(event.getClientX(), event.getClientY());
//				}
			}
			
		};
		
		canvas.addMouseEventHandler(moveHandler, MouseMoveEvent.getType());
		
		MouseDownHandler downHandler = new MouseDownHandler() {
			
			@Override
			public void onMouseDown(MouseDownEvent event) {
				VPoint p = new VPoint(event.getClientX() - VUIElement.this.canvas.getAbsoluteLeft(), event.getClientY() - VUIElement.this.canvas.getAbsoluteTop());
				if(VUIElement.this.contains(p)){
					VUIElement.this.setSelected(true);
					VUIElement.this.setMouseDownPoint(new VPoint(event.getClientX(), event.getClientY()));
				}
			}
		};
		
		canvas.addMouseEventHandler(downHandler, MouseDownEvent.getType());
		
		MouseUpHandler upHandler = new MouseUpHandler() {
			
			@Override
			public void onMouseUp(MouseUpEvent event) {
//				VPoint p = new VPoint(event.getClientX() - VUIElement.this.canvas.getAbsoluteLeft(), event.getClientY() - VUIElement.this.canvas.getAbsoluteTop());
//				if(VUIElement.this.contains(p)){
					VUIElement.this.setSelected(false);
					VUIElement.this.setMouseUpPoint(new VPoint(event.getClientX(), event.getClientY()));
//				}
			}
		};
		
		canvas.addMouseEventHandler(upHandler, MouseUpEvent.getType());
		
		MouseOverHandler overHandler = new MouseOverHandler() {
			
			@Override
			public void onMouseOver(MouseOverEvent event) {
//				VPoint p = new VPoint(event.getClientX() - VUIElement.this.canvas.getAbsoluteLeft(), event.getClientY() - VUIElement.this.canvas.getAbsoluteTop());
//				if(VUIElement.this.contains(p)){
					VUIElement.this.setHighlighted(true);
					VUIElement.this.setMouseOverPoint(new VPoint(event.getClientX(), event.getClientY()));
//				}
			}
		};
		
		canvas.addMouseEventHandler(overHandler, MouseOverEvent.getType());
		
		MouseOutHandler outHandler = new MouseOutHandler() {
			
			@Override
			public void onMouseOut(MouseOutEvent event) {
//				VPoint p = new VPoint(event.getClientX() - VUIElement.this.canvas.getAbsoluteLeft(), event.getClientY() - VUIElement.this.canvas.getAbsoluteTop());
//				if(!VUIElement.this.contains(p)){
					VUIElement.this.setHighlighted(false);
					VUIElement.this.setMouseOutPoint(new VPoint(event.getClientX(), event.getClientY()));
//				}
			}
		};
		
		canvas.addMouseEventHandler(outHandler, MouseOutEvent.getType());
	}
	
	abstract protected void processMoveEvent(MouseMoveEvent event);


	public VPoint getMouseDownPoint() {
		return mouseDownPoint;
	}


	public void setMouseDownPoint(VPoint mouseDownPoint) {
		this.mouseDownPoint = mouseDownPoint;
	}


	public VPoint getMouseUpPoint() {
		return mouseUpPoint;
	}


	public void setMouseUpPoint(VPoint mouseUpPoint) {
		this.mouseUpPoint = mouseUpPoint;
	}


	public VPoint getMouseOverPoint() {
		return mouseOverPoint;
	}


	public void setMouseOverPoint(VPoint mouseOverPoint) {
		this.mouseOverPoint = mouseOverPoint;
	}


	public VPoint getMouseOutPoint() {
		return mouseOutPoint;
	}


	public void setMouseOutPoint(VPoint mouseOutPoint) {
		this.mouseOutPoint = mouseOutPoint;
	}


	HandlerManager ensureHandlers() {
		return handlerManager == null ? handlerManager = createHandlerManager()
				: handlerManager;
	}
	
	protected HandlerManager createHandlerManager() {
		return new HandlerManager(this);
	}
	
	public VUIElement getNext() {
		return next;
	}

	public VUIElement getPrevious() {
		return prev;
	}
	
	public void setNext(VUIElement next){
		this.next = next;
	}
	
	public void setPrevious(VUIElement prev){
		this.prev = prev;
	}

	public VCanvas getCanvas() {
		return canvas;
	}

	public void setCanvas(VCanvas canvas) {
		this.canvas = canvas;
	}

	public VUIElement getPrev() {
		return prev;
	}

	public void setPrev(VUIElement prev) {
		this.prev = prev;
	}

	public String getSelectedColor() {
		return selectedColor;
	}

	public void setSelectedColor(String selectedColor) {
		this.selectedColor = selectedColor;
		setChanged(true);
	}

	public String getSelectedFillColor() {
		return selectedFillColor;
	}

	public void setSelectedFillColor(String selectedFillColor) {
		this.selectedFillColor = selectedFillColor;
		setChanged(true);
	}

	public String getHighlightedColor() {
		return highlightedColor;
	}

	public void setHighlightedColor(String highlightedColor) {
		this.highlightedColor = highlightedColor;
		setChanged(true);
	}

	public String getHighlightedFillColor() {
		return highlightedFillColor;
	}

	public void setHighlightedFillColor(String highlightedFillColor) {
		this.highlightedFillColor = highlightedFillColor;
		setChanged(true);
	}

	abstract public void moveTo(VPoint p);
	
	abstract public VPoint getCenter();
	
	public String getId(){
		return this.id;
	}
	
	public void setId(String id){
		this.id = id;
	}
	
	public boolean isSelected() {
		return this.selected;
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
		setChanged(true);
	}
	
	public boolean isPressed() {
		return this.pressed;
	}
	
	public void setPressed(boolean pressed) {
		this.pressed = pressed;
		setChanged(true);
	}
	
	public void setFillColor(String fillColor){
		this.fillColor = fillColor;
		setChanged(true);
	}
	
	public String getFillColor(){
		return this.fillColor;
	}
	
	public void setColor(String color){
		this.color = color;
		setChanged(true);
	}
	
	public String getColor(){
		return this.color;
	}

	/**
	 * @param borderWidth the borderWidth to set
	 */
	public void setBorderWidth(int borderWidth) {
		this.borderWidth = borderWidth;
		setChanged(true);
	}

	/**
	 * @return the borderWidth
	 */
	public int getBorderWidth() {
		return borderWidth;
	}

	public void addHandler(){
//		canvas.addMouseEventHandler(handler, type);
	}
	
	@Override
	public void fireEvent(GwtEvent<?> event) {
		handlerManager.fireEvent(event);
//		GwtEvent.Type<?> type = event.getAssociatedType();
//		
//		List<EventHandler> listernerList = this.handlers.get(type.getName());
//
//		if(MouseDownEvent.getType().getName().equals(type.getName())){
//			for(EventHandler listener : listernerList){
////				((MouseDownHandler)listener).onMouseDown(event);
//			}
//		}else if(MouseUpEvent.getType().getName().equals(type.getName())){
//			
//		}else if(MouseOverEvent.getType().getName().equals(type.getName())){
//			
//		}else if(MouseOutEvent.getType().getName().equals(type.getName())){
//			
//		}else if(MouseMoveEvent.getType().getName().equals(type.getName())){
//			
//		}else if(MouseWheelEvent.getType().getName().equals(type.getName())){
//			
//		}
		
	}
	
	protected void draw(Context2d context) {
			context.save();
			if(this.isSelected()){
				context.setStrokeStyle(getSelectedColor());
			}else if(this.isHighlighted()){
				context.setStrokeStyle(getHighlightedColor());
			}else if(getColor() != null && getColor().length() > 0){
				context.setStrokeStyle(getColor());
			}
			if(getBorderWidth() > 0){
				context.setLineWidth(getBorderWidth());
			}
			
			if(this.isSelected()){
				context.setFillStyle(getSelectedFillColor());
			}else if(this.isHighlighted()){
				context.setFillStyle(getHighlightedFillColor());
			}else if(getFillColor() != null && getFillColor().length() > 0){
				context.setFillStyle(getFillColor());
			}
						
		

	}
	
	abstract public boolean contains(VPoint p);
	
	/**
	 * @param context
	 * @param uidl
	 */
	protected void update(UIDL uidl) {		
			String prefix = getPrefix();			
			String strokecolor = uidl.getStringAttribute(prefix + "strokecolor");
			int strokewidth = uidl.getIntAttribute(prefix + "strokewidth");
			String fillStyleColor = uidl.getStringAttribute(prefix + "fillstyle");
			locky = uidl.getBooleanAttribute(prefix + "locky");
			String selectedColor = uidl.getStringAttribute(prefix + "selectedcolor");
			String selectedFillColor = uidl.getStringAttribute(prefix + "selectedfillcolor");
			String highlightedColor = uidl.getStringAttribute(prefix + "highlightedcolor");
			String highlightedFillColor = uidl.getStringAttribute(prefix + "highlightedfillcolor");
			String desc = uidl.getStringAttribute(prefix + "description");
			this.setRole(uidl.getStringAttribute(getPrefix() + "role"));
						
			this.setDescription(desc);
			if(selectedColor.length() > 0){
				this.setSelectedColor(selectedColor);
			}
			
			if(selectedFillColor.length() > 0){
				this.setSelectedFillColor(selectedFillColor);
			}
			
			if(highlightedColor.length() > 0){
				this.setHighlightedColor(highlightedColor);
			}
			
			if(highlightedFillColor.length() > 0){
				this.setHighlightedFillColor(highlightedFillColor);
			}
			
			setColor(strokecolor);
			setBorderWidth(strokewidth);
			setFillColor(fillStyleColor);
	}


	public void setHighlighted(boolean highlighted) {
		this.highlighted = highlighted;
		setChanged(true);
	}


	public boolean isHighlighted() {
		return highlighted;
	}
	
	public static boolean pointInPolygon(VPoint[] vertices, VPoint p) {

		int i, j=vertices.length-1 ;
		boolean  oddNodes=false;

		for (i=0; i<vertices.length; i++) {
			if (vertices[i].getY()<p.getY() && vertices[j].getY()>=p.getY()
					||  vertices[j].getY()<p.getY() && vertices[i].getY()>=p.getY()) {
				if (vertices[i].getX()+(p.getY()-vertices[i].getY())/(vertices[j].getY()-vertices[i].getY())*(vertices[j].getX()-vertices[i].getX())<p.getX()) {
					oddNodes=!oddNodes;
				}
			}
			j=i;
		}

		return oddNodes;
	}
	
	public static double squaredDistanceFromLine(VPoint p, VPoint start, VPoint end, boolean isSegment){
		double r_numerator = (p.getX()-start.getX())*(end.getX()-start.getX())
				+ (p.getY()-start.getY())*(end.getY()-start.getY());
		double r_denomenator = (end.getX()-start.getX())*(end.getX()-start.getX())
				+ (end.getY()-start.getY())*(end.getY()-start.getY());
		double r = r_numerator / r_denomenator;

//	    double px = start.getX() + r*(end.getX()-start.getX());
//	    double py = start.getY() + r*(end.getY()-start.getY());
	    
		double s =  ((start.getY()-p.getY())*(end.getX()-start.getX())
	    		-(start.getX()-p.getX())*(end.getY() - start.getY()) ) / r_denomenator;

	    double squaredDistanceLine = s*s*r_denomenator;
	    
	    if(!isSegment){
	    	return squaredDistanceLine;
	    }

	//
	// (xx,yy) is the point on the lineSegment closest to (cx,cy)
	//
		double squaredDistanceSegment;
//		double xx = px;
//		double yy = py;

		if ( (r >= 0) && (r <= 1) ){
			squaredDistanceSegment = squaredDistanceLine;
		} else{

			double dist1 = (p.getX()-start.getX())*(p.getX()-start.getX())
					+ (p.getY()-start.getY())*(p.getY()-start.getY());
			double dist2 = (p.getX()-end.getX())*(p.getX()-end.getX())
					+ (p.getY()-end.getY())*(p.getY()-end.getY());
			if (dist1 < dist2){
//				xx = start.getX();
//				yy = start.getY();
				squaredDistanceSegment = dist1;
			} else{
//				xx = end.getX();
//				yy = end.getY();
				squaredDistanceSegment = dist2;
			}
		}
		
		return squaredDistanceSegment;
	}


	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	
	public List<VUIElement> elementsUnderPoint(VPoint p){
		List<VUIElement> list = new ArrayList<VUIElement>();
		if(this.contains(p)){
			list.add(this);
		}
		return list;
	}
	
	public VUIElement elementUnderPoint(VPoint p){
		if(this.contains(p)){
			return this;
		}
		return null;
	}


	/**
	 * @return the groupId
	 */
	public String getGroupId() {
		return groupId;
	}
	
	public String getPrefix(){

		String prefix = "";
		if(id.length() != 0){
			prefix = id + ".";
		}
/*		
		if(groupId.length() != 0){
			prefix = groupId + ".";
		}
*/		return prefix;
	}

	abstract public void moveBy(VPoint delta);

	/**
	 * @return the role
	 */
	public String getRole() {
		return role;
	}

	/**
	 * @param role the role to set
	 */
	public void setRole(String role) {
		this.role = role;
		setChanged(true);
	}

	public boolean isChanged() {
		return changed;
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}
	/*
	protected void initiateConnectionEvent(MouseDownEvent event) {
		throw new UnsupportedOperationException("Method is not supported by element type " + this.role);
	}
	
	protected void highlightConnectionEvent(MouseMoveEvent event) {
		throw new UnsupportedOperationException("Method is not supported by element type " + this.role);
	}
	
	protected void lowlightConnectionEvent(MouseMoveEvent event) {
		throw new UnsupportedOperationException("Method is not supported by element type " + this.role);
	}
	
	protected void finalizeConnectionEvent(MouseUpEvent event) {
		throw new UnsupportedOperationException("Method is not supported by element type " + this.role);
	}
	
	protected void updateConnectorEvent(MouseMoveEvent event) {
		throw new UnsupportedOperationException("Method is not supported by element type " + this.role);
	}
	
	protected void abortConnectionEvent(MouseUpEvent event) {
		throw new UnsupportedOperationException("Method is not supported by element type " + this.role);
	}*/
	
	public int getZIndex(){
		return this.canvas.getElementIndex(this);
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public boolean isLocky() {
		return locky;
	}
	public void setLocky(boolean locky) {
		this.locky = locky;
	}
}
