import java.net.*;
import java.text.*;

/**
   This class does TCP-based pinging of a remote destination using a
   specific port

   @author   Steven Pigeon <pigeon@iro.umontreal.ca>
*/
public class TcpPinger {
    /** Holds the last collected times */
    private String m_tcp_times;

    /** A reference to ClientInfo */
    private ClientInfo m_info;

    /**
       Returns the last collected times as a String.

       <p>The output is structured as follows

       <p>protocol addr:port nb_sent nb_received timeout [times]+

       <p>for example:

       <p><tt>TCP 132.204.24.179:50 10 0 1000 !6.713ms !4.896ms !3.770ms !4.588ms !8.609ms * !21.504ms !3.359ms !8.367ms !3.439ms</tt>

       @return The last collected times
    */
    public String getLastPings() { return m_tcp_times; }

    /**
       Clears the last collected times
    */
    public void clearPings() { m_tcp_times = ""; }

    /**
       Pings an external IP address using a specific port. A string
       containing the summary and times gathered is constructed, and
       accessible through TCP_Ping.getLastPings() after having called
       ping(). If an error occured, getLastPings() is undefined (may contain
       previous call's values).

       <p>The output is structured as follows

       <p>protocol addr:port nb_sent nb_received timeout [times]+

       <p>and times may be prefixed by ! if the connection is refused or
       fails rapidly, * (without time, just * alone) if it timed out, and
       prefixed by ? if some other error occured

       @see   TcpPinger#getLastPings()
       @see   TcpPinger#clearPings()
       @param addr The address to ping
       @param port The port to ping

       @return 0 (for compatibility with other pinger-classes that return the exit code)
    */
    public int ping(InetAddress addr,int port) {
        DecimalFormat format = new DecimalFormat("0.000");
        InetSocketAddress sock_addr = new InetSocketAddress(addr,port);
        String times = "";
        int fails = 0;
        
        for (int p = 0; p < m_info.getNumberOfPings(); p++) {
            Socket ping_socket = new Socket();
            String prefix = " ";

            if (p != 0) {
                try {
                    // Sleep half a second
                    Thread.sleep(500);
                }
                catch (Exception e) { /* Slept less? */ }
            }

            boolean timed_out = false;
            long start = System.nanoTime();
            
            try {
                ping_socket.connect(sock_addr, m_info.getTCPTimeOut());
            }
            catch (ConnectException e) {
                fails++;
                prefix = " !";
            }
            catch (SocketTimeoutException e) {
                fails++;
                prefix = " *";
                timed_out = true;
            }
            catch (Exception e) {
                fails++;
                prefix = " ?";
            }

            long stop = System.nanoTime();

            // if the connection was refused/quick error'd : it has ! as a prefix
            // if the connection timed out, it is shown as * (without time)
            // if some other error occured, it is prefixed with ?
            //
            times += prefix + (timed_out ? "" : format.format((stop-start) / 1.0e6f) + "ms" );
        }

        // The string returned is structured as follows:
        // protocol addr:port sent received timeoutvalue [ times ]+
        //
        m_tcp_times = "TCP " + addr.toString().split("/")[1] + ":" + port +
            " " + m_info.getNumberOfPings() + " " + (m_info.getNumberOfPings()-fails) +
            " " + m_info.getTCPTimeOut() + times;

        return 0;
    }

    /**
       Creates a TcpPinger (linked to a ClientInfo configuration)
       @param this_info A reference to a ClientInfo
       @see ClientInfo
    */
    public TcpPinger(ClientInfo this_info ) {
        m_tcp_times = "";
        m_info = this_info;
    }
}
