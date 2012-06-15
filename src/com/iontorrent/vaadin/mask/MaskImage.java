/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.mask;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.iontorrent.rawdataaccess.wells.BitMask;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.torrentscout.explorer.MaskEditDensityPanel;
import com.iontorrent.wellmodel.WellCoordinate;
import com.vaadin.terminal.StreamResource;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class MaskImage implements StreamResource.StreamSource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ByteArrayOutputStream imagebuffer = null;
	ExplorerContext exp;

	MaskEditDensityPanel pan;
	RenderedImage image;
	BitMask mask;
	public MaskImage(ExplorerContext exp, BitMask mask, int bucket) {
		this.exp = exp;
		if (exp != null) {
			this.mask = mask;
			//p("Creating mask image for :"+mask.getName()+" at "+mask.getRelCoord());
			 pan = new MaskEditDensityPanel(exp.getExp(), mask, exp.getRasterSize());
			 image = null;
			 pan.setContext(mask, bucket);
			 pan.redrawImage();
			 
			
		}
	}
	
	/*
	 * We need to implement this method that returns the resource as a stream.
	 */

	public RenderedImage getImage() {
		p("Get mask Image for"+mask.getName());
		if (image == null) {
			/* Create an image and draw something on it. */
			p("Calling pan.myCreateImage");
			pan.redrawImage();
			image = pan.myCreateImage();
			if (image == null) {
				p("Image is null of " + pan.getClass().getName());
			}			
			
		}
		//p("Got image for "+mask.getName()+" with size "+image.getWidth()+"/"+image.getHeight());
		return image;
	}
	  public WellCoordinate getWellCoordinate(int x, int y) {
	        return pan.getWellCoordinate(x, y);
	    }

	public InputStream getStream() {
	//	p("GetStream called for mask "+mask.getName());
		
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
			err("Could not create mask editor image", e);
			return null;
		}
	}

	private static void err(String msg, Exception ex) {
//		//system.out.println("MaskImage: " + msg);
		Logger.getLogger(MaskImage.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void err(String msg) {
	//	//system.out.println("MaskImage: " + msg);
		Logger.getLogger(MaskImage.class.getName()).log(Level.SEVERE, msg);
	}

	private static void warn(String msg) {
	//	//system.out.println("MaskImage: " + msg);
		Logger.getLogger(MaskImage.class.getName()).log(Level.WARNING, msg);
	}

	private static void p(String msg) {
		//System.out.println("MaskImage:" + msg);
		Logger.getLogger(MaskImage.class.getName()).log(Level.INFO, msg);
	}
}
