/**
 * 
 */
package com.vaadin.graphics.canvas.widgetset.client.ui;

/**
 * @author kapil - kapil.verma@globallogic.com
 * @author chenopodium - croth@nobilitas.com
 * 
 */
class VPoint {
	private double x, y;

	public VPoint() {
		this(0, 0);
	}

	public String toString() {
		return "Vpoint(" + x + "/" + y + ")";
	}

	public VPoint(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public VPoint(VPoint v) {
		this(v.x, v.y);
	}

	public void add(double x, double y) {
		this.x += x;
		this.y += y;
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public void add(VPoint v) {
		add(v.x, v.y);
	}

	public void sub(VPoint v) {
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

	public void mult(VPoint v) {
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

	public void set(VPoint v) {
		x = v.x;
		y = v.y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public static VPoint sub(VPoint a, VPoint b) {
		return new VPoint(a.x - b.x, a.y - b.y);
	}

	public static VPoint add(VPoint a, VPoint b) {
		return new VPoint(a.x + b.x, a.y + b.y);
	}

	public static VPoint mult(VPoint v, double c) {
		return new VPoint(v.x * c, v.y * c);
	}
}
