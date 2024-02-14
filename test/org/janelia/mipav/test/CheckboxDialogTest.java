package org.janelia.mipav.test;

import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;

public class CheckboxDialogTest implements ActionListener, ItemListener {

	public static void main(String[] args) {
		CheckboxDialogTest cdt = new CheckboxDialogTest();
		//nothing else here
	}
	
	// add variables here
	JFrame frame;
	JButton button;
	JCheckBox checkbox;
	LayoutManager layout;
	
	public CheckboxDialogTest() {
		//write code here
		this.frame = new JFrame("Hello world");
		this.layout = new FlowLayout();
		
		createGUI();
	}
	
	private void createGUI() {
		//frame.pack();
		button = new JButton("Hi!");
		checkbox = new JCheckBox();
		frame.add(checkbox);
		frame.add(button);
		frame.setLayout(layout);
		button.addActionListener(this);
		checkbox.addItemListener(this);
		frame.pack();
		frame.setVisible(true);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub
		System.out.println("Item state changed");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		System.out.println("Action performed");
	}

}
