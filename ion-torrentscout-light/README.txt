======== Dependencies:=============

sudo apt-get install tomcat6 tomcat6-admin tomcat6-common

============= Files:==============
Tomcat Java and Memory Settings:
/etc/default/tomcat6
change line with JAVA_OPTS to:
JAVA_OPTS="-Djava.awt.headless=true -server -Xms2048m -Xmx8000m -XX:MaxPermSize=512M"
(to allow for more concurrent users, increase -Xmx setting. 
One user requires about  3-4 GB at peak time, such as when loading raw data)

Access to web admin (to deploy, undeploy, check sessions etc)
/etc/tomcat6/tomcat-users.xml
Uncomment user section so that it contains:
<user username="tomcat" password="tomcat" roles="admin, manager, manager-gui"/>

Torrent Scout Light Application file:
/var/lib/tomcat6/webapps/TSL.war

Torrent Scout Desktop Application file (optional):
/var/lib/tomcat6/webapps/torrentscout.war

Log settings
/var/lib/tomcat6/conf/logging.properties
set debug level to SEVERE, maybe write log file to another location than /var/log

============= Deployment/Management:==============
Restart Tomcat: sudo /etc/init.d tomcat6 restart

Deploy app: copy .war file to /var/lib/tomcat6/webapps
(may require restart of tomcat)

