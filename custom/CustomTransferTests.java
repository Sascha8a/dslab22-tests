package dslab.custom;

import dslab.*;
import dslab.mailbox.IMailboxServer;
import dslab.transfer.ITransferServer;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CustomTransferTests extends TestBase {
    private static final Log LOG = LogFactory.getLog(CustomTransferTests.class);
    private int dmtpServerPort;
    private final TestInputStream trasnferIn = new TestInputStream();
    private final TestInputStream mailboxIn  = new TestInputStream();

    @Before
    public void setUp() throws Exception {
        String mailboxComponentId = "mailbox-earth-planet";
        IMailboxServer mailboxServer = ComponentFactory.createMailboxServer(mailboxComponentId, mailboxIn, out);
        int dmapServerPort = new Config(mailboxComponentId).getInt("dmap.tcp.port");
        int mailboxDmtpServerPort = new Config(mailboxComponentId).getInt("dmtp.tcp.port");

        String transferComponentId = "transfer-1";
        ITransferServer transferServer = ComponentFactory.createTransferServer(transferComponentId, trasnferIn, out);
        this.dmtpServerPort = new Config(transferComponentId).getInt("tcp.port");

        new Thread(mailboxServer).start();
        new Thread(transferServer).start();

        LOG.info("Waiting for sockets to appear");
        Sockets.waitForSocket("localhost", dmapServerPort, Constants.COMPONENT_STARTUP_WAIT);
        Sockets.waitForSocket("localhost", this.dmtpServerPort, Constants.COMPONENT_STARTUP_WAIT);
        Sockets.waitForSocket("localhost", mailboxDmtpServerPort, Constants.COMPONENT_STARTUP_WAIT);
    }

    @After
    public void tearDown() throws Exception {
        trasnferIn.addLine("shutdown"); // send "shutdown" command to command line
        mailboxIn.addLine("shutdown"); // send "shutdown" command to command line
        Thread.sleep(Constants.COMPONENT_TEARDOWN_WAIT);
    }

    @Test(timeout = 15000)
    public void errorOnMissingBegin() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(this.dmtpServerPort, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("from trillian@earth.planet", "error");
            client.sendAndVerify("subject hello", "error");
            client.sendAndVerify("data hello from junit", "error");
            client.sendAndVerify("send", "error");
            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void errorOnMissingSender() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(this.dmtpServerPort, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("subject hello", "ok");
            client.sendAndVerify("data hello from junit", "ok");
            client.sendAndVerify("send", "error");
            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void errorOnMissingSubject() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(this.dmtpServerPort, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from trillian@earth.planet", "ok");
            client.sendAndVerify("to trillian@earth.planet", "ok");
            client.sendAndVerify("data hello from junit", "ok");
            client.sendAndVerify("send", "error");
            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void errorOnMissingData() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(this.dmtpServerPort, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from trillian@earth.planet", "ok");
            client.sendAndVerify("to trillian@earth.planet", "ok");
            client.sendAndVerify("subject hello", "ok");
            client.sendAndVerify("send", "error");
            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void errorOnInvalidSender() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(this.dmtpServerPort, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from ASD", "error");
            client.sendAndVerify("to trillian@earth.planet", "ok");
            client.sendAndVerify("subject hello", "ok");
            client.sendAndVerify("data hello from junit", "ok");
            client.sendAndVerify("send", "error");
            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void errorOnInvalidReceiver() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(this.dmtpServerPort, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from trillian@earth.planet", "ok");
            client.sendAndVerify("to INVALID", "error");
            client.sendAndVerify("subject hello", "ok");
            client.sendAndVerify("data hello from junit", "ok");
            client.sendAndVerify("send", "error");
            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void errorOnInvalidReceivers() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(this.dmtpServerPort, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from trillian@earth.planet", "ok");
            client.sendAndVerify("to trillian@earth.planet,INVALID", "error");
            client.sendAndVerify("subject hello", "ok");
            client.sendAndVerify("data hello from junit", "ok");
            client.sendAndVerify("send", "error");
            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void errorOnTooManyArguments() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(this.dmtpServerPort, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin INVALID", "error");
            client.sendAndVerify("begin", "ok");

            client.sendAndVerify("from", "error");
            client.sendAndVerify("from trillian@earth.planet INVALID", "error");
            client.sendAndVerify("from trillian@earth.planet", "ok");

            client.sendAndVerify("to", "error");
            client.sendAndVerify("to trillian@earth.planet INVALID", "error");
            client.sendAndVerify("to trillian@earth.planet,", "error");
            client.sendAndVerify("to ,", "error");
            client.sendAndVerify("to ,,,,,", "error");
            client.sendAndVerify("to trillian@earth.planet", "ok");

            client.sendAndVerify("subject", "error");
            client.sendAndVerify("subject hello", "ok");

            client.sendAndVerify("data", "error");
            client.sendAndVerify("data hello from junit", "ok");

            client.sendAndVerify("send INVALID", "error");
            client.sendAndVerify("send", "ok");

            client.sendAndVerify("quit INVALID", "error");
            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void errorOnDouble() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(this.dmtpServerPort, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("begin", "error");

            client.sendAndVerify("from trillian@earth.planet", "ok");
            client.sendAndVerify("from trillian@earth.planet", "error");

            client.sendAndVerify("to trillian@earth.planet", "ok");
            client.sendAndVerify("to trillian@earth.planet", "error");

            client.sendAndVerify("subject hello", "ok");
            client.sendAndVerify("subject hello", "error");

            client.sendAndVerify("data hello from junit", "ok");
            client.sendAndVerify("data hello from junit", "error");

            client.sendAndVerify("send", "ok");

            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void cannotSendTwice() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(this.dmtpServerPort, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("begin", "ok");
            client.sendAndVerify("from trillian@earth.planet", "ok");
            client.sendAndVerify("to trillian@earth.planet", "ok");
            client.sendAndVerify("subject hello", "ok");
            client.sendAndVerify("data hello from junit", "ok");
            client.sendAndVerify("send", "ok");
            client.sendAndVerify("send", "error");

            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void protocolError() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(this.dmtpServerPort, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("INVALID", "error");
        }

        try (JunitSocketClient client = new JunitSocketClient(this.dmtpServerPort, err)) {
            client.verify("ok DMTP");
            client.sendAndVerify("", "error");
        }
    }
}
