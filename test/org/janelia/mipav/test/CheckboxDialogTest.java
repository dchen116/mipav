package org.janelia.mipav.test;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.sun.media.controls.VFlowLayout;

public class CheckboxDialogTest implements ActionListener, ItemListener {

	public static void main(String[] args) {
		CheckboxDialogTest cdt = new CheckboxDialogTest();
		// nothing else here
	}

	// add variables here
	JFrame frame;
	JButton button;
	JPanel panelButton;
	JOptionPane panelButtonDialog;
	JDialog dialogAfterButton;
	JCheckBox checkbox1;
	JCheckBox checkbox2;
	LayoutManager layout;
	StringBuffer choices;

	
	public CheckboxDialogTest() {
		//write code here
		this.frame = new JFrame("Hello World");
		this.layout = new FlowLayout();
		
		createGUI();
	}
	
	private void createGUI() {
		// create button
		button = new JButton("Hi!");
		//add button to frame
		frame.add(button);
		//add button to actionListener
		button.addActionListener(this);
		
		// create JPanel for the button
		panelButton = new JPanel();
		panelButton.add(button); // Add button to JPanel
		// And JPanel needs to be added to the JFrame itself!
		frame.getContentPane().add(panelButton);
		
		// a dialog pop out after the frame
		panelButtonDialog = new JOptionPane("You clicked 'Hi!'");
		dialogAfterButton = panelButtonDialog.createDialog("Dialog");
		dialogAfterButton.setModalityType(ModalityType.APPLICATION_MODAL);


		// Create checkbox + set event + add to ItemListener for more than one items
		checkbox1 = new JCheckBox("Item1");
		checkbox1.setMnemonic(KeyEvent.VK_C);
		checkbox1.setSelected(false);
		frame.add(checkbox1);
		checkbox1.addItemListener(this);

		checkbox2 = new JCheckBox("Item2");
		checkbox2.setMnemonic(KeyEvent.VK_C);
		checkbox2.setSelected(true);
		frame.add(checkbox2);
		checkbox2.addItemListener(this);

		// Indicates what's on the geek.
		choices = new StringBuffer("12");
		
		JPanel panel = new JPanel(new GridBagLayout());
		
		JTextField field = new JTextField(20);
		panel.add(field);
		
		JTextArea area = new JTextArea(10, 20);
		panel.add(area);
		
		JRadioButton radioButton;
		
		frame.add(panel);


		// Display the window.
		frame.setLayout(layout);
		frame.pack();
		frame.setVisible(true);	

	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		//System.out.println("Item state changed");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		int index = 0;
		char c = '-';
		Object source = e.getItemSelectable();

/*
		if (source == checkbox1) {
			index = 0;
			c = '1';
			System.out.println("Item 1");
			JOptionPane.showMessageDialog(frame, "Item 1 is selected.");
		} else if (source == checkbox2){
			index = 1;
			c = '2';			
			System.out.println("Item 2");
			JOptionPane.showMessageDialog(frame, "Item 2 is selected");
		}

		// Now that we know which checkbox was pushed, find out
		// whether it was selected or deselected.
		if (e.getStateChange() == ItemEvent.DESELECTED) {
			c = '-';
			System.out.println("Item deselected");
		}
		*/
		
		if (source == checkbox1) {
			index = 0;
		} else if (source == checkbox2){
			index = 1;
		}
		
		
		if (e.getStateChange() == ItemEvent.SELECTED) {
			if (source == checkbox1) {
				c = '1';
				System.out.println("Item 1");
			} else if (source == checkbox2){
				c = '2';			
				System.out.println("Item 2");
			}
		} else if (e.getStateChange() == ItemEvent.DESELECTED) {
			c = '-';
			System.out.println("Item deselected");	
		}
		

		// Apply the change to the string.
		choices.setCharAt(index, c);
		panelButtonDialog.setMessage("You pressed 'Hi!'. Selected checkboxes: " + choices.toString());
		dialogAfterButton = panelButtonDialog.createDialog("Dialog");
		dialogAfterButton.setModalityType(ModalityType.APPLICATION_MODAL);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("Action performed");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		dialogAfterButton.setVisible(true);
		System.out.println(choices.toString());
		
		boolean checkbox1_selected = checkbox1.getSelectedObjects() != null;
		boolean checkbox2_selected = checkbox2.getSelectedObjects() != null;
		System.out.println("Checkbox 1: " + checkbox1_selected);
		System.out.println("Checkbox 2: " + checkbox2_selected);

		
		
		
		
	}
}

