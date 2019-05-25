# nifi-ftps
Standalone NAR with FTPS support (while waiting for support in base NIFI)

## Why this NAR?

FTPS support has started [some time ago](https://issues.apache.org/jira/browse/NIFI-2188), but was then moved to [a different idea
](https://issues.apache.org/jira/browse/NIFI-2278).

Since I cannot wait on it : I took the FTP part of the base NIFI and added FTPS support. Since verification is important, also
added some host verification

## Current state
Written with NIFI 1.7.1 (you should be able to get it working on other versions as well by changing the dependencies, changed
classes will not have changed a lot). 

To allow self-signed certificates, you must specify the option in the processor. 
Support for checking trusted certificates implemented against the CA certificates in the JRE keystore (so if you have any self-signed CA certificates,
install them there)

Tested with FileZilla and with [test.rebex.net](https://test.rebex.net/).

**Note** : it forces the FTPS "PROT P" option (encrypted transfer) over TLS (no more SSL) , but it does not support the "TLS session resumption" option
**Note2** : since I have no proxy to test with, the proxy options are disabled
