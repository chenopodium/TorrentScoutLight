/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.db;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import com.iontorrent.dbaccess.RundbExperiment;
import com.iontorrent.dbaccess.RundbQualitymetrics;
import com.iontorrent.dbaccess.RundbResults;
import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.scout.experimentviewer.exptree.ExpDateNodeFilter;
import com.iontorrent.scout.experimentviewer.exptree.ExpNamesNodeFilter;
import com.iontorrent.scout.experimentviewer.exptree.MyResult;
import com.iontorrent.scout.experimentviewer.exptree.MyRig;
import com.iontorrent.scout.experimentviewer.exptree.NodeFilter;
import com.iontorrent.scout.experimentviewer.exptree.Q17BasesNodeFilter;
import com.iontorrent.scout.experimentviewer.exptree.Q17MaxLenNodeFilter;
import com.iontorrent.scout.experimentviewer.options.PersistenceHelper;
import com.iontorrent.utils.io.FileTools;
import com.iontorrent.vaadin.ExperimentWindow;
import com.iontorrent.vaadin.TSVaadin;
import com.iontorrent.vaadin.utils.InputDialog;
import com.iontorrent.vaadin.utils.IntInputDialog;
import com.iontorrent.vaadin.utils.OptionsDialog;
import com.iontorrent.vaadin.utils.WindowOpener;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ConversionException;
import com.vaadin.data.Property.ReadOnlyException;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.Resource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.terminal.UserError;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class DbWindow extends WindowOpener {

	ExperimentContext context;
	// static NodeFilter[] FILTERS = {
	// new ExpDateNodeFilter(3),
	// new ExpDateNodeFilter(7),
	// new ExpDateNodeFilter(14),
	// new ExpDateNodeFilter(30),
	// new ExpDateNodeFilter(60),
	// new ExpDateNodeFilter(90),
	// new ExpChiptypeNodeFilter("314"),
	// new ExpChiptypeNodeFilter("314", true),
	// new ExpChiptypeNodeFilter("316"), new ExpChiptypeNodeFilter("318"),
	// new RunStatusNodeFilter("complete"), new ExpStatusNodeFilter("complete"),
	// new RunStatusNodeFilter("complete", true), new
	// ExpStatusNodeFilter("complete", true)
	// };
	
	GridLayout grid;
	Link link;
	NodeFilter expfilter;
	int default_days = 14;
	private TSVaadin app;
	private String URL;
	List<RundbExperiment> allexperiments;
	List<RundbExperiment> lastexperiments;
	//List<RundbExperiment> topexperiments;
	ArrayList<MyRig> lastrigs;
	//ArrayList<MyRig> toprigs;
	ArrayList<MyRig> allrigs;
	String filtertitle;
	Tab maintab;
	ArrayList<MyResult> lastresults;
	
	EntityManager entityManager;
	Tree lasttree;
	Table basestree;
	Table maxtree;
	// Tree tree;
	TabSheet tabsheet;
	TextField selected;
	TextField db;
	DecimalFormat dec = new DecimalFormat("#.##");
	
	public DbWindow(TSVaadin app, Window main, String description, int x, int y) {
		super("Browse DB", main, description, x, y, 710, 810);
		this.app = app;

	}

	@Override
	public void windowOpened(Window win) {
		// Component with an icon from a custom theme
		this.setURL(app.getDbURL());

		addTopComponents(win);

		tabsheet = new TabSheet();
		tabsheet.setWidth("650px");
		tabsheet.setHeight("670px");
		VerticalLayout v = new VerticalLayout();
		tabsheet.addTab(v);
		
		maintab = tabsheet.getTab(v);
		maintab.setCaption("Past few weeks");
		lasttree = createTab(v);

		v = new VerticalLayout();
		// chartTab.addComponent(new Label("Chart"));
		tabsheet.addTab(v);
		tabsheet.getTab(v).setCaption("Last top Q17 bases");
		basestree = createTableTab(v);
		
		v = new VerticalLayout();
		// chartTab.addComponent(new Label("Chart"));
		tabsheet.addTab(v);
		tabsheet.getTab(v).setCaption("Last max Q17 len");
		maxtree = createTableTab(v);

		win.addComponent(tabsheet);

		createDbContext();

		if (entityManager == null) {
			appwindow.showNotification("No db found", "<br/>Could not connect to db " + getURL() + "<br>Specify it in the URL with ?url=...", Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		loadDataAfterGotContext();

	}

	private void addTopComponents(Window win) {
		 grid = new GridLayout(6, 3);
		db = new TextField();
		db.setValue(URL);
		db.setImmediate(true);
		db.setWidth("350px");
		grid.addComponent(new Label("DB url:"), 0, 0);
		grid.addComponent(db, 1, 0);
		db.addListener(new Property.ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				app.setDbURL("" + db.getValue());
				reopen();
			}

		});

		Button dbok = new Button("Connect");
		dbok.addListener(new Button.ClickListener() {

			public void buttonClick(Button.ClickEvent event) {
				app.setDbURL("" + db.getValue());
				reopen();
			}
		});
		grid.addComponent(dbok, 3, 0);

		selected = new TextField();
		selected.setWidth("350px");
		selected.setValue("");
		// selected.setEnabled(false);

		Button ok = new Button("Load");
		// ok.setWidth("50px");
		ok.setIcon(new ThemeResource("img/ok.png"));

		// Handle button clicks
		ok.addListener(new Button.ClickListener() {

			public void buttonClick(Button.ClickEvent event) {
				// app.setExperimentContext(context);
				if (context == null) {
					appwindow.showNotification("No analysis selected", "<br/>Select a run under the PGM/Experiment nodes", Window.Notification.TYPE_HUMANIZED_MESSAGE);
				} else {
					app.showExpContext(context);
				}
			}
		});

		grid.addComponent(ok, 3, 1);

		grid.addComponent(new Label("Selected:"), 0, 1);
		grid.addComponent(selected, 1, 1);

		 Button help = new Button();
		 help.setDescription("Click me to get information on this window");
		 help.setIcon(new ThemeResource("img/help-hint.png"));
	        grid.addComponent(help, 4,1);
	        help.addListener(new Button.ClickListener() {
	            public void buttonClick(Button.ClickEvent event) {
	            	 app.showHelpMessage("Help", getHelpMessage());
	            }
	        });
	        
	        
	        Button filter = new Button();
	        filter.setDescription("Filter/search database");
	        filter.setIcon(new ThemeResource("img/filter.png"));
		        grid.addComponent(filter, 5,1);
		        filter.addListener(new Button.ClickListener() {
		            public void buttonClick(Button.ClickEvent event) {
		                doFilterAction();
		            }
		        });    
		win.addComponent(grid);
	}
	public void doFilterAction() {
		// ask for past days
		OptionsDialog input = new OptionsDialog(mywindow, 
				"How would you like to filter the db runs?", "Filter by...", "... runs of last X days", 
				"... experiments containing string X", 
				new OptionsDialog.Recipient() {

			@Override
			public void gotInput(int selection) {
				if (selection < 0) return;
				// / do the search
				if (selection == 0) {
					// ask for nr of days
					IntInputDialog input = new IntInputDialog(mywindow, "Find runs", "Find runs of last X days:", new IntInputDialog.Recipient() {
						@Override
						public void gotInput(int days) {							
							if (days >0 ) {								
								 expfilter = new ExpDateNodeFilter(days);
								 filtertitle = "Past "+days+" days";
								 app.logModule(getName(), "filter "+days+" days");
								 app.showMessage("Filtering","Experiments done in the past "+days+" days");
								 afterGotFilter();
							}							
						}

					}, "14");
				}
				else if (selection == 1) {
					// ask for flows
					// ExpNamesNodeFilter
					InputDialog input = new InputDialog(mywindow, "Experiment name contains string:", new InputDialog.Recipient() {
						public void gotInput(String name) {							
							if (name == null || name.trim().length()<1) return;						
							 expfilter = new ExpNamesNodeFilter(name);
							 filtertitle = "Exp with "+name;
							 app.logModule(getName(), "filter "+name);
							 app.showMessage("Filtering","Experiments containing "+name);
							 afterGotFilter();												
						}

					}, "267");
				}
				else {
					err("Strange selection");
				}
			}

		});
	}
	
	  public String getHelpMessage() {
	        String msg = "<ul>";
	        msg += "<li>Open the PGM node to view the experiments, and open the expeirment nodes to view the runs</li>";
	        msg += "<li>The filter button lets you view older experiments, or experiments containing a certain string</li>";
	        msg += "<li>Click on an experiment to view info about it</li>";
	        msg += "<li>Click on a run to select it and to view info about it</li>";
	        msg += "<li>Click load to open the currently selected <b>run</b> and view data in other components</li>";
	        msg += "</ul>";
	        return msg;
	 }

	  private Table createTableTab(AbstractLayout lay) {
		  
		  final Table  table = new Table();	        
		   
	        //table.setWidth("650px");
	        
	        
	        table.setSizeFull();
	        table.setHeight("636px");
	        table.addStyleName("welltable");
	        table.addContainerProperty("PGM", String.class, null);
	        table.addContainerProperty("Run", String.class, null);
	        table.addContainerProperty("Date", Date.class, null);
	        table.addContainerProperty("Q17 bases", Long.class, null);
	        table.addContainerProperty("Q17 max len", Integer.class, null);
	        table.addContainerProperty("Q17 mean len", Float.class, null);
	     //   table.addGeneratedColumn(table, null)
	        
	        table.setSelectable(true);
	        table.setImmediate(true);

            table.addListener(new Property.ValueChangeListener() {

                public void valueChange(ValueChangeEvent event) {
                    Object id = table.getValue();
                    if (id == null) {
                        return;
                    }
                    int row = (Integer)id;	                     
                    MyResult r = (MyResult)lastresults.get(row);
                    selectRun(r);
                }
            });
            lay.addComponent(table);
            return table;
	  }
	  
	  private void addToTable(Table table, final ArrayList<MyResult> res,  NodeFilter resultfilter) {
			table.removeAllItems();
	        for (int row = 0; row < res.size(); row++) {
		        MyResult r = res.get(row);
		        if (resultfilter.passes(r)) {
			        Object[] rowdata = new Object[6];
					rowdata[0] = r.getPgmname();
					String name = r.getResultsName();
					int len = name.length();
					 if (name.endsWith(".html") || name.endsWith(".jsp") || name.lastIndexOf(".") > (len-5)) {
				           File f = new File(r.getReportLink()) ;
				           f = f.getParentFile();
				           name = f.getName();
				           r.setResultsName(name);
				        }
					rowdata[1] = r.getResultsName();
					rowdata[2] = r.getDate();
					RundbQualitymetrics q = r.getQaulityMetrics();
					if (q!= null) {
						rowdata[3] = q.getQ17Bases();
						rowdata[4] = q.getQ17MaxReadLength();
						rowdata[5] = q.getQ17MeanReadLength();
					}
					
				    table.addItem(rowdata, new Integer(row));
		        }
	        }
	            
	     
	  }
	private Tree createTab(AbstractLayout lay) {
		Tree tree = new Tree();
		tree.addContainerProperty("icon", Resource.class, null);
		tree.addContainerProperty("caption", String.class, null);
		tree.addContainerProperty("run", MyResult.class, null);
		tree.addContainerProperty("experiment", RundbExperiment.class, null);
		tree.setItemIconPropertyId("icon");
		// tree.seti
		tree.setItemCaptionPropertyId("caption");
		
		// tree.setItemCaptionMode(AbstractSelect.ITEM_CAPTION_MODE_PROPERTY);
		tree.setSelectable(true);
		lay.addComponent(tree);
		return tree;
	}

	class RigSorter implements Comparator {

		@Override
		public int compare(Object o1, Object o2) {
			MyRig a = (MyRig)o1;
			MyRig b= (MyRig)o2;
			return (a.getName()).compareTo(b.getName());			
		}
		
	}
	class ExperimentSorter implements Comparator {

		@Override
		public int compare(Object o1, Object o2) {
			RundbExperiment a = (RundbExperiment)o1;
			RundbExperiment b= (RundbExperiment)o2;
			// or sort by date?
			return (b.getDate()).compareTo(a.getDate());			
		}
		
	}
	public ArrayList<MyRig> buildTreeFromRigsAndExperiments(List<RundbExperiment> experiments) {
		// p("Got " + experiments.size() + " experiments");
		ArrayList<MyRig> tmprigs = new ArrayList<MyRig>();
		
		// create map
		// p("Creating rig map");
		HashMap<String, MyRig> map = new HashMap<String, MyRig>();
		// sort by PGM!
		
		for (RundbExperiment ex : experiments) {

			String rname = ex.getPgmName();
			MyRig rig = map.get(rname.toLowerCase());
			if (rig == null) {
				MyRig myrig = new MyRig(rname);
				tmprigs.add(myrig);
				// p("Got rig: "+myrig.getName());
				myrig.setExperiments(new ArrayList<RundbExperiment>());
				map.put(rname.toLowerCase(), myrig);
			}
		}

		// List<RundbExperiment> exp = new ArrayList();

		for (RundbExperiment ex : experiments) {
			MyRig rig = map.get(ex.getPgmName().toLowerCase());
			if (rig != null) {
				rig.getExperiments().add(ex);
				// p("Adding exp "+ex.getExpName()+" to rig "+rig.getName());
			} else {
				p("Found no rig for exp: " + ex.getExpName() + ":" + ex.getPgmName());
			}
		}

		ArrayList<MyRig> myrigs = new ArrayList<MyRig>();
		for (MyRig rig : tmprigs) {
			if (rig.getExperiments() != null && rig.getExperiments().size() > 0) {
				myrigs.add(rig);
			} else {
				map.remove(rig.getName().toLowerCase());
			}
		}
		return myrigs;

	}

	private void createDbContext() {
		if (getURL() == null) {
			return;
		}
		app.showMessage("Trying to access db " + getURL() + "....", "Browse DB");
		PersistenceHelper persist = new PersistenceHelper();
		entityManager = null;
		boolean ok = persist.setURL(getURL());
		if (ok) {
			// ok = persist.testURL();
			// txtUrl.setText(persist.getURL());
			entityManager = persist.getEntityManager();
		}
		if (!ok) {
			db.setComponentError(new UserError("Could not connect to " + getURL()));
			appwindow.showNotification("Db problem", "<br/>Could not connect to " + getURL(), Window.Notification.TYPE_WARNING_MESSAGE);
		}
	}

	private void loadDataAfterGotContext() {
		
		 String sql = "SELECT c FROM RundbExperiment c where c.date > :arg1 ";
	        Query query = entityManager.createQuery(sql);
	        long cur = System.currentTimeMillis();
	        long hour = 1000*3600;
	        long week = hour*24*7;
	        long halfyear = week*4*6;
	     
	        Date mydate = new Date(cur-halfyear);
	        query.setParameter("arg1", mydate, TemporalType.TIMESTAMP);
	        p("Got query: "+query.toString()+", sql="+sql+", with arg1="+ mydate);
	        
		allexperiments = query.getResultList();

		// query =
		// entityManager.createQuery("SELECT c FROM RundbReportstorage c");
		// storages = query.getResultList();
		if (allexperiments == null || allexperiments.size() < 1) {
			appwindow.showNotification("No experiments found", "<br/>I found no data in the database", Window.Notification.TYPE_WARNING_MESSAGE);
			return;
		}
		p("Nr of all (past 6 months) experiments: " + allexperiments.size());

		if (expfilter == null) {
			expfilter = new ExpDateNodeFilter(default_days);
			filtertitle = "Past "+default_days+" days";
		}

		afterGotFilter();
		

	}

	private void afterGotFilter() {
		lastexperiments = filterExperiments(allexperiments, expfilter);
		if (lastexperiments.size() < 1) lastexperiments = allexperiments;
		p("Nr of LAST experiments: " + lastexperiments.size());
		lastrigs = buildTreeFromRigsAndExperiments(lastexperiments);

		lastresults = MyRig.getListOfResults(lastrigs);
		long topq17bases = MyResult.getTopQ17Bases(lastresults, 20);
		long topq17max = MyResult.getTopQ17MaxReadLen(lastresults, 20);
		// double topq17mean = MyResult.getTopQ17MeanReadLen(lastresults, 10);
		// now create the filters

		
		// still use a filter just so we can display some useful value in the tree node
		createTreeModelFromData(lastrigs, lasttree, new Q17BasesNodeFilter(0), lastresults.size()<50);
		
		maintab.setCaption(filtertitle);
		this.addToTable(basestree, lastresults, new Q17BasesNodeFilter(topq17bases));
		this.addToTable(maxtree, lastresults, new Q17MaxLenNodeFilter(topq17max));
		//createTreeModelFromData(lastrigs, basestree, new Q17BasesNodeFilter(topq17bases), true);
		//createTreeModelFromData(lastrigs, maxtree, new Q17MaxLenNodeFilter(topq17max), true);
	}

	private ArrayList<RundbExperiment> filterExperiments(List<RundbExperiment> list, NodeFilter filter) {
		ArrayList<RundbExperiment> res = new ArrayList<RundbExperiment>();
		for (RundbExperiment ex : list) {
			if (filter.passes(ex)) {
				res.add(ex);
				// check runs too?
			}
		}
		return res;
	}

	public String getStatus(MyResult result) {

		if (result.isCompleted()) {
			return "";
		} else if (result.isError()) {
			return "(error) ";
		} else if (result.isStarted()) {
			return "(started) ";
		} else {
			return "(" + result.getStatus() + ") ";
		}
	}

	public String getStatus(RundbExperiment exp) {
		String s = exp.getFtpStatus();
		if (s == null) {
			s = "";
		}
		s = s.toLowerCase().trim();
		if (s.startsWith("complete")) {
			return "";
		} else if (s.length() > 0) {
			int p = 0;
			try {
				p = (Integer.parseInt(s));
			} catch (Exception e) {
				return "(" + s + ") ";
			}

			return "(" + p + "%) ";

		} else {
			return "? ";
		}
	}

	public boolean isBB(RundbExperiment exp) {
		String t = exp.getChipType();
		if (t == null) {
			t = "";
		}
		t = t.replaceAll("\"", "");

		return (t.toLowerCase().startsWith("9") || (exp.getExpName() != null && exp.getExpName().indexOf("block") > -1));
	}

	private void createTreeModelFromData(ArrayList<MyRig> myrigs, final Tree tree, NodeFilter resultfilter, boolean expand) {
		tree.removeAllItems();
		
		Collections.sort(myrigs, new RigSorter());
		String passed_name = app.getParameters("run_name");
		p("Create tree: passed run_name=" + passed_name);
		for (MyRig rig : myrigs) {
			boolean foundinrig = false;

			Collections.sort(rig.getExperiments(), new ExperimentSorter());
			for (RundbExperiment exp : rig.getExperiments()) {
				boolean foundinexp = false;
				// if (expfilter.passes(exp)) {
				String type = exp.getChipType();
				type = type.replace("\"", "");
				String expname = getStatus(exp) + type + ": " + exp.getExpName();

				for (RundbResults res : exp.getRundbResultsCollection()) {
					MyResult myres = new MyResult(res, rig);
					if (resultfilter == null || resultfilter.passes(myres)) {
						if (!foundinrig) {
							foundinrig = true;
							Item rigit = tree.addItem(rig);
							rigit.getItemProperty("icon").setValue(new ThemeResource("../torrentscout/img/computer-3.png"));
							rigit.getItemProperty("caption").setValue(rig.getName());
						}
						if (!foundinexp) {
							foundinexp = true;
							Item expit = tree.addItem(exp);
							expit.getItemProperty("caption").setValue(expname);
							if (isBB(exp)) {
								expit.getItemProperty("icon").setValue(new ThemeResource("../torrentscout/img/chip_bb.png"));
							} else {
								expit.getItemProperty("icon").setValue(new ThemeResource("../torrentscout/img/chip.png"));
							}
							expit.getItemProperty("experiment").setValue(exp);
							tree.setParent(exp, rig);
						}
						Item it = tree.addItem(myres);

					//	File f = new File(res.getReportLink());
						
						String name = res.getResultsName();
//						if (f != null) {
//							name = f.getName();
//						}
						String caption = getStatus(myres) + name;
						if (resultfilter != null) {
							String val = resultfilter.getRelevantValue(myres);
							if (val.length()> 6) {
								long nr = Long.parseLong(val);
								if (val.length()>9) {
									val = ""+dec.format((double)(nr/1000000000.0d))+" GB";									
								}
								else val = ""+dec.format((double)(nr/1000000.0d))+" MB";		
							}
							else val = val +" Q17 bases";
							caption = caption + " ("+val+")";
						}
						it.getItemProperty("caption").setValue(caption);
						if (name.indexOf("_tn") > 0 || name.indexOf("-tn")>0 || name.indexOf("thumb") > 0) {
							it.getItemProperty("icon").setValue(new ThemeResource("../torrentscout/img/zoom-out.png"));
						} else
							it.getItemProperty("icon").setValue(new ThemeResource("../torrentscout/img/view-list-icons-2.png"));
						it.getItemProperty("run").setValue(myres);
						tree.setParent(myres, exp);

						tree.setChildrenAllowed(res, false);
						// for (RundbAnalysismetrics an :
						// res.getRundbAnalysismetricsCollection()) {
						// it = tree.addItem(an);
						// it.getItemProperty("caption").setValue("Analysis metrics");
						// it.getItemProperty("icon").setValue(new
						// ThemeResource("../torrentscout/img/view-list-icons-2.png"));
						// tree.setParent(res, exp);
						// }
						// check if there was a parameters in the main app
						if (passed_name != null) {
							if (myres.getResultsName().indexOf(passed_name) > -1 || passed_name.indexOf(myres.getResultsName()) > -1) {
								p("selecting item " + myres.getResultsName());
								this.selectItem(it);
							}
						}
					}
				}
				
				if (expand) tree.expandItemsRecursively(exp);
				// }
			}
			if (expand) tree.expandItemsRecursively(rig);
		}
		tree.addListener(new ItemClickListener() {

			public void itemClick(ItemClickEvent event) {
				p("Got item: " + event.getItem());

				Item it = event.getItem();
				selectItem(it);
			}
		});
		tree.addListener(new ValueChangeListener() {

			@Override
			public void valueChange(ValueChangeEvent event) {
				p("Got item: " + event.getProperty());

				Item it = tree.getItem(tree.getValue());
				selectItem(it);
			}
		});
	}

	private void showRunInfo(MyResult res) {
		String msg = "Run name: " + res.getResultsName() + "<br>";
		msg += "Experiment name: " + res.getExperimentName() + "<br>";
		File f = new File(res.getReportLink());
	//	String what = "file";
		msg += "Report link: " + res.getReportLink() +"<br>";
		msg += "Nr flows: " + res.getNrFlows() + "<br>";
		msg += "Status: " + res.getStatus() + "<br>";
		if (res.getQaulityMetrics() != null) {
			RundbQualitymetrics q = res.getQaulityMetrics();
			String val = ""+q;
			long nr = q.getQ17Bases();
			if (nr > 1000000000) {
				val = ""+dec.format((double)(nr/1000000000.0d))+" GB";									
			}
			else if (nr > 1000000) val = ""+dec.format((double)(nr/1000000.0d))+" MB";					
			else val = val +"  bases";
			
			msg += "Q17 bases: " + val+ "<br>";
			msg += "Q17 max read  length: " + q.getQ17MaxReadLength() + "<br>";
			msg += "Q17 mean read length: " + q.getQ17MeanReadLength() + "<br>";
		} else
			msg += "Found no quality metrics<br>";

		app.showBottomMessage("Run Info", msg);
	}

	private void showExpInfo(RundbExperiment res) {
		String msg = "Experiment name: " + res.getExpName() + "<br>";
		msg += "Project: " + res.getProject() + "<br>";
		msg += "Sample: " + res.getSample() + "<br>";
		msg += "Raw Folder: " + res.getExpDir() + "<br>";
		msg += "Nr flows: " + res.getFlows() + "<br>";
		
		msg += "Date: " + res.getDate() + "<br>";
		msg += "Chip type: " + res.getChipType() + "<br>";
		msg += "Cycles: " + res.getCycles() + "<br>";
		
		msg += "FTP Status: " + res.getFtpStatus() + "<br>";
		msg += "Storage host: " + res.getStorageHost() + "<br>";
		
		app.showBottomMessage("Experiment Info", msg);
	}

	private void selectItem(Item it) throws ConversionException, ReadOnlyException {
	//	p("select item: " + it);
		if (it == null) {
			return;
		}
		Property prun = it.getItemProperty("run");
		if (prun != null && prun.getValue() != null) {
			MyResult res = (MyResult) prun.getValue();

			selectRun(res);

		} else {
		//	p("Maybe experiment?");
			// maybe experiment
			Property pexp = it.getItemProperty("experiment");
			if (pexp != null) {
				RundbExperiment res = (RundbExperiment) pexp.getValue();
				if (res != null) showExpInfo(res);
				else
					p("Got exp property " + pexp + ", but no value");
			} else
				p("Got no experiment either: ids= " + it.getItemPropertyIds());
		}
	}

	private void selectRun(MyResult res) {
		showRunInfo(res);
		ExperimentContext exp = res.createContext();
		String dir = exp.getResDirFromDb();
//	p("Got a run dir res dir: " + dir);
		dir = findDir(dir);
		p("Dir from db is now converted to: " + dir);

		exp.setResultsDirectory(dir);
		exp.setCacheDir(dir + "plugin_out/torrentscout/");
		File f= new File(exp.getCacheDir());
		if (!f.exists()) f.mkdirs();
		p("Created experiment context: " + exp);
		selected.setValue(exp.getResultsName());
		context = exp;
		selected.requestRepaint();
		
		if( link != null) grid.removeComponent(link);
		// link must contain url
		String server = app.getServerName();		
		String url = server+exp.getReportLink();
		link = new Link("Open report", new ExternalResource(url));
		link.setTargetName("_blank");
        grid.addComponent(link, 1, 2);
	}

	private String findDir(String d) {
		if (!d.startsWith("/")) d = "/" + d;
		d = FileTools.addSlashOrBackslash(d);
		File f = new File(d);
		if (f.exists()) return d;

		String www = "/var/www" + d;
		f = new File(www);
		if (f.exists()) return www;

		String res = "/results/analysis" + d;
		f = new File(res);
		if (f.exists()) return res;

		// not what it was, not /var/www, not /results/analysis
		if (d.startsWith("/output") && !d.startsWith("/output/")) {
			int slash = d.indexOf("/", 5);
			String nr = d.substring(7, slash);
			p("got nr " + nr + " from " + d);
			d = d.substring(slash);
			d = "/results" + nr + "/analysis/output" + d;
			p("Changed dir to: " + d);
			f = new File(d);
			if (f.exists()) return d;
		}
		return d;
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
		//system.out.println("DbtWindow: " + msg);
		Logger.getLogger(DbWindow.class.getName()).log(Level.INFO, msg);
	}

	/**
	 * @return the URL
	 */
	public String getURL() {
		return URL;
	}

	/**
	 * @param URL
	 *            the URL to set
	 */
	public void setURL(String URL) {
		this.URL = URL;
		if (URL == null || URL.length() < 1) {
			this.URL = "localhost:5432/iondb";
		}
	}
}
