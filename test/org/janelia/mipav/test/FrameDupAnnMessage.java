package org.janelia.mipav.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class FrameDupAnnMessage implements ActionListener, ItemListener {

	// add variables here
	JFrame frame;
	JButton buttonOk, buttonCopyAll, buttonBrowse;
	JPanel panelButton, checkPanel, radioPanel;
	JOptionPane panelButtonDialog, panelNoDupAnnDialog;
	JDialog dialogAfterButton, noDupAnnDialog;
	JCheckBox checkbox1, checkbox2;
	LayoutManager layout;
	StringBuffer choices;
	JRadioButton radioButtonA, radioButtonB;
	String newline = "\n";
	JTextField textField;
	JTextArea textArea;
	JFileChooser fileChooser;
	File file, directory;

	public FrameDupAnnMessage() {
		// write code here
		this.frame = new JFrame("Duplicated Annotation Found!");
		this.layout = new FlowLayout();

		createGUI();
	}

	private void createGUI() {
		frame.setLayout(new GridBagLayout());

		// create buttonOK
		buttonOk = new JButton("OK!");
		// add button to actionListener
		buttonOk.addActionListener(this);
		// add buttonCopyAll
		buttonCopyAll = new JButton("Copy All");
		buttonCopyAll.setBounds(70, 270, 150, 50);
		buttonCopyAll.addActionListener(this);
		// add buttonBrowse
		buttonBrowse = new JButton("Browse to save");
		buttonBrowse.setBounds(70, 270, 150, 50);
		buttonBrowse.addActionListener(this);

		// create JPanel for the button
		panelButton = new JPanel();
		panelButton.add(buttonOk); // Add button to JPanel
		panelButton.add(buttonCopyAll);
		panelButton.add(buttonBrowse);

		// a dialog pop out after the frame
		panelButtonDialog = new JOptionPane("You clicked 'OK!'");
		dialogAfterButton = panelButtonDialog.createDialog("Dialog");
		dialogAfterButton.setModalityType(ModalityType.APPLICATION_MODAL);
		
		// Create checkbox + set event + add to ItemListener for more than one items
		checkbox1 = new JCheckBox("Item 1: Save to file");
		checkbox1.setMnemonic(KeyEvent.VK_C);
		checkbox1.setSelected(false);
		checkbox1.addItemListener(this);

		checkbox2 = new JCheckBox("Item 2: Copy and Paste");
		checkbox2.setMnemonic(KeyEvent.VK_C);
		checkbox2.setSelected(false);
		checkbox2.addItemListener(this);

		// Indicates what's on the geek.
		choices = new StringBuffer("12");

		// Put the check boxes in a column in a panel
		checkPanel = new JPanel(new GridLayout(0, 1));
		checkPanel.add(checkbox1);
		checkPanel.add(checkbox2);
		frame.getContentPane();

		// a dialog pop out when there is no content in the textArea
		panelNoDupAnnDialog = new JOptionPane("No Duplicated Annotations is found!");
		noDupAnnDialog = panelNoDupAnnDialog.createDialog("Dialog");
		noDupAnnDialog.setModalityType(ModalityType.APPLICATION_MODAL);
		
		// Create TextArea
		textArea = new JTextArea(20, 70);
		textArea.setEditable(true);
		JScrollPane scrollPane = new JScrollPane(textArea);
		// Add Components to this panel.
		GridBagConstraints c = new GridBagConstraints();
		// Set GridLayout for textArea
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH; // fill both gridx and gridy
		c.weightx = 1; // distribute space
		c.weighty = 1;
		frame.add(scrollPane, c);

		// Set GridLayout for checkboxes
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		frame.add(checkbox1, c); // add checkbox1 to the frame

		c.gridy = 2;
		frame.add(checkbox2, c); // add checkbox2 to the frame

		// Anchor the "OK" button to the right corner(EAST) of the frame
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.VERTICAL;
		c.gridx = 1;
		c.gridy = 1;
		c.gridheight = 2; // see the height of the button the same as the height of checkbox1 + checkbox2
		c.weightx = 1;
		frame.add(buttonOk, c); // add button to the frame

		// Set GridLayout for buttonCopyAll
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 3;
		c.gridheight = 1;
		c.gridwidth = 1;
		frame.add(buttonCopyAll, c); // add buttonCopyAll to the frame


		// Anchor the buttonBrowse to the right (EAST) of the frame
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.VERTICAL;
		c.gridx = 1;
		c.gridy = 4;
		c.gridheight = 1; // see the height of the button the same as the height of checkbox1 + checkbox2
		c.weightx = 1;
		frame.add(buttonBrowse, c); // add button to the frame

		// Display the window.
		frame.setBounds(80, 80, 80, 80);
		frame.pack();
		frame.setVisible(true);

		fileChooser = new JFileChooser();
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
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
				System.out.println("Item 1:Save to file: " + System.getProperty("user.dir"));
			} else if (source == checkbox2) {
				c = '2';
				System.out.println("Item 2:Copy and Paste");
			}
		} else if (e.getStateChange() == ItemEvent.DESELECTED) {
			c = '-';
			System.out.println("Item deselected");
		}

		// Apply the change to the string.
		choices.setCharAt(index, c);
		panelButtonDialog.setMessage("You pressed 'OK!'. Selected checkboxes: " + choices.toString());

		boolean checkbox1_selected = checkbox1.getSelectedObjects() != null;
		boolean checkbox2_selected = checkbox2.getSelectedObjects() != null;
		System.out.println("Checkbox 1: " + checkbox1_selected);
		System.out.println("Checkbox 2: " + checkbox2_selected);

		if ((checkbox1_selected == true) && (checkbox2_selected == false)) {
			panelButtonDialog.setMessage("You pressed 'OK!'." + "\n" + "Selected checkboxes: " + "Item 1:Save to file: "
					+ System.getProperty("user.dir"));
		} else if ((checkbox1_selected == false) && (checkbox2_selected == true)) {
			panelButtonDialog.setMessage(
					"You pressed 'OK!'." + "\n" + " Selected checkboxes: " + "Item 2:Copy and Paste yourself");
		} else if ((checkbox1_selected == true) && (checkbox2_selected == true)) {
			panelButtonDialog
					.setMessage("You pressed 'OK!'." + "\n" + "Selected checkboxes: " + "\n" + "Item 1:Save to file: "
							+ System.getProperty("user.dir") + "\n" + "and " + "\n" + "Item 2:Copy and Paste yourself");
		} else if ((checkbox1_selected == false) && (checkbox2_selected == false)) {
			panelButtonDialog.setMessage("You pressed 'OK!'." + "\n" + "No Checkbox was Selected");
		}

		//dialogAfterButton = panelButtonDialog.createDialog("Dialog");
		//dialogAfterButton.setModalityType(ModalityType.APPLICATION_MODAL);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("Action performed");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		System.out.println(choices.toString());

		boolean checkbox1_selected = checkbox1.getSelectedObjects() != null;
		boolean checkbox2_selected = checkbox2.getSelectedObjects() != null;
		System.out.println("Checkbox 1: " + checkbox1_selected);
		System.out.println("Checkbox 2: " + checkbox2_selected);

		String data = textArea.getText().trim(); // read contents of text area into 'data'
		if (!data.equals("")) {
			if (e.getSource() == buttonOk) {
				dialogAfterButton.setVisible(true);
			} else if (e.getSource() == buttonCopyAll) {
				// select all and copy to Clipboard
				StringSelection stringSelection = new StringSelection(textArea.getText());
				Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
				clpbrd.setContents(stringSelection, null);
				JOptionPane.showMessageDialog(null, "Texts are copied to clipboard!");
			} else if (e.getSource() == buttonBrowse) {
				// fileChooser = new JFileChooser("C:\\Users\\chend\\Desktop");
				// fileChooser = new JFileChooser(latticeStraighten.outputDirectory);

				fileChooser.setSelectedFile(new File("Duplicated_Annotations.txt"));

				int returnVal = fileChooser.showSaveDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					file = fileChooser.getSelectedFile();
					// save to file
					try (FileWriter fw = new FileWriter(fileChooser.getSelectedFile())) {
						fw.write(textArea.getText());
					} catch (Exception ex) {
						ex.printStackTrace();
					}

					textArea.append("\n\n  <<" + file.getName() + ">> is saved to: "
							+ fileChooser.getSelectedFile().getAbsolutePath() + newline);
				}
			}
		} else {
			noDupAnnDialog.setVisible(true);
			System.out.println("no duplicated annotations.");

		}
		// Make sure the new text is visible, even if there
		// was a selection in the text area.
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	public void appendMessage(String str) {
		textArea.append(str);
	}

	public void setCurrentDirectory(String opDirectory) {

		directory = new File(opDirectory);
		fileChooser.setCurrentDirectory(directory);
	}
	
	  public static void main(String[] arg) { FrameDupAnnMessage fda = new
	  FrameDupAnnMessage(); fda.appendMessage("duplicate annotations");
	 // FrameDupAnnMessage(); fda.appendMessage("");
	  //fda.setCurrentDirectory("C:\\Users\\chend\\Desktop"); 
	  }
	 
}
