/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.iontorrent.vaadin.iono;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.iontorrent.expmodel.ExperimentContext;
import com.iontorrent.ionogram.MultiFlowPanel;
import com.vaadin.terminal.StreamResource;

/**
 *
 * @author Chantal Roth 
 * chantal.roth@lifetech.com
 */
public class MultiFlowImage implements StreamResource.StreamSource {

    ByteArrayOutputStream imagebuffer = null;
    int reloads = 0;
    MultiFlowPanel pan;
    boolean norm;
    boolean raw;
    ExperimentContext exp;
    RenderedImage image;
    int maxflow;
    int minflow;
    public MultiFlowImage(ExperimentContext exp, MultiFlowPanel pan, int minflow, int maxflow) {
        	this.pan = pan;
        	this.exp = exp;
        	this.maxflow = maxflow;
        	this.minflow = minflow;
        	getImage(minflow, maxflow);
    }
    /* We need to implement this method that returns
     * the resource as a stream. */

    public RenderedImage getImage(int minflow, int maxflow) {
    	if (image != null) return image;
    	
    	 int flows = Math.max(10, exp.getWellContext().getNrFlwos());
    	 flows = Math.min(maxflow, flows);
         pan.setSize(flows*40,200);
         p("minflow="+minflow+", maxflow is: "+maxflow+", nrflows="+flows);
         pan.setMaxFlow(flows);
         pan.setMinFlow(minflow);
         image = pan.myCreateImage(flows*40,200);
         p("Got image of size "+image.getWidth()+"/"+image.getHeight());
         return image;
    }
    public InputStream getStream() {
        p("MultiFlowImage GetStream called");
        if (pan == null) {
            err("Got no experiment context");
            return null;
        }
              
        image = getImage(minflow, maxflow); 
        try {
            /* Write the image to a buffer. */
            imagebuffer = new ByteArrayOutputStream();

            ImageIO.write(image, "png", imagebuffer);
            /* Return a stream from the buffer. */
            return new ByteArrayInputStream(
                    imagebuffer.toByteArray());
        } catch (IOException e) {
            err("Could not create MultiFlowImage image", e);
            return null;
        }
    }

    private static void err(String msg, Exception ex) {
        //system.out.println("MultiFlowImage: " + msg);
        Logger.getLogger(MultiFlowImage.class.getName()).log(Level.SEVERE, msg, ex);
    }

    private static void err(String msg) {
        //system.out.println("MultiFlowImage: " + msg);
        Logger.getLogger(MultiFlowImage.class.getName()).log(Level.SEVERE, msg);
    }

    private static void warn(String msg) {
        //system.out.println("MultiFlowImage: " + msg);
        Logger.getLogger(MultiFlowImage.class.getName()).log(Level.WARNING, msg);
    }

    private static void p(String msg) {
        //system.out.println("MultiFlowImage: " + msg);
        Logger.getLogger(MultiFlowImage.class.getName()).log(Level.INFO, msg);
    }
}
