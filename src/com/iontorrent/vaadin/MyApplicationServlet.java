package com.iontorrent.vaadin;

import java.io.BufferedWriter;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.vaadin.terminal.gwt.server.ApplicationServlet;

public class MyApplicationServlet extends ApplicationServlet {

   
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	 protected void writeAjaxPageHtmlHeadStart(final BufferedWriter page,
	            final HttpServletRequest request) throws IOException {
	        // write html header
	        page.write("<!DOCTYPE html PUBLIC \"-//W3C//DTD "
	                + "XHTML 1.0 Transitional//EN\" "
	                + "\"http://www.w3.org/TR/xhtml1/"
	                + "DTD/xhtml1-transitional.dtd\">\n");

	        page.write("<html xmlns=\"http://www.w3.org/1999/xhtml\""
	                + ">\n<head>\n");
	        page.write("<!--- Added via MyApplicationServlet.writeAjaxPageHtmlHeadStart, Chantal Roth -->\n" +
	        		"<meta http-equiv=\"X-UA-Compatible\" content=\"IE=Edge\"/>\n");
	    }
	

}
