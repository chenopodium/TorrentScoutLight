/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.process;

import java.awt.Point;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.iontorrent.rawdataaccess.pgmacquisition.RawType;
import com.iontorrent.rawdataaccess.wells.BitMask;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.torrentscout.explorer.process.RasterPanel;
import com.iontorrent.torrentscout.explorer.process.SubregionView;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.terminal.StreamResource;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class SubregionImage implements StreamResource.StreamSource {

	ByteArrayOutputStream imagebuffer = null;
	ExplorerContext exp;

	RasterPanel pan;
	RenderedImage image;
	RawType type;
	int flow;
	int frame;

	public SubregionImage(ExplorerContext exp, BitMask showmask, int pix_per_well) {
		this.exp = exp;
		pan = new RasterPanel(pix_per_well);
		pan.update(exp);
		// if (showuse || showbg) {
		pan.redrawImages(showmask);
		// }

	}

	public SubregionView getSubregionView() {
		return pan.getSubregionView();
	}
	public Point getPointFromWell(WellCoordinate coord) {
		return pan.getPointFromWell(coord);
	}

	public WellCoordinate getWellCoordinate(int x, int y) {
		return pan.getWellCoordinate(x, y);
	}

	/*
	 * We need to implement this method that returns the resource as a stream.
	 */

	public RenderedImage getImage() {
		if (image == null) {
			/* Create an image and draw something on it. */
			image = pan.myCreateImage(exp.getFrame());
			if (image == null) {
				p("Image is null of " + pan.getClass().getName());
			}
		}
		return image;
	}

	public InputStream getStream() {
	//	p("GetStream called");
		if (exp == null) {
			err("Got no explorer context");
			return null;
		}
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
			err("Could not create score image", e);
			return null;
		}
	}

	private static void err(String msg, Exception ex) {
		//system.out.println("SubregionImage: " + msg);
		Logger.getLogger(SubregionImage.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
		//system.out.println("SubregionImage: " + msg);
		Logger.getLogger(SubregionImage.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
		//system.out.println("SubregionImage: " + msg);
		Logger.getLogger(SubregionImage.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		//system.out.println("SubregionImage: " + msg);
		Logger.getLogger(SubregionImage.class.getName()).log(Level.INFO, msg);
	}
}
