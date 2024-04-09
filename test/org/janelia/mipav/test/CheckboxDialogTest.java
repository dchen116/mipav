package org.janelia.mipav.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import com.sun.media.controls.VFlowLayout;

public class CheckboxDialogTest implements ActionListener, ItemListener {

	public static void main(String[] args) {
		CheckboxDialogTest cdt = new CheckboxDialogTest();
		// nothing else here
	}

	// add variables here
	JFrame frame;
	JButton button;
	JPanel panelButton, checkPanel, radioPanel;
	JOptionPane panelButtonDialog;
	JDialog dialogAfterButton;
	JCheckBox checkbox1, checkbox2;
	LayoutManager layout;
	StringBuffer choices;
	JRadioButton radioButtonA, radioButtonB;
	String newline = "\n";
	//JTextField textField;
	JTextArea textArea;
	JComponent jboard;

	public CheckboxDialogTest() {
		// write code here
		this.frame = new JFrame("Hello World");
		this.layout = new FlowLayout();

		createGUI();
	}

	private void createGUI() {
		frame.setLayout(new GridBagLayout());

		//Create a jboard to give a title
		jboard = new JPanel();
		jboard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        "Here are the error messages:"),
                BorderFactory.createEmptyBorder(80, 80, 80, 80)));
		jboard.setLayout(new BorderLayout());
		
		// create button
		button = new JButton("Hi!");
		// add button to frame
		//frame.add(button);
		// add button to actionListener
		button.addActionListener(this);

		// create JPanel for the button
		panelButton = new JPanel();
		panelButton.add(button); // Add button to JPanel
		// And JPanel needs to be added to the JFrame itself!
		//frame.getContentPane().add(panelButton, BorderLayout.PAGE_END);

		// a dialog pop out after the frame
		panelButtonDialog = new JOptionPane("You clicked 'Hi!'");
		dialogAfterButton = panelButtonDialog.createDialog("Dialog");
		dialogAfterButton.setModalityType(ModalityType.APPLICATION_MODAL);

		// Create checkbox + set event + add to ItemListener for more than one items
		checkbox1 = new JCheckBox("Item 1: Save to file         ");
		checkbox1.setMnemonic(KeyEvent.VK_C);
		checkbox1.setSelected(false);
		//frame.add(checkbox1);
		checkbox1.addItemListener(this);

		checkbox2 = new JCheckBox("Item 2: Copy and Paste");
		checkbox2.setMnemonic(KeyEvent.VK_C);
		checkbox2.setSelected(false);
		checkbox2.setOpaque(false);
		//frame.add(checkbox2);
		checkbox2.addItemListener(this);

		// Indicates what's on the geek.
		choices = new StringBuffer("12");

		// Put the check boxes in a column in a panel
		checkPanel = new JPanel(new GridLayout(0, 1));
		checkPanel.add(checkbox1);
		checkPanel.add(checkbox2);
		checkPanel.setBackground(Color.BLUE);
		checkPanel.setOpaque(true);
		//frame.add(checkPanel, BorderLayout.PAGE_END);
		frame.getContentPane().setBackground(Color.RED);
/*
		// Create the radio buttons.
		radioButtonA = new JRadioButton("Item A");
		radioButtonA.setMnemonic(KeyEvent.VK_B);
		radioButtonA.setActionCommand("Item A");
		radioButtonA.setSelected(true);

		radioButtonB = new JRadioButton("Item B");
		radioButtonB.setMnemonic(KeyEvent.VK_B);
		radioButtonB.setActionCommand("Item B");

		// Group the radio buttons.
		ButtonGroup group = new ButtonGroup();
		group.add(radioButtonA);
		group.add(radioButtonB);

		// Register a listener for the radio buttons.
		radioButtonA.addActionListener(this);
		radioButtonB.addActionListener(this);

		// create JPanel for the radio button
		radioPanel = new JPanel(new GridLayout(0, 1));
		radioPanel.add(radioButtonA); // Add button to JPanel
		radioPanel.add(radioButtonB);
		// And JPanel needs to be added to the JFrame itself!
		frame.getContentPane().add(radioPanel);
		frame.add(radioPanel, BorderLayout.LINE_START);
*/
		// Create TextFiled
//		JPanel panel = new JPanel(new GridBagLayout());
//		panel.setBackground(Color.YELLOW);
//		panel.setOpaque(true);

//		textField = new JTextField(20);
//		textField.addActionListener(this);
//		panel.add(textField);
//		frame.add(textField);

		textArea = new JTextArea(20, 70);
		textArea.setEditable(true);
//		panel.add(textArea);
//		frame.add(panel);
		
		JScrollPane scrollPane = new JScrollPane(textArea);
		jboard.add(scrollPane, BorderLayout.CENTER);//add textArea in the jboard	
		
		// Add Components to this panel.
		GridBagConstraints c = new GridBagConstraints();
		
//		c.gridwidth = GridBagConstraints.REMAINDER;
//		c.gridx = 0;
//		 c.gridy = 0;
//		 c.gridheight = 1;
	//	c.fill = GridBagConstraints.HORIZONTAL;
//		panel.add(textField, c);
//		frame.add(panel, BorderLayout.PAGE_START);
		
		//TEXT AREA
//		c.fill = GridBagConstraints.CENTER;
//		 c.gridx = 1;
//		 c.gridy = 1;
//		c.weightx = 0.5;
//		c.weighty = 0.5;
//		panel.add(scrollPane, c);
		
		// Set GridLayout for textArea
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
//  	frame.add(scrollPane, c);//add textArea in the jboard	
			
		frame.add(jboard, c);//add the jboard to the frame
		
		// Move checkboxes and "OK!" button under JTextArea
//		JPanel bottomPanel = new JPanel(new GridBagLayout());
//		bottomPanel.setBackground(Color.CYAN);
//		bottomPanel.setOpaque(true);
	
		// Set GridLayout for checkboxes
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		frame.add(checkbox1, c); // Add checkPanel to bottomPanel
		
		c.gridy = 2;
		frame.add(checkbox2, c);
		
		// Anchor the "OK" button to the right corner(EAST) of the frame
		//c.gridy = 1;
		//c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.VERTICAL;
		c.gridx = 1;
		c.gridy = 1;
		c.gridheight = 2;//see the height of the button the same as the height of checkbox1 + checkbox2
		c.weightx = 1;
		frame.add(button, c); // Add panelButton to bottomPanel

		//frame.add(bottomPanel, BorderLayout.SOUTH); // Add bottomPanel to frame's PAGE_END
		//frame.add(bottomPanel, BorderLayout.SOUTH);
	
		// Display the window.
		//frame.setLayout(layout);
		frame.setBounds(80, 80, 80, 80);
		frame.pack();
		frame.setVisible(true);

	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		// System.out.println("Item state changed");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		int index = 0;
		char c = '-';
		Object source = e.getItemSelectable();

		// push checkbox with selected or deselected
		if (source == checkbox1) {
			index = 0;
		} else if (source == checkbox2) {
			index = 1;
		}

		if (e.getStateChange() == ItemEvent.SELECTED) {
			if (source == checkbox1) {
				c = '1';
				System.out.println("Item 1");
			} else if (source == checkbox2) {
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

		// For TextFiled and TextArea
//		String text = textField.getText();
//		textArea.append(text + newline);
//		textField.selectAll();
		// Make sure the new text is visible, even if there
		// was a selection in the text area.
		textArea.setCaretPosition(textArea.getDocument().getLength());

	}
}
