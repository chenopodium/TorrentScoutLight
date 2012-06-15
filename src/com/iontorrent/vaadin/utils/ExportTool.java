package com.iontorrent.vaadin.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

import org.iontorrent.seq.Coord;
import org.iontorrent.seq.Read;
import org.iontorrent.seq.alignment.Alignment;
import org.iontorrent.seq.sam.SamUtils;
import org.iontorrent.seq.sam.WellToSamIndex;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.rawdataaccess.pgmacquisition.DataAccessManager;
import com.iontorrent.rawdataaccess.wells.BfMaskDataPoint;
import com.iontorrent.rawdataaccess.wells.BitMask;
import com.iontorrent.rawdataaccess.wells.WellData;
import com.iontorrent.sequenceloading.SequenceLoader;
import com.iontorrent.sff.SffRead;
import com.iontorrent.threads.Task;
import com.iontorrent.threads.TaskListener;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.torrentscout.explorer.ExportUtil;
import com.iontorrent.utils.ErrorHandler;
import com.iontorrent.utils.ProgressListener;
import com.iontorrent.utils.StringTools;
import com.iontorrent.utils.io.FileTools;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.gene.GeneWindow;

import com.iontorrent.wellalgorithms.NearestNeighbor;
import com.iontorrent.wellmodel.RasterData;
import com.iontorrent.wellmodel.WellCoordinate;
import com.iontorrent.wellmodel.WellSelection;
import com.vaadin.terminal.FileResource;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class ExportTool implements TaskListener, ProgressListener {
	TSVaadin app;
	Window win;
	ExperimentContext exp;
	ExplorerContext maincont;
	BitMask mask;
	ProgressIndicator indicator;
	SequenceLoader loader;
	int limit = 100;
	int total = 0;
	AbstractLayout lay;
	String totalmsg;

	WellCoordinate corner;
	WellCoordinate center;
	ExportUtil util;
	boolean html;
	boolean align;
	String type;
	String ext;

	public ExportTool(TSVaadin app, Window win, BitMask mask, AbstractLayout lay) {
		this.app = app;
		this.win = win;
		this.lay = lay;
		this.exp = app.getExperimentContext();
		this.maincont = app.getExplorerContext();
		this.mask = mask;
		exp.setServerUrl(app.getServerName());

		total = maincont.getRasterSize() * maincont.getRasterSize();
		if (mask != null) {
			total = mask.getTotalSub(maincont.getRasterSize());
			totalmsg = "There are " + total + " wells in mask "
					+ mask.getName() + " in area (center) "
					+ (maincont.getAbsCenterAreaCoord());
		} else {
			totalmsg = "There are " + total + " wells in area (center) "
					+ (maincont.getAbsCenterAreaCoord());
		}

		center = maincont.getRelativeCenterAreaCoord();

		corner = maincont.getRelativeCorner();
	}

	public void doExportAction() {
		// if the mask is null or too large, narrow it down!
		askWhatToExport();
	}

	public ProgressIndicator getIndicator() {
		return indicator;
	}

	private void askWhatToExport() {
		OptionsDialog opt = new OptionsDialog(win,
				"What would you like to export?", "Export...",
				"... alignments", "... ionogram data", "... raw data",
				new OptionsDialog.Recipient() {

					@Override
					public void gotInput(final int selection) {
						if (selection < 0)
							return;

						// ask for limit
						final IntInputDialog input = new IntInputDialog(
								app.getMainWindow(),
								"Max nr of wells to export around "
										+ (maincont.getAbsCenterAreaCoord()),
								totalmsg + ", current limit:" + limit,
								new IntInputDialog.Recipient() {
									public void gotInput(int val) {
										limit = val;
										// / do the search
										if (selection == 0 || selection == 1) {
											exportAlignOrIono(selection);
										} else if (selection == 2) {
											app.logModule("export", "raw");
											exportRawData();
										}
									}
								}, "" + limit, 400);

					}

				});
	}

	private void exportAlignOrIono(final int selection) {
		align = selection == 0;
		if (align)
			type = "alignments";
		else
			type = "ionograms";

		if (align) {
			OptionsDialog opt = new OptionsDialog(win,
					"What format do you prefer?", "", "HTML format",
					"Excel (.csv) format", new OptionsDialog.Recipient() {

						@Override
						public void gotInput(final int sel) {
							if (sel < 0) {
								return;
							}
							html = sel == 0;
							if (html)
								ext = ".html";
							else
								ext = ".csv";
							exportAlignOrIono();
						}
					});
		} else {
			html = false;
			ext = ".csv";
			exportAlignOrIono();
		}

	}

	private void exportAlignOrIono() {
		if (exp.getServerUrl() == null)
			exp.setServerUrl("");
		util = new ExportUtil(mask, maincont, align);

		String header = util.getHeader(html, limit, align).toString();
		File f = null;
		try {
			f = File.createTempFile("export_" + type + "_" + exp.getId() + "_"
					+ maincont.getAbsoluteCorner().getCol() + "_"
					+ maincont.getAbsoluteCorner().getRow(), ext);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (f != null) {
			p("Writing data to temp file :" + f.getAbsolutePath());
			f.deleteOnExit();

			loader = SequenceLoader.getSequenceLoader(this.exp, false, false);
			loader.setInteractive(false);
			FileTools.writeStringToFile(f, header, false);
			if (!loader.hasSffIndex()
					|| !loader.getSamUtils().hasWellToLocIndex()) {
				IndexThread index = new IndexThread(this, align, f);
				indicator = new ProgressIndicator(new Float(0.0));
				indicator.setHeight("40px");
				app.showLongMessage("Indexing... ",
						"I have to create a few index files first... ");
				indicator.setDescription("Indexing SFF file...");
				indicator.setPollingInterval(5000);
				if (lay != null)
					lay.addComponent(indicator);
				index.execute();
			} else
				startExportThread(f);
		}
	}

	private void startExportThread(File f) {
		indicator = new ProgressIndicator(new Float(0.0));
		indicator.setHeight("40px");

		indicator.setDescription("Exporting data...");
		indicator.setPollingInterval(5000);
		if (lay != null)
			lay.addComponent(indicator);
		WorkThread t = new WorkThread(this, align, f);
		t.execute();

		app.showMessage("Export", "Got all index files, starting export of "
				+ Math.min(limit, total) + " wells...");
	}

	public void setProgressValue(int p) {
		if (indicator != null)
			indicator.setValue(((double) p / 100.0d));
		// progress.setValue("Creating composite image: " + p + "%");
	}

	@Override
	public void setMessage(String msg) {
		indicator.setDescription(msg);
	}

	@Override
	public void stop() {

	}

	public void iterateToExport(boolean align, File f) {
		// win.showNotification("Exporting", "Starting export",
		// Window.Notification.TYPE_HUMANIZED_MESSAGE);

		int relcornerx = maincont.getRelativeCorner().getCol();
		int relcornery = maincont.getRelativeCorner().getRow();

		StringBuilder out = new StringBuilder();
		int count = 0;
		int every = 100;
		if (align)
			every = 5;
		// hor.addComponent(indicator);
		if (limit > 0)
			total = Math.min(limit, total);
		for (int x = 0; x < maincont.getRasterSize(); x++) {
			for (int y = 0; y < maincont.getRasterSize(); y++) {
				// append flow, x, y
				if (mask == null || mask.getSub(x, y)) {
					count++;
					if (count % every == 0) {
						indicator.setValue(new Float((float) count
								/ (float) total));
					}
					if (align)
						try {
							out = out
									.append(util.exportAlignments(relcornerx
											+ x, relcornery + y,
											count % 50 == 0, html));
						} catch (Exception e) {
							err("Got an error when exporting alignment: " + e);
						}
					else
						out = out.append(util.exportIonograms(relcornerx + x,
								relcornery + y));

					if (count % 10 == 0) {
						FileTools.writeStringToFile(f, out.toString(), true);
						out = new StringBuilder();
					}
					if (limit > 0 && count >= limit) {
						FileTools.writeStringToFile(f, out.toString(), true);
						return;
					}
				}

			}

		}
	}

	// Another thread to do some work
	class WorkThread extends Task {
		public File f;
		public boolean align;

		public WorkThread(TaskListener list, boolean align, File f) {
			super(list);
			this.f = f;
			this.align = align;
		}

		@Override
		protected Void doInBackground() {
			p("========== STARTING EXPORT THREAD ======");
			try {
				if (align && limit > 500 && (mask == null || mask.getTotalSub(maincont.getRasterSize()) > 500)) {
                    iterateToExportManyAlignments(f);
                } else {
                    iterateToExport(align, f);
                }

			} catch (Exception e) {
				err("Got an error exporting data: " + ErrorHandler.getString(e));
			}
			return null;

		}

		@Override
		public boolean isSuccess() {
			return true;
		}

	}
	 public void iterateToExportManyAlignments(File f) {
         StringBuffer out = new StringBuffer();
         int count = 0;
     
         int every = 20;
         SamUtils sam = loader.getSamUtils();
         SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.SILENT);
         final SAMFileReader inputSam = new SAMFileReader(loader.getBamFile());

         inputSam.setValidationStringency(SAMFileReader.ValidationStringency.SILENT);
         //p("Stringency "+inputSam.);
         SAMRecordIterator it = inputSam.iterator();
         SAMRecord old = null;

          int relcornerx = maincont.getRelativeCorner().getCol();
         int relcornery = maincont.getRelativeCorner().getRow();

         TreeSet<String> set = new TreeSet<String>();
         for (int x = 0; x < maincont.getRasterSize(); x++) {
             for (int y = 0; y < maincont.getRasterSize(); y++) {
                 // append flow, x, y
                 if (mask == null || mask.getSub(x, y)) {
                     String key = (x +relcornerx)+ ":" + (y+relcornery);
                     set.add(key);
                 }
             }
         }
         String nl = ", ";
         if (html) {
             nl = "<br>";
         }
         boolean done = false;
         for (; it.hasNext() && !done;) {
             SAMRecord rec = null;
             int tries = 0;
             while (rec == null && it.hasNext() && tries < 5 && !done) {
                 tries++;
                 try {
                     rec = it.next();
                 } catch (Exception e) {
                     err("Tries: " + tries + ":" + e.getMessage());
                     e.printStackTrace();
                 }
             }
             if (tries >= 3) {
                 break;
             }
             if (rec != null && old != null && rec.getReadName().equalsIgnoreCase(old.getReadName())) {
                 break;
             }

             if (rec != null) {
                 Alignment al = sam.extractAlignment(rec);

                 if (al != null) {
                     try {
                         String name = rec.getReadName();
                         Coord coord = WellToSamIndex.extractWellCoord(name);
                         int x = coord.x;
                         int y = coord.y;
                         String key = x + ":" + y;
                         if (set.contains(key)) {
                             // check if in coord list!

                             if (rec.getFlags() == 16) {
                                 Alignment rev = al.getReverseAlignment();
                                 rev.calculateStats();
                             }
                             try {
                                 count++;
                                 if (count % every == 0) {
                                     this.setProgressValue(count * 100 / total);
                                 }

                                 String msg = "";
                                 if (!html) {
                                     msg = "\n" + (x + exp.getColOffset()) + ", " + (y
                                             + exp.getRowOffset()) + ", ";
                                 } else {
                                     msg = "<br><H3>Alignment at " + (x + exp.getColOffset()) + ", " + (y
                                             + exp.getRowOffset()) + "\n</H3>";
                                 }

                                 BfMaskDataPoint bf = exp.getWellContext().getMask().getDataPointAt(x, y);
                                 Read read = loader.getRead(x, y, null);
                                 if (html) {
                                     msg += "BF flags: " + bf.toString() + "<br>";
                                 }
                                 msg = util.getStringFromAlignment(al, html, msg, read, nl);
                                 if (!html) {
                                	 msg = msg + ", " + bf.toString();
                                 }
                                 //out = out.append(exportAlignments(relcornerx + x, relcornery + y, count < 10, html));
                                 out = out.append(msg);
                                 if (count % 100 == 0) {
                                     FileTools.writeStringToFile(f, out.toString(), true);
                                     out = new StringBuffer();
                                 }
                                 if (limit > 0 && count >= limit) {
                                     FileTools.writeStringToFile(f, out.toString(), true);
                                     done = true;
                                 }
                             } catch (Exception e) {
                                 err("Got an error when exporting alignment: " + e);
                             }
                         }
                     } catch (Exception e) {
                         err(e.getMessage());

                     }
                 }
             }
         }
         inputSam.close();
     }
	

	@Override
	public void taskDone(Task task) {
		if (indicator != null) {
			if (lay != null)
				lay.removeComponent(indicator);
		}
		if (task instanceof IndexThread) {
			IndexThread ind = (IndexThread) task;
			startExportThread(ind.f);
		} else {
			WorkThread t = (WorkThread) task;
			if (app != null) {
				app.showMessage("Export done", "Starting file download of "
						+ t.f);
				if (t.f.getName().endsWith(".html")) {
					FileResource down = new FileResource(t.f, app);
					app.getMainWindow().open(down, "_blank", html ? 1000 : 800,
							600, 1);

				} else {
					app.showMessage("Export done", "About to download result "
							+ t.f + " (" + t.f.length() / 1000000 + " MB)...");
					FileDownloadResource down = new FileDownloadResource(t.f,
							this.app);
										
					app.getMainWindow().open(down, "_blank", html ? 1000 : 800,
							600, 1);
				}
			}
		}
	}

	// Another thread to do some work
	class IndexThread extends Task {
		boolean has;
		boolean align;
		File f;

		public IndexThread(TaskListener list, boolean align, File f) {
			super(list);
			this.f = f;
			this.align = align;
		}

		@Override
		public boolean isSuccess() {
			// TODO Auto-generated method stub
			return has;
		}

		@Override
		protected Void doInBackground() {
			try {
				loader.createSffIndex();
				loader.getSamUtils().createWellToLocIndex();
				loader.createGenomeToReadIndex();
				has = loader.hasSffIndex();
				indicator.setValue(new Float(1.0));
			} catch (Exception e) {
				err("Got an error when computing index file: "
						+ ErrorHandler.getString(e));
			}
			return null;

		}

	}

	public void exportRawData() {
		OptionsDialog input = new OptionsDialog(
				win,
				"What would you like to export?",
				"Export...",
				"... all NN subtracted data for selected mask for multiple flows",
				"... all RAW data for selected mask for multiple flows (subtract 1st frame to make it 0 based)",
				"... all RAW data for selected mask for multiple flows (do NOT subtract 1st frame)",
				new OptionsDialog.Recipient() {

					@Override
					public void gotInput(final int selection) {
						if (selection < 0)
							return;
						// / do the search

						// ask for flows
						InputDialog input = new InputDialog(win,
								"Export flows (eg 0-4):",
								new InputDialog.Recipient() {
									public void gotInput(String sflows) {
										if (sflows == null
												|| sflows.trim().length() < 1)
											return;
										ArrayList<Integer> flows = parseFlows(sflows);
										exportAllRawData(flows, selection == 0,
												selection == 1);
									}

								}, "0-4");

					}

				});
	}

	public void exportAllRawData(ArrayList<Integer> flows, boolean nn,
			boolean subtractFirstFrame) {
		if (flows == null || flows.size() < 1)
			return;
		DataAccessManager manager = DataAccessManager.getManager(app
				.getExperimentContext().getWellContext());
		int r = maincont.getRasterSize();
		WellCoordinate rel = maincont.getRelativeCenterAreaCoord();

		boolean head = true;
		File f = null;
		try {
			f = File.createTempFile("export_raw" + "_" + exp.getId() + "_"
					+ maincont.getAbsoluteCorner().getCol() + "_"
					+ maincont.getAbsoluteCorner().getRow(), ".csv");
			f.deleteOnExit();
			app.showMessage("Export", "Exporting flows " + flows);
			for (int flow : flows) {
				try {
					RasterData data = manager.getRasterDataForArea(null,
							maincont.getRasterSize(), rel, flow,
							maincont.getFiletype(), null, 0, -1,
							subtractFirstFrame);
					// maybe no nn?
					if (nn)
						data = computeNN(flow);

					String csv = data.toCsv(head, flow, mask, limit);
					// append is set to false for first flow
					if (head) {
						// add some info
						WellCoordinate c = maincont.getAbsoluteCorner();

						String title = "Data from " + exp.getRawDir() + " \n"
								+ "\"Corner coordinates: " + c.getCol() + "/"
								+ c.getRow() + " - " + (c.getCol() + r) + "/"
								+ (c.getRow() + r) + "\"\n" + "\"filetype: "
								+ maincont.getFiletype() + "\"\n"
								+ "NN subtracted: " + nn + "\n";
						csv = title + csv;
					}

					FileTools.writeStringToFile(f, csv, !head);
					head = false;

				} catch (Exception e) {
					err(ErrorHandler.getString(e));
				}

			}
		} catch (Exception e) {
			err(ErrorHandler.getString(e));
			app.showError("Export", "Was unable to save the data to file");
			return;
		}
		app.showMessage("Exporting Raw Data", "Exporting flows done");
		FileDownloadResource down = new FileDownloadResource(f, this.app);

		app.getMainWindow().open(
				down,
				"Export of flows " + flows + " starting at "
						+ maincont.getAbsoluteCorner());
	}

	public RasterData computeNN(int flow) {
		p("computeNN. Maincont is: " + maincont);
		if (maincont.getData() == null) {
			app.showLongMessage(
					"NN Calculation",
					"I see no data yet - did you already pick a region?<br>(Even if you see something somewhere, if you didn't actually select a region, it might just show some sample data)");
			return null;
		}
		RasterData nndata = null;
		try {

			int span = Math.max(8, maincont.getSpan());
			p("got span: " + span);
			exp.setFlow(flow);
			NearestNeighbor nn = new NearestNeighbor(this.exp, span,
					maincont.getMedianFunction());
			BitMask ignore = maincont.getIgnoreMask();
			BitMask take = maincont.getBgMask();

			if (take != null && take == ignore) {
				app.showLongMessage(
						"NN Calculation",
						"You selected the same mask for ignore and bg :-).<br>You should select another mask for the bg (or you get a null result. I will just return the old data.");
				return maincont.getData();
			}

			// p("Masked neighbor subtraction: ignore mask " + ignore +
			// " and empty mask " + take);
			nndata = nn.computeBetter(maincont.getData(), ignore, take, null,
					span);

		} catch (Exception e) {
			app.showError("NN", "Error with nn: " + ErrorHandler.getString(e));
			return null;
		}
		if (nndata == null) {
			app.showLongMessage(
					"NN Calculation",
					"I was not able to do the masked neighbor subtraction - I got no error but also no result :-) ");
		}
		return nndata;
	}

	public ArrayList<Integer> parseFlows(String sflows) {
		ArrayList<Integer> flows = new ArrayList<Integer>();
		if (sflows == null) {
			return flows;
		}
		flows = StringTools.parseInts(sflows);
		p("parsed flows: " + flows);
		return flows;
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(ExportTool.class.getName()).log(Level.SEVERE, msg, ex);
	}

	static void err(String msg) {
		Logger.getLogger(ExportTool.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		Logger.getLogger(ExportTool.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		// system.out.println("ExportTool: " + msg);
		Logger.getLogger(ExportTool.class.getName()).log(Level.INFO, msg);
	}

}
