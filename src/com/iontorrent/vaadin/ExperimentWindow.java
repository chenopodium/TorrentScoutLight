/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.iontorrent.expmodel.CompositeExperiment;
import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.sequenceloading.SequenceLoader;
import com.iontorrent.vaadin.utils.DataDialog;
import com.iontorrent.vaadin.utils.FileBrowserWindow;
import com.iontorrent.vaadin.utils.TextDialog;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.terminal.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class ExperimentWindow extends WindowOpener {

	private TSVaadin app;
	ExperimentContext exp;

	TextField raw;
	TextField ca;
	TextField results;
	TextField type;
	TextField rname;
	TextField sff;
	TextField bam;

	public ExperimentWindow(TSVaadin app, Window main, String description, int x, int y) {
		super("Pick folders", main, description, x, y, 570, 350);
		this.app = app;

	}

	public void setContext(ExperimentContext context) {
		this.exp = context;
		p("Setting context to: " + exp);
	}

	@Override
	public void windowOpened(final Window win) {

		// app.showMessage("Loading an experiment",
		// "<br/>Click OK to load the experiment");
		String w = "350px";

		GridLayout h = new GridLayout(3, 8);

		win.addComponent(h);
		exp = app.getExperimentContext();
		p("Got exp context:" + exp);

		boolean check = maybeCreateExp();

		rname = new TextField();
		rname.setWidth(w);
		rname.setValue(exp.getResultsName());
		rname.setImmediate(true);
		int y = 0;
		Label lbl = new Label("Results name:");
		lbl.setWidth("80px");
		h.addComponent(lbl, 0, y);
		h.addComponent(rname, 1, y++);

		type = new TextField();
		type.setWidth("50px");
		type.setImmediate(true);
		String chip = exp.getChipType();
		if (chip == null) {
			chip = "";
		}
		chip.replace("\"", "");
		type.setValue(chip);
		type.setReadOnly(true);

		h.addComponent(new Label("Chip type:"), 0, y);

		// h.setComponentAlignment(em, Alignment.MIDDLE_LEFT);
		h.addComponent(type, 1, y++);
		// h.addComponent(em, 2, y++);

		// Component with an icon from a custom theme
		results = new TextField();
		results.setWidth(w);
		results.setDescription("The folder that contains all results data, including bfmask.bin (possibly in a subfolder), 1.wells, .bam and .sff files (in the basecaller folder)");
		results.setValue(exp.getResultsDirectory());
		results.setRequired(true);
		results.setImmediate(true);
		h.addComponent(new Label("<b>Results path:</b>", Label.CONTENT_XML), 0, y);
		h.addComponent(results, 1, y);
		Button bres = new Button();
		bres.setIcon(new ThemeResource("img/folder.png"));
		bres.addListener(new Button.ClickListener() {

			public void buttonClick(Button.ClickEvent event) {
				// win.sh
				app.showMessage("Opening file browser", "<br/>Retrieving files and folders...");

				openBrowser("Pick a folder with results (.bam, .sff, bfmask.bin etc)", null, results);
			}
			// dbWindow.open();
		});
		h.addComponent(bres, 2, y++);

		raw = new TextField();
		raw.setValue(exp.getRawDir());
		raw.setWidth(w);
		raw.setRequired(true);
		raw.setDescription("Folder with the .dat files");
		raw.setImmediate(true);
		h.addComponent(new Label("<b>Raw path:</b>", Label.CONTENT_XML), 0, y);
		h.addComponent(raw, 1, y);
		Button braw = new Button();
		braw.setIcon(new ThemeResource("img/folder.png"));
		braw.addListener(new Button.ClickListener() {

			public void buttonClick(Button.ClickEvent event) {
				openBrowser("Pick a folder with .dat files", null, raw);
			}
		});
		h.addComponent(braw, 2, y++);

		ca = new TextField();
		ca.setValue(exp.getCacheDir());
		ca.setWidth(w);
		ca.setRequired(true);
		ca.setDescription("The folder where temporary index and image files are stored");
		rname.setImmediate(true);
		h.addComponent(new Label("<b>Cache path:</b>", Label.CONTENT_XML), 0, y);
		h.addComponent(ca, 1, y);
		Button bca = new Button();
		bca.setIcon(new ThemeResource("img/folder.png"));
		bca.addListener(new Button.ClickListener() {

			public void buttonClick(Button.ClickEvent event) {
				openBrowser("Pick a folder with write permission to save index files", null, ca);
			}
		});
		h.addComponent(bca, 2, y++);

		sff = new TextField();
		sff.setValue(exp.getSffFilePath());
		sff.setWidth(w);
		sff.setImmediate(true);
		sff.setDescription("To view ionograms, find reads based on quality and sequenece, and to view alignment, specify a .sff file");
		// sff.setIcon(new ThemeResource("img/document.png"));
		h.addComponent(new Label("SFF file"), 0, y);
		h.addComponent(sff, 1, y);
		Button bsff = new Button();
		bsff.setIcon(new ThemeResource("img/document.png"));
		bsff.addListener(new Button.ClickListener() {

			public void buttonClick(Button.ClickEvent event) {
				openBrowser("Pick a .sff file", ".sff", sff);
			}
		});
		h.addComponent(bsff, 2, y++);

		bam = new TextField();
		bam.setValue(exp.getBamFilePath());
		bam.setWidth(w);
		bam.setDescription("To find reads based on alignment patterns, matches and to view alignment, specify a .bam file");
		h.addComponent(new Label("BAM file"), 0, y);
		h.addComponent(bam, 1, y);
		Button bbam = new Button();
		bbam.setIcon(new ThemeResource("img/document.png"));
		bbam.addListener(new Button.ClickListener() {

			public void buttonClick(Button.ClickEvent event) {
				openBrowser("Pick a .BAM file", ".bam", bam);
			}
		});
		h.addComponent(bbam, 2, y++);

		Button ok = new Button("Ok");
		ok.setIcon(new ThemeResource("img/ok.png"));
		h.addComponent(ok, 0, y);
		// Handle button clicks
		ok.addListener(new Button.ClickListener() {

			public void buttonClick(Button.ClickEvent event) {
				// If the field value is bad, set its error.
				// (Allow only alphanumeric characters.)
				handleOk();
			}

		});
		
		Button help = new Button();
		help.setDescription("Click me to get information on this window");
		help.setIcon(new ThemeResource("img/help-hint.png"));
		h.addComponent(help, 1, y);
		help.addListener(new Button.ClickListener() {
			public void buttonClick(Button.ClickEvent event) {
				 app.showHelpMessage("Help", getHelpMessage());
			}
		});
		
		Button link = new Button();
		link.setIcon(new ThemeResource("img/link.png"));
		link.setDescription("Generate URL that automatically opens this experiment - copy it and paste it later on");
		h.addComponent(link, 2, y);
		// Handle button clicks
		link.addListener(new Button.ClickListener() {

			public void buttonClick(Button.ClickEvent event) {
				// If the field value is bad, set its error.
				// (Allow only alphanumeric characters.)
				generateLink();
			}

		});

		if (check)  checkFolders();
	}

	private void generateLink() {
		if (exp == null) return;
		//blackbird.itw?restartApplication&res_dir=/var/www/output/Home/D10-155-r25428_full_25346/&raw_dir=/ion-data/results/d10/R_2012_08_23_14_07_36_user_D10-155-r25428/
		String url = "http://"+app.getServer()+"/TSL?restartApplication&res_dir="+exp.getResultsDirectory()+"&raw_dir="+exp.getRawDir();
		TextDialog dia = new TextDialog(app.getMainWindow(), "Link", url);
		
	}
	public String getHelpMessage() {
		String msg = "<ul>";
		msg += "<li>check the folders to make sure you have the correct paths</li>";
		msg += "<li>specify the paths to your own files that are not in the database</li>";
		msg += "</ul>";
		return msg;
	}

	private boolean maybeCreateExp() {
		boolean check = true;
		if (exp == null) {
			check = false;
			exp = new ExperimentContext();
			exp.setResultsName("test experiment");
			exp.setCacheDir("/tmp/");
			exp.setResultsDirectory("/results");
			exp.setRawDir("/results");
			exp.setSffFilename("");
			exp.setBamFilename("");
			p("App server is: " + app.getServer());
			if (app.getServer().startsWith("10.25.3.124")) { // Corvette.ite
				exp.setSffFilename("");
				exp.setChipType("BB");
				exp.setBamFilename("");
				exp.setRawDir("/results/R_2011_10_26_18_31_34_user_COR-4");
				exp.setResultsDirectory("/results/R_2011_10_26_18_31_34_user_COR-4");
				exp.setCacheDir("/home/ionadmin/tmp");
			} else if ( app.getServer().toLowerCase().startsWith("pando") || app.getServer().startsWith("10.0.0.") || app.getServer().startsWith("192.168.") || app.getServer().startsWith("0.0") || app.getServer().startsWith("127.0") || app.getServer().startsWith("local")) { // localhost
				exp.setChipType("314");
				//exp.setSffFilename("R_2011_06_19_15_31_08_user_JIM-279-r118902-1t_08_enriched-br_Auto_JIM-279-r118902-1t_08_enriched-br_4711.sff");
			//	exp.setBamFilename("R_2011_06_19_15_31_08_user_JIM-279-r118902-1t_08_enriched-br_Auto_JIM-279-r118902-1t_08_enriched-br_4711.bam");
				exp.setRawDir("s:\\data\\raw\\");
				exp.setResultsDirectory("s:\\data\\results\\");
				exp.setSffFilename("xx");
				exp.setBamFilename("xx");
				// exp.setRawDir("s:\\data\\bb\\raw\\");
				// exp.setResultsDirectory("s:\\data\\bb\\res\\");
				exp.setCacheDir(exp.getResultsDirectory());
				check= true;
			} else if (app.getServer().startsWith("10.0.0.")) { // localhost
				exp.setChipType("314");
				//exp.setSffFilename("R_2011_06_19_15_31_08_user_JIM-279-r118902-1t_08_enriched-br_Auto_JIM-279-r118902-1t_08_enriched-br_4711.sff");
			//	exp.setBamFilename("R_2011_06_19_15_31_08_user_JIM-279-r118902-1t_08_enriched-br_Auto_JIM-279-r118902-1t_08_enriched-br_4711.bam");
				exp.setRawDir("/results/PGM_test/cropped_CB1-42");
				exp.setResultsDirectory("/results/analysis/output/Home/test_1.4_007");
				// exp.setRawDir("s:\\data\\bb\\raw\\");
				// exp.setResultsDirectory("s:\\data\\bb\\res\\");
				exp.setCacheDir(exp.getResultsDirectory());
				check= true;
			///
			} else if (app.getServer().startsWith("0.0")) { // localhost
				exp.setChipType("314");
				exp.setSffFilename("");
				exp.setBamFilename("");
				exp.setRawDir("/results/");
				exp.setResultsDirectory("/results/analysis/output/Home");
				exp.setCacheDir("/tmp/");
			}

			// check for paramters
			String name = app.getParameters("run_name");
			if (name != null) {
				exp.setResultsName(name);
			}
			
			String raw = app.getParameters("raw_dir");
			if (raw != null) {
				exp.setRawDir(raw);
				check = true;
			}
			String db = app.getParameters("db");
			if (db != null) {
				exp.setServerUrl(db);
			}
			String sff = app.getParameters("sff");
			if (sff != null) {
				exp.setSffFilename(sff);
			}
			String bam = app.getParameters("bam");
			if (bam != null) {
				exp.setBamFilename(bam);
			}
			String res = app.getParameters("res_dir");
			if (res != null) {
				exp.setResultsDirectory(res);
				check = true;
			}
		}
		if (exp.isThumbnails()) {
			exp.setThumbnailsRaw();
			p("Got thumbnails, setting raw: " + exp.getRawDir());
		}
		// check for sff and bam files
		if (!exp.hasBam()) {
			String file = SequenceLoader.findFile(".bam", exp.getBamDir(), false, "tf.bam", false);
			p("Trying to find bam with sequence loader in bam folder:" + file);
			if (file != null) exp.setBamFilename(new File(file).getName());
		}
		if (!exp.hasBam() && exp.getResultsDirectory().equals(exp.getBamDir())) {
			exp.setBamDir(exp.getResultsDirectory());
			String file = SequenceLoader.findFile(".bam", exp.getResultsDirectory(), false, "tf.bam", false);
			p("Trying to find bam with sequence loader in results folder:" + file);
			if (file != null) exp.setBamFilename(new File(file).getName());
		}
		if (!exp.hasSff()) {
			String file = SequenceLoader.findFile(".sff", exp.getBasecallerDir(), false, "tf.sff", false);
			p("Trying to find sff with sequence loader:" + file);
			if (file != null) exp.setSffFilename(new File(file).getName());
		}
		if (!exp.hasSff()) {
			p("Got no sff, using rawlib");
			exp.setSffFilename("rawlib.sff");
		} else
			p("Seems like I found sff: " + exp.getSffFileName());
		if (!exp.hasBam()) {
			p("Got no bam, using rawlib");
			exp.setBamFilename("rawlib.bam");
		} else
			p("Seems like I found bam: " + exp.getBamFileName());
		
		if (!exp.hasBam()) {
			p("Got no bam, using rawtf");
			exp.setBamFilename("rawtf.bam");
		}
		return check;
	}

	public void checkFolders() {
		if (results.getValue() == null && raw.getValue() == null && ca.getValue()==null) return;
		
		File f1 = new File("" + results.getValue());
		File f2 = new File("" + raw.getValue());
		File f3 = new File("" + ca.getValue());
		if (!f1.exists()) {
			results.setComponentError(new UserError("Directory " + f1 + " not found"));
		} else {
			results.setComponentError(null);
		}
		if (!f2.exists()) {
			String msg = "Directory " + f2 + " not found<br>Maybe the data is (relatively :-) old and was moved or deleted?<br>You can still view ionograms and alignments etc";
			raw.setComponentError(new UserError(msg));
			app.showLongMessage("Raw Data?" , msg);
		} else {
			raw.setComponentError(null);
		}
		
		if (!f3.exists()) {
			ca.setComponentError(new UserError("Directory " + f3 + " not found (and could not create last part of path)"));
		} else {
			ca.setComponentError(null);
		}

		if (!new File("" + sff.getValue()).exists()) {
			sff.setComponentError(new UserError("File " + sff.getValue() + " not found (but you can continue)"));
		} else {
			sff.setComponentError(null);
		}
		if (!new File("" + bam.getValue()).exists()) {
			bam.setComponentError(new UserError("File " + bam.getValue() + " not found (but you can continue)"));
		} else {
			bam.setComponentError(null);
		}
	}
	public void handleOk() {
		File f1 = new File("" + results.getValue());
		File f2 = new File("" + raw.getValue());
		File f3 = new File("" + ca.getValue());
		boolean close = true;
		boolean error = false;
		if (!f1.exists()) {
			results.setComponentError(new UserError("Directory " + f1 + " not found"));
			error = true;
		} else {
			results.setComponentError(null);
		}
		if (!f2.exists()) {
			//error = true;
			close= false;
			String msg = "Directory " + f2 + " not found<br>Maybe the data is (relatively :-) old and was moved or deleted?";
			raw.setComponentError(new UserError(msg));
			app.showLongMessage("Raw Data?" , msg);
		} else {
			raw.setComponentError(null);
		}
		if (!f3.exists()) {
			File f = f3.getParentFile();
			if (f.exists()) {
				f3.mkdir();
			}
		}
		if (!f3.exists()) {
			error = true;
			ca.setComponentError(new UserError("Directory " + f3 + " not found (and could not create last part of path)"));
		} else {
			ca.setComponentError(null);
		}

		if (!new File("" + sff.getValue()).exists()) {
			sff.setComponentError(new UserError("File " + sff.getValue() + " not found (but you can continue)"));
		} else {
			sff.setComponentError(null);
		}
		if (!new File("" + bam.getValue()).exists()) {
			bam.setComponentError(new UserError("File " + bam.getValue() + " not found (but you can continue)"));
		} else {
			bam.setComponentError(null);
		}
		if (!error) {
			createExp(close);
		}
	}

	private void createExp(boolean close) {
		app.setExperimentContext(exp);

		app.showTopMessage("Experiment Opened", "Now pick a region on the chip");
		exp.setResultsName("" + rname.getValue());
		exp.setChipType("" + type.getValue());
		exp.setSffFilename(new File(sff.getValue().toString()).getName());
		exp.setBamFilename(new File(bam.getValue().toString()).getName());
		exp.setRawDir(raw.getValue().toString());
		exp.setResultsDirectory(results.getValue().toString());
		exp.setCacheDir(ca.getValue().toString());
		if (exp.doesExplogHaveBlocks()) {
			p("Composite: parsing blocks");
			CompositeExperiment comp = new CompositeExperiment(exp);
			comp.maybParseBlocks();
			p("Got blocks: " + comp.getBlocks());
			app.setCompositeExperiment(comp);
			// exp = comp.getThumbnailsContext();
		}

		app.showExperiment();
		if (close) close();
	}

	protected void openBrowser(String title, String ext, final TextField tf) {

		String desc = null;
		if (ext == null) ext = "dir";
		FileBrowserWindow browser = new FileBrowserWindow(title, desc, new FileBrowserWindow.Recipient() {
			@Override
			public void fileSelected(File file) {
				p("Got file:" + file);
				if (file != null) {
					tf.setValue(file.toString());
					p("tf value is now: " + tf.getValue());
				}

			}

			public boolean allowInList(File f, boolean toSave) {
				if (f == null) return false;
				if (!f.canRead()) return false;

				String dir = f.getAbsolutePath().toString();
				if (dir.indexOf("/etc/") > -1 || dir.indexOf("/init.d/") > -1) return false;
				if (dir.startsWith("etc/") || dir.startsWith("init.d/")) return false;

				return true;
			}

		}, FileBrowserWindow.OPEN, appwindow, new File("" + tf.getValue()), ext);
		browser.open();
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(ExperimentWindow.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		Logger.getLogger(ExperimentWindow.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(ExperimentWindow.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		//system.out.println("ExperimentWindow: " + msg);
		Logger.getLogger(ExperimentWindow.class.getName()).log(Level.INFO, msg);
	}

}
