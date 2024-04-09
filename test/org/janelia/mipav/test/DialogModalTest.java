package org.janelia.mipav.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
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

public class DialogModalTest extends JComponent implements ActionListener, ItemListener {
	
	JFrame myFrame;
	JDialog myDialog1;
    JCheckBox SnowButton;
    JCheckBox RainButton;
    JCheckBox SunshineButton;
    static StringBuffer choices;
	
	
	public DialogModalTest(JFrame myFrame, JDialog myDialog1) {
		super();
		this.myFrame = myFrame;
		this.myDialog1 = myDialog1;
		this.SnowButton = SnowButton;
		this.RainButton = RainButton;
		this.SunshineButton = SunshineButton;
		
		SnowButton.addActionListener(this);
		SnowButton.addItemListener(this);
		
		CheckBox();
	}


	public static void main(String[] args) {
		// creating a JFrame, adding a button, and then displaying the first dialog I
		// created yesterday when the button is pushed
		// create a Frame with title and exit when close
		JFrame myframe = new JFrame("this is the title of the frame");
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
		

		DialogModalTest a = new DialogModalTest(myframe, mydialog1);
		//DialogModalTest b = new DialogModalTest(myframe, mydialog1);

		// create actionListener
		button.addActionListener(a);
		//checkbox.addItemListener(a);
		
		createAndShowGUI();
	}
	
	public void CheckBox() {
		JFrame myframe = new JFrame("this is the title of the frame");
		myframe.setMinimumSize(new Dimension(500, 300));

		// create CheckBox
		JCheckBox SnowButton = new JCheckBox("Snow");
		SnowButton.setMnemonic(KeyEvent.VK_C);
		SnowButton.setSelected(true);

		JCheckBox RainButton = new JCheckBox("Rain");
		RainButton.setMnemonic(KeyEvent.VK_C);
		RainButton.setSelected(true);

		JCheckBox SunshineButton = new JCheckBox("Sunshine");
		SunshineButton.setMnemonic(KeyEvent.VK_C);
		SunshineButton.setSelected(true);

		// Register a listener for the check boxes.
		SnowButton.addItemListener(this);
		RainButton.addItemListener(this);
		SunshineButton.addItemListener(this);

		// Indicates what's on the geek.
		choices = new StringBuffer("sru");

		// Put the check boxes in a column in a panel
		JPanel checkPanel = new JPanel(new GridLayout(0, 1));
		checkPanel.add(SnowButton);
		checkPanel.add(RainButton);
		checkPanel.add(SunshineButton);

		// Create and set up the content pane.
		JComponent newContentPane = new JCheckBox();
		newContentPane.setOpaque(true); // content panes must be opaque
		myframe.setContentPane(newContentPane);

		// Display the window.
		myframe.pack();
		myframe.setVisible(true);

	}



	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("you pressed button");
		myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myDialog1.setVisible(true);
	}
	
    
	 /** Listens to the check boxes. */
    public void itemStateChanged(ItemEvent e) {
        int index = 0;
        char c = '-';
        Object source = e.getItemSelectable();

        if (source == SnowButton) {
            index = 0;
            c = 's';
        } else if (source == RainButton) {
            index = 1;
            c = 'r';
        } else if (source == SunshineButton) {
            index = 2;
            c = 'u';
        }
        
      //Now that we know which button was pushed, find out
        //whether it was selected or deselected.
        if (e.getStateChange() == ItemEvent.DESELECTED) {
            c = '-';
        }

        //Apply the change to the string.
        choices.setCharAt(index, c);
    }
    
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    
    private static void createAndShowGUI() {
        //Create and set up the window.
		JFrame myframe = new JFrame("WeatherCheckBox");
		myframe.setMinimumSize(new Dimension(500, 300));
		myframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		  //Create and set up the content pane.
        JComponent newContentPane = new JCheckBox();
        newContentPane.setOpaque(true); //content panes must be opaque
        myframe.setContentPane(newContentPane);

		// Display the window.
		myframe.pack();
		myframe.setVisible(true);
    }
    
}

