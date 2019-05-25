_JAVA_OPTIONS=-Djdk.net.URLClassPath.disableClassPathURLCheck=true ~/tools/apache-maven-3.6.0/bin/mvn clean compile package
 sudo cp nifi-ftps-nar/target/nifi-ftps-nar-1.0-SNAPSHOT.nar ~/docker/nifi/data/nifi/lib/
