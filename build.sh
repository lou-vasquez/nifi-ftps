#!/usr/bin/env bash
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-oracle.x86_64
_JAVA_OPTIONS=-Djdk.net.URLClassPath.disableClassPathURLCheck=true ~/tools/apache-maven-3.6.0/bin/mvn clean compile package $1
 sudo cp nifi-ftps-nar/target/nifi-*.nar ~/docker/nifi/data/nifi/lib/
