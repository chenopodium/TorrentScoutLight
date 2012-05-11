/**
 * 
 */
package com.vaadin.graphics.event;

import com.vaadin.graphics.canvas.shape.Point;
import com.vaadin.graphics.canvas.shape.UIElement;


/**
 * @author kapil - kapil.verma@globallogic.com
 *
 */
public class MouseMoveEvent extends MouseEvent {

	private Point p;
	
	/**
	 * @param source
	 */
	public MouseMoveEvent(UIElement source, Point p) {
		super(source);
		this.type = Type.MOVE;
		this.p = p;
	}

	public Point getPoint() {
		return this.p;
	}

}
