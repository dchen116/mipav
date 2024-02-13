package org.janelia.mipav.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import gov.nih.mipav.view.MipavUtil;
import gov.nih.mipav.view.Preferences;
import gov.nih.mipav.view.ViewUserInterface;

public class DialogModalTest implements ActionListener, ItemListener {
	
	JFrame myFrame;
	JDialog myDialog1;
    JCheckBox SnowButton;
    JCheckBox RainButton;
    JCheckBox SunshineButton;
	
	
	public DialogModalTest(JFrame myFrame, JDialog myDialog1, JCheckBox SnowButton, JCheckBox RainButton, JCheckBox SunshineButton  ) {
		super();
		this.myFrame = myFrame;
		this.myDialog1 = myDialog1;
		this.RainButton = SnowButton;
		this.RainButton = RainButton;
		this.SunshineButton = SunshineButton;
		
	}


	public static void main(String[] args) {
		// Demonstration of simple JDialog
		/*
		  JOptionPane pane = new JOptionPane("this is some message"); JDialog mydialog
		  = pane.createDialog("this is the title"); //mydialog.show();
		  mydialog.setModalityType(ModalityType.MODELESS); mydialog.setVisible(true);
		  System.out.println("Hello world");
		 */

		// first dialog pops the second dialog(modeless), which sleep for 5 sec to pop
		// the third dialog
/*
		JOptionPane pane1 = new JOptionPane("Press 'OK' to move from modal to modeless dialog");
		JDialog mydialog1 = pane1.createDialog("Dialog #1");
		mydialog1.setModalityType(ModalityType.APPLICATION_MODAL);
		mydialog1.setVisible(true);
		System.out.println("OK");

		JOptionPane pane2 = new JOptionPane("You pressed 'OK'");
		JDialog mydialog2 = pane2.createDialog("Dialog #2");
		mydialog2.setModalityType(ModalityType.MODELESS);

		JOptionPane pane3 = new JOptionPane("It's been 5 seconds");
		JDialog mydialog3 = pane3.createDialog("Dialog #3");
		mydialog3.setModalityType(ModalityType.APPLICATION_MODAL);
		// mydialog3.setVisible(true);

		int delay = 5000; // milliseconds
		Timer timer = new Timer(delay, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent evt) {
				mydialog2.dispose();
				mydialog3.setVisible(true);
			}
		});
		timer.setRepeats(false);
		timer.start();

		mydialog2.setVisible(true);
		System.out.println("OK2");
		*/
		
		// creating a JFrame, adding a button, and then displaying the first dialog you
		// created yesterday when the button is pushed
		// create a Frame with title and exit when close
		JFrame myframe = new JFrame("this is a frame title");
		myframe.setMinimumSize(new Dimension(500, 300));
		
		// Create a menu bar
		JMenuBar menuBar = new JMenuBar();
		
		//Build the first menu.
		JMenu menu = new JMenu("Menu");
		menu.setMnemonic(KeyEvent.VK_A);
		menu.getAccessibleContext().setAccessibleDescription(
		        "The only menu in this program that has menu items");
		menuBar.add(menu);

		//a group of JMenuItems
		JMenuItem menuItem = new JMenuItem("A text-only menu item",
		                         KeyEvent.VK_T);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_1, ActionEvent.ALT_MASK));
		menuItem.getAccessibleContext().setAccessibleDescription(
		        "This doesn't really do anything");
		menu.add(menuItem);
		
		myframe.setJMenuBar(menuBar);
		
		
		
		// create a label
		JLabel Label = new JLabel("this is a label");
		Label.setPreferredSize(new Dimension(50, 100));
		myframe.getContentPane().add(Label, BorderLayout.CENTER);
		Label.setForeground(Color.WHITE);
		Label.setBackground(Color.BLUE);
		Label.setOpaque(true);

		
		
		// create JButton and JPanel
		JButton button = new JButton("open dialog");
		JPanel panel = new JPanel();
		
		// Add button to JPanel
		panel.add(button);
		// And JPanel needs to be added to the JFrame itself!
		myframe.getContentPane().add(panel, BorderLayout.SOUTH);
		System.out.println("ok");
		
		// Display the window.
		// pack() method to size the frame
		myframe.pack();
		myframe.setVisible(true);
		
		// a dialog pop out after the frame
		JOptionPane pane1 = new JOptionPane("Press 'OK'");
		JDialog mydialog1 = pane1.createDialog("Dialog");
		mydialog1.setModalityType(ModalityType.APPLICATION_MODAL);
		
		
		// add checkbox
		JCheckBox 
		
		
		
		// create actionListener
		button.addActionListener(
			new DialogModalTest(myframe, mydialog1)
		);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("you pressed button");
		myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myDialog1.setVisible(true);
	}
		
}

