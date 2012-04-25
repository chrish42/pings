import java.applet.*;
import java.awt.*;

public class PingsApplet extends Applet {

    private final String SERVER_HOSTNAME = "localhost";
    private final int SERVER_PORT = 6543;

    private PingsClient m_pings_client;

    public void init() {
        // Initialization (on page load).
        m_pings_client = new PingsClient(SERVER_HOSTNAME, SERVER_PORT);
        m_pings_client.start();
    }

    public void destroy() {
        // Don't leave the Pings client thread running!
        m_pings_client.halt();
    }

    public void paint(Graphics g) {
        // Draw applet here.
        g.setColor(Color.gray);
    }
}
