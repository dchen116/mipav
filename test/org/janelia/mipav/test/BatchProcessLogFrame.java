package org.janelia.mipav.test;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class BatchProcessLogFrame implements ActionListener {
	// add variables here
	JFrame frame;
	JButton buttonOk, buttonCopyAll, buttonBrowse;
	JPanel panelButton, checkPanel, radioPanel;
	JOptionPane panelButtonDialog, panelNoDupAnnDialog;
	JDialog dialogAfterButton, noDupAnnDialog;
	LayoutManager layout;
	StringBuffer choices;
	JRadioButton radioButtonA, radioButtonB;
	String newline = "\n";
	JTextField textField;
	JTextArea textArea;
	JFileChooser fileChooser;
	File file, directory;

	public BatchProcessLogFrame() {
		// write code here
		this.frame = new JFrame("Batch Process Log");
		this.layout = new FlowLayout();

		createGUI();
	}

	private void createGUI() {
		frame.setLayout(new GridBagLayout());

		// create buttonOK
		buttonOk = new JButton("Ok to Close");
		// add button to actionListener
		buttonOk.addActionListener(this);
		// add buttonCopyAll
		buttonCopyAll = new JButton("Copy All");
		//buttonCopyAll.setBounds(70, 270, 150, 50);
		//buttonCopyAll.setPreferredSize(new Dimension(200,500));
		buttonCopyAll.addActionListener(this);
		// add buttonBrowse
		buttonBrowse = new JButton("Browse to save");
		//buttonBrowse.setBounds(70, 270, 150, 50);
		//buttonBrowse.setPreferredSize(new Dimension(200,500));
		buttonBrowse.addActionListener(this);

		// create JPanel for the button
		panelButton = new JPanel();
		panelButton.add(buttonOk); // Add button to JPanel
		panelButton.add(buttonCopyAll);
		panelButton.add(buttonBrowse);

		// a dialog pop out when there is no content in the textArea
		panelNoDupAnnDialog = new JOptionPane("No duplicated annotations are found!");
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
		c.gridwidth = 3;
		c.fill = GridBagConstraints.BOTH; // fill both gridx and gridy
		c.weightx = 1; // distribute space
		c.weighty = 1;
		frame.add(scrollPane, c);

		// Set GridLayout for buttonCopyAll
		c.fill = GridBagConstraints.VERTICAL;
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 1;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weightx = 1; // distribute space
		c.weighty = 0;
		frame.add(buttonCopyAll, c); // add buttonCopyAll to the frame

		// Anchor the buttonBrowse to the right (EAST) of the frame
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		c.gridx = 2;
		c.gridy = 1;
		c.gridheight = 1; // see the height of the button the same as the height of checkbox1 + checkbox2
		c.weightx = 1;
		frame.add(buttonBrowse, c); // add button to the frame

		// Anchor the "OK" button to the CENTER of the frame
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.NONE;
		c.gridx = 1;
		c.gridy = 2;
		c.gridheight = 2; // see the height of the button the same as the height of checkbox1 + checkbox2
		c.weightx = 1;
		frame.add(buttonOk, c); // add button to the frame

		// Display the window.
		frame.setBounds(80, 80, 80, 80);
		frame.pack();
		frame.setVisible(true);

		fileChooser = new JFileChooser();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("Action performed");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		String data = textArea.getText().trim(); // read contents of text area into 'data'
		if (!data.equals("")) {
			if (e.getSource() == buttonOk) {
				frame.dispose();
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
			if (e.getSource() == buttonOk) {
				frame.dispose();
			} else {
				noDupAnnDialog.setVisible(true);
				System.out.println("no duplicated annotations.");
			}
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
}
