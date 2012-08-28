#!/usr/bin/env python
# Copyright (C) 2010 Ion Torrent Systems, Inc. All Rights Reserved

import sys
import re
import string
import socket
from optparse import OptionParser


#python ${DIRNAME}/create_jnlp_link.py \
#	--analysis-name=${ANALYSIS_NAME} \	
#	--results-dir=${RESULTS_DIR} \
#	--url-root=${URL_ROOT}

parser = OptionParser()
parser.add_option('-a', '--analysis-name', help='the analysis name', dest='analysis_name') 
parser.add_option('-r', '--results-dir', help='the plugin results directory', dest='results_dir') 
parser.add_option('-n', '--analysis-dir', help='the analysis directory', dest='analysis_dir') 
parser.add_option('-w', '--raw-dir', help='the raw data directory', dest='raw_dir') 
parser.add_option('-u', '--url-root', help='the url to the torrent server that contains the postgres db', dest='url_root')
parser.add_option('-p', '--plugin-dir', help='plugin directory', dest='plugin_dir')
parser.add_option('-c', '--chip-type', help='chip type', dest='chip_type')
parser.add_option('-s', '--sff', help='sff filename', dest='sff') 
parser.add_option('-b', '--bam', help='bam filename', dest='bam') 
(options, args) = parser.parse_args()

# Put link in the results directory
fn_html = options.results_dir + '/desktop_torrentscout_version.html'
fn_html_block = options.results_dir + '/torrentscout_block.html'
server_url = options.url_root
# won't work! Will return the IP address of the cruncher node!
#server_url = socket.gethostname()
# read  /etc/torrentserver/cluster_settings.py
#ionadmin@nog:/etc/torrentserver$ more cluster_settings.py
# Copyright (C) 2012 Ion Torrent Systems, Inc. All Rights Reserved
# Cluster settings for Torrent Suite Compute nodes
#PLUGINSERVER_HOST = 'blackbird.ite'
#PLUGINSERVER_PORT = 9191
#JOBSERVER_HOST = 'blackbird.ite'
#JOBSERVER_PORT = 10000

if sys.version_info < (2, 7):
	slash_pos = string.find(server_url, '/', 8)
	if slash_pos > 0:
		server_url = server_url[0:slash_pos]
else :
	slash_pos = server_url.find('/', 8)
	if slash_pos > 0:
		server_url = server_url[0:slash_pos]
		
images = server_url+options.plugin_dir	
# Write the html link
fhtml = open(fn_html_block, "w")
fhtml.write('<html>\n')
fhtml.write('<head></head>\n')
fhtml.write('<script type="text/javascript">\n')
fhtml.write('function openTSL(){\n')
fhtml.write('    var url=getServer()+":8080/TSL?restartApplication&run_name='+options.analysis_name+'&raw_dir='+options.raw_dir+'&res_dir='+options.analysis_dir+'&bam='+options.bam+'&sff='+options.sff+'";\n')
fhtml.write('    window.open(url, "Torrent Scout Light", "scrollbars=1,menubar=1,resizable=1,status=1,toolbar=1,width=1200, height=1000");\n')
fhtml.write('}\n')
fhtml.write('</script>\n')
fhtml.write('<script type="text/javascript">\n')
fhtml.write('function help(){\n')
fhtml.write('    var url=getServer()+":8080/TSL/VAADIN/help.pdf";\n')
fhtml.write('    window.open(url, "Torrent Scout Light Help", "scrollbars=1,menubar=1,resizable=1,status=1,toolbar=1,width=1200, height=1000");\n')
fhtml.write('}\n')
fhtml.write('</script>\n')
fhtml.write('<script type="text/javascript">\n')
fhtml.write('function getServer(){\n')
fhtml.write('	var url = document.URL;\n')
fhtml.write('	var pos =  url.indexOf("/", 8 );\n')
fhtml.write('	if (  pos != -1) {\n')
fhtml.write('		url = url.substring(0, pos);\n')
fhtml.write('	}\n')
fhtml.write('	return url;\n')
fhtml.write('}\n')
fhtml.write('</script>\n')
fhtml.write('<span style=\'font-family:"Lucida Sans Unicode", "Lucida Grande", Sans-Serif; font-size:11pt\'>\n')
fhtml.write('<a href="javascript:openTSL(this)" id="tsl">Open Torrent Scout Light (Web)</a> \n')
fhtml.write('<br>View ionograms, alignments and raw data for single wells, compute median signals on selected areas, view heat maps and find reads based on read properties, alignment characteristics or genome position ')
fhtml.write('<a href="javascript:help(this)" id="help">(Documentation)</a> \n')
fhtml.write('</span> \n')
fhtml.write('</body></html>')
fhtml.close()
fhtml = open(fn_html, "w")
fhtml.write('<html><head><title>Torrent Scout On "'+server_url+'"</title>\n<link rel="stylesheet" type="text/css" href="/site_media/stylesheet.css"/></head>\n')
fhtml.write('<style type="text/css">\n')
fhtml.write('a:visited { color: orange; }\n')
fhtml.write('a:link { color: orange; }\n')
fhtml.write('</style>\n')
fhtml.write('<body style="background-color: rgb(0, 0, 0); color: #FFFFFF">\n')
fhtml.write('<script type="text/javascript">\n')
fhtml.write('function openMasterLaunch(initialheap, maxheap){\n')
fhtml.write('    var jnlp = "'+server_url+':8080/torrentscout/master.jnlp?initial="+initialheap+"&max="+maxheap+"&properties=run_name=' + options.analysis_name + '";\n')
fhtml.write('    returnPage=jnlp;  \n')
fhtml.write('    var minimumVersion = \'1.6.0\';\n')
fhtml.write('    var url=jnlp;    \n')
fhtml.write('    location.href=url;\n')
fhtml.write('}\n')
fhtml.write('</script>\n')
fhtml.write('<script type="text/javascript">\n')
fhtml.write('function help(){\n')
fhtml.write('    var url=getServer()+":8080/TSL/VAADIN/help.pdf";\n')
fhtml.write('    window.open(url, "Torrent Scout Light Help", "scrollbars=1,menubar=1,resizable=1,status=1,toolbar=1,width=1200, height=1000");\n')
fhtml.write('}\n')
fhtml.write('</script>\n')
fhtml.write('<script type="text/javascript">\n')
fhtml.write('function openTSL(){\n')
fhtml.write('    var url=getServer()+":8080/TSL?restartApplication&run_name='+options.analysis_name+'&raw_dir='+options.raw_dir+'&res_dir='+options.analysis_dir+'";\n')
fhtml.write('    window.open(url, "Torrent Scout Light", "scrollbars=1,menubar=1,resizable=1,status=1,toolbar=1,width=1200, height=1000");\n')
fhtml.write('}\n')
fhtml.write('</script>\n')
fhtml.write('<script type="text/javascript">\n')
fhtml.write('function getServer(){\n')
fhtml.write('	var url = document.URL;\n')
fhtml.write('	var pos =  url.indexOf("/", 8 );\n')
fhtml.write('	if (  pos != -1) {\n')
fhtml.write('		url = url.substring(0, pos);\n')
fhtml.write('	}\n')
fhtml.write('	return url;\n')
fhtml.write('}\n')
fhtml.write('</script>\n')
fhtml.write('<script type="text/javascript">\n')
fhtml.write('function processSelection(v) {\n')
fhtml.write('  switch(v) {\n')
fhtml.write('    case "1":\n')
fhtml.write('      openTSL();\n')
fhtml.write('      break;\n')
fhtml.write('    case "2":\n')
fhtml.write('      openMasterLaunch(\'768m\', \'1200m\');\n')
fhtml.write('      break;    \n')
fhtml.write('	case "3":\n')
fhtml.write('      openMasterLaunch(\'1024m\', \'2048m\');\n')
fhtml.write('      break;    \n')
fhtml.write('	case "4":\n')
fhtml.write('      openMasterLaunch(\'2048m\', \'4096m\');\n')
fhtml.write('      break;    \n')
fhtml.write('	case "5":\n')
fhtml.write('      openMasterLaunch(\'2048m\', \'6500m\');\n')
fhtml.write('      break;    \n')
fhtml.write('	case "6":\n')
fhtml.write('      openMasterLaunch(\'2G\', \'8G\');\n')
fhtml.write('      break;\n')
fhtml.write('	case "7":\n')
fhtml.write('      openMasterLaunch(\'4G\', \'10G\');\n')
fhtml.write('      break;\n')
fhtml.write('    }\n')
fhtml.write('  }\n')
fhtml.write('</script>\n')
fhtml.write('<img alt="startup" src="/tsl-startup.png">\n')
fhtml.write(' <br>\n')
fhtml.write('<select id="memory" name="Open Torrent Scout with " size="1" value="0"  \n')
fhtml.write('onchange="processSelection(this.value);" style="background-color: rgb(0, 0, 0); color: #DDDD00">\n')
fhtml.write('<option value="0">Open Torrent Scout ... </option>\n')
fhtml.write('<option value="1">Web Version (Torrent Scout Light)</option>\n')
fhtml.write('<option value="2">1.2 GB memory (Java 32 bit) (cropped data)</option>\n')
fhtml.write('<option value="3">2 GB memory (Java 64 bit) (314 chips) (default)</option>\n')
fhtml.write('<option value="4">4 GB memory (Java 64 bit) (316 chips) </option>\n')
fhtml.write('<option value="5">6 GB memory (Java 64 bit) (318 chips) </option>\n')
fhtml.write('<option value="6">8 GB memory (Java 64 bit) (318+ chips) </option>\n')
fhtml.write('</select><br><br>\n')
fhtml.write('<table>\n')
fhtml.write('<tr><td>Run name:</td><td>'+options.analysis_name+'</td></tr>\n')
fhtml.write('<tr><td>Results folder: </td><td><b>'+options.analysis_dir+'</b></td></tr>\n')
fhtml.write('<tr><td>Raw folder: </td><td><b>'+options.raw_dir+'</b></td></tr>\n')
fhtml.write('</table>\n')
fhtml.write('<br>\n')
fhtml.write('<font size=2>*To launch TorrentScout, you will need <b><font color="DDDD00">\n')
fhtml.write('<a href="http://www.java.com/en/download/manual.jsp">Sun Java</a></font></b>. For 1.5+ GB memory, you will need \n')
fhtml.write('<b><font color="DDDD00"><a href="http://www.java.com/en/download/manual.jsp">Java 64 bit</a></font></b></font><br>\n')
fhtml.write('<hr>\n')
fhtml.write('<font size=2><i>Please go to the <a href="https://github.com/iontorrent/TorrentScoutLight">GitHub</a> or the <a href="https://iontorrent.jira.com/wiki/display/TSC/Home">Ion Torrent</a> Torrent Scout page for documentation and source,')
fhtml.write(' or send an email to <a href="mailto:chantal.roth@lifetech.com?subject=Torrent%20Scout%20Feedback&amp;cc=croth@nobilitas.com&amp;body=Hi%20Chantal,%0D%0A%0D%0A">Chantal Roth</a> for help or feedback.')
fhtml.write('<br>For help on Torrent Scout Light (Web version), click <a href="javascript:help(this)" id="help">here</a></i></font>\n')
fhtml.write('<br><font size=2 color="222222">server: <script type="text/javascript">document.write(getServer());</script></font>\n')
fhtml.write('</body></html>')
fhtml.close()

