package clients;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Graphical interface for a client
 */
public class ClientGUI implements ActionListener {

	DocumentClient client;
	private JFrame mainFrame;
	private JPanel leftPanel;
	private JPanel rightPanel;
	private JTextArea mainTextArea;
	private JTextArea infoTextArea;
	private JTextField entryField;
	private JButton buttonDisplayCache;
	private JButton buttonGetDocument;
	private JButton buttonUploadDocument;
	private JButton buttonDocumentsList;
	private JButton buttonModify;
	private JButton buttonQuit;

	public ClientGUI() {
		mainFrame = new JFrame();
		mainFrame.setLayout(new GridLayout(1, 2));

		mainTextArea = new JTextArea();
		mainTextArea.setEditable(false);
		leftPanel = new JPanel(new FlowLayout());
		mainFrame.add(leftPanel);

		leftPanel.add(mainTextArea);

		entryField = new JTextField(30);
		leftPanel.add(entryField);

		buttonDisplayCache = new JButton("Display cache");
		buttonGetDocument = new JButton("Get document");
		buttonUploadDocument = new JButton("Upload document");
		buttonDocumentsList = new JButton("Documents list");
		buttonModify = new JButton("Modify");
		buttonQuit = new JButton("Quit");
		leftPanel.add(buttonDisplayCache, BorderLayout.SOUTH);
		leftPanel.add(buttonGetDocument, BorderLayout.SOUTH);
		leftPanel.add(buttonUploadDocument, BorderLayout.SOUTH);
		leftPanel.add(buttonDocumentsList, BorderLayout.SOUTH);
		leftPanel.add(buttonModify, BorderLayout.SOUTH);
		leftPanel.add(buttonQuit, BorderLayout.SOUTH);
		buttonDisplayCache.addActionListener(this);
		buttonGetDocument.addActionListener(this);
		buttonUploadDocument.addActionListener(this);
		buttonDocumentsList.addActionListener(this);
		buttonModify.addActionListener(this);
		buttonQuit.addActionListener(this);

		rightPanel = new JPanel(new FlowLayout());
		infoTextArea = new JTextArea();
		infoTextArea.setEditable(false);
		rightPanel.add(infoTextArea);
		mainFrame.add(rightPanel);

		mainFrame.setSize(700, 300);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);
	}

	public void setHandler(DocumentClient client) {
		this.client = client;
		mainFrame.setTitle(client.getName());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton btn = (JButton) e.getSource();
			if (btn == buttonDisplayCache) {
				mainTextArea.setText(client.getCache().textContent());
			} 
			else if (btn == buttonQuit) {
				exit();
			} 
			else if (btn == buttonGetDocument) {
				client.requestDocument(entryField.getText());
				entryField.setText("");
			} 
			else if (btn == buttonUploadDocument) {
				if (! client.unlockDocument(entryField.getText())) {
					displayMessage("This document doesn't exist.");
				}
				entryField.setText("");
			} 
			else if (btn == buttonDocumentsList) {
				client.requestDocumentsList();
			}
			else if (btn == buttonModify) {
				if (client.getCache().exists(entryField.getText())) {
					JDialog modifyDialog = new JDialog(mainFrame, true);
					modifyDialog.setLayout(new FlowLayout());
					JTextArea modifyArea = new JTextArea();
					modifyArea.setText(new String(client.getCache().getDocumentContent(entryField.getText())));
					modifyDialog.add(modifyArea);
					ModifyFrameListener listener = new ModifyFrameListener(modifyDialog, modifyArea);
					JButton buttonOK = new JButton("OK");
					buttonOK.addActionListener(listener);
					modifyDialog.add(buttonOK);
					JButton buttonCancel = new JButton("Cancel");
					buttonCancel.addActionListener(listener);
					modifyDialog.add(buttonCancel);
					modifyDialog.setSize(300, 300);
					modifyDialog.setVisible(true);					
				}
				else {
					displayMessage("This document doesn't exist.");
				}
			}
		}
	}

	public void displayMessage(String message) {
		JOptionPane.showMessageDialog(mainFrame, message);
	}

	// TODO do not display in mainTextArea
	public void displayDocument(String documentContent) {
		mainTextArea.setText(documentContent);
	}

	public void updateInfo(String text) {
		infoTextArea.setText(text);
	}

	public void exit(){
		mainFrame.dispose();
		System.exit(0);
	}
	
	/**
	 * Inner class listening the modifying frame events
	 */
	private class ModifyFrameListener implements ActionListener {
		
		private JDialog dialog;
		private JTextArea textArea;
		public ModifyFrameListener(JDialog d, JTextArea ta) {
			dialog = d;
			textArea = ta;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() instanceof JButton) {
				if (((JButton)e.getSource()).getText().equals("Cancel")) {
					dialog.dispose();
				}
				else if (((JButton)e.getSource()).getText().equals("OK")) {
					client.modifyDocument(entryField.getText(), textArea.getText());
					dialog.dispose();
				}
			}
		}
		
	}

}
