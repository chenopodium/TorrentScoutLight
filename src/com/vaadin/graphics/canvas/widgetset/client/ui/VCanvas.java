package com.vaadin.graphics.canvas.widgetset.client.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.CanvasGradient;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.GestureStartEvent;
import com.google.gwt.event.dom.client.GestureStartHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.TouchEndEvent;
import com.google.gwt.event.dom.client.TouchEndHandler;
import com.google.gwt.event.dom.client.TouchMoveEvent;
import com.google.gwt.event.dom.client.TouchMoveHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.UIDL;

/**
 * Client side widget which communicates with the server. Messages from the
 * server are shown as HTML and mouse clicks are sent to the server.
 * 
 * @author chenopodium - croth@nobilitas.com
 */
public class VCanvas extends Composite implements Paintable {

	/** Set the CSS class name to allow styling. */
	public static final String CLASSNAME = "v-canvas";

	public static final String CLICK_EVENT_IDENTIFIER = "click";

	/** The client side widget identifier */
	protected String paintableId;

	/** Reference to the server connection object. */
	protected ApplicationConnection client;

	private Canvas canvas;
	private Canvas backBuffer;

	private int widthCache;

	private int heightCache;

	// mouse positions relative to canvas
	int mouseX, mouseY;

	// timer refresh rate, in milliseconds
	static final int refreshRate = 50;

	// canvas size, in px
	static int height = 400;
	static int width = 400;

	Context2d context;
	Context2d backBufferContext;

	CssColor redrawColor = CssColor.make("rgba(255,255,255,0.6)");

	private final Map<String, CanvasGradient> gradients = new HashMap<String, CanvasGradient>();

	private Map<String, VUIElement> children = new HashMap<String, VUIElement>();

	private List<VUIElement> childrenList = new ArrayList<VUIElement>();

	protected VPort connectionStartPort;

	protected VPort connectionEndPort;

	private Image bgimage;

	/**
	 * The constructor should first call super() to initialize the component and
	 * then handle any initialization relevant to Vaadin.
	 */
	public VCanvas() {
		super();

		width = Window.getClientWidth();
		height = Window.getClientWidth();

		canvas = Canvas.createIfSupported();
		backBuffer = Canvas.createIfSupported();

		// init the canvas
		canvas.setWidth(width + "px");
		canvas.setHeight(height + "px");
		canvas.setCoordinateSpaceWidth(width);
		canvas.setCoordinateSpaceHeight(height);
		backBuffer.setCoordinateSpaceWidth(width);
		backBuffer.setCoordinateSpaceHeight(height);
		// RootPanel.get(holderId).add(canvas);
		context = canvas.getContext2d();
		backBufferContext = backBuffer.getContext2d();

		// init handlers
		initHandlers();

		// setup timer
		final Timer timer = new Timer() {
			@Override
			public void run() {
				doUpdate();
			}
		};
		timer.scheduleRepeating(refreshRate);

		initWidget(canvas); // All Composites need to call initWidget()

		setStyleName(CLASSNAME);
	}

	void doUpdate() {
		// update the back canvas
		backBufferContext.setFillStyle(redrawColor);
		backBufferContext.fillRect(0, 0, width, height);
		drawBg();
		for (VUIElement ele : childrenList) {
			// if(ele.isChanged())
			ele.draw(backBufferContext);
		}

		context.drawImage(backBufferContext.getCanvas(), 0, 0);

	}

	void initHandlers() {
		canvas.addDoubleClickHandler(new DoubleClickHandler() {

			@Override
			public void onDoubleClick(DoubleClickEvent event) {
				// TODO Auto-generated method stub
				int x = event.getClientX() - getAbsoluteLeft();
				int y = event.getClientY() - getAbsoluteTop();
				VUIElement ele = null;
				VPoint p = new VPoint(x, y);
				for (VUIElement element : childrenList) {
					if (element.contains(p)) {
						ele = element;
					}
				}

				if (client == null) {
					return;
				}
				if (ele != null) {
					p("mouse DOUBLE Click: Adding " + ele + " to variable change");
					client.updateVariable(paintableId, "childid", ele.getId(), false);
					client.updateVariable(paintableId, "class", ele.getClass().getName(), false);
				}
				client.updateVariable(paintableId, "mx", x, false);
				client.updateVariable(paintableId, "my", y, false);
				client.updateVariable(paintableId, "event", "mousedouble", true);
			}
			
		});
		canvas.addMouseDownHandler(new MouseDownHandler() {
			public void onMouseDown(MouseDownEvent event) {
				int x = event.getClientX() - getAbsoluteLeft();
				int y = event.getClientY() - getAbsoluteTop();
				VUIElement ele = null;
				VPoint p = new VPoint(x, y);
				for (VUIElement element : childrenList) {
					if (element.contains(p)) {
						ele = element;
					}
				}

				if (client == null) {
					return;
				}

				if (ele != null) {

					p("mouseDOWN: Adding " + ele + " to variable change");
					client.updateVariable(paintableId, "childid", ele.getId(), false);
					client.updateVariable(paintableId, "class", ele.getClass().getName(), false);
				}

				client.updateVariable(paintableId, "mx", x, false);
				client.updateVariable(paintableId, "my", y, false);
				client.updateVariable(paintableId, "event", "mousedown", true);
			}
		});
		canvas.addMouseUpHandler(new MouseUpHandler() {
			public void onMouseUp(MouseUpEvent event) {

				int x = event.getClientX() - getAbsoluteLeft();
				int y = event.getClientY() - getAbsoluteTop();

				VPoint p = new VPoint(x, y);
				VUIElement ele = null;
				for (VUIElement element : childrenList) {
					if (element.contains(p)) {
						ele = element;
					}
				}

				if (client == null) {
					return;
				}
				if (ele != null) {
					p("mouseUP: Adding " + ele + " " + x + "/" + y + " to variable change");
					client.updateVariable(paintableId, "childid", ele.getId(), false);
					client.updateVariable(paintableId, "class", ele.getClass().getName(), false);
				}
				client.updateVariable(paintableId, "mx", x, false);
				client.updateVariable(paintableId, "my", y, false);
				client.updateVariable(paintableId, "event", "mouseup", true);
			}
		});

		canvas.addMouseMoveHandler(new MouseMoveHandler() {

			public void onMouseMove(MouseMoveEvent event) {

				int x = event.getClientX() - getAbsoluteLeft();
				int y = event.getClientY() - getAbsoluteTop();
				// VPoint p = new VPoint(x, y);
				// VUIElement ele = null;
				// for(VUIElement element: childrenList){
				// if(element.contains(p)){
				// ele = element;
				// }
				// }
				//
				if (client == null) {
					return;
				}
				// if (ele != null) {
				// //p("mouseMOVE: Adding "+ele+" to variable change");
				// client.updateVariable(paintableId, "childid", ele.getId(),
				// false);
				// client.updateVariable(paintableId, "class",
				// ele.getClass().getName(), false);
				// }
				client.updateVariable(paintableId, "mx", x, false);
				client.updateVariable(paintableId, "my", y, false);
				client.updateVariable(paintableId, "event", "mousemove", true);
			}
		});

		canvas.addMouseOutHandler(new MouseOutHandler() {
			public void onMouseOut(MouseOutEvent event) {
				mouseX = -200;
				mouseY = -200;

				mouseX = event.getRelativeX(canvas.getElement());
				mouseY = event.getRelativeY(canvas.getElement());

				int x = event.getClientX() - getAbsoluteLeft();
				int y = event.getClientY() - getAbsoluteTop();

				/*
				 * VPoint p = new VPoint(x, y); for(VUIElement element:
				 * childrenList){ if(element.contains(p)){
				 * MouseOutEvent.fireNativeEvent(event.getNativeEvent(),
				 * element); } }
				 */
			}
		});

		canvas.addTouchMoveHandler(new TouchMoveHandler() {
			public void onTouchMove(TouchMoveEvent event) {
				event.preventDefault();
				if (event.getTouches().length() > 0) {
					Touch touch = event.getTouches().get(0);
					mouseX = touch.getRelativeX(canvas.getElement());
					mouseY = touch.getRelativeY(canvas.getElement());

					// VPoint p = new VPoint(mouseY, mouseY);
					// for(VUIElement element: childrenList){
					// if(element.contains(p)){
					// TouchMoveEvent.fireNativeEvent(event.getNativeEvent(),
					// element);
					// }
					// }
				}
				// event.preventDefault();
			}
		});

		canvas.addTouchEndHandler(new TouchEndHandler() {
			public void onTouchEnd(TouchEndEvent event) {
				event.preventDefault();
				mouseX = -200;
				mouseY = -200;
			}
		});

		canvas.addGestureStartHandler(new GestureStartHandler() {
			public void onGestureStart(GestureStartEvent event) {
				event.preventDefault();
			}
		});
	}

	private static void p(String msg) {
		com.allen_sauer.gwt.log.client.Log.debug("VCanvas: " + msg);
	}

	/**
	 * Called whenever an update is received from the server
	 */
	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
		// This call should be made first. Ensure correct implementation,
		// and let the containing layout manage caption, etc.
		if (client.updateComponent(this, uidl, true)) {
			return;
		}

		// Save reference to server connection object to be able to send
		// user interaction later
		this.client = client;

		// Save the UIDL identifier for the component
		paintableId = uidl.getId();

		// canvas.clear(); // Always redraw fully

		for (Iterator<Object> iter = uidl.getChildIterator(); iter.hasNext();) {
			UIDL childUIDL = (UIDL) iter.next();
			String command = childUIDL.getTag().toLowerCase();
			if (command.equals("stroke")) {
				context.stroke();
			} else if (command.equals("restorecontext")) {
				context.restore();
			} else if (command.equals("savecontext")) {
				context.save();
			} else if (command.equals("beginpath")) {
				context.beginPath();
			} else if (command.equals("closepath")) {
				context.closePath();
			} else if (command.equals("clear")) {
				context.setFillStyle(redrawColor);
				context.fillRect(0, 0, width, height);
			} else if (command.equals("setglobalalpha")) {
				handleSetGlobalAlpha(childUIDL);
			} else if (command.equals("addcolorstop")) {
				handleAddColorStop(childUIDL);
			} else if (command.equals("setglobalcompositeoperation")) {
				handleSetGlobalCompositeOperation(childUIDL);
			} else if (command.equals("setstrokecolor")) {
				handleStrokeColorCommand(childUIDL);
			} else if (command.equals("setbackgroundcolor")) {
				handleBackgroundColorCommand(childUIDL);
			} else if (command.equals("translate")) {
				handleTranslateCommand(childUIDL);
			} else if (command.equals("scale")) {
				handleScaleCommand(childUIDL);
			} else if (command.equals("arc")) {
				handleArcCommand(childUIDL);
			} else if (command.equals("createlineargradient")) {
				handleCreateLinearGradient(childUIDL);
			} else if (command.equals("createradialgradient")) {
				handleCreateRadialGradient(childUIDL);
			} else if (command.equals("cubiccurveto")) {
				handleCubicCurveTo(childUIDL);
			} else if (command.equals("drawimage1")) {
				handleDrawImage1(childUIDL);
			} else if (command.equals("setbackground")) {
				this.handleBgImage(childUIDL);
			} else if (command.equals("drawimage2")) {
				handleDrawImage2(childUIDL);
			} else if (command.equals("drawimage3")) {
				handleDrawImage3(childUIDL);
			} else if (command.equals("fill")) {
				context.fill();
			} else if (command.equals("fillrect")) {
				handleFillRect(childUIDL);
			} else if (command.equals("lineto")) {
				handleLineTo(childUIDL);
			} else if (command.equals("moveto")) {
				handleMoveTo(childUIDL);
			} else if (command.equals("quadraticcurveto")) {
				handleQuadraticCurveTo(childUIDL);
			} else if (command.equals("rect")) {
				handleRect(childUIDL);
			} else if (command.equals("rotate")) {
				handleRotate(childUIDL);
			} else if (command.equals("setgradientfillstyle")) {
				handleSetGradientFillStyle(childUIDL);
			} else if (command.equals("setfillstyle")) {
				handleSetFillStyle(childUIDL);
			} else if (command.equals("setlinecap")) {
				handleSetLineCap(childUIDL);
			} else if (command.equals("setlinekoin")) {
				handleSetLineJoin(childUIDL);
			} else if (command.equals("setlinewidth")) {
				handleSetLineWidth(childUIDL);
			} else if (command.equals("setmiterlimit")) {
				handleSetMiterLimit(childUIDL);
			} else if (command.equals("setcolorstrokestyle")) {
				handleSetColorStrokeStyle(childUIDL);
			} else if (command.equals("setgradientstrokestyle")) {
				handleSetGradientStrokeStyle(childUIDL);
			} else if (command.equals("strokerect")) {
				handleStrokeRect(childUIDL);
			} else if (command.equals("transform")) {
				handleTransform(childUIDL);
			} else if (command.equals("drawrect")) {
				handleDrawRect(childUIDL);
			} else if (command.equals("draw")) {
				handleDrawElement(childUIDL);
			} else {
				System.err.println("Invalid command: " + command);
			}
		}
	}

	private void handleAddColorStop(UIDL uidl) {
		String gradientName = uidl.getStringAttribute("gradient");
		if (!gradients.containsKey(gradientName)) {
			System.err.println("handleAddColorStop: Gradient not foud with name " + gradientName);
			return;
		}

		double offset = uidl.getDoubleAttribute("offset");
		String color = uidl.getStringAttribute("color");
		gradients.get(gradientName).addColorStop(offset, color);
	}

	private void handleSetGlobalCompositeOperation(UIDL uidl) {
		String mode = uidl.getStringAttribute("mode");
		// if (mode.equalsIgnoreCase("DESTINATION_OVER")) {
		// context.setGlobalCompositeOperation("destination-over");
		// } else if (mode.equalsIgnoreCase("SOURCE_OVER")) {
		// context.setGlobalCompositeOperation("source-over");
		// } else {
		context.setGlobalCompositeOperation(mode);
		// }
	}

	private void handleSetGlobalAlpha(UIDL uidl) {
		double alpha = uidl.getDoubleAttribute("alpha");

		context.setGlobalAlpha(alpha);
	}

	private void handleCreateLinearGradient(UIDL uidl) {
		String name = uidl.getStringAttribute("name");
		double x0 = uidl.getDoubleAttribute("x0");
		double y0 = uidl.getDoubleAttribute("y0");
		double x1 = uidl.getDoubleAttribute("x1");
		double y1 = uidl.getDoubleAttribute("y1");

		CanvasGradient newGradient = context.createLinearGradient(x0, y0, x1, y1);

		gradients.put(name, newGradient);
	}

	private void handleCreateRadialGradient(UIDL uidl) {
		String name = uidl.getStringAttribute("name");
		double x0 = uidl.getDoubleAttribute("x0");
		double y0 = uidl.getDoubleAttribute("y0");
		double r0 = uidl.getDoubleAttribute("r0");
		double x1 = uidl.getDoubleAttribute("x1");
		double y1 = uidl.getDoubleAttribute("y1");
		double r1 = uidl.getDoubleAttribute("r1");

		CanvasGradient newGradient = context.createRadialGradient(x0, y0, r0, x1, y1, r1);

		gradients.put(name, newGradient);
	}

	private void handleCubicCurveTo(UIDL uidl) {
		double cp1x = uidl.getDoubleAttribute("cp1x");
		double cp1y = uidl.getDoubleAttribute("cp1y");
		double cp2x = uidl.getDoubleAttribute("cp2x");
		double cp2y = uidl.getDoubleAttribute("cp2y");
		double x = uidl.getDoubleAttribute("x");
		double y = uidl.getDoubleAttribute("y");

		context.bezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y);
	}

	private void handleBgImage(UIDL uidl) {
		String url = uidl.getStringAttribute("url");

		//p("Got bg image URL:" + url);
		// Canvas.drawImage won't show the image unless it's already loaded
		// URL test = new URL(url);
		Image.prefetch(url);

		Image image = new Image(url);
		bgimage = image;
		//p("Setting background image");
		drawBg();
		//RootPanel.get().add(image);
		context.drawImage(ImageElement.as(image.getElement()), 0, 0);
	}

	private void handleDrawImage1(UIDL uidl) {
		String url = uidl.getStringAttribute("url");
		final double offsetX = uidl.getDoubleAttribute("offsetX");
		final double offsetY = uidl.getDoubleAttribute("offsetY");

		// Canvas.drawImage won't show the image unless it's already loaded
		Image.prefetch(url);

		Image image = new Image(url);
		if (canvas.getElement().getNodeName().equalsIgnoreCase("canvas")) {
			image.addLoadHandler(new LoadHandler() {
				public void onLoad(LoadEvent event) {
					Image loaded = (Image) event.getSource();
					context.drawImage(ImageElement.as(loaded.getElement()), offsetX, offsetY);
				}
			});
			image.setVisible(false);
			RootPanel.get().add(image);
		} else {
			context.drawImage(ImageElement.as(image.getElement()), offsetX, offsetY);
		}

		context.drawImage(ImageElement.as(image.getElement()), offsetX, offsetY);
		// backBufferContext.drawImage(ImageElement.as(image.getElement()),
		// offsetX, offsetY);
	}

	private void handleDrawImage2(UIDL uidl) {
		String url = uidl.getStringAttribute("url");
		double offsetX = uidl.getDoubleAttribute("offsetX");
		double offsetY = uidl.getDoubleAttribute("offsetY");
		double height = uidl.getDoubleAttribute("height");
		double width = uidl.getDoubleAttribute("width");

		// Canvas.drawImage won't show the image unless it's already loaded
		Image.prefetch(url);

		Image image = new Image(url);
		context.drawImage(ImageElement.as(image.getElement()), offsetX, offsetY, height, width);
		// backBufferContext.drawImage(ImageElement.as(image.getElement()),
		// offsetX, offsetY);
	}

	private void handleDrawImage3(UIDL uidl) {
		String url = uidl.getStringAttribute("url");
		double sourceX = uidl.getDoubleAttribute("sourceX");
		double sourceY = uidl.getDoubleAttribute("sourceY");
		double sourceHeight = uidl.getDoubleAttribute("sourceHeight");
		double sourceWidth = uidl.getDoubleAttribute("sourceWidth");
		double destX = uidl.getDoubleAttribute("destX");
		double destY = uidl.getDoubleAttribute("destY");
		double destHeight = uidl.getDoubleAttribute("destHeight");
		double destWidth = uidl.getDoubleAttribute("destWidth");

		// Canvas.drawImage won't show the image unless it's already loaded
		Image.prefetch(url);

		Image image = new Image(url);
		context.drawImage(ImageElement.as(image.getElement()), sourceX, sourceY, sourceHeight, sourceWidth, destX, destY, destHeight, destWidth);
	}

	private void drawBg() {
		if (bgimage != null) {
			// p("Drawing bg image");
			// context.drawImage(ImageElement.as(bgimage.getElement()), 0, 0);
			// this.backBufferContext.draw
			this.backBufferContext.drawImage(ImageElement.as(bgimage.getElement()), 0, 0);
		}
	}

	private void handleFillRect(UIDL uidl) {
		double startX = uidl.getDoubleAttribute("startX");
		double startY = uidl.getDoubleAttribute("startY");
		double width = uidl.getDoubleAttribute("width");
		double height = uidl.getDoubleAttribute("height");

		context.fillRect(startX, startY, width, height);
	}

	private void handleLineTo(UIDL uidl) {
		double x = uidl.getDoubleAttribute("x");
		double y = uidl.getDoubleAttribute("y");

		context.lineTo(x, y);
	}

	private void handleMoveTo(UIDL uidl) {
		double x = uidl.getDoubleAttribute("x");
		double y = uidl.getDoubleAttribute("y");

		context.moveTo(x, y);
	}

	private void handleQuadraticCurveTo(UIDL uidl) {
		double cpx = uidl.getDoubleAttribute("cpx");
		double cpy = uidl.getDoubleAttribute("cpy");
		double x = uidl.getDoubleAttribute("x");
		double y = uidl.getDoubleAttribute("y");

		context.quadraticCurveTo(cpx, cpy, x, y);
	}

	private void handleRect(UIDL uidl) {
		double startX = uidl.getDoubleAttribute("startX");
		double startY = uidl.getDoubleAttribute("startY");
		double width = uidl.getDoubleAttribute("width");
		double height = uidl.getDoubleAttribute("height");

		context.rect(startX, startY, width, height);
	}

	private void handleRotate(UIDL uidl) {
		double angle = uidl.getDoubleAttribute("angle");

		context.rotate(angle);
	}

	private void handleSetGradientFillStyle(UIDL uidl) {
		String gradientName = uidl.getStringAttribute("gradient");

		if (gradients.containsKey(gradientName)) {
			context.setFillStyle(gradients.get(gradientName));
		} else {
			System.out.println("handleSetGradientFillStyle: Gradient not foud with name " + gradientName);
		}
	}

	private void handleSetFillStyle(UIDL uidl) {
		String color = uidl.getStringAttribute("color");

		if (color.equalsIgnoreCase("transparent")) {
			context.setFillStyle("");
		} else {
			context.setFillStyle(color);
		}
	}

	private void handleSetLineCap(UIDL uidl) {
		String lineCap = uidl.getStringAttribute("lineCap");
		context.setLineCap(lineCap);
	}

	private void handleSetLineJoin(UIDL uidl) {
		String lineJoin = uidl.getStringAttribute("lineJoin");

		context.setLineJoin(lineJoin);
	}

	private void handleSetLineWidth(UIDL uidl) {
		double width = uidl.getDoubleAttribute("width");

		context.setLineWidth(width);
	}

	private void handleSetMiterLimit(UIDL uidl) {
		double miterLimit = uidl.getDoubleAttribute("miterLimit");

		context.setMiterLimit(miterLimit);
	}

	private void handleSetGradientStrokeStyle(UIDL uidl) {
		String gradientName = uidl.getStringAttribute("gradient");

		if (gradients.containsKey(gradientName)) {
			context.setStrokeStyle(gradients.get(gradientName));
		} else {
			System.out.println("handleSetStrokeStyle: Gradient not found with name " + gradientName);
		}
	}

	private void handleSetColorStrokeStyle(UIDL uidl) {
		String color = uidl.getStringAttribute("color");

		context.setStrokeStyle(color);
	}

	private void handleStrokeRect(UIDL uidl) {
		double startX = uidl.getDoubleAttribute("startX");
		double startY = uidl.getDoubleAttribute("startY");
		double width = uidl.getDoubleAttribute("width");
		double height = uidl.getDoubleAttribute("height");

		context.strokeRect(startX, startY, width, height);
	}

	private void handleTransform(UIDL uidl) {
		double m11 = uidl.getDoubleAttribute("m11");
		double m12 = uidl.getDoubleAttribute("m12");
		double m21 = uidl.getDoubleAttribute("m21");
		double m22 = uidl.getDoubleAttribute("m22");
		double dx = uidl.getDoubleAttribute("dx");
		double dy = uidl.getDoubleAttribute("dy");

		context.transform(m11, m12, m21, m22, dx, dy);
	}

	private void handleBackgroundColorCommand(UIDL uidl) {
		String rgb = uidl.getStringAttribute("rgb");

		redrawColor = CssColor.make(rgb);
		backBufferContext.setFillStyle(redrawColor);
		backBufferContext.fillRect(0, 0, width, height);

		// context.setBackgroundColor(rgb);
	}

	private void handleStrokeColorCommand(UIDL uidl) {
		String rgb = uidl.getStringAttribute("rgb");

		context.setStrokeStyle(rgb);
	}

	private void handleArcCommand(UIDL uidl) {
		double x = uidl.getDoubleAttribute("x");
		double y = uidl.getDoubleAttribute("y");
		double radius = uidl.getDoubleAttribute("radius");
		double startAngle = uidl.getDoubleAttribute("startAngle");
		double endAngle = uidl.getDoubleAttribute("endAngle");
		boolean antiClockwise = uidl.getBooleanAttribute("antiClockwise");

		context.arc(x, y, radius, startAngle, endAngle, antiClockwise);
	}

	private void handleTranslateCommand(UIDL uidl) {
		double x = uidl.getDoubleAttribute("x");
		double y = uidl.getDoubleAttribute("y");

		context.translate(x, y);
	}

	private void handleScaleCommand(UIDL uidl) {
		double x = uidl.getDoubleAttribute("x");
		double y = uidl.getDoubleAttribute("y");

		context.scale(x, y);
	}

	private void handleDrawRect(UIDL uidl) {
		context.save();
		String strokecolor = uidl.getStringAttribute("strokecolor");
		double strokewidth = uidl.getDoubleAttribute("strokewidth");
		double x = uidl.getDoubleAttribute("x");
		double y = uidl.getDoubleAttribute("y");
		double w = uidl.getDoubleAttribute("w");
		double h = uidl.getDoubleAttribute("h");
		String fillStyleColor = uidl.getStringAttribute("fillstyle");

		if (strokecolor.length() > 0) {
			context.setStrokeStyle(strokecolor);
		}
		if (strokewidth > 0) {
			context.setLineWidth(strokewidth);
		}
		if (fillStyleColor.length() > 0) {
			context.setFillStyle(fillStyleColor);
		}
		context.beginPath();
		context.strokeRect(x, y, w, h);
		context.closePath();

		if (fillStyleColor.length() > 0) {
			context.fillRect(x, y, w, h);
		}

		context.restore();
	}

	private void handleDrawElement(UIDL uidl) {

		String id = uidl.getStringAttribute("elementid");
		if (children.get(id) != null) {
			VUIElement ele = children.get(id);
			ele.update(uidl);
		} else {
			VUIElement ele = VUIElement.createFromUIDL(uidl, this);
			addChild(ele);
		}
	}

	@Override
	public void setWidth(String width) {
		try {
			int newWidth = Integer.parseInt(width.replace("px", ""));
			if (newWidth == widthCache) {
				return;
			}

			widthCache = newWidth;

		} catch (Exception ignored) {
			System.out.println("Width '" + width + "' not an integer");
			return;
		}

		super.setWidth(width);
		canvas.setWidth(width);

		// This will reset the contents
		canvas.setCoordinateSpaceWidth(widthCache);

		// Request a redraw
		if (client != null) {
			client.updateVariable(paintableId, "sizeChanged", "" + widthCache + " x " + heightCache, true);
		}
	}

	@Override
	public void setHeight(String height) {

		try {
			int newHeight = Integer.parseInt(height.replace("px", ""));
			if (newHeight == heightCache) {
				return;
			}

			heightCache = newHeight;
		} catch (Exception ignored) {
			System.out.println("Height '" + height + "' not an integer");
			return;
		}

		super.setHeight(height);
		canvas.setHeight(height);

		// This will reset the contents
		canvas.setCoordinateSpaceHeight(heightCache);

		// Request redraw
		if (client != null) {
			client.updateVariable(paintableId, "sizeChanged", "" + widthCache + " x " + heightCache, true);
		}
	}

	public int addChild(VUIElement child) {
		int index = this.childrenList.size();
		if (child.getId() == null || child.getId().length() == 0) {
			child.setId(getNewId());
		}
		if (this.children.containsKey(child.getId())) {
			return -1;
		}
		this.childrenList.add(child);
		this.children.put(child.getId(), child);
		return index;
	}

	private String getNewId() {
		return "auto-gen-" + this.childrenList.size();
	}

	public int removeChild(VUIElement child) {
		if (!this.children.containsKey(child.getId())) {
			return -1;
		}
		int index = this.childrenList.indexOf(child);
		this.childrenList.remove(child);
		this.children.remove(child.getId());
		return index;
	}

	public VUIElement getChild(String childId) {
		return this.children.get(childId);
	}

	public HandlerRegistration addMouseEventHandler(final EventHandler handler, MouseEvent.Type type) {
		return addHandler(handler, type);
	}

	public VUIElement elementUnderPoint(VPoint p) {
		for (VUIElement element : childrenList) {
			if (element.elementUnderPoint(p) != null) {
				return element;
			}
		}

		return null;
	}

	public List<VUIElement> elementsUnderPoint(VPoint p) {
		List<VUIElement> list = new ArrayList<VUIElement>();
		for (VUIElement element : childrenList) {
			list.addAll(element.elementsUnderPoint(p));
		}

		return list;
	}

	public int getElementIndex(VUIElement element) {
		return this.childrenList.indexOf(element);
	}

	public int shiftUp(VUIElement element) {
		return shiftBy(element, 1);
	}

	public int shiftDown(VUIElement element) {
		return shiftBy(element, -1);
	}

	public int shiftBy(VUIElement element, int delta) {
		synchronized (childrenList) {
			int i = this.childrenList.indexOf(element);
			int j;

			if (delta == 0) {
				return i;
			} else if (i + delta >= this.childrenList.size()) {
				j = this.childrenList.size() - 1;
			} else if (i + delta < 0) {
				j = 0;
			} else {
				j = i + delta;
			}

			if (i > j) {
				for (; i > j;) {
					this.childrenList.set(i, this.childrenList.get(--i));
				}
			} else {
				for (; i < j;) {
					this.childrenList.set(i, this.childrenList.get(++i));
				}
			}
			this.childrenList.set(i, element);

			return j;
		}
	}
}
