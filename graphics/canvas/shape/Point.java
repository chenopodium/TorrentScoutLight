/**
 * 
 */
package com.vaadin.graphics.canvas.shape;

/**
 * @author kapil - kapil.verma@globallogic.com
 *
 */
public class Point {
	private double x, y;

	public String toString() {
		return "Point("+x+"/"+y+")";
	}
	public Point() {
		this(0, 0);
	}

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public Point(Point v) {
		this(v.x, v.y);
	}

	public void add(double x, double y) {
		this.x += x;
		this.y += y;
	}

	public void add(Point v) {
		add(v.x, v.y);
	}

	public void sub(Point v) {
		sub(v.x, v.y);
	}

	public void sub(double x, double y) {
		this.x -= x;
		this.y -= y;
	}

	public void mult(double x, double y) {
		this.x *= x;
		this.y *= y;
	}

	public void mult(Point v) {
		mult(v.x, v.y);
	}

	public void mult(double c) {
		mult(c, c);
	}

	public double mag() {
		if (x == 0 && y == 0) {
			return 0;
		} else {
			return Math.sqrt(x * x + y * y);
		}
	}

	public double magSquared() {
		return x * x + y * y;
	}

	public void set(Point v) {
		x = v.x;
		y = v.y;
	}
	
	public double getX(){
		return x;
	}
	
	public double getY(){
		return y;
	}

	public static Point sub(Point a, Point b) {
		return new Point(a.x - b.x, a.y - b.y);
	}
	
	public static Point add(Point a, Point b) {
		return new Point(a.x + b.x, a.y + b.y);
	}

	public static Point mult(Point v, double c) {
		return new Point(v.x * c, v.y * c);
	}
}
