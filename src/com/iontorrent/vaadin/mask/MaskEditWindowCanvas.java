/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.mask;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.rawdataaccess.wells.BitMask;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.torrentscout.explorer.edit.AbstractOperation;
import com.iontorrent.torrentscout.explorer.edit.MaskCommandParser;
import com.iontorrent.torrentscout.explorer.edit.OperationFactory;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.fit.FitWindowCanvas;
import com.iontorrent.vaadin.process.ProcessWindowCanvas;
import com.iontorrent.vaadin.utils.ExportTool;
import com.iontorrent.vaadin.utils.FileBrowserWindow;
import com.iontorrent.vaadin.utils.InputDialog;
import com.iontorrent.vaadin.utils.OptionsDialog;
import com.iontorrent.vaadin.utils.InputDialog.Recipient;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.graphics.canvas.Canvas;
import com.vaadin.graphics.canvas.shape.Point;
import com.vaadin.graphics.canvas.shape.UIElement;
import com.vaadin.terminal.StreamResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Select;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class MaskEditWindowCanvas extends WindowOpener implements Property.ValueChangeListener {

	private TSVaadin app;
	ExplorerContext maincont;
	ExperimentContext exp;
	MaskSelect maskselect;
	MaskCommandParser parser;
	MaskSelect sela;
	MaskSelect selb;
	MaskSelect selc;
	Select selop;
	TextArea text;
	int gradmin;
	int gradmax;
	String savefile;

	Canvas canvashist;

	BitMask curmask;
	BitMask maska;
	BitMask maskb;
	HorizontalLayout hor;
	AbstractOperation op;
	TabSheet tabsheet;
	String lastcalc;
	VerticalLayout editLayout;
	VerticalLayout pickLayout;
	VerticalLayout calcLayout;
	Component selectedtab = null;

	public MaskEditWindowCanvas(TSVaadin app, Window main, String description, int x, int y) {
		super("Edit masks and export", main, description, x, y, 490, 550);
		this.app = app;

	}

	@Override
	public void openButtonClick(Button.ClickEvent event) {
		if (app.getExperimentContext() == null) {
			mainwindow.showNotification("No Experiment Selected", "<br/>Please open an experiment first", Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		exp = app.getExperimentContext();
		maincont = app.getExplorerContext();
		maincont.setAbsCenterAreaCoord(exp.getWellContext().getAbsoluteCoordinate());
		super.openButtonClick(event);
	}

	@Override
	public void windowOpened(final Window mywindow) {
		p("====================== windowOpened with curmask: " + curmask + " ===============================");
		p("Center Coord: " + maincont.getAbsCenterAreaCoord());
		// p("Curmask: "+curmask);
		String name = null;
		if (curmask != null) name = curmask.getName();
		if (curmask == null || !curmask.getRelCoord().equals(maincont.getRelativeCorner())) {
			if (name == null) name = "keypass";
			if (curmask != null) {
				p("Mask corner coord " + curmask.getRelCoord() + " is not same as corner" + maincont.getRelativeCorner());
			}
			p("Need to find new curmask, ideally " + name);
			if (maincont.getMasks() != null && maincont.getMasks().size() > 0) {
				curmask = find(name, true);
				if (curmask == null)curmask = find(name, false);
				if (curmask == null) curmask = maincont.getMasks().get(0);
			}
		}
		p("Curmask is now: " + curmask);
		maincont.setPreferrednrwidgets(5);

		createGui();

	}

	private void saveFile(boolean ask) {
		File f = new File("/tmp/mymask.mask");
		app.logModule(getName(), "save mask");
		if (savefile != null) f = new File(savefile);
				
			FileBrowserWindow browser = new FileBrowserWindow("Pick a .mask file", null, new FileBrowserWindow.Recipient() {
				@Override
				public void fileSelected(File file) {
					p("Got file:"+file);
					if (file != null && !file.isDirectory()) {
						String msg = maincont.storeContext(file.toString());
						if (msg == null || msg.length()<1) {
							app.showMessage("Saved", "Stored all masks in file "+file);
						}
						else app.showMessage("Problem", "Could not save to file "+file+"<br>"+msg);
					}
					else app.showMessage("Not saving", "Won't save to file "+file);
					
				}
				public boolean allowInList(File f, boolean toSave) {
					if (f == null) return false;
					if (f.isDirectory()) {
						if (!f.canRead()) return false;
						String dir= f.getAbsolutePath().toString();
						if (dir.startsWith("/") || dir.startsWith("\"")) dir = dir.substring(1);
						if (dir.startsWith("etc/")|| dir.startsWith("init.d/")) return false;
					}
					
					return true;
				}
				
			}, FileBrowserWindow.SAVE, mainwindow, f, ".mask");
			browser.open();
	}
	private void openFile(boolean ask) {
		File f = new File("/tmp/mymasks.mask");
		if (savefile != null) f = new File(savefile);
				
			FileBrowserWindow browser = new FileBrowserWindow("Pick a .mask file", null,  new FileBrowserWindow.Recipient() {
				@Override
				public void fileSelected(File file) {
					p("Got file:"+file);
					if (file != null && file.isFile()) {
						p("++++++++++++++++++++++ loading context+++++++++++++++++++++");
						String msg = maincont.loadContext(file.toString());
						int x = maincont.getRelativeCenterAreaCoord().getCol();
						int y = maincont.getRelativeCenterAreaCoord().getRow();
						int r = maincont.getRasterSize()/2;
						if (maincont.getWidgets()!= null) maincont.getWidgets().clear();
						x = Math.max(0, x-r);
						y = Math.max(0, y-r);
						//maincont.setRelDataAreaCoord(new WellCoordinate(x, y));
						p("Got ExpCoord: "+maincont.getExp().getWellContext().getCoordinate());						
						p("Got center: "+maincont.getRelativeCenterAreaCoord());
						p("Got mask corner coord: "+maincont.getMasks().get(0).getRelCoord());
						ProcessWindowCanvas process = app.getProcessWindow();
						p("Got coord in process: "+process.getCoord());
						//p("Got mask coord: "+maincont.getd);
						ArrayList<BitMask> masks = maincont.getMasks();
						process.clear();
						process.loadData();
						curmask = null;											
						app.reopenProcess(false);
						p("++++++++++++++++++++++ SETTING MASKS");
						maincont.setMasks(masks);
						String names = "";
						for (BitMask m: maincont.getMasks()) {
							p("Got: "+m.getName());
						}	
						maincont.setMasks(masks);
						app.reopenProcess(false);
						reopen();						
						app.reopenAutomate();
						
						if (msg == null || msg.length()<1) {
							 names = "";
							for (BitMask m: maincont.getMasks()) {
								names+=m.getName()+"<br>";
							}							
							app.showLongMessage("Loaded", "Loaded all masks at "+maincont.getAbsCenterAreaCoord()+" from file "+file+"<br>"+names);							
							
						}
						else app.showMessage("Problem", "Could not load masks from file "+file+"<br>"+msg);
						p("+++++++++++++++++++++ loading context DONE +++++++++++++++++++++");
					}
					
				}
				public boolean allowInList(File f, boolean toSave) {
					if (f == null) return false;
					if (f.isDirectory()) {
						if (!f.canRead()) return false;
						String dir= f.getAbsolutePath().toString();
						if (dir.startsWith("/") || dir.startsWith("\"")) dir = dir.substring(1);
						if (dir.startsWith("etc/")|| dir.startsWith("init.d/")) return false;
					}
					
					return true;
				}
				
			},mainwindow, f, ".mask");
			browser.open();
	}


	private void createGui() {
		tabsheet = new TabSheet();
		tabsheet.setWidth("450px");
		tabsheet.setHeight("450px");

		mywindow.addComponent(tabsheet);

		pickLayout = new VerticalLayout();
		// chartTab.addComponent(new Label("Chart"));
		tabsheet.addTab(pickLayout);
		tabsheet.getTab(pickLayout).setCaption("Pick Masks");

		editLayout = new VerticalLayout();
		tabsheet.addTab(editLayout);
		tabsheet.getTab(editLayout).setCaption("View/Save");

		calcLayout = new VerticalLayout();
		// chartTab.addComponent(new Label("Chart"));
		tabsheet.addTab(calcLayout);
		tabsheet.getTab(calcLayout).setCaption("Calculate");

		VerticalLayout helpLayout = new VerticalLayout();
		// chartTab.addComponent(new Label("Chart"));
		tabsheet.addTab(helpLayout);
		tabsheet.getTab(helpLayout).setCaption("Help");

		addEditorGui(editLayout);
		addCalcGui(calcLayout);
		addPickGui(pickLayout);
		addHelpGui(helpLayout);

		if (selectedtab == null) selectedtab = editLayout;
		if (selectedtab != null) {
			p("Got tab:" + selectedtab);
			tabsheet.setImmediate(true);
			tabsheet.setSelectedTab(selectedtab);

		}

		tabsheet.addListener(new SelectedTabChangeListener() {

			@Override
			public void selectedTabChange(SelectedTabChangeEvent event) {
				selectedtab = tabsheet.getSelectedTab();
				p("selected tab: " + selectedtab);
			}

		});
	}

	private class CalcListener implements Property.ValueChangeListener {
		public void valueChange(Property.ValueChangeEvent event) {
			Property id = event.getProperty();
			if (id.getValue() instanceof BitMask) {
				BitMask m = (BitMask) id.getValue();
				curmask = m;
				p("Current mask is: " + curmask);
				selectedtab = calcLayout;

			}
		}
	}

	private class PickListener implements Property.ValueChangeListener {
		public void valueChange(Property.ValueChangeEvent event) {
			Property id = event.getProperty();
			if (id.getValue() instanceof BitMask) {
				BitMask m = (BitMask) id.getValue();
				curmask = m;
				p("Current mask is: " + curmask);
				selectedtab = pickLayout;
			}
		}
	}

	private void addCalcGui(AbstractOrderedLayout parent) {
		VerticalLayout ver = new VerticalLayout();

		HorizontalLayout hor = new HorizontalLayout();
		ArrayList<BitMask> masks = maincont.getMasks();

		// p("masks are now: ");
		// for (BitMask m: maincont.getMasks()) {
		// p(m.getName());
		// }
		if (maska == null) maska = find("bead", false);
		if (maska == null) maska = curmask;
		if (maskb == null) maskb = find("dud", false);
		if (maskb == null) maskb = masks.get(1);

		sela = new MaskSelect("first", null, "First argument", maincont, -1, null, maska);
		selb = new MaskSelect("second", null, "Second argument (depends on type of operation)", maincont, -1, null, maskb);
		selc = new MaskSelect("result", null, "Resulting mask", maincont, -1, null, null);

		sela.addGuiElements(hor);

		selop = new Select();
		int i = 0;
		for (AbstractOperation aop : OperationFactory.getOps()) {
			selop.addItem(aop);
			if (i == 0) {
				selop.setValue(aop);
				this.op = aop;
			}
			i++;
		}

		selop.setWidth("70px");
		selop.addListener(new Property.ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				Property id = event.getProperty();
				if (id.getValue() instanceof AbstractOperation) {
					op = (AbstractOperation) id.getValue();
					int nr = op.getNrArgs();
					if (nr == 1) {
						selb.setEnabled(false);
					} else
						selb.setEnabled(true);
				}

			}
		});
		hor.addComponent(selop);
		// add operators
		selb.addGuiElements(hor);
		selc.addGuiElements(hor);
		selc.addItem("New Mask");

		// add execute button
		Button ex = new Button("Execute");
		hor.addComponent(ex);
		ex.addListener(new Button.ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				String msg = "";
				final BitMask a = sela.getSelection();
				if (a == null) {
					app.showMessage("Mask Calculator", "Select a mask");
					return;
				}
				if (op == null) {
					app.showMessage("Mask Calculator", "Select an operation");
					return;
				}
				int nr = op.getNrArgs();
				final BitMask b = selb.getSelection();
				if (b == null) {
					if (nr > 1) {
						app.showMessage("Mask Calculator", "Select a second mask");
						return;
					}
				}

				curmask = selc.getSelection();
				if (curmask != null) compute(a, b, curmask);
				else {
					InputDialog input = new InputDialog(mainwindow, "Name of result mask: ", new Recipient() {

						@Override
						public void gotInput(String name) {
							if (name == null || name.length() < 1) {
								return;
							}
							curmask = new BitMask(a);
							curmask.setName(name);
							p("Created mask with name: " + name);
							maincont.addMask(curmask);
							compute(a, b, curmask);
						}

					});
				}

			}

		});

		// add text field
		parent.addComponent(ver);
		ver.addComponent(hor);

		// VerticalLayout h = new VerticalLayout();
		text = new TextArea();
		text.setWidth("430px");
		text.setHeight("70px");
		if (lastcalc != null) text.setValue(lastcalc);
		else
			text.setValue("bgmask = empty and (not pinned)");

		text.setImmediate(true);
		ver.addComponent(text);

		Label help = new Label(this.getHelpText(), Label.CONTENT_XHTML);

		Button parse = new Button("Parse");
		ver.addComponent(parse);
		ver.addComponent(help);
		parse.addListener(new Button.ClickListener() {

			@Override
			public void buttonClick(ClickEvent event) {
				String cmd = (String) text.getValue();
				String msg = "<html>" + getHelpText() + "</html>";
				if (cmd == null || cmd.length() < 1) {

					app.showMessage("Mask Calculator", msg);
					return;
				}
				for (BitMask m : maincont.getMasks()) {
					if (m.getName() == null) {
						err("Mask has no name: " + m);
					}
				}
				parser = new MaskCommandParser(maincont);
				app.logModule(getName(), "parsing "+cmd);
				String err = parser.doParseAction(cmd);
				curmask = parser.getResult();
				if (err != null) app.showMessage("Mask Editor", err);
				reopen();
				tabsheet.setSelectedTab(editLayout);

			}
		});
		// h.addComponent(text);

		// ver.addComponent(h);

	}

	private String removeFirstPart(String n) {
		int pos = n.indexOf(" ");
		if (pos > -1) return n.substring(pos).trim();
		else
			return n;
	}

	private void compute(BitMask a, BitMask b, BitMask c) {
		String sa = a.getName();
		sa = removeFirstPart(sa);
		String msg = c.getName() + "=" + sa + " " + op.getName() + " ";

		if (b != null) {
			String sb = b.getName();
			sb = removeFirstPart(sb);
			msg += sb;
		}

		lastcalc = msg;
		p("Computing: " + msg);
		app.logModule(getName(), "compute "+msg);
		app.showMessage(this, msg);
		boolean ok = op.execute(a, b, c);
		if (!ok) {
			app.showError(this, "Calculation failed");
		}
		// maincont.maskChanged(c);

		reopen();
		tabsheet.setSelectedTab(this.editLayout);
	}

	private void addEditorGui(AbstractOrderedLayout v) {
		hor = new HorizontalLayout();
		maskselect = new MaskSelect("show", "Show mask", "Pick a mask to view", maincont, -1, (Property.ValueChangeListener) this, curmask);

		maskselect.addGuiElements(hor);

		final Button open = new Button();
		open.setIcon(new ThemeResource("img/document-open-2.png"));
		open.setDescription("Load masks that you have previously saved");
		open.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				openFile(true);
			}

		});
		hor.addComponent(open);
		
		final Button export = new Button();
		export.setIcon(new ThemeResource("img/export.png"));
		export.setDescription("Save masks or export alignments, ionograms and/or raw data");
		export.addListener(new Button.ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				doExportAction();
			}

		});
		hor.addComponent(export);
		v.addComponent(hor);

		HorizontalLayout otherhor = new HorizontalLayout();
		v.addComponent(otherhor);

		// maybe add a second mask?
		addMaskPanel(otherhor, curmask);
		// addMaskPanel(hor, curmask);

	}

	public void doExportAction() {
		OptionsDialog input = new OptionsDialog(mainwindow,
				"What would you like to export?", "Export...",
				"... save masks to file", "... other data such as raw data, ionograms, alignments",
				new OptionsDialog.Recipient() {

					@Override
					public void gotInput(final int selection) {
						if (selection < 0)
							return;
						// / do the search
						if (selection == 0) {
							saveFile(true);
						} else{ 
							ExportTool export = new ExportTool(app, mainwindow, curmask, hor);
							export.doExportAction();

						}
					}

				});
	}
	private BitMask find(String name, boolean strict) {
		return maincont.find(name, strict);		
	}

	private void addHelpGui(AbstractOrderedLayout v) {
		v.addComponent(new Label(getHelpMessage(), Label.CONTENT_XHTML));
	}

	public String getHelpMessage() {
		String msg = "<ul>";
		msg += "<li>Pick the ignore, bg and signal masks used in the automate and process component</li>";
		msg += "<li>Combine masks to create your won mask (see also the Fit component)</li>";
		msg += "<li>Look at masks to make sure they look reasonable </li>";
		msg += "</ul>";
		return msg;
	}

	private void addPickGui(AbstractOrderedLayout v) {
		// VerticalLayout lay = new VerticalLayout();
		GridLayout lay = new GridLayout(2, 3);
		if (maincont.getIgnoreMask() == null) {
			ArrayList<BitMask> masks = maincont.getMasks();
			if (masks != null && masks.size() > 0) {
				maincont.setIgnoreMask(masks.get(0));
			}
		}
		Property.ValueChangeListener list = new PickListener();
		MaskSelect pin = new MaskSelect("ignore", null, "Pick the ignore mask that is used by other components", maincont, MaskSelect.PIN, list, maincont.getIgnoreMask());
		lay.addComponent(new Label("Ignore mask: "));
		pin.addGuiElements(lay);

		MaskSelect bg = new MaskSelect("bg", null, "Pick the background mask that is used by other components", maincont, MaskSelect.BG, list, maincont.getBgMask());
		lay.addComponent(new Label("Background mask: "));
		bg.addGuiElements(lay);

		MaskSelect use = new MaskSelect("signal", null, "Pick the signal masks that is used by the Automate component", maincont, MaskSelect.USE, list, maincont.getSignalMask());
		lay.addComponent(new Label("Signal mask: "));
		use.addGuiElements(lay);

		v.addComponent(lay);
		v.addComponent(new Label("Notes: <br>The signal mask is used in Automate to compute the mean signal<br>The background mask is the one used in Process for NN bg subtraction", Label.CONTENT_XHTML));

	}

	private void addMaskPanel(AbstractOrderedLayout v, BitMask curmask) {
		p("Creating bitmask image with " + curmask + " at coord " + curmask.getRelCoord());
		MaskImage himage = new MaskImage(maincont, curmask);

		StreamResource imageresource = new StreamResource((StreamResource.StreamSource) himage, exp.getFileKey() + "_" + exp.getWellContext().getAbsoluteCoordinate().getX() + "_" + exp.getWellContext().getAbsoluteCoordinate().getY() + curmask.getName() + (int) (Math.random() * 1000) + ".png", app);
		imageresource.setCacheTime(100);
		String relative = imageresource.getApplication().getRelativeLocation(imageresource);
		String appurl = imageresource.getApplication().getURL().toString();
		String url = relative;
		url = appurl + url.replace("app://", "");
		p("URl from stream resource sub: " + url);
		// app.getMainWindow().open(imageresource, "_blank");
		canvashist = new Canvas();
		// canvashist.setImmediate(true);
		canvashist.setBackgroundColor("black");
		canvashist.setWidth("400px");
		canvashist.setHeight("400px");

		String bg = url;
		canvashist.setBackgroundImage(bg);
		v.addComponent(canvashist);

		canvashist.addListener(new Canvas.CanvasMouseUpListener() {
			@Override
			public void mouseUp(Point p, UIElement child) {
				int x = (int) p.getX();
				// p("curmask: Got mouse UP: " + p + ", child=" + child);
				// int bin = himage.getBin(x);

			}
		});

	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(MaskEditWindowCanvas.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(MaskEditWindowCanvas.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(MaskEditWindowCanvas.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		//System.out.println("MaskWindowCanvas: " + msg);
		Logger.getLogger(MaskEditWindowCanvas.class.getName()).log(Level.INFO, msg);
	}

	@Override
	public void valueChange(ValueChangeEvent event) {
		Property id = event.getProperty();
		if (id.getValue() instanceof BitMask) {
			BitMask m = (BitMask) id.getValue();
			curmask = m;
			p("Show changed: Current mask is: " + curmask);
			selectedtab = editLayout;
			reopen();
			tabsheet.setSelectedTab(editLayout);

		}

	}

	private String getHelpText() {
		String msg = "<ul>";
		msg += "<li>Mask names: (say the mask is 1. ignore)<br>" + "- you can use the number 3<br>" + "- just the part aftr the space, such as bead</li>";
		msg += "<li>And operation: and, & </li>";
		msg += "<li>Or operation: or, |</li>";
		msg += "<li>Plus/Minus operation: add, +, minus, -, subtract</li>";
		msg += "<li>Not operation: not, !, ~ </li>";
		msg += "<li>Xor: xor </li>";
		msg += "<li>Copy operation: copy, duplicate </li>";
		msg += "<li>Shift diagonal: shift, diag, shiftdiag; </li>";
		msg += "<li>Shift left/right: shiftleft, left, &lt; shiftright, right, &gt; </li>";
		msg += "<li>Shift up/down: shiftup, up, ^, shiftdown, down, v </li>";
		msg += "<li>Example: ignore = (empty or pinned)</li>";
		msg += "<li>Example: newbg = (empty minus somemask)</li>";
		msg += "</ul>";
		return msg;
	}
}