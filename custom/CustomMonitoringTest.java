package dslab.custom;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import dslab.monitoring.IMonitoringServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dslab.ComponentFactory;
import dslab.Constants;
import dslab.TestBase;
import dslab.util.Config;

/**
 * Tests whether the UDP-based monitoring protocol is implemented correctly on the server side.
 */
public class CustomMonitoringTest extends TestBase {
    private static final Log LOG = LogFactory.getLog(CustomMonitoringTest.class);

    private String componentId = "monitoring";

    private IMonitoringServer component;
    private InetSocketAddress addr;

    @Before
    public void setUp() throws Exception {
        component = ComponentFactory.createMonitoringServer(componentId, in, out);
        addr = new InetSocketAddress("127.0.0.1", new Config(componentId).getInt("udp.port"));

        new Thread(component).start();
        Thread.sleep(Constants.COMPONENT_STARTUP_WAIT);
    }

    @After
    public void tearDown() throws Exception {
        in.addLine("shutdown");
        Thread.sleep(Constants.COMPONENT_TEARDOWN_WAIT);
    }

    @Test(timeout = 15000)
    public void server_doesNotCrash() throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            String str = "THIS IS INVALID DATA";
            socket.send(new DatagramPacket(str.getBytes(), str.length(), addr));
        }

        Thread.sleep(2500);

        try (DatagramSocket socket = new DatagramSocket()) {
            String str = "127.0.0.1:42 test@a.com";
            socket.send(new DatagramPacket(str.getBytes(), str.length(), addr));
        }

        Thread.sleep(2500);
        in.addLine("addresses"); // send "addresses" command to command line
        Thread.sleep(2500);
        String output = String.join(",", out.getLines());
        assertThat(output, containsString("test@a.com 1"));
    }
}
