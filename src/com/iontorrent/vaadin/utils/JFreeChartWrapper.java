package com.iontorrent.vaadin.utils;
/*
 * Copyright 2009 IT Mill Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.vaadin.Application;
import com.vaadin.terminal.ApplicationResource;
import com.vaadin.terminal.DownloadStream;
import com.vaadin.terminal.Resource;
import com.vaadin.terminal.Sizeable;
import com.vaadin.terminal.gwt.server.AbstractWebApplicationContext;
import com.vaadin.ui.Embedded;

/**
 * A simple JFreeChart wrapper that renders charts in SVG to browser.
 * 
 * To use this component, you'll need all the common JFreeChart and Batik
 * libraries.
 * 
 * For MSIE it will fall back to PNG rendering (not that nice when printing).
 * 
 * Supported sizes are currently just pixels, inches, centimeters (converted to
 * pixels by 96dpi). Set them to wrapper.
 * 
 * TODO make it support relative sizes (should be possible to do cleanly with
 * SVG)
 * 
 * TODO when browsers develop SVG could be painted to target instead of
 * registering application resource. This would shorten rendering time and
 * lessen memory consumption on server. This already works ok for webkit
 * browsers. Firefox and Opera fail with gradients.
 * 
 * @author mattitahvonen
 */
@SuppressWarnings("serial")
public class JFreeChartWrapper extends Embedded {

        public enum RenderingMode {
                SVG, PNG, AUTO
        }

        // 809x 500 ~g olden ratio
        private static final int DEFAULT_WIDTH = 809;
        private static final int DEFAULT_HEIGHT = 500;

        private final JFreeChart chart;
        private ApplicationResource res;
        private RenderingMode mode = RenderingMode.AUTO;
        private boolean gzipEnabled = false;
        private int graphWidthInPixels = -1;
        private int graphHeightInPixels = -1;
        private String aspectRatio = "none"; // stretch to fill whole space
        private ChartRenderingInfo info;
        
        public JFreeChartWrapper(JFreeChart chartToBeWrapped) {
                chart = chartToBeWrapped;
                setWidth(DEFAULT_WIDTH, UNITS_PIXELS);
                setHeight(DEFAULT_HEIGHT, UNITS_PIXELS);
        }

        public JFreeChartWrapper(JFreeChart chartToBeWrapped,
                        RenderingMode renderingMode) {
                this(chartToBeWrapped);
                setRenderingMode(renderingMode);
        }

        /**
         * Compress SVG charts in wrapper. It makes sense to put this on if the
         * server does not automatically compress responses.
         * 
         * @param compress
         *            true to enable component level compression, default false
         */
        public void setGzipCompression(boolean compress) {
                this.gzipEnabled = compress;
                requestRepaint();
        }

        private void setRenderingMode(RenderingMode newMode) {
                if (newMode == RenderingMode.PNG) {
                        setType(TYPE_IMAGE);
                } else {
                        setType(TYPE_OBJECT);
                        setMimeType("image/svg+xml");
                }
                mode = newMode;
        }

        @Override
        public void attach() {
                super.attach();
                AbstractWebApplicationContext context = (AbstractWebApplicationContext) getApplication()
                                .getContext();
                if (mode == RenderingMode.AUTO) {
                        if (context.getBrowser().isIE()
                                        && context.getBrowser().getBrowserMajorVersion() < 9) {
                                setRenderingMode(RenderingMode.PNG);
                        } else {
                                // all decent browsers support SVG
                                setRenderingMode(RenderingMode.SVG);
                        }
                }
                getApplication().addResource((ApplicationResource) getSource());
        }

        @Override
        public void detach() {
                getApplication().removeResource((ApplicationResource) getSource());
                super.detach();
        }

        /**
         * This method may be used to tune rendering of the chart when using
         * relative sizes. Most commonly you should use just use common methods
         * inherited from {@link Sizeable} interface.
         * <p>
         * Sets the pixel size of the area where the graph is rendered. Most
         * commonly developer may need to fine tune the value when the
         * {@link JFreeChartWrapper} has a relative size.
         * 
         * @see JFreeChartWrapper#getGraphWidth()
         * @see #setSvgAspectRatio(String)
         * @param width
         */
        public void setGraphWidth(int width) {
                graphWidthInPixels = width;
        }

        /**
         * This method may be used to tune rendering of the chart when using
         * relative sizes. Most commonly you should use just use common methods
         * inherited from {@link Sizeable} interface.
         * <p>
         * Sets the pixel size of the area where the graph is rendered. Most
         * commonly developer may need to fine tune the value when the
         * {@link JFreeChartWrapper} has a relative size.
         * 
         * @see JFreeChartWrapper#getGraphHeigt()
         * @see #setSvgAspectRatio(String)
         * @param height
         */
        public void setGraphHeight(int height) {
                graphHeightInPixels = height;
        }

        /**
         * Gets the pixel width into which the graph is rendered. Unless explicitly
         * set, the value is derived from the components size, except when the
         * component has relative size.
         */
        public int getGraphWidth() {
                if (graphWidthInPixels > 0) {
                        return graphWidthInPixels;
                }
                int width;
                float w = getWidth();
                if (w < 0) {
                        return DEFAULT_WIDTH;
                }
                switch (getWidthUnits()) {
                case UNITS_CM:
                        width = (int) (w * 96 / 2.54);
                        break;
                case UNITS_INCH:
                        width = (int) (w * 96);
                        break;
                case UNITS_PERCENTAGE:
                        width = DEFAULT_WIDTH;
                        break;
                default:
                        width = (int) w;
                        break;
                }
                return width;
        }

        /**
         * Gets the pixel height into which the graph is rendered. Unless explicitly
         * set, the value is derived from the components size, except when the
         * component has relative size.
         */
        public int getGraphHeight() {
                if (graphHeightInPixels > 0) {
                        return graphHeightInPixels;
                }
                int height;
                float w = getHeight();
                if (w < 0) {
                        return DEFAULT_HEIGHT;
                }
                switch (getWidthUnits()) {
                case UNITS_CM:
                        height = (int) (w * 96 / 2.54);
                        break;
                case UNITS_INCH:
                        height = (int) (w * 96);
                        break;
                case UNITS_PERCENTAGE:
                        height = DEFAULT_HEIGHT;
                        break;
                default:
                        height = (int) w;
                        break;
                }
                return height;
        }

        public String getSvgAspectRatio() {
                return aspectRatio;
        }

        /**
         * See SVG spec from W3 for more information.
         * 
         * 
         * Default is "none" (stretch), another common value is "xMidYMid" (stretch
         * proportionally, align middle of the area).
         * 
         * @param svgAspectRatioSetting
         */
        public void setSvgAspectRatio(String svgAspectRatioSetting) {
                aspectRatio = svgAspectRatioSetting;
        }

        public ChartRenderingInfo getInfo(int w, int h ) {
        	 info = new ChartRenderingInfo();
        	BufferedImage img = chart.createBufferedImage(w, h, info);
        	Graphics2D g = img.createGraphics();
        	chart.draw(g,new Rectangle(w, h), info);        	
        	return info;
        }
        @Override
        public Resource getSource() {
                if (res == null) {
                        res = new ApplicationResource() {

                                private ByteArrayInputStream bytestream = null;

                                ByteArrayInputStream getByteStream() {
                                        if (chart != null && bytestream == null) {
                                                int widht = getGraphWidth();
                                                int height = getGraphHeight();
                                                info = new ChartRenderingInfo();

                                                if (mode == RenderingMode.SVG) {

                                                        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
                                                                        .newInstance();
                                                        DocumentBuilder docBuilder = null;
                                                        try {
                                                                docBuilder = docBuilderFactory
                                                                                .newDocumentBuilder();
                                                        } catch (ParserConfigurationException e1) {
                                                                throw new RuntimeException(e1);
                                                        }
                                                        Document document = docBuilder.newDocument();
                                                        Element svgelem = document.createElement("svg");
                                                        document.appendChild(svgelem);

                                                        // Create an instance of the SVG Generator
                                                        SVGGraphics2D svgGenerator = new SVGGraphics2D(
                                                                        document);

                                                        // draw the chart in the SVG generator
                                                       
                                                        chart.draw(svgGenerator, new Rectangle(widht,
                                                                        height), info);
                                                        Element el = svgGenerator.getRoot();
                                                        el.setAttributeNS(null, "viewBox", "0 0 " + widht
                                                                        + " " + height + "");
                                                        el.setAttributeNS(null, "style",
                                                                        "width:100%;height:100%;");
                                                        el.setAttributeNS(null, "preserveAspectRatio",
                                                                        getSvgAspectRatio());

                                                        // Write svg to buffer
                                                        ByteArrayOutputStream baoutputStream = new ByteArrayOutputStream();
                                                        Writer out;
                                                        try {
                                                                OutputStream outputStream = gzipEnabled ? new GZIPOutputStream(
                                                                                baoutputStream) : baoutputStream;
                                                                out = new OutputStreamWriter(outputStream,
                                                                                "UTF-8");
                                                                /*
                                                                 * don't use css, FF3 can'd deal with the result
                                                                 * perfectly: wrong font sizes
                                                                 */
                                                                boolean useCSS = false;
                                                                svgGenerator.stream(el, out, useCSS, false);
                                                                outputStream.flush();
                                                                outputStream.close();
                                                                bytestream = new ByteArrayInputStream(
                                                                                baoutputStream.toByteArray());
                                                        } catch (UnsupportedEncodingException e) {
                                                                // TODO Auto-generated catch block
                                                                e.printStackTrace();
                                                        } catch (SVGGraphics2DIOException e) {
                                                                // TODO Auto-generated catch block
                                                                e.printStackTrace();
                                                        } catch (IOException e) {
                                                                // TODO Auto-generated catch block
                                                                e.printStackTrace();
                                                        }
                                                } else {
                                                        // Draw png to bytestream
                                                        try {
                                                        	
                                                                byte[] bytes = ChartUtilities.encodeAsPNG(chart
                                                                                .createBufferedImage(widht, height));
                                                                bytestream = new ByteArrayInputStream(bytes);
                                                        } catch (IOException e) {
                                                                // TODO Auto-generated catch block
                                                                e.printStackTrace();
                                                        }

                                                }

                                        } else {
                                                bytestream.reset();
                                        }
                                        return bytestream;
                                }

                                public Application getApplication() {
                                        return JFreeChartWrapper.this.getApplication();
                                }

                                public int getBufferSize() {
                                        if (getByteStream() != null) {
                                                return getByteStream().available();
                                        } else {
                                                return 0;
                                        }
                                }

                                public long getCacheTime() {
                                        return 0;
                                }

                                public String getFilename() {
                                        if (mode == RenderingMode.PNG) {
                                                return "graph.png";
                                        } else {
                                                return gzipEnabled ? "graph.svgz" : "graph.svg";
                                        }
                                }

                                public DownloadStream getStream() {
                                        DownloadStream downloadStream = new DownloadStream(
                                                        getByteStream(), getMIMEType(), getFilename());
                                        if (gzipEnabled && mode == RenderingMode.SVG) {
                                                downloadStream.setParameter("Content-Encoding", "gzip");
                                        }
                                        return downloadStream;
                                }

                                public String getMIMEType() {
                                        if (mode == RenderingMode.PNG) {
                                                return "image/png";
                                        } else {
                                                return "image/svg+xml";
                                        }
                                }
                        };
                }
                return res;
        }
}
