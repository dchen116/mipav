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

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/*
 * Create a frame with message that presented all the duplicated annotation that was found and be able to save the time point of the duplicated annotation
 */
public class DupAnnFound implements ActionListener {

	public static void main(String[] args) {
		DupAnnFound daf = new DupAnnFound();

	}

	// add variables here
	JFrame frame;
	LayoutManager layout;
	JButton buttonOK, buttonCopy;
	JPanel panelButton;
	JOptionPane paneDialog;
	JDialog dialog;
	JTextField textField;
	JTextArea textArea;
	JScrollPane scrollPane;
	String newline = "\n";
	String str;

	public DupAnnFound() {
		this.frame = new JFrame("Duplicated Annotation Found!");
		this.layout = new FlowLayout();

		createGUI();
	}

	private void createGUI() {
		// Create button "OK"
		buttonOK = new JButton("OK to save");
		// Add button to the frame
		frame.add(buttonOK);
		// Add button to ActionListener
		buttonOK.addActionListener(this);
		
		// Create button "Copy Text"
		buttonCopy = new JButton("Copy Text");
		frame.add(buttonCopy);
		buttonCopy.addActionListener(this);
		
		// Create JPanel for the button
		panelButton = new JPanel();
		// Add Button to JPanel
		panelButton.add(buttonOK);
		panelButton.add(buttonCopy);
		// Add panel to the frame
		frame.getContentPane().add(panelButton);
		
		// Create dialog pop after hit "ok" from the frame
		paneDialog = new JOptionPane ("You have saved the message in (file path)");
		dialog = paneDialog.createDialog("Dialog");
		dialog.setModalityType(ModalityType.APPLICATION_MODAL);
		
		
		
		
		// Create TextArea and TextField
		JPanel panelText = new JPanel(new GridBagLayout());
		
		textField = new JTextField(20);
		textField.addActionListener(this);
		panelText.add(textField); //add textField to the panel
		frame.add(textField); //add textField to the frame
		String message = "Type the Text you want here. . . .";
		textField.setText(message);
		
		
		// Create TextArea and copy to clipboard
		textArea = new JTextArea(5, 20);
		textArea.setEditable(true);
		/*
		StringSelection stringSelection = new StringSelection(textArea.getText());
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
		*/
		
		
		panelText.add(textArea); //add textArea to panel
		frame.add(panelText); //add panelText to frame
		// Add scroll feature
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setPreferredSize(new Dimension(450, 110));

		// Add Components to this panel.
		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;

		c.fill = GridBagConstraints.HORIZONTAL;
		panelText.add(textField, c);

		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		panelText.add(scrollPane, c);
		
		// Display the window
		frame.setLayout(layout);
		frame.pack();
		frame.setVisible(true);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();
		System.out.println(actionCommand);
		
		System.out.println("Action Performed");
		
		if (actionCommand.equals(buttonOK.getText())){
			JFileChooser chooser = new JFileChooser();
			 chooser.showOpenDialog(frame);
			 File file = chooser.getSelectedFile();
			 System.out.println(file.toString());
			 frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			 dialog.setVisible(true);
		} else if (actionCommand.equals(buttonCopy.getText())){
			// for TextArea
			String text = textArea.getText();
			//textArea.append(text + newline);
			textField.selectAll();
			// Make sure the new text is visible, even if there
			// was a selection in the text area.
			textArea.setCaretPosition(textArea.getDocument().getLength());
			
			StringSelection stringSelection = new StringSelection(textArea.getText());
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
			System.out.println("text is copied to clipboard");	
		}
	}
}
