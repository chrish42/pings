import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

//TODO : change the System.out.println with appropriate log event

/**
 * This class holds the GUI part of the applet. It creates the GUI objects :
 * button, fields, label and the ping globe and is responsible for their drawing
 * 
 * <p>
 * @author RaphaelBonaque
*/
public class PingsGUI implements ActionListener {
	
	private PingsApplet applet;
	
	//GUI related variables, they holds the GUI components
	private JButton pause_button, rename_button ;
	private JLabel pings_counter_display, client_info_display;
	private JTextField nickname_field;
	private PingsGlobe ping_globe;
	private Container button_container;
	
	//State variables
	private int pings_counter = 0;
	private GeoipInfo client_geoip_info = null;
	
	/**
	* Create and initialize the components of the GUI.
	* <p>
	* That is :the globe , one pause/resume button and a field to change the 
	* client's nickname.
	* The globe is turnable, zoomable and show an effect for pings
	* The buttons comes with tooltips and shortcuts.
	*/
	PingsGUI (PingsApplet parent) {
		applet = parent;
		
		//Recover the content and background of the applet to add components
		button_container = applet.getContentPane();
		button_container.setBackground (Color.BLACK);
		
		//Add the pause/resume button to the applet
		pause_button = new JButton ("Pause");				
		pause_button.setMnemonic(KeyEvent.VK_P);
		pause_button.setToolTipText("Pause or resume the pings");
		pause_button.setActionCommand("pause");
		pause_button.addActionListener(this);
		button_container.add (pause_button);
		
		//Add the button to change the nickname
		rename_button = new JButton ("Ok");
		rename_button.setMnemonic(KeyEvent.VK_U);
		rename_button.setToolTipText("Update your name for the leaderboard");
		rename_button.setActionCommand("rename");
		rename_button.addActionListener(this);
		rename_button.setEnabled(false);
		
		//Add the field to change the nickname
		nickname_field = new JTextField(15);
		nickname_field.setText(applet.pings_clients[0].getNickname());
		nickname_field.getDocument().addDocumentListener(
			new DocumentListener() {
				@Override
				public void changedUpdate(DocumentEvent e) {
					check_enable_button();
				}
				@Override
				public void insertUpdate(DocumentEvent e) {
					check_enable_button();
				}
				@Override
				public void removeUpdate(DocumentEvent e) {
					check_enable_button();
				}
			});
		button_container.add (nickname_field);		
		
		button_container.add (rename_button);
		
		//Add the display for the number of pings done
		pings_counter_display = new JLabel("No pings sent yet");
		button_container.add (pings_counter_display);
		
		//Add the display for the client info
		client_info_display = new JLabel("");
		button_container.add (client_info_display);
		
		//Add the globe
		ping_globe = new PingsGlobe();
		applet.getContentPane().add(ping_globe);
		
		//Set the layout
		setLayout();
		
		//Add an observer to the client to update the GUI.
		for (int i = 0; i < applet.nb_client_threads; i++) {
			applet.pings_clients[i].addObserver(new clientThreadObserver());
		}
	}
	
	/**
	 * Position the component of the GUI.
	 * 
	 * Currently use absolute positioning to gather most of the component in the
	 * top-left corner.
	 */
	private void setLayout() {
		//Set the globe to use the full space available
		ping_globe.setBounds(0, 0, applet.getWidth(), applet.getHeight());
		
		//First raw of display
		Dimension name_size = nickname_field.getPreferredSize();
		Dimension update_name_size =  rename_button.getPreferredSize();
		Dimension client_info_size = client_info_display.getPreferredSize();
		
		int row_height = name_size.height;
		
		nickname_field.setBounds(5,5,
				name_size.width,row_height);
		
		rename_button.setBounds(8 + name_size.width, 5,
				update_name_size.width, row_height);
		
		client_info_display.setBounds(11 + name_size.width + update_name_size.width, 5,
				client_info_size.width,row_height );
		
		//Second raw of display
		Dimension counter_size = pings_counter_display.getPreferredSize();
		Dimension pause_size = pause_button.getMinimumSize();
		
		pings_counter_display.setBounds(5, 8 + row_height,
				counter_size.width, row_height);
		
		pause_button.setBounds(8 + counter_size.width,8 + row_height,
				pause_size.width, row_height);
	}
	
	/**
	 * Update the counter displaying the number of ping sent
	 */	
	private void updatePingsCounterDisplay() {
		if (pings_counter == 0) {
			pings_counter_display.setText("No pings sent yet");
		}
		else {
			pings_counter_display.setText(pings_counter + " pings sent");
		}
		setLayout();
	}
	
	private void updateClientInfoDisplay(String ip_adress, GeoipInfo client_info) {
		String info_str = ": " + ip_adress + " ";
		if (client_info.city != null && !client_info.city.equals("")) info_str += client_info.city + ", ";
		info_str += client_info.country;
		client_info_display.setText(info_str);
		ping_globe.setOrigin(client_info);
		setLayout();
		applet.repaint();
	}
	
	public void check_enable_button() {
		if (nickname_field.getText().equals(applet.pings_clients[0].getNickname())) {
			rename_button.setEnabled(false);
		}
		else {
			rename_button.setEnabled(true);
		}
	}
	

	
	class clientThreadObserver implements Observer {
		
		private PingsGlobe.PingGUI gui_effect = null;
		private InetAddress last_address = null ;
		
		public clientThreadObserver() {
			super();
		}
		
		@Override
		public void update(Observable o, Object arg) {
			PingsClient client = (PingsClient)o;
			
			if (!client.getSourceGeoip().equals(client_geoip_info)) {
				client_geoip_info = client.getSourceGeoip();
				//FIXME: get ip from server
				SwingUtilities.invokeLater( new Runnable() {
					public void run() {
						updateClientInfoDisplay("", client_geoip_info);
					}
				});
				return;
			}
			
			GeoipInfo current_ping_geoip = client.getCurrentDestGeoip();
			InetAddress current_ping_adress = client.getCurrentPingDest();
			
			//If there are several PingsClient threads then this might still be 
			// a 
			if (current_ping_adress == null) return;
			
			//If there is a new ping add it to the counter and register an 
			//effect for the globe
			if (gui_effect == null && last_address != current_ping_adress) {
				last_address = current_ping_adress;
				
				pings_counter++;
				SwingUtilities.invokeLater( new Runnable() {
					public void run() {
						updatePingsCounterDisplay();
					}
				});
				
				gui_effect = ping_globe.addPing(current_ping_geoip);
			}
			//Else if it's the last ping update it
			else if (gui_effect != null && last_address == current_ping_adress) {
				gui_effect.updatePingGUIValue(client.getCurrentPingResult());
				gui_effect = null;
			}
			//Else there are two case :
			//_ either the destination address is the same (and with the 
			//current implementation there is no way to know it). This case has a
			//very low probability.
			//_ or we somehow missed the result of the previous ping, hence 
			//we need to do some workaround for the old ping and declare a 
			//new one.
			else {
				if (gui_effect != null) gui_effect.unknownError();
				String value = client.getCurrentPingResult();
				gui_effect = ping_globe.addPing(current_ping_geoip);
				if (!value.equals("")) {
					gui_effect.updatePingGUIValue(client.getCurrentPingResult());
					gui_effect = null;
				}
			}
		}
	}
	
	/**
	 * This method is used to refresh the Pause/Resume button to make it show
	 * 'Pause' or 'Resume' and act accordingly depending on the client state 
	 * expressed by the 'm_is_running' variable.
	 * <p>
	 * If the client is running this makes the button show "Pause" otherwise it 
	 * shows "Resume".
	 */
	private void refreshPauseButton() {
			if (!applet.pings_clients[0].isRunning()) {
				pause_button.setText("Resume");
				pause_button.setActionCommand("resume");
			}
			else {
				pause_button.setText("Pause");
				pause_button.setActionCommand("pause");
			}
			setLayout();
	}
	
	/**
	 * The listener for events in the applet : the interactions with the 
	 * components (other than the globe) are handled here.
	 */
	public void actionPerformed (ActionEvent event) {
		
		//Find the issued command and store it in the 'command' variable
		String command = event.getActionCommand();
		
		//Handle the pause/resume button
		if (command.equals("pause")) {
			//Pause the client if it was running, do nothing otherwise
			
			boolean was_running = applet.pings_clients[0].isRunning();
			//Issue a warning if the client was not running
			if (!was_running) {				
				//System.out.println ("Was already paused.");
			}
			else {
				applet.stop();
			}
			//Then refresh the button
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					refreshPauseButton();
				}
			});
				
		}
		else if (command.equals("resume")) {
			//resume the client if it was paused, do nothing otherwise

			boolean was_running = applet.pings_clients[0].isRunning();
			//Issue a warning if the client was running
			if (was_running) {
				//System.out.println ("Was already running.");
			}
			else {
				applet.start();
			}
			//Then refresh the button
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					refreshPauseButton();
				}
			});
		}
		
		//Handle the rename button
		else if (command.equals("rename")) {
			String new_name = nickname_field.getText();
			for (int i = 0; i < applet.nb_client_threads; i++) {
				applet.pings_clients[i].setNickname(new_name);
			}
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
					rename_button.setEnabled(false);
				}
			});
			System.out.println("Nick updated to " + new_name);
		}
		
		//Handle any other unknown component
		else {
			//FIXME
			System.out.println ("Error in button interface.");
		}
	}
}
