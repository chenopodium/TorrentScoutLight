package com.vaadin.graphics.canvas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.graphics.canvas.shape.Point;
import com.vaadin.graphics.canvas.shape.Rect;
import com.vaadin.graphics.canvas.shape.UIElement;
import com.vaadin.graphics.event.MouseDownEvent;
import com.vaadin.graphics.event.MouseEvent;
import com.vaadin.graphics.event.MouseUpEvent;
import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.ui.AbstractComponent;

/**
 * Server side component for the VCanvas widget.
 * 
 * @author chenopodium - croth@nobilitas.com
 */
@com.vaadin.ui.ClientWidget(com.vaadin.graphics.canvas.widgetset.client.ui.VCanvas.class)
public class Canvas extends AbstractComponent {
	private static final long serialVersionUID = -5388297546218777306L;

	public static final String BEVEL = "BEVEL";
	public static final String BUTT = "BUTT";
	public static final String DESTINATION_OVER = "DESTINATION_OVER";
	public static final String SOURCE_OVER = "SOURCE_OVER";
	public static final String MITER = "MITER";
	public static final String TRANSPARENT = "TRANSPARENT";
	public static final String ROUND = "ROUND";
	public static final String SQUARE = "SQUARE";

	private final List<Map<String, Object>> commands = new ArrayList<Map<String, Object>>();

	private final List<CanvasMouseDownListener> downListeners = new ArrayList<CanvasMouseDownListener>();

	private final List<CanvasMouseUpListener> upListeners = new ArrayList<CanvasMouseUpListener>();

	private final List<CanvasMouseMoveListener> moveListeners = new ArrayList<CanvasMouseMoveListener>();

	private final List<UIElement> children = new ArrayList<UIElement>();

	private final Map<String, UIElement> childrenMap = new HashMap<String, UIElement>();

	public Canvas() {
		super();

		this.downListeners.add(new CanvasMouseDownListener() {

			public void mouseDown(Point p, int clickcount, UIElement child) {
				p("Got mouse Down event, click count: " + clickcount+", child:"+child);
				if (child != null) {
					for (UIElement element : children) {
						if (element.getId().equalsIgnoreCase(child.getId())) {
							MouseEvent event = new MouseDownEvent(element, p);
							p("Firing event on child: "+child);
							element.fireMouseEvent(event);
							// event.setUiSource(element);
						}
					}
				}
			}
		});

		this.upListeners.add(new CanvasMouseUpListener() {

			public void mouseUp(Point p, UIElement child) {
				// p("Got mouse Up event, child="+child);
				if (child != null) {
					for (UIElement element : children) {
						if (element.getId() == child.getId()) {
							// p("mouseUp: found child "+element);
							MouseEvent event = new MouseUpEvent(element, p);
							element.fireMouseEvent(event);
							// p("mouseUp: Firing event on "+element);

						}
					}
				}
			}
		});

		// this.moveListeners.add(new CanvasMouseMoveListener() {
		//
		// public void mouseMove(Point p) {
		// //p("Got mouse Move event");
		// for(UIElement element: children){
		// if(element.contains(p)){
		// MouseEvent event = new MouseMoveEvent(element, p);
		// element.fireMouseEvent(event);
		// if (event.getUiSource() == null) p("Mouse move, no source");
		//
		// }
		// }
		// }
		// });

	}

	public void createLinearGradient(String name, double x0, double y0, double x1, double y1) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "createLinearGradient");
		arguments.put("name", name);
		arguments.put("x0", x0);
		arguments.put("y0", y0);
		arguments.put("x1", x1);
		arguments.put("y1", y1);

		commands.add(arguments);

		requestRepaint();
	}

	public void createRadialGradient(String name, double x0, double y0, double r0, double x1, double y1, double r1) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "createRadialGradient");
		arguments.put("name", name);
		arguments.put("x0", x0);
		arguments.put("y0", y0);
		arguments.put("r0", r0);
		arguments.put("x1", x1);
		arguments.put("y1", y1);
		arguments.put("r1", r1);
		commands.add(arguments);

		requestRepaint();
	}

	public void cubicCurveTo(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "cubicCurveTo");
		arguments.put("cp1x", cp1x);
		arguments.put("cp1y", cp1y);
		arguments.put("cp2x", cp2x);
		arguments.put("cp2y", cp2y);
		arguments.put("x", x);
		arguments.put("y", y);
		commands.add(arguments);

		requestRepaint();
	}

	private static void p(String msg) {
		//System.out.println("Canvas: " + msg);
		//Logger.getLogger(Canvas.class.getName()).log(Level.INFO, msg);
	}

	public void setBackgroundImage(String url) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "setbackground");
		arguments.put("url", url);
		commands.add(arguments);
		// this.saveContext();
		requestRepaint();
	}

	public void drawImage(String url, double offsetX, double offsetY) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "drawImage1");
		arguments.put("url", url);
		arguments.put("offsetX", offsetX);
		arguments.put("offsetY", offsetY);
		commands.add(arguments);
		// this.saveContext();
		requestRepaint();
	}

	public void drawImage(String url, double offsetX, double offsetY, double width, double height) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "drawImage2");
		arguments.put("url", url);
		arguments.put("offsetX", offsetX);
		arguments.put("offsetY", offsetY);
		arguments.put("width", width);
		arguments.put("height", height);
		commands.add(arguments);

		requestRepaint();
	}

	public void drawImage(String url, double sourceX, double sourceY, double sourceWidth, double sourceHeight, double destX, double destY, double destWidth, double destHeight) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "drawImage3");
		arguments.put("url", url);
		arguments.put("sourceX", sourceX);
		arguments.put("sourceY", sourceY);
		arguments.put("sourceWidth", sourceWidth);
		arguments.put("sourceHeight", sourceHeight);
		arguments.put("destX", destX);
		arguments.put("destY", destY);
		arguments.put("destWidth", destWidth);
		arguments.put("destHeight", destHeight);
		commands.add(arguments);

		requestRepaint();
	}

	public void fill() {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "fill");
		commands.add(arguments);

		requestRepaint();
	}

	public void fillRect(double startX, double startY, double width, double height) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "fillRect");
		arguments.put("startX", startX);
		arguments.put("startY", startY);
		arguments.put("width", width);
		arguments.put("height", height);
		commands.add(arguments);

		requestRepaint();
	}

	public void lineTo(double x, double y) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "lineTo");
		arguments.put("x", x);
		arguments.put("y", y);
		commands.add(arguments);

		requestRepaint();
	}

	public void moveTo(double x, double y) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "moveTo");
		arguments.put("x", x);
		arguments.put("y", y);
		commands.add(arguments);

		requestRepaint();
	}

	public void quadraticCurveTo(double cpx, double cpy, double x, double y) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "quadraticCurveTo");
		arguments.put("cpx", cpx);
		arguments.put("cpy", cpy);
		arguments.put("x", x);
		arguments.put("y", y);
		commands.add(arguments);

		requestRepaint();
	}

	public void rect(double startX, double startY, double width, double height) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "rect");
		arguments.put("startX", startX);
		arguments.put("startY", startY);
		arguments.put("width", width);
		arguments.put("height", height);
		commands.add(arguments);

		requestRepaint();
	}

	public void rotate(double angle) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "rotate");
		arguments.put("angle", angle);
		commands.add(arguments);

		requestRepaint();
	}

	public void setGradientFillStyle(String gradient) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "setGradientFillStyle");
		arguments.put("gradient", gradient);
		commands.add(arguments);

		requestRepaint();
	}

	public void setFillStyle(String color) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "setFillStyle");
		arguments.put("color", color);
		commands.add(arguments);

		requestRepaint();
	}

	public void setLineCap(String lineCap) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "setLineCap");
		arguments.put("lineCap", lineCap);
		commands.add(arguments);

		requestRepaint();
	}

	public void setLineJoin(String lineJoin) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "setLineJoin");
		arguments.put("lineJoin", lineJoin);
		commands.add(arguments);

		requestRepaint();
	}

	public void setLineWidth(double width) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "setLineWidth");
		arguments.put("width", width);
		commands.add(arguments);

		requestRepaint();
	}

	public void setMiterLimit(double miterLimit) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "setMiterLimit");
		arguments.put("miterLimit", miterLimit);
		commands.add(arguments);

		requestRepaint();
	}

	public void setGradientStrokeStyle(String gradient) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "setGradientStrokeStyle");
		arguments.put("gradient", gradient);
		commands.add(arguments);

		requestRepaint();
	}

	public void setColorStrokeStyle(String color) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "setColorStrokeStyle");
		arguments.put("color", color);
		commands.add(arguments);

		requestRepaint();
	}

	public void strokeRect(double startX, double startY, double width, double height) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "strokeRect");
		arguments.put("startX", startX);
		arguments.put("startY", startY);
		arguments.put("width", width);
		arguments.put("height", height);
		commands.add(arguments);

		requestRepaint();
	}

	public void transform(double m11, double m12, double m21, double m22, double dx, double dy) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "transform");
		arguments.put("m11", m11);
		arguments.put("m12", m12);
		arguments.put("m21", m21);
		arguments.put("m22", m22);
		arguments.put("dx", dx);
		arguments.put("dy", dy);
		commands.add(arguments);

		requestRepaint();
	}

	public void arc(double x, double y, double radius, double startAngle, double endAngle, boolean antiClockwise) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "arc");
		arguments.put("x", x);
		arguments.put("y", y);
		arguments.put("radius", radius);
		arguments.put("startAngle", startAngle);
		arguments.put("endAngle", endAngle);
		commands.add(arguments);

		requestRepaint();
	}

	public void translate(double x, double y) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "translate");
		arguments.put("x", x);
		arguments.put("y", y);
		commands.add(arguments);

		requestRepaint();
	}

	public void scale(double x, double y) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "scale");
		arguments.put("x", x);
		arguments.put("y", y);
		commands.add(arguments);

		requestRepaint();
	}

	public void stroke() {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "stroke");
		commands.add(arguments);

		requestRepaint();
	}

	public void saveContext() {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "saveContext");
		commands.add(arguments);

		requestRepaint();
	}

	public void restoreContext() {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "restoreContext");
		commands.add(arguments);

		requestRepaint();
	}

	public void setBackgroundColor(String rgb) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "setBackgroundColor");
		arguments.put("rgb", rgb);
		commands.add(arguments);

		requestRepaint();
	}

	public void setStrokeColor(String rgb) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "setStrokeColor");
		arguments.put("rgb", rgb);
		commands.add(arguments);

		requestRepaint();
	}

	public void beginPath() {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "beginPath");
		commands.add(arguments);

		requestRepaint();
	}

	public void clear() {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "clear");
		commands.add(arguments);

		requestRepaint();
	}

	@Deprecated
	// - only for internal testing
	public void drawRect(Rect block) {
		// reset();
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("strokecolor", block.getColor());
		arguments.put("strokewidth", block.getBorderWidth());
		arguments.put("x", block.getStart().getX());
		arguments.put("y", block.getStart().getY());
		arguments.put("w", block.getEnd().getX() - block.getStart().getX());
		arguments.put("h", block.getEnd().getY() - block.getStart().getY());
		arguments.put("fillstyle", block.getFillColor());
		arguments.put("command", "drawrect");
		commands.add(arguments);
		requestRepaint();
	}

	public void drawUIElement(UIElement ele) {
		if (!children.contains(ele)) {
			addChild(ele);
		}
		Map<String, Object> arguments = ele.getDrawInstructions();
		arguments.put("command", "draw");
		arguments.put("elementid", ele.getId());
		commands.add(arguments);
		requestRepaint();
	}

	public void updateUIElement(UIElement ele) {

		Map<String, Object> arguments = ele.getDrawInstructions();
		arguments.put("command", "update");
		arguments.put("elementid", ele.getId());
		commands.add(arguments);
		requestRepaint();
	}

	public void reset() {
		commands.clear();
		clear();
	}

	@Override
	public void paintContent(PaintTarget target) throws PaintException {
		super.paintContent(target);

		for (Map<String, Object> command : commands) {
			target.startTag((String) command.get("command"));

			for (String key : command.keySet()) {
				if (key.equals("command")) {
					continue; // This is already in tag name
				}

				Object value = command.get(key);
				if (value instanceof Double) {
					target.addAttribute(key, (Double) value);
				} else if (value instanceof Integer) {
					target.addAttribute(key, (Integer) value);
				} else if (value instanceof Boolean) {
					target.addAttribute(key, (Boolean) value);
				} else if (value instanceof String[]) {
					target.addAttribute(key, (String[]) value);
				} else {
					target.addAttribute(key, value + "");
				}
			}

			target.endTag((String) command.get("command"));
		}
	}

	@Override
	public void changeVariables(Object source, Map variables) {

		// if (!variables.containsValue("mousemove")){
		// p("changeVariables");
		// Iterator it = variables.keySet().iterator();
		// while (it.hasNext()) {
		// String key = it.next().toString();
		// p("  key: "+key+"="+variables.get(key));
		// }
		// }
		if (variables.containsKey("sizeChanged")) {
			// System.out.println("Canvass size now "
			// + variables.get("sizeChanged").toString());
			requestRepaint();
		} else if (variables.containsKey("event")) {
			String eventtype = (String) variables.get("event");
			Integer x = (Integer) variables.get("mx");
			Integer y = (Integer) variables.get("my");
			Point p = new Point(x, y);
			if (eventtype.equals("mousedown")) {
				String childid = (String) variables.get("childid");
				UIElement child = getChildWithId(childid);
				// XX todo: also send child
				fireMouseDown(p, 1, child);
			} else if (eventtype.equals("mousedouble")) {
				String childid = (String) variables.get("childid");
				UIElement child = getChildWithId(childid);
				// XX todo: also send child
				fireMouseDown(p, 2, child);

			} else if (eventtype.equals("mouseup")) {
				String childid = (String) variables.get("childid");
				UIElement child = getChildWithId(childid);
				fireMouseUp(p, child);
			} else if (eventtype.equals("mousemove")) {
				// Integer x2 = (Integer) variables.get("mx2");
				// Integer y2 = (Integer) variables.get("my2");
				fireMouseMove(p);
			} else {
				System.err.println("Unknown event type: " + eventtype);
			}
		}

	}

	private UIElement getChildWithId(String childid) {
		UIElement child = null;
		if (childid != null) {
			// p("Got child id: "+childid+", checking children");
			for (UIElement element : children) {
				if (element.getId().equalsIgnoreCase(childid)) {
					//p("mouseup: got matching element :" + element);
					child = element;
				}
			}
		}
		return child;
	}

	public void setStrokeStyle(int r, int g, int b) {
		setStrokeColor(String.format("#%02x%02x%02x", r, g, b));
	}

	public void setFillStyle(int r, int g, int b) {
		setFillStyle(String.format("#%02x%02x%02x", r, g, b));
	}

	public void setGlobalAlpha(double alpha) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "setGlobalAlpha");
		arguments.put("alpha", alpha);
		commands.add(arguments);

		requestRepaint();
	}

	public void closePath() {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "closePath");
		commands.add(arguments);

		requestRepaint();
	}

	public void setGlobalCompositeOperation(String mode) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "setGlobalCompositeOperation");
		arguments.put("mode", mode);
		commands.add(arguments);

		requestRepaint();
	}

	public void addColorStop(String gradient, double offset, String color) {
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("command", "addColorStop");
		arguments.put("gradient", gradient);
		arguments.put("offset", offset);
		arguments.put("color", color);
		commands.add(arguments);

		requestRepaint();
	}

	public interface CanvasMouseDownListener {
		public void mouseDown(Point p, int clickcount, UIElement child);
	}

	public void addListener(CanvasMouseDownListener listener) {
		if (!downListeners.contains(listener)) {
			downListeners.add(listener);
		}
	}

	public void removeListener(CanvasMouseDownListener listener) {
		if (downListeners.contains(listener)) {
			downListeners.remove(listener);
		}
	}

	private void fireMouseDown(Point p, int count, UIElement child) {
		for (CanvasMouseDownListener listener : downListeners) {
			listener.mouseDown(p, count, child);
		}
	}

	public interface CanvasMouseUpListener {
		public void mouseUp(Point p, UIElement child);
	}

	public void addListener(CanvasMouseUpListener listener) {
		if (!upListeners.contains(listener)) {
			upListeners.add(listener);
		}
	}

	public void removeListener(CanvasMouseUpListener listener) {
		if (upListeners.contains(listener)) {
			upListeners.remove(listener);
		}
	}

	private void fireMouseUp(Point p, UIElement child) {
		for (CanvasMouseUpListener listener : upListeners) {
			listener.mouseUp(p, child);
		}
	}

	public interface CanvasMouseMoveListener {
		public void mouseMove(Point p);
	}

	public void addListener(CanvasMouseMoveListener listener) {
		if (!moveListeners.contains(listener)) {
			moveListeners.add(listener);
		}
	}

	public void removeListener(CanvasMouseMoveListener listener) {
		if (moveListeners.contains(listener)) {
			moveListeners.remove(listener);
		}
	}

	private void fireMouseMove(Point p) {
		for (CanvasMouseMoveListener listener : moveListeners) {
			listener.mouseMove(p);
		}
	}

	public List<UIElement> getChildren() {
		return this.children;
	}

	public int addChild(UIElement child) {
		int index = this.children.size();
		if (this.childrenMap.containsKey(child.getId())) {
			return -1;
		}
		this.children.add(child);
		this.childrenMap.put(child.getId(), child);
		return index;
	}

}
