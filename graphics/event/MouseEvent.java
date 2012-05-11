/**
 * 
 */
package com.vaadin.graphics.event;

import java.util.EventObject;

import com.vaadin.graphics.canvas.shape.Point;

/**
 * @author kapil - kapil.verma@globallogic.com
 *
 */
public abstract class MouseEvent extends EventObject {

	public static enum Type{UP, DOWN, MOVE};
	
	protected Type type;
	
	/**
	 * @param source
	 */
	public MouseEvent(Object source) {
		super(source);
	}

	public MouseEvent.Type getType() {
		return type;
	}
	
	public abstract Point getPoint();
}
