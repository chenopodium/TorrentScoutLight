/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.fit;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.torrentscout.explorer.fit.AbstractHistoFunction;
import com.iontorrent.torrentscout.explorer.fit.HistoPanel;
import com.iontorrent.utils.stats.HistoStatistics;
import com.iontorrent.utils.stats.StatPoint;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.terminal.StreamResource;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class HistImage implements StreamResource.StreamSource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ByteArrayOutputStream imagebuffer = null;
	ExplorerContext exp;

	HistoPanel pan;
	RenderedImage image;
	int cutleft;
	int cutright;
	StatPoint addedStats;

	public HistImage(ExplorerContext exp, StatPoint stat, double[][] histodata, int cutleft, int cutright) {
		this.exp = exp;
		if (exp != null) {
			p("Creating HistImage: cuts=" + cutleft + "-" + cutright);
			this.cutleft = cutleft;
			this.cutright = cutright;
			addedStats = null;
			createHistoPanel(stat, histodata);

		}
	}

	public void zoom() {
		if (pan != null) {
			image = null;
			// zoom and pan != null
			p("Trying to zoom in");
			double minx = -1000000;
			double maxx = Double.MAX_VALUE;
			minx = Math.max(minx, pan.getLeftX());
			maxx = Math.min(pan.getRightX(), maxx);

			if (minx > maxx) {
				double tmp = minx;
				minx = maxx;
				maxx = tmp;
			}
			AbstractHistoFunction func = exp.getHistoFunction();
			func.setMinx(minx);
			func.setMaxx(maxx);
			func.execute();
			StatPoint stat = func.getDataPoints();
			double[][]  histodata = func.getResult();
			createHistoPanel(stat, histodata);
			//if (addedStats != null) addToHistoPanel(addedStats);
		}
	}
	public void addToHistoPanel(StatPoint stat) {
		if (pan != null) {
			image = null;
			addedStats = stat;
			pan.addStats(stat);
		}
	}

	public HistoStatistics getHisto() {
		return pan.getHisto();
	}

	public void createHistoPanel(StatPoint stat, double[][] histodata) {
		// public HistoPanel(ExplorerContext maincont, StatPoint stat,
		// double[][] histodata, int nrbuckets, boolean normalize, boolean
		// second) {
		image= null;
		pan = new HistoPanel(exp, stat, histodata, 150, true, true);
		pan.setLeftpos(cutleft);
		pan.setRightpos(cutright);
		pan.setDrawScissors(false);
	}

	public WellCoordinate findCoordForBin(int bin) {
		return pan.findCoordForBin(bin);
	}

	public int getBin(int x) {
		return pan.getBin(x);
	}

	public int getXForXval(int x) {
		return pan.getXForXval(x);
	}

	public int getXForBucket(int b) {
		return pan.getXForBucket(b);
	}

	// public Point getPointFromWell(WellCoordinate coord) {
	// return pan.getPointFromWell(coord);
	// }
	//
	// public WellCoordinate getWellCoordinate(int x, int y) {
	// return pan.getWellCoordinate(x, y);
	// }

	/*
	 * We need to implement this method that returns the resource as a stream.
	 */

	public RenderedImage getImage() {
		p("Get Image");
		if (image == null) {
			/* Create an image and draw something on it. */
			p("Calling pan.myCreateImage");
			image = pan.myCreateImage();
			if (image == null) {
				p("Image is null of " + pan.getClass().getName());
			}

		}
		p("Got image for " + pan.getClass().getName() + " with size " + image.getWidth() + "/" + image.getHeight());
		return image;
	}

	public InputStream getStream() {
		// p("GetStream called");

		getImage();

		try {
			/* Write the image to a buffer. */
			imagebuffer = new ByteArrayOutputStream();
			ImageIO.write(image, "png", imagebuffer);
			/* Return a stream from the buffer. */
			imagebuffer.flush();
			ByteArrayInputStream res = new ByteArrayInputStream(imagebuffer.toByteArray());
			imagebuffer.close();
			return res;
		} catch (IOException e) {
			err("Could not create hist image", e);
			return null;
		}
	}

	private static void err(String msg, Exception ex) {
		//system.out.println("HistImage: " + msg);
		Logger.getLogger(HistImage.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		//system.out.println("HistImage: " + msg);
		Logger.getLogger(HistImage.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		//system.out.println("HistImage: " + msg);
		Logger.getLogger(HistImage.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		//system.out.println("HistImage:" + msg);
		Logger.getLogger(HistImage.class.getName()).log(Level.INFO, msg);
	}
}
