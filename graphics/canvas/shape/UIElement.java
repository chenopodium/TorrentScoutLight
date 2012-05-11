/**
 * 
 */
package com.vaadin.graphics.canvas.shape;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.graphics.canvas.widgetset.client.ui.VCanvas;
import com.vaadin.graphics.event.MouseEvent;
import com.vaadin.graphics.event.listener.MouseEventListener;

/**
 * @author kapil - kapildverma@gmail.com
 * @author chenopodium - croth@nobilitas.com
 *
 */
public abstract class UIElement {
	
	public static enum ElementRole{ELEMENT, CONNECTOR, IPORT, OPORT, IOPORT};
	
	private VCanvas canvas;

	private String id = "";
	private String groupId = "";
	
	private UIElement next;
	private UIElement prev;
	private Point mouseDownPoint;
	private Point mouseUpPoint;
	private Point mouseOverPoint;
	private Point mouseOutPoint;
	private boolean selected = false;
	private boolean highlighted = false;
	private boolean pressed;
	private String color = "";
	private String fillColor = "";
	private String selectedColor = "";
	private String selectedFillColor = "";
	private String highlightedColor = "";
	private String highlightedFillColor = "";
	private int borderWidth = -1;
	private boolean locky;
	
	private ElementRole role = UIElement.ElementRole.ELEMENT;
	
	abstract public void moveTo(Point p);
	
	abstract public Point getCenter();
	
	abstract public boolean contains(Point p);
	
	abstract public void addListener(MouseEventListener listener, MouseEvent.Type eventType);
	
	abstract public void fireMouseEvent(MouseEvent event);
	
	abstract public void add(Point p);
	
	public static int counter = 0;
	
	private String description;
	
	public String toString() {
		return getClass().getName()+"@"+getCenter();
	}

	public Map<String, Object> getDrawInstructions(){
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put(getPrefix() + "selectedcolor", selectedColor);
		arguments.put(getPrefix() + "selectedfillcolor", selectedFillColor);
		arguments.put(getPrefix() + "highlightedcolor", highlightedColor);
		arguments.put(getPrefix() + "highlightedfillcolor", highlightedFillColor);
		arguments.put(getPrefix() + "role", this.role);
		arguments.put(getPrefix() + "locky", this.locky);
		arguments.put(getPrefix() + "description", this.description);
		return arguments;
	}
	
	private static synchronized void incrementCounter(){
			counter++;
	}
	
	public UIElement(){
		UIElement.incrementCounter();
		this.setId(counter + "");
	}
	
	
	public VCanvas getCanvas() {
		return canvas;
	}

	public void setCanvas(VCanvas canvas) {
		this.canvas = canvas;
	}

	public UIElement getPrev() {
		return prev;
	}

	public void setPrev(UIElement prev) {
		this.prev = prev;
	}

	public Point getMouseDownPoint() {
		return mouseDownPoint;
	}

	public void setMouseDownPoint(Point mouseDownPoint) {
		this.mouseDownPoint = mouseDownPoint;
	}

	public Point getMouseUpPoint() {
		return mouseUpPoint;
	}

	public void setMouseUpPoint(Point mouseUpPoint) {
		this.mouseUpPoint = mouseUpPoint;
	}

	public Point getMouseOverPoint() {
		return mouseOverPoint;
	}

	public void setMouseOverPoint(Point mouseOverPoint) {
		this.mouseOverPoint = mouseOverPoint;
	}

	public Point getMouseOutPoint() {
		return mouseOutPoint;
	}

	public void setMouseOutPoint(Point mouseOutPoint) {
		this.mouseOutPoint = mouseOutPoint;
	}

	public String getHighlightedColor() {
		return highlightedColor;
	}

	public void setHighlightedColor(String highlightedColor) {
		this.highlightedColor = highlightedColor;
	}

	public String getHighlightedFillColor() {
		return highlightedFillColor;
	}

	public void setHighlightedFillColor(String highlightedFillColor) {
		this.highlightedFillColor = highlightedFillColor;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		if(id != null)
			this.id = id;
	}
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	
	public boolean isSelected() {
		return this.selected;
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	public boolean isPressed() {
		return this.pressed;
	}
	
	public void setPressed(boolean pressed) {
		this.pressed = pressed;
	}
	
	public void setFillColor(String fillColor){
		if(fillColor != null)
			this.fillColor = fillColor;
	}
	
	public String getFillColor(){
		return this.fillColor;
	}
	
	public void setColor(String color){
		if(color != null)
			this.color = color;
	}
	
	public String getColor(){
		return this.color;
	}

	/**
	 * @param borderWidth the borderWidth to set
	 */
	public void setBorderWidth(int borderWidth) {
		this.borderWidth = borderWidth;
	}

	/**
	 * @return the borderWidth
	 */
	public int getBorderWidth() {
		return borderWidth;
	}
	
	public UIElement getNext() {
		return next;
	}

	public UIElement getPrevious() {
		return prev;
	}
	
	public void setNext(UIElement next){
		this.next = next;
	}
	
	public void setPrevious(UIElement prev){
		this.prev = prev;
	}
	
	public static boolean pointInPolygon(Point[] vertices, Point p) {

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

	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
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

	/**
	 * @return the selectedColor
	 */
	public String getSelectedColor() {
		return selectedColor;
	}

	/**
	 * @param selectedColor the selectedColor to set
	 */
	public void setSelectedColor(String selectedColor) {
		this.selectedColor = selectedColor;
	}

	/**
	 * @return the selectedFillColor
	 */
	public String getSelectedFillColor() {
		return selectedFillColor;
	}

	/**
	 * @param selectedFillColor the selectedFillColor to set
	 */
	public void setSelectedFillColor(String selectedFillColor) {
		this.selectedFillColor = selectedFillColor;
	}

	/**
	 * @return the highlighted
	 */
	public boolean isHighlighted() {
		return highlighted;
	}

	/**
	 * @param highlighted the highlighted to set
	 */
	public void setHighlighted(boolean highlighted) {
		this.highlighted = highlighted;
	}

	/**
	 * @return the type
	 */
	public ElementRole getRole() {
		return role;
	}

	/**
	 * @param type the type to set
	 */
	public void setRole(ElementRole type) {
		this.role = type;
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
