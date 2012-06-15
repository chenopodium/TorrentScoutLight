package com.iontorrent.vaadin.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.iontorrent.utils.ErrorHandler;
import com.vaadin.Application;
import com.vaadin.terminal.DownloadStream;
import com.vaadin.terminal.FileResource;

@SuppressWarnings("serial")
public class FileDownloadResource extends FileResource {

	
	public FileDownloadResource( File file, Application app) {
		super(file, app);
		
	}

	public DownloadStream getStream() {
		try {
			final DownloadStream ds = new DownloadStream(
					new FileInputStream(getSourceFile()), getMIMEType(),
					getFilename());
			if (!getFilename().endsWith(".html")){
				ds.setParameter("Content-Disposition", "attachment; filename="	+ getFilename());
			}				
			ds.setCacheTime(getCacheTime());
			return ds;
		} catch (final FileNotFoundException e) {
			ExportTool.err(ErrorHandler.getString(e));
			
		}
		return null;
	}
}