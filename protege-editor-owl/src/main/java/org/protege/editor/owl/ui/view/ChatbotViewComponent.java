package org.protege.editor.owl.ui.view;

import org.protege.editor.owl.ui.view.llm.LLMService;
import javax.swing.*;
import java.awt.*;

public class ChatbotViewComponent extends AbstractOWLViewComponent {
    private JTextArea chatHistory;
    private JTextField inputField;
    private JButton sendButton;
    private JButton clearButton;
    private LLMService llmService;

    @Override
    protected void initialiseOWLView() throws Exception {
        llmService = new LLMService();
        setLayout(new BorderLayout());

        // Chat history area
        chatHistory = new JTextArea();
        chatHistory.setEditable(false);
        chatHistory.setLineWrap(true);
        chatHistory.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatHistory);
        add(scrollPane, BorderLayout.CENTER);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputField = new JTextField();
        sendButton = new JButton("Send");

        // Add clear button
        clearButton = new JButton("Clear Chat");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearButton);
        buttonPanel.add(sendButton);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(inputPanel, BorderLayout.SOUTH);

        // Welcome message
        chatHistory.append("Assistant: Hello! I'm your ontology assistant. How can I help you today?\n\n");

        // Add action listeners
        sendButton.addActionListener(e -> sendMessage());
        clearButton.addActionListener(e -> clearChat());
        inputField.addActionListener(e -> sendMessage());
    }

    private void sendMessage() {
        String userMessage = inputField.getText().trim();
        if (!userMessage.isEmpty()) {
            // Display user message
            chatHistory.append("You: " + userMessage + "\n\n");
            inputField.setText("");

            // Show "thinking" indicator
            sendButton.setEnabled(false);
            inputField.setEnabled(false);
            chatHistory.append("Assistant: Thinking...\n");

            // Process in background
            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() {
                    return llmService.sendMessage(userMessage);
                }

                @Override
                protected void done() {
                    try {
                        // Remove "thinking" message
                        String text = chatHistory.getText();
                        text = text.replace("Assistant: Thinking...\n", "");
                        chatHistory.setText(text);

                        // Show response
                        String response = get();
                        chatHistory.append("Assistant: " + response + "\n\n");
                        chatHistory.setCaretPosition(chatHistory.getDocument().getLength());
                    } catch (Exception e) {
                        chatHistory.append("Error: Failed to get response\n\n");
                    } finally {
                        sendButton.setEnabled(true);
                        inputField.setEnabled(true);
                        inputField.requestFocus();
                    }
                }
            };
            worker.execute();
        }
    }

    private void clearChat() {
        chatHistory.setText("");
        llmService.clearHistory();
        chatHistory.append("Assistant: Chat history cleared. How can I help you?\n\n");
    }

    @Override
    public void disposeOWLView() {
        // Cleanup if needed
    }
}