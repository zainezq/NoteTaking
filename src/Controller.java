
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.HTMLEditor;
import javafx.stage.FileChooser;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Modality;
import javafx.stage.Stage;


import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Controller {


    @FXML
    public BorderPane mainBorderPane;
    public Menu file;
    public TextField noteTitleTextField;
    public SplitPane yourSplitPane;
    public ButtonBar deleteBar;
    @FXML
    private ListView<String> previewList;

    @FXML
    private HTMLEditor editor;

    private ObservableMap<String, String> notes = FXCollections.observableHashMap();
    private String currentNoteTitle = null; // Track the currently selected note

    private File loadedFile;

    public void initialize() {




        // Bind the dividerPositions property to the Scene width property
        yourSplitPane.setDividerPositions(0.2); // Initial position

        // Add a listener to dynamically adjust the position on window resize
        yourSplitPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                yourSplitPane.setDividerPositions(0.2);
                newScene.widthProperty().addListener((obs2, oldWidth, newWidth) -> {
                    yourSplitPane.setDividerPositions(0.2);
                });
            }
        });


        // Initialize the previewList with existing note titles
        previewList.setItems(FXCollections.observableArrayList(notes.keySet()));


        // Handle note selection in the previewList
        previewList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                // Save the content of the current note before switching
                if (currentNoteTitle != null) {
                    String currentContent = editor.getHtmlText();
                    notes.put(currentNoteTitle, currentContent);
                }

                // Load the content of the selected note
                loadNoteIntoEditor(newValue);
                currentNoteTitle = newValue;
            }
        });

        editor.setDisable(true);
        editor.setHtmlText("Please click 'New Note' to get started...");
    }


    // Load and display the content of the selected note in the editor
    private void loadNoteIntoEditor(String noteTitle) {
        String noteContent = notes.get(noteTitle);
        editor.setHtmlText(noteContent);
    }

    @FXML
    private void handleRenameNote() {
        int selectedIndex = previewList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            // Prompt the user for a new title
            TextInputDialog dialog = new TextInputDialog(previewList.getItems().get(selectedIndex));
            dialog.setTitle("Rename Note");
            dialog.setHeaderText("Enter a new title for the note:");
            dialog.setContentText("New Title:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(newTitle -> {
                // Update the title in the list and the map
                String oldTitle = previewList.getItems().get(selectedIndex);
                previewList.getItems().set(selectedIndex, newTitle);
                notes.put(newTitle, notes.remove(oldTitle));
            });
        }
    }

    @FXML
    private void handleAddToNote() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add New Note");
        dialog.setHeaderText("Enter a title for the new note:");
        dialog.setContentText("Title:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(title -> {
            if (!notes.containsKey(title)) {
                // Add the new note to the map
                notes.put(title, "");
                previewList.getItems().add(title);
                // Select the newly added note in the list
                previewList.getSelectionModel().select(title);
                // Enable the editor when a new note is created
                editor.setDisable(false);
                // Clear the editor or load the note content if needed
                editor.setHtmlText("");
            } else {
                showAlert("Error", "A note with the same title already exists.", AlertType.ERROR);
            }
        });
    }


    @FXML
    private void handleNew() {
        // Check if there's unsaved content in the editor
        if (!isContentUnsaved()) {
            // If there's unsaved content, ask the user for confirmation
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("You have unsaved changes. Do you want to save them?");
            alert.setContentText("Choose your option.");

            ButtonType saveButton = new ButtonType("Save");
            ButtonType discardButton = new ButtonType("Discard");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent()) {
                if (result.get() == saveButton) {
                    // Save the content
                    handleSave();
                } else if (result.get() == discardButton) {
                    // Discard unsaved changes and clear the editor
                    editor.setHtmlText("<html><head></head><body contenteditable=\"true\"></body></html>");
                    previewList.getItems().clear();
                }
                // If the user chooses Cancel, do nothing
            }
        } else {

            handleAddToNote();
        }
    }

    // Helper method to check if there's unsaved content
    private boolean isContentUnsaved() {
        // Check if the editor contains any content
        String currentContent = editor.getHtmlText();
        return !currentContent.isEmpty();
    }


    private String convertTextToHtml(String plainText) {
        // Replace newline characters with HTML line breaks
        String htmlContent = plainText.replace("\n", "<br>");

        // Wrap the text in HTML body and paragraph tags
        return "<html><body><p>" + htmlContent + "</p></body></html>";
    }

    @FXML
    private void handleLoad() {
        // Open a file chooser dialog to select a file to load
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File selectedFile = fileChooser.showOpenDialog(editor.getScene().getWindow());

        if (selectedFile != null) {
            // Read the content of the selected file and set it in the editor
            loadedFile = selectedFile;

            System.out.println("Reading and loading file...");

            try {
                // Implement a method to read file content
                String fileContent = readFile(selectedFile);

                if (!fileContent.isEmpty()) {
                    String toHtml = readFile(selectedFile);
                    String HTMLText = convertTextToHtml(toHtml);

                    // Split the content into the first line (title) and the rest (content)
                    String[] lines = fileContent.split("\n");

                    // Extract the first line as the title
                    String title = lines[0];


                    // Add the title to the previewList
                    previewList.getItems().add(title);

                    // Select the newly added note in the list
                    previewList.getSelectionModel().select(title);

                    // Enable the editor and load the content
                    editor.setDisable(false);
                    editor.setHtmlText(HTMLText);
                }

            } catch (IOException e) {
                showAlert("Error", "Error reading the file", AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleSave() {
        // Save the content of the editor to a file
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File selectedFile = fileChooser.showSaveDialog(editor.getScene().getWindow());

        if (selectedFile != null) {
            // Implement a method to save content to a file
            String contentToSave = editor.getHtmlText();
            saveFile(selectedFile, contentToSave);
        }
    }


    @FXML
    private void handleDelete() {
        int selectedIndex = previewList.getSelectionModel().getSelectedIndex();

        if (selectedIndex >= 0) {
            // Check if there are unsaved changes
            if (isContentUnsaved()) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setHeaderText("You have unsaved changes. Do you want to save them?");
                alert.setContentText("Choose your option.");

                ButtonType saveButton = new ButtonType("Save");
                ButtonType discardButton = new ButtonType("Discard");
                ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent()) {
                    if (result.get() == saveButton) {
                        // Save the content
                        handleSave();
                    } else if (result.get() == discardButton) {
                        // Discard unsaved changes
                        previewList.getItems().remove(selectedIndex);
                        clearEditor(); //editor.setHtmlText("");
                        if (!previewList.getItems().isEmpty()) {
                            // If there are notes left, select the previous or next one
                            int newIndex = Math.min(selectedIndex, previewList.getItems().size() - 1);
                            previewList.getSelectionModel().select(newIndex);
                            loadNoteIntoEditor(previewList.getItems().get(newIndex));
                        }
                    }
                    // If the user chooses Cancel, do nothing
                }
            } else {
                // No unsaved changes, proceed with deleting the note
                previewList.getItems().remove(selectedIndex);
                clearEditor();
                if (!previewList.getItems().isEmpty()) {
                    // If there are notes left, select the previous or next one
                    int newIndex = Math.min(selectedIndex, previewList.getItems().size() - 1);
                    previewList.getSelectionModel().select(newIndex);
                    loadNoteIntoEditor(previewList.getItems().get(newIndex));
                }
            }
        }
    }

    private void clearEditor() {
        // Clear the editor content
        editor.setHtmlText("");
    }


    // Utility method to read content from a file (implement as needed)
    private String readFile(File file) throws IOException {
        // Implement file reading logic here
        String content = ""; // Initialize an empty string

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                content += scanner.nextLine() + "\n";
            }
        }

        // Print the content read from the file
        System.out.println("File Content:\n" + content);

        return content;
    }


    // Utility method to save content to a file (implement as needed)
    private void saveFile(File file, String content) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            // Handle the exception appropriately (e.g., show an error dialog)
            e.printStackTrace();
        }
    }

    // Utility method to show an alert
    private void showAlert(String title, String message, AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleExportSelected() {
        List<String> selectedItems = previewList.getSelectionModel().getSelectedItems();

        if (selectedItems.isEmpty()) {
            // No items selected, show an alert
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("No notes selected");
            alert.setHeaderText("You haven't selected any notes to export");
            alert.showAndWait();
        } else {
            // Create a file chooser for saving the zip file
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Files", "*.zip"));
            File saveFile = fileChooser.showSaveDialog(editor.getScene().getWindow());

            if (saveFile != null) {
                // Save selected notes as a zip file
                saveNotesAsZip(selectedItems, saveFile);
            }
        }
    }
    private void saveNotesAsZip(List<String> noteTitles, File destinationZipFile) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(destinationZipFile))) {
            for (String noteTitle : noteTitles) {
                String noteContent = notes.get(noteTitle);
                if (noteContent != null) {
                    // Create a zip entry for each note
                    ZipEntry entry = new ZipEntry(noteTitle + ".txt");
                    zipOutputStream.putNextEntry(entry);
                    zipOutputStream.write(noteContent.getBytes());
                    zipOutputStream.closeEntry();
                }
            }
        } catch (IOException e) {
            showAlert("Error", "Error saving notes as ZIP", AlertType.ERROR);
        }
    }


    public void handleExportAll() {
        if (!previewList.getItems().isEmpty()) {
            saveNotesAsZip();
        }
        else {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("No notes selected");
            alert.setHeaderText("You haven't selected any notes to export");
            alert.showAndWait();
        }
    }

    public void handleTutorial() {
        String url = "https://github.com/zainezq/NoteTaking/tree/main";

        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void handleAbout() {

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.showAndWait();

    }

    private void saveNotesAsZip() {
        // Implement the logic to save notes as a ZIP file here
        // You can use a FileChooser to allow the user to choose the save location
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP Files", "*.zip"));
        File saveFile = fileChooser.showSaveDialog(previewList.getScene().getWindow());

        if (saveFile != null) {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(saveFile))) {
                // Iterate through the notes and save each as a separate entry in the ZIP file
                for (String note : previewList.getItems()) {
                    //ZipEntry entry = new ZipEntry("note" + previewList.getItems().indexOf(note) + ".txt");

                    ZipEntry entry = new ZipEntry(note + ".txt");
                    zipOutputStream.putNextEntry(entry);
                    zipOutputStream.write(note.getBytes());
                    zipOutputStream.closeEntry();
                }
            } catch (IOException e) {
                showAlert("Error", "Error saving notes as ZIP", AlertType.ERROR);
            }
        }
    }


    private void saveNotesAsTxt() {
        // Implement the logic to save notes as a single TXT file here
        // You can use a FileChooser to allow the user to choose the save location
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File saveFile = fileChooser.showSaveDialog(previewList.getScene().getWindow());

        if (saveFile != null) {
            try (PrintWriter writer = new PrintWriter(saveFile)) {
                // Iterate through the notes and save them
                for (String note : previewList.getItems()) {
                    writer.println(note);
                }
            } catch (IOException e) {
                showAlert("Error", "Error saving notes as TXT", AlertType.ERROR);
            }
        }
    }

    public void handleExit() {
        // Check if there are notes
        if (!previewList.getItems().isEmpty()) {
            // Create a custom exit confirmation dialog
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.setTitle("Exit Confirmation");
            alert.setHeaderText("Do you want to save your notes?");
            alert.setContentText("Choose your option:");

            ButtonType saveAsZipButton = new ButtonType("Save as Zip");
            ButtonType saveAsTxtButton = new ButtonType("Save as Txt");
            ButtonType saveToFileButton = new ButtonType("Save to File"); // New option
            ButtonType exitWithoutSavingButton = new ButtonType("Exit without saving");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(saveAsZipButton, saveAsTxtButton, saveToFileButton, exitWithoutSavingButton, cancelButton);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == saveAsZipButton) {
                    // Save notes as a zip file
                    // Implement zip saving logic here
                    saveNotesAsZip();
                    Platform.exit();
                } else if (result.get() == saveAsTxtButton) {
                    // Save notes as a single txt file
                    // Implement txt saving logic here
                    saveNotesAsTxt();
                    Platform.exit();
                } else if (result.get().equals(saveToFileButton)) {
                    // Save the current note to the file it was loaded from
                    if (currentNoteTitle != null) {
                        saveNoteToFile(currentNoteTitle);
                    }
                    Platform.exit();
                } else if (result.get() == exitWithoutSavingButton) {
                    // Exit without saving
                    Platform.exit();

                }

            } else {
                // Dialog closed without a choice, do nothing
                return;            }
        } else {
            Platform.exit();
        }


    }

    private void saveNoteToFile(String noteTitle) {
        if (notes.containsKey(noteTitle) && loadedFile != null) {
            String content = notes.get(noteTitle);

            try (PrintWriter writer = new PrintWriter(loadedFile)) {
                writer.print(content);
                System.out.println("File saved successfully: " + loadedFile.getAbsolutePath());
            } catch (IOException e) {
                showAlert("Error", "Error saving note to file: " + e.getMessage(), AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    // decided not to use a settings page for the first version
    /*
    public void handleSetting() {
        try {
            // Load the Settings.fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Future_Updates/controller-view.fxml"));
            Parent root = loader.load();
            // Create a new stage (window) for the settings
            Stage settingsStage = new Stage();
            settingsStage.initModality(Modality.APPLICATION_MODAL); // Makes it a modal window
            settingsStage.setTitle("Settings");
            settingsStage.setScene(new Scene(root));
            settingsStage.setMinWidth(800);
            settingsStage.setMinHeight(600);
            settingsStage.setWidth(1000);
            settingsStage.setHeight(800);

            // Show the settings window
            settingsStage.showAndWait(); // Use showAndWait if you want it to be a modal window
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
*/
    }



