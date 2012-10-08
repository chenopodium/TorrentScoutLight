cd /var/log/tomcat5

grep 'TSVaadin: Got DB URL' catalina.out  | sort |uniq -c

grep RemoteHost catalina.out  | sort |uniq -c
grep RemoteHost *.log  | sort |uniq -c

gunzip *.gz -c | grep Remote  | sort | uniq -c | awk '{ print $1", "$4 }'


grep torrentscout localhost_access_log.2012-04-* -h | awk '{ print $1", "substr($4,2,6) }' | sort | uniq -c |  awk '{ print $1", "$2" "$3}' 

grep torrentscout *.log -h | awk '{ print $1", "substr($4,2,6) }' | sort | uniq -c |  awk '{ print $1", "$2" "$3}' 


awk '/suchbegriff/ { print $ $4 }' catalina.out | sort
	Nr calls	 from address	 date
	84	 126.119.3.17	 10/Dec
	88	 150.135.175.237	 08/Dec
	3	 198.22.92.11	 07/Dec
	3	 24.229.166.183	 07/Dec
	3	 24.229.242.120	 11/Dec
	1	 38.110.159.162	 07/Dec
	313	 64.58.142.148	 08/Dec
	95	 64.58.142.148	 11/Dec
	1	 66.249.67.184	 11/Dec
total	591		
installs	18.46875		



Nov 30
	34 , 142.157.18.131
     88 , 217.11.34.57
     98 , 61.125.129.65
34 , 132.183.104.138
     34 , 132.183.13.29
      1 , 150.135.175.201
     46 , 150.70.172.106
      8 , 150.70.172.200
     35 , 152.17.132.218
     34 , 24.201.7.30
    133 , 64.58.142.148
     73 , 69.173.108.193
     35 , 80.187.102.244
  33 , 132.183.13.29
     34 , 196.3.51.241
    195 , 217.11.34.57
     66 , 64.58.142.148
      1 , 66.249.67.248
      1 , 75.36.135.198

	 
Sept 25-Oct 2:
	34 , 198.160.190.11
    131 , 198.22.92.11
    419 , 84.72.84.130
     34 , 99.16.67.125

Oct 2-4
  2 , 122.108.254.84
      1 , 12.238.241.98
     62 , 130.102.115.129
     25 , 198.140.178.250
     67 , 217.11.34.57
     77 , 64.58.142.148
    431 , 84.72.84.130
