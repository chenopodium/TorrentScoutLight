/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.utils;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.iontorrent.guiutils.heatmap.GradientPanel;
import com.iontorrent.rawdataaccess.wells.BitMask;
import com.iontorrent.torrentscout.explorer.ExplorerContext;
import com.iontorrent.torrentscout.explorer.MaskEditDensityPanel;
import com.vaadin.terminal.StreamResource;

/**
 * 
 * @author Chantal Roth chantal.roth@lifetech.com
 */
public class GradientImage implements StreamResource.StreamSource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ByteArrayOutputStream imagebuffer = null;
	GradientPanel pan;
	RenderedImage image;
	int w;
	int h;
	public GradientImage(GradientPanel pan, int w, int h) {		
		this.pan = pan;
		this.w = w;
		this.h = h;
	}

	public RenderedImage getImage() {
		p("Get mask Image for gradient panel");
		if (image == null) {
			/* Create an image and draw something on it. */			
			pan.repaint();
			image = pan.myCreateImage(w, h);
			if (image == null) {
				p("Image is null of " + pan.getClass().getName());
			}					
		}
		return image;
	}

	public InputStream getStream() {
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
			err("Could not create gradient panel image", e);
			return null;
		}
	}

	private static void err(String msg, Exception ex) {
		Logger.getLogger(GradientImage.class.getName()).log(Level.SEVERE, msg, ex);
	}

	private static void p(String msg) {
		Logger.getLogger(GradientImage.class.getName()).log(Level.INFO, msg);
	}
}
