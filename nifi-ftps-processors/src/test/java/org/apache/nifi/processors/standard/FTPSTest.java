package org.apache.nifi.processors.standard;

import org.apache.nifi.processors.standard.util.FTPSTransfer;
import org.apache.nifi.processors.standard.util.FileTransfer;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * This interactive test requires a working FTPS server... you can run it manually, and you will need to change
 * the credentials
 */
public class FTPSTest {


    private TestRunner sot;


    @Test
    public void testBasicList() {
        this.sot = TestRunners.newTestRunner(ListFTPS.class);
        this.sot.setProperty(FileTransfer.HOSTNAME,"192.168.1.50");
        this.sot.setProperty(FileTransfer.USERNAME,"jef");
        this.sot.setProperty(FileTransfer.PASSWORD,"ikke");
        this.sot.setProperty(FTPSTransfer.ALLOW_SELFSIGNED,"true");

        this.sot.run();
        assertEquals(18,this.sot.getFlowFilesForRelationship(ListFTPS.REL_SUCCESS).size());
    }

    @Test
    public void testBasicGet() {
        this.sot = TestRunners.newTestRunner(GetFTPS.class);
        this.sot.setProperty(FileTransfer.HOSTNAME,"192.168.1.50");
        this.sot.setProperty(FileTransfer.USERNAME,"jef");
        this.sot.setProperty(FileTransfer.PASSWORD,"ikke");
        this.sot.setProperty(FTPSTransfer.ALLOW_SELFSIGNED,"true");
        this.sot.setProperty(FileTransfer.REMOTE_PATH,"transfer");
        this.sot.setProperty(FileTransfer.DELETE_ORIGINAL, "false");

        this.sot.run();
        assertEquals(2,this.sot.getFlowFilesForRelationship(GetFTPS.REL_SUCCESS).size());
    }
}
