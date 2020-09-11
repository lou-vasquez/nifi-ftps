# nifi-ftps
Standalone NAR with FTPS support (while waiting for support in base NIFI).
Modified for use at NCICS:
- have not tested latest!
- latest nar version (1.12.0)
- host validation off (we authenticate not them)
- self signed (we authenticate not them)

## TODO:
- verify working (post 1.9)
- variable for host validation
- variable for self sign

## History

- Forked from https://github.com/kullervo16/nifi-ftps
- Updated to 1.9.2
- Updated to 1.12.0

## Why this extension?

FTPS support has started [some time ago](https://issues.apache.org/jira/browse/NIFI-2188), but was then moved to [a different idea
](https://issues.apache.org/jira/browse/NIFI-2278).

Since I cannot wait on it : I took the FTP part of the base NIFI and added FTPS support. Since verification is important, also
added some host verification

## Installation

```
mvn clean install
# docker cp nifi-ftps-nar/target/nifi-standard-nar-1.9.2.nar nifi-container:/opt/nifi/nifi-current/lib
# NOTE be sure to remove existing standard nar (if above copy does not)
# restart docker/nifi
```

Since the code only injects an FTPSClient iso an FTPClient, we use all the logic of the FTP classes. This means that there is a very large
dependency on the standard processors NAR.

However, using the construct of adding a NAR dependency, this lead to IllegalAccessExceptions when loading the FTPS processors.

So I decided to inject the additional JAR into the standard processor NAR. This way, the JAR feels right at home :-)

Installation is therefore building the "patched" standard processor NAR and replacing the original one by this one.

## 1.12.0 State
Testing Underway
(minor mods)

## 1.9.2 State (past tag)
Written with NIFI 1.9.2 (thanks for all the commented out solutions, kullervo16). 

- self signed allowed
- host validation not required

Manually tested (ListFTPS, FetchFTPS) against external data provider ftps

## 1.7 State (pre-fork kullervo16 version)
Tested with FileZilla and with [test.rebex.net](https://test.rebex.net/).

**Note** : it forces the FTPS "PROT P" option (encrypted transfer) over TLS (no more SSL) , but it does not support the "TLS session resumption" option

**Note 2** : since I have no proxy to test with, the proxy options are disabled
