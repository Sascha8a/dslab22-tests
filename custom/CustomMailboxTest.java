package dslab.custom;

import dslab.mailbox.IMailboxServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dslab.ComponentFactory;
import dslab.Constants;
import dslab.JunitSocketClient;
import dslab.Sockets;
import dslab.TestBase;
import dslab.util.Config;

public class CustomMailboxTest extends TestBase {

    private static final Log LOG = LogFactory.getLog(CustomMailboxTest.class);
    private String componentId = "mailbox-earth-planet";
    private IMailboxServer component;
    private int dmapServerPort;
    private int dmtpServerPort;

    @Before
    public void setUp() throws Exception {
        component = ComponentFactory.createMailboxServer(componentId, in, out);
        dmapServerPort = new Config(componentId).getInt("dmap.tcp.port");
        dmtpServerPort = new Config(componentId).getInt("dmtp.tcp.port");

        new Thread(component).start();

        LOG.info("Waiting for server sockets to appear");
        Sockets.waitForSocket("localhost", dmapServerPort, Constants.COMPONENT_STARTUP_WAIT);
        Sockets.waitForSocket("localhost", dmtpServerPort, Constants.COMPONENT_STARTUP_WAIT);
    }

    @After
    public void tearDown() throws Exception {
        in.addLine("shutdown"); // send "shutdown" command to command line
        Thread.sleep(Constants.COMPONENT_TEARDOWN_WAIT);
    }

    @Test(timeout = 15000)
    public void login_withInValidLogin() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("login INVALID INVALID", "error");
            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void login_restrictedAccess() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");

            client.sendAndVerify("list", "error");
            client.sendAndVerify("show 1", "error");
            client.sendAndVerify("delete 1", "error");
            client.sendAndVerify("logout", "error");

            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void login_twice() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");

            client.sendAndVerify("login trillian 12345", "ok");
            client.sendAndVerify("login trillian 12345", "error");

            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void login_logout() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");

            client.sendAndVerify("login trillian 12345", "ok");
            client.sendAndVerify("logout", "ok");

            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void login_wrongPassword() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");

            client.sendAndVerify("login trillian INVALID", "error");

            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void login_arguments() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");

            client.sendAndVerify("login", "error");
            client.sendAndVerify("login INVALID", "error");
            client.sendAndVerify("login INVALID INVALID", "error");
            client.sendAndVerify("login INVALID INVALID INVALID", "error");

            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void list_arguments() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");

            client.sendAndVerify("login trillian 12345", "ok");
            client.sendAndVerify("list INVALID", "error");

            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void show_arguments() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");

            client.sendAndVerify("login trillian 12345", "ok");
            client.sendAndVerify("show", "error");
            client.sendAndVerify("show INVALID", "error");
            client.sendAndVerify("show INVALID INVALID", "error");

            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void delete_arguments() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");

            client.sendAndVerify("login trillian 12345", "ok");
            client.sendAndVerify("delete", "error");
            client.sendAndVerify("delete INVALID", "error");
            client.sendAndVerify("delete INVALID INVALID", "error");

            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void logout_arguments() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");

            client.sendAndVerify("logout", "error");

            client.sendAndVerify("login trillian 12345", "ok");
            client.sendAndVerify("logout INVALID", "error");
            client.sendAndVerify("logout INVALID INVALID", "error");

            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void quit_arguments() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");

            client.sendAndVerify("quit INVALID", "error");

            client.sendAndVerify("quit", "ok bye");
        }
    }

    @Test(timeout = 15000)
    public void protocolError() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("INVALID", "error");
        }
    }

    @Test(timeout = 15000)
    public void protocolError_noInput() throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");
            client.sendAndVerify("", "error");
        }
    }
}
