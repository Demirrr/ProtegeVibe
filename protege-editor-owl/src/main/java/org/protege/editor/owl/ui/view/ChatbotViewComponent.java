package org.protege.editor.owl.ui.view;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatbotViewComponent extends AbstractOWLViewComponent {
    private JTextArea chatHistory;
    private JTextField inputField;
    private JButton sendButton;
    private JScrollPane scrollPane;

    @Override
    protected void initialiseOWLView() throws Exception {
        setLayout(new BorderLayout());

        // Chat history area
        chatHistory = new JTextArea();
        chatHistory.setEditable(false);
        chatHistory.setLineWrap(true);
        chatHistory.setWrapStyleWord(true);
        scrollPane = new JScrollPane(chatHistory);
        add(scrollPane, BorderLayout.CENTER);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputField = new JTextField();
        sendButton = new JButton("Send");

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(inputPanel, BorderLayout.SOUTH);

        // Add action listeners
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            chatHistory.append("You: " + message + "\n");
            // Add auto-scroll to bottom
            chatHistory.setCaretPosition(chatHistory.getDocument().getLength());
            inputField.setText("");
            inputField.requestFocus();
        }
    }

    @Override
    public void disposeOWLView() {
        // Clean up resources if needed
    }
}