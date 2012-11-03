## java home on SSDL-APP
export JAVA_HOME=/usr/lib/jvm/java-6-sun-1.6.0.26/jre
#./runinstance.sh hibernate org.hibernate.* 120000
#./runinstance.sh spring org.springframework.* 120000
#./runinstance.sh flickr com.aetrion.flickr 120000

./runinstance.sh jdo2 javax.jdo.* 100

#./runinstance.sh jdo2 javax.jdo.* 12000
#./runinstance.sh jogamp com.jogamp.* 12000
#./runinstance.sh jai com.sun.media.jai 12000

#runinstance javasql java.sql.* 10000 
