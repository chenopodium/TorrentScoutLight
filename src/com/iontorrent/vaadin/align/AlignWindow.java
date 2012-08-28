/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.align;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.iontorrent.seq.Read;
import org.iontorrent.seq.alignment.Alignment;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.sequenceloading.SequenceLoader;
import com.iontorrent.threads.Task;
import com.iontorrent.threads.TaskListener;
import com.iontorrent.utils.ErrorHandler;
import com.iontorrent.utils.ProgressListener;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.iontorrent.wellmodel.WellContext;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.RichTextArea;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class AlignWindow extends WindowOpener implements ProgressListener, TaskListener {

	private TSVaadin app;
	ProgressIndicator indicator;
	HorizontalLayout h;
	SequenceLoader loader;
	ExperimentContext exp;
	WellContext context;
	WellCoordinate coord;
	RichTextArea area;
	WorkThread t ;
	public AlignWindow(TSVaadin app, Window main, String description, int x, int y) {
		super("Alignment", main, description, x, y, 1050, 700);
		this.app = app;

	}

	@Override
	public void openButtonClick(Button.ClickEvent event) {
		if (app.getExperimentContext() == null) {
			appwindow.showNotification("No Experiment Selected", "<br/>Please open an experiment first", Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		exp = app.getExperimentContext();
		if (!exp.hasBam()) {
			appwindow.showNotification("Bam File not found", "<br/>Could not find the file " + exp.getBamFilePath(), Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		if (!exp.hasSff()) {
			appwindow.showNotification("Sff File not found", "<br/>Could not find the file " + exp.getSffFilePath(), Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		app.openTable();
		super.openButtonClick(event);
	}

	@Override
	public void windowOpened(Window mywindow) {
		p("Creating AlignWindow ");

		exp = app.getExperimentContext();
		context = exp.getWellContext();
		coord = exp.getWellContext().getCoordinate();
		if (coord == null) {
			coord = new WellCoordinate(510, 510);
			app.getExperimentContext().getWellContext().setCoordinate(coord);
		}
		int x = coord.getX();
		int y = coord.getY();
		p("Getting alignment for " + x + "/" + y);
		h = new HorizontalLayout();
		mywindow.addComponent(h);
		area = new RichTextArea();
		area.addStyleName("no-toolbar");
		area.setWidth("1000px");
		area.setHeight("600px");

		mywindow.addComponent(area);
		loader = SequenceLoader.getSequenceLoader(exp, false, false);
		loader.setInteractive(false);

		if (exp.getFlowOrder() == null || exp.getFlowOrder().trim().length() < 2) {
			err("Got no flow order");
			exp.setFlowOrder(loader.getFlowOrder());
		}
		if (exp.getFlowOrder() == null || exp.getFlowOrder().trim().length() < 2) {
			app.showMessage("Got no flow order", "Unknown flow order - will search *all* flows");

		}
		if (!loader.hasSffIndex() || !loader.getSamUtils().hasWellToLocIndex()) {
			p("need to create index first");
			this.createIndex();
		} else
			updateAreaWithRead();
	}

	public String getHelpMessage() {
		String msg = "<ul>";
		msg += "<li>view the alignment and flowgram</li>";
		msg += "<li>check the q lengths</li>";
		msg += "<li>see to what flow an error maps to (for indels this could be a few flows before)</li>";
		msg += "</ul>";
		return msg;
	}

	private void updateAreaWithRead() {
		Read read = loader.getRead(coord.getCol(), coord.getRow(), null);
		String error = loader.getMsg();

		// area.setReadOnly(true);

		if (read == null && error != null) {
			area.setValue("Got no read: " + error);
			return;
		}

		String msg = "";
		String title = "<H2>Run " + exp.getResultsName() + "</H2>";
		title += "<b><font color='000099'>Alignment at " + context.getAbsoluteCoordinate() + "</font></b><br>";
		// expContext.
		Alignment al = null;
		if (read != null) {
			msg += "<b>Sff read " + read.getName().trim() + "</b>: ";
			msg += read.toSequenceString() + "<br>";
			al = read.getAlign();
		} else {
			msg = "I found no sff read at this location. ";
			if (error != null && error.length() > 0) {
				msg += " Reason:" + error;
				error = null;
			}
		}

		if (al != null) {
			msg += "<br><b>Flags:</b> " + read.getFlags() + " (reverse: " + read.isReverse() + ")";
			// msg += "<br><b>Analysis command line:</b><br> " +
			// read.getCommandLine();
			msg += "<br><b>Genome position:</b> " + read.getAlignmentStart() + "-" + read.getAlignmentEnd();

			if (read.isReverse()) {
				Alignment rev = al.getReverseAlignment();
				rev.setSeq2(read);
				rev.calculateStats();
				msg += "<br><br><b>Alignment in sequencing order:</b>";
				msg += rev.toHtml();
				msg += "<br><b>Alignment in reverse order (forward relative to reference):</b>";
			}
			msg += al.toHtml();
			msg += "<br><b>Reference:</b> " + read.getAlign().getRefSeq1().toSequenceString();
			msg += "<br><b>Cigar string:</b> " + read.getCigarString();
			msg += "<br><b>MD string:</b> " + read.getMd();

		} else {
			msg += "<br>I see no alignment.";
			if (error != null && error.length() > 0) {
				msg += " Reason:" + error;
				error = null;
			}
		}
		if (read != null && read.getFlowgram() != null) {
			msg += "<br><b>Library Key</b>:" + read.getKey() + "<br>";
			msg += "<br><b>Flow Order</b>:" + read.getFlowOrder() + "<br>";
			msg += "<br><b>Flowgram</b>:";
			msg += read.getHtmlFlowGramInfo();			
			msg += "<br><b>Flow positions</b>:";
            msg += read.getHtmlFlowPosInfo();
		//	msg += "<br><b>Flow Index</b>:<br>" + Arrays.toString(read.getAbsoluteFlowIndex()) + "<br>";
			//msg += "<br><b>Flowgram</b>:<br>" + Arrays.toString(read.getFlowgram()) + "<br>";
		}

		area.setValue(title + "<font face='Courier' size='2'>" + msg + "</font>");
	}

	private void createIndex() {
		if (indicator != null) h.removeComponent(indicator);
		indicator = new ProgressIndicator(new Float(0.0));
		indicator.setHeight("40px");
		// indicator.setCaption("Creating whole Proton image");
		indicator.setDescription("Createing index files...");
		indicator.setPollingInterval(5000);
		h.addComponent(indicator);
		p("Creating index files");
		// thread
		t = new WorkThread(this);
		t.execute();
		indicator.setDescription("I am creating index files so that I can find the read and alignment for a given well");
		area.setValue("I am creating index files so that I can find the read and alignment for a given well");
		app.showMessage(this, "I need to compute a few index files first...");

	}

	@Override
	public void setMessage(String arg0) {

	}

	@Override
	public void taskDone(Task task) {
		boolean has = task.isSuccess();
		if (!has) {

			// mask.readData(flag, file, true);
			app.showError(AlignWindow.this, "Something went wrong when creating the index files");
		}
		this.afterIndexCreate();
	}
	public void close() {
		super.close();
		if (t != null && !t.isCancelled()) {
			t.cancel(true);
			t = null;
		}		
		
	}

	@Override
	public void setProgressValue(int prog) {
		p("Got progress value: " + prog + ", calling updateMultiFlow");
		if (indicator != null) indicator.setValue(((double) prog / 100.0d));

	}

	// Another thread to do some work
	class WorkThread extends Task {
		boolean has;

		public WorkThread(TaskListener list) {
			super(list);
			
		}

		@Override
		public boolean isSuccess() {
			// TODO Auto-generated method stub
			return has;
		}

		@Override
		protected Void doInBackground() {
			try {
				p("Creating sffindex");
				loader.createSffIndex();
				p("Creating createWellToLocIndex");
				loader.getSamUtils().createWellToLocIndex();
				p("Creating createGenomeToReadIndex");
				loader.createGenomeToReadIndex();
				Read read = loader.getRead(coord.getCol(), coord.getRow(), null);

				has = loader.hasSffIndex();
				if (!has) {
					p("Still no sff index, msg from loader: "+loader.getMsg());
					p("sff file: "+loader.getSffFile());
					p("exists sff file: "+loader.getSffFile().exists());
					
					
				}
				indicator.setValue(new Float(1.0));
			} catch (Exception e) {
				err("Got an error when computing the heat map: " + ErrorHandler.getString(e));
			}
			return null;

		}

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	private void afterIndexCreate() {
		updateAreaWithRead();
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(AlignWindow.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		System.out.println("AlignWindow: " + msg);
		Logger.getLogger(AlignWindow.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(AlignWindow.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		System.out.println("AlignWindow: " + msg);
		Logger.getLogger(AlignWindow.class.getName()).log(Level.INFO, msg);
	}
}
