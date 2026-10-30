package com.terminal.controller;

import com.terminal.ui.NewWindow;
import javafx.scene.Scene;
import javafx.scene.control.*;

import java.io.IOException;

/**
 * Provides find and replace operations for a specific {@link TextArea}.
 *
 * <p>Operates independently, maintaining its own search state ({@code lastIndex})
 * for the target text area.</p>
 */
public class FindReplace {

    private final TextArea textArea;
    private final TextField findField;
    private final TextField replaceField;
    private int lastIndex = 0;

    /**
     * Initializes the Find & Replace dialog for the given text area.
     *
     * <p>Looks up the dialog controls from the FXML scene and assigns their
     * event handlers.</p>
     *
     * @param textArea the target text area
     * @throws IOException if the FXML resource cannot be loaded
     */
    public FindReplace(TextArea textArea) throws IOException {
        this.textArea = textArea;
        NewWindow window = new NewWindow("/com/terminal/view/findReplaceWindow.fxml","Find & Replace",false,false);
        Scene scene = window.getStage().getScene();

        findField = (TextField) scene.getRoot().lookup("#find");
        replaceField = (TextField) scene.getRoot().lookup("#replace");
        Button findNext = (Button) scene.getRoot().lookup("#findNext");
        Button replaceNext = (Button) scene.getRoot().lookup("#replaceNext");
        Button replaceAll = (Button) scene.getRoot().lookup("#replaceAll");

        findNext.setOnAction(event -> findNext());
        replaceNext.setOnAction(event -> replaceNext());
        replaceAll.setOnAction(event -> replaceAll());
    }

    /**
     * Finds the next occurrence of the search string and highlights it.
     *
     * <p>When no further occurrences are found, resets the search position to
     * {@code 0} so the next search starts from the beginning.</p>
     */
    public void findNext() {
        String text = textArea.getText();
        String find = findField.getText();

        if (find == null || find.isEmpty()) return;

        int index = text.indexOf(find, lastIndex);

        if(index != -1){
            textArea.selectRange(index, index + find.length());
            lastIndex = index + find.length();
        }

        if(index == -1 && lastIndex > 0){
            lastIndex = 0;
        }
    }

    /**
     * Replaces the currently selected text if it exactly matches the search string,
     * then automatically advances the search to the next occurrence.
     */
    public void replaceNext() {
        String find = findField.getText();
        String replace = replaceField.getText();

        if (find == null || find.isEmpty()) return;
        String selected = textArea.getSelectedText();

        if(selected.equals(find)) {
            int start = textArea.getSelection().getStart();
            int end = textArea.getSelection().getEnd();
            textArea.replaceText(start, end, replace);
            lastIndex = start + replace.length();
        }

        findNext();
    }

    /**
     * Replaces all occurrences of the search string in the entire text area
     * and resets the search index to {@code 0}.
     */
    public void replaceAll() {
        String find = findField.getText();
        String replace = replaceField.getText();

        if (find == null || find.isEmpty()) return;
        String text = textArea.getText();

        textArea.setText(text.replace(find, replace));

        lastIndex = 0;
    }
}
