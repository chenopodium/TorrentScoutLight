DEPENDENCIES/REQUIREMENTS:
=========================
- requires Tomcat (version 6 or later)

- requires at least 4 GB of memory on the server side for PGMs and at least 8 GB on server side for Proton chips
  edit the file /etc/default/tomcat6 (or tomcat5, tomcat7)
  edit the line with JAVA_OPTS to something like:  
  JAVA_OPTS="-Djava.awt.headless=true -Xms2G -Xmx8G -XX:MaxPermSize=512M"

DEPLOYMENT:
==========
- copy the .war file (TSL.war) to /var/lib/tomcatxy/webapps
- you might have to restart tomcat: sudo /etc/init.d/tomcat6 restart

STARTING:
========
- the default url is http://yourserver:8080/TSL (if the .war file name was TSL.war)
- if the ion-jk package is also installed, you don't need the port 8080 anymore and you can just use http://yourserver/TSL

RESTART:
=======
- to restart, append ?restartApplication as in:  http://yourserver/TSL?restartApplication

PASSING ARGUMENTS:
=================
- you can pass the run name, folders and bam/sff file names to TSL so that it will open the experiment automatically:
run_name=Auto_CDC-38-intp1-15-jm-r20863_1436_tn&
raw_dir=/ion-data/results/cdC/R_2012_03_14_20_49_03_user_CDC-38-intp1-15-jm-r20863
res_dir=/results/analysis/output/Home/Auto_CDC-38-intp1-15-jm-r20863_1436_tn_31844
bam=R_2012_03_14_20_49_03_user_CDC-38-intp1-15-jm-r20863_Auto_CDC-38-intp1-15-jm-r20863_1436_tn.bam
sff=R_2012_03_14_20_49_03_user_CDC-38-intp1-15-jm-r20863_Auto_CDC-38-intp1-15-jm-r20863_1436_tn.sff

Example:
http://server.com/TSL?restartApplication&run_name=Auto_CDC-38-intp1-15-jm-r20863_1436_tn&raw_dir=/ion-data/results/cdC/R_2012_03_14_20_49_03_user_CDC-38-intp1-15-jm-r20863/thumbnail&res_dir=/results/analysis/output/Home/Auto_CDC-38-intp1-15-jm-r20863_1436_tn_31844&bam=R_2012_03_14_20_49_03_user_CDC-38-intp1-15-jm-r20863_Auto_CDC-38-intp1-15-jm-r20863_1436_tn.bam&sff=R_2012_03_14_20_49_03_user_CDC-38-intp1-15-jm-r20863_Auto_CDC-38-intp1-15-jm-r20863_1436_tn.sff

DEVELOPMENT:
============
- use Eclipse with the Vaadin addition http://vaadin.com/eclipse
- to create .war file: File/export, Web/.war file

DOCUMENTATION:
=============
- see the docs folder