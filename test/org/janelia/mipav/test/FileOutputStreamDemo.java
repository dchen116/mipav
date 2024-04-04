package org.janelia.mipav.test;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.google.common.io.Files;

import gov.nih.mipav.view.renderer.WildMagic.WormUntwisting.LatticeModel;

public class FileOutputStreamDemo {

	public FileOutputStreamDemo(String string, boolean append) {
		// TODO Auto-generated constructor stub
	}
	

	public static void main(String[] args) throws IOException
	    {
	    }
	
	public static void writeToFile(String text) {

			try {
				  // create a fileoutputstream object
				System.out.println(System.getProperty("user.dir"));
							
	            FileOutputStream fout = new FileOutputStream("DuplicatedAnnotation.txt", false);

		        // Print writer -to-do
		        //File file = new File ("C:\\Users\\chend\\eclipse-workspace\\mipav\\DuplicatedAnnotation.txt");
		        //File file = new File ("""C:\Users\chend\eclipse-workspace\mipav\DuplicatedAnnotation.txt""");
		        //file.getParentFile().mkdirs();
		        //PrintWriter writer = new PrintWriter (file);

		        
		        PrintWriter writer = new PrintWriter(fout);
		        writer.println ("Here are the errors need to be fixed:\n");
		        writer.println(text);
		        writer.close ();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
	    }
}
