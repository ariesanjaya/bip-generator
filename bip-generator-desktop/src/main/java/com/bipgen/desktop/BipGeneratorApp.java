package com.bipgen.desktop;

import com.bipgen.model.*;
import com.bipgen.service.BIP32Service;
import com.bipgen.service.BIP38Service;
import com.bipgen.service.PaperWalletService;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class BipGeneratorApp extends Application {

    private final BIP32Service bip32Service = new BIP32Service();
    private final BIP38Service bip38Service = new BIP38Service();
    private final PaperWalletService paperWalletService = new PaperWalletService();

    // Shared state from Tab 1
    private MasterKeyResult lastMasterKeyResult;
    private DerivedKeyResult lastDerivedKeyResult;
    private String lastPassphrase = "";

    // Monospace font for key/address fields
    private static final Font MONO_FONT_SMALL = Font.font("Monospace", 11);

    private static final String BG_COLOR = "#f8f9fa";
    private static final String CARD_STYLE = "-fx-background-color: white; -fx-border-color: #dee2e6; "
            + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 16;";
    private static final String BUTTON_STYLE = "-fx-background-color: #3b82f6; -fx-text-fill: white; "
            + "-fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;";
    private static final String BUTTON_HOVER_STYLE = "-fx-background-color: #2563eb; -fx-text-fill: white; "
            + "-fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;";
    private static final String SECTION_TITLE_STYLE = "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1e293b;";

    @Override
    public void start(Stage stage) {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: " + BG_COLOR + ";");

        Tab bip32Tab = new Tab("BIP32 - HD Wallet");
        bip32Tab.setContent(createBip32Tab());

        Tab bip38Tab = new Tab("BIP38 - Encrypt/Decrypt");
        bip38Tab.setContent(createBip38Tab());

        Tab paperTab = new Tab("Paper Wallet");
        paperTab.setContent(createPaperWalletTab());

        tabPane.getTabs().addAll(bip32Tab, bip38Tab, paperTab);

        Scene scene = new Scene(tabPane, 900, 750);
        stage.setTitle("BIP Generator Desktop");
        stage.setScene(scene);
        stage.show();
    }

    // ======================== BIP32 TAB ========================

    private ScrollPane createBip32Tab() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + BG_COLOR + ";");

        // --- Generate Master Key Section ---
        VBox genCard = createCard();
        Label genTitle = new Label("Generate Master Key");
        genTitle.setStyle(SECTION_TITLE_STYLE);

        HBox wordCountRow = new HBox(10);
        wordCountRow.setAlignment(Pos.CENTER_LEFT);
        Label wcLabel = new Label("Word Count:");
        ComboBox<String> wordCountCombo = new ComboBox<>(FXCollections.observableArrayList("12", "24"));
        wordCountCombo.setValue("12");
        wordCountRow.getChildren().addAll(wcLabel, wordCountCombo);

        TextField mnemonicField = createTextField("Custom mnemonic (optional)");
        TextField passphraseField = createTextField("Passphrase (optional)");

        Button generateBtn = createButton("Generate Master Key");
        TextArea masterResultArea = createResultArea(5);

        generateBtn.setOnAction(e -> {
            generateBtn.setDisable(true);
            masterResultArea.setText("Generating...");
            String mnemonic = mnemonicField.getText().trim();
            String passphrase = passphraseField.getText().trim();
            int wc = Integer.parseInt(wordCountCombo.getValue());

            runBackground(() -> {
                try {
                    MasterKeyResult result = bip32Service.generateMasterKey(
                            mnemonic.isEmpty() ? null : mnemonic, wc,
                            passphrase.isEmpty() ? "" : passphrase);
                    Platform.runLater(() -> {
                        lastMasterKeyResult = result;
                        lastPassphrase = passphrase;
                        masterResultArea.setText(
                                "Mnemonic: " + result.mnemonic() + "\n"
                                + "Master Private Key (xprv): " + result.masterPrivateKey() + "\n"
                                + "Master Public Key (xpub): " + result.masterPublicKey() + "\n"
                                + "Seed: " + result.seed());
                        generateBtn.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        masterResultArea.setText("Error: " + ex.getMessage());
                        generateBtn.setDisable(false);
                    });
                }
            });
        });

        genCard.getChildren().addAll(genTitle, wordCountRow, mnemonicField, passphraseField, generateBtn, masterResultArea);

        // --- Derive Single Key Section ---
        VBox deriveCard = createCard();
        Label deriveTitle = new Label("Derive Child Key");
        deriveTitle.setStyle(SECTION_TITLE_STYLE);

        TextField masterKeyField = createTextField("Master key (xprv...)");
        masterKeyField.setFont(MONO_FONT_SMALL);
        TextField pathField = createTextField("Derivation path");
        pathField.setText("m/44'/0'/0'/0/0");

        Button deriveBtn = createButton("Derive Key");
        TextArea deriveResultArea = createResultArea(4);

        deriveBtn.setOnAction(e -> {
            deriveBtn.setDisable(true);
            deriveResultArea.setText("Deriving...");
            String mk = masterKeyField.getText().trim();
            String path = pathField.getText().trim();

            runBackground(() -> {
                try {
                    DerivedKeyResult result = bip32Service.deriveChildKey(mk, path);
                    Platform.runLater(() -> {
                        lastDerivedKeyResult = result;
                        deriveResultArea.setText(
                                "Path: " + result.path() + "\n"
                                + "Address: " + result.address() + "\n"
                                + "Private Key (WIF): " + result.privateKey() + "\n"
                                + "Public Key: " + result.publicKey());
                        deriveBtn.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        deriveResultArea.setText("Error: " + ex.getMessage());
                        deriveBtn.setDisable(false);
                    });
                }
            });
        });

        deriveCard.getChildren().addAll(deriveTitle, masterKeyField, pathField, deriveBtn, deriveResultArea);

        // --- Derive Multiple Section ---
        VBox multiCard = createCard();
        Label multiTitle = new Label("Derive Multiple Addresses");
        multiTitle.setStyle(SECTION_TITLE_STYLE);

        TextField multiMasterField = createTextField("Master key (xprv...)");
        multiMasterField.setFont(MONO_FONT_SMALL);
        TextField patternField = createTextField("Path pattern (use * for index)");
        patternField.setText("m/44'/0'/0'/0/*");

        HBox countRow = new HBox(10);
        countRow.setAlignment(Pos.CENTER_LEFT);
        Label countLabel = new Label("Count:");
        Spinner<Integer> countSpinner = new Spinner<>(1, 100, 5);
        countSpinner.setEditable(true);
        countSpinner.setPrefWidth(100);
        countRow.getChildren().addAll(countLabel, countSpinner);

        Button multiBtn = createButton("Derive Multiple");

        // TableView for multiple addresses
        TableView<DerivedAddressResult> addressTable = new TableView<>();
        addressTable.setPrefHeight(200);
        addressTable.setStyle("-fx-font-family: 'Monospace'; -fx-font-size: 11;");

        TableColumn<DerivedAddressResult, String> idxCol = new TableColumn<>("#");
        idxCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().index())));
        idxCol.setPrefWidth(40);

        TableColumn<DerivedAddressResult, String> pathCol = new TableColumn<>("Path");
        pathCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().path()));
        pathCol.setPrefWidth(160);

        TableColumn<DerivedAddressResult, String> addrCol = new TableColumn<>("Address");
        addrCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().address()));
        addrCol.setPrefWidth(300);

        TableColumn<DerivedAddressResult, String> privCol = new TableColumn<>("Private Key (WIF)");
        privCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().privateKey()));
        privCol.setPrefWidth(360);

        addressTable.getColumns().addAll(List.of(idxCol, pathCol, addrCol, privCol));

        multiBtn.setOnAction(e -> {
            multiBtn.setDisable(true);
            String mk = multiMasterField.getText().trim();
            String pattern = patternField.getText().trim();
            int count = countSpinner.getValue();

            runBackground(() -> {
                try {
                    DerivedMultipleResult result = bip32Service.deriveMultipleAddresses(mk, pattern, count);
                    Platform.runLater(() -> {
                        ObservableList<DerivedAddressResult> items = FXCollections.observableArrayList(result.addresses());
                        addressTable.setItems(items);
                        multiBtn.setDisable(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        addressTable.setItems(FXCollections.observableArrayList());
                        showAlert("Error", ex.getMessage());
                        multiBtn.setDisable(false);
                    });
                }
            });
        });

        multiCard.getChildren().addAll(multiTitle, multiMasterField, patternField, countRow, multiBtn, addressTable);

        // --- Validate Mnemonic Section ---
        VBox validateCard = createCard();
        Label validateTitle = new Label("Validate Mnemonic");
        validateTitle.setStyle(SECTION_TITLE_STYLE);

        TextField validateField = createTextField("Enter mnemonic to validate");
        Button validateBtn = createButton("Validate Mnemonic");
        Label validateResult = new Label();
        validateResult.setWrapText(true);

        validateBtn.setOnAction(e -> {
            String mnemonic = validateField.getText().trim();
            if (mnemonic.isEmpty()) {
                validateResult.setText("Please enter a mnemonic.");
                validateResult.setStyle("-fx-text-fill: #dc2626;");
                return;
            }
            MnemonicValidationResult result = bip32Service.validateMnemonic(mnemonic);
            if (result.valid()) {
                validateResult.setText("Valid mnemonic (" + result.wordCount() + " words)");
                validateResult.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
            } else {
                validateResult.setText("Invalid: " + result.error());
                validateResult.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            }
        });

        validateCard.getChildren().addAll(validateTitle, validateField, validateBtn, validateResult);

        root.getChildren().addAll(genCard, deriveCard, multiCard, validateCard);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: " + BG_COLOR + ";");
        return scrollPane;
    }

    // ======================== BIP38 TAB ========================

    private ScrollPane createBip38Tab() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + BG_COLOR + ";");

        // --- Encrypt Section ---
        VBox encryptCard = createCard();
        Label encTitle = new Label("Encrypt Private Key (BIP38)");
        encTitle.setStyle(SECTION_TITLE_STYLE);

        TextField encPrivKeyField = createTextField("Private key (WIF format)");
        encPrivKeyField.setFont(MONO_FONT_SMALL);
        PasswordField encPasswordField = new PasswordField();
        encPasswordField.setPromptText("Password (min 4 characters)");
        encPasswordField.setStyle("-fx-padding: 8; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #d1d5db;");

        Button encryptBtn = createButton("Encrypt");
        TextArea encResultArea = createResultArea(3);

        ProgressIndicator encProgress = new ProgressIndicator();
        encProgress.setMaxSize(24, 24);
        encProgress.setVisible(false);

        HBox encBtnRow = new HBox(10, encryptBtn, encProgress);
        encBtnRow.setAlignment(Pos.CENTER_LEFT);

        encryptBtn.setOnAction(e -> {
            encryptBtn.setDisable(true);
            encProgress.setVisible(true);
            encResultArea.setText("Encrypting...");
            String privKey = encPrivKeyField.getText().trim();
            String password = encPasswordField.getText();

            runBackground(() -> {
                try {
                    BIP38EncryptResult result = bip38Service.encrypt(privKey, password);
                    Platform.runLater(() -> {
                        encResultArea.setText(
                                "Encrypted Key: " + result.encryptedKey() + "\n"
                                + "Compressed: " + result.compressed());
                        encryptBtn.setDisable(false);
                        encProgress.setVisible(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        encResultArea.setText("Error: " + ex.getMessage());
                        encryptBtn.setDisable(false);
                        encProgress.setVisible(false);
                    });
                }
            });
        });

        encryptCard.getChildren().addAll(encTitle, encPrivKeyField, encPasswordField, encBtnRow, encResultArea);

        // --- Decrypt Section ---
        VBox decryptCard = createCard();
        Label decTitle = new Label("Decrypt Private Key (BIP38)");
        decTitle.setStyle(SECTION_TITLE_STYLE);

        TextField decEncKeyField = createTextField("Encrypted key (6P...)");
        decEncKeyField.setFont(MONO_FONT_SMALL);
        PasswordField decPasswordField = new PasswordField();
        decPasswordField.setPromptText("Password");
        decPasswordField.setStyle("-fx-padding: 8; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #d1d5db;");

        Button decryptBtn = createButton("Decrypt");
        TextArea decResultArea = createResultArea(3);

        ProgressIndicator decProgress = new ProgressIndicator();
        decProgress.setMaxSize(24, 24);
        decProgress.setVisible(false);

        HBox decBtnRow = new HBox(10, decryptBtn, decProgress);
        decBtnRow.setAlignment(Pos.CENTER_LEFT);

        decryptBtn.setOnAction(e -> {
            decryptBtn.setDisable(true);
            decProgress.setVisible(true);
            decResultArea.setText("Decrypting...");
            String encKey = decEncKeyField.getText().trim();
            String password = decPasswordField.getText();

            runBackground(() -> {
                try {
                    BIP38DecryptResult result = bip38Service.decrypt(encKey, password);
                    Platform.runLater(() -> {
                        decResultArea.setText(
                                "Private Key (WIF): " + result.privateKey() + "\n"
                                + "Address: " + result.address() + "\n"
                                + "Compressed: " + result.compressed());
                        decryptBtn.setDisable(false);
                        decProgress.setVisible(false);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        decResultArea.setText("Error: " + ex.getMessage());
                        decryptBtn.setDisable(false);
                        decProgress.setVisible(false);
                    });
                }
            });
        });

        decryptCard.getChildren().addAll(decTitle, decEncKeyField, decPasswordField, decBtnRow, decResultArea);

        root.getChildren().addAll(encryptCard, decryptCard);

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: " + BG_COLOR + ";");
        return scrollPane;
    }

    // ======================== PAPER WALLET TAB ========================

    private VBox createPaperWalletTab() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + BG_COLOR + ";");
        root.setAlignment(Pos.TOP_CENTER);

        VBox card = createCard();
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(600);

        Label title = new Label("Paper Wallet Generator");
        title.setStyle(SECTION_TITLE_STYLE);

        Label description = new Label(
                "Generate a Paper Wallet PDF using data from the BIP32 tab.\n"
                + "First generate a master key and derive an address in Tab 1,\n"
                + "then come here to save the PDF.");
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: #64748b; -fx-padding: 0 0 10 0;");

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);

        Button generatePdfBtn = createButton("Generate & Save PDF");

        generatePdfBtn.setOnAction(e -> {
            if (lastMasterKeyResult == null) {
                statusLabel.setText("No master key generated yet. Go to BIP32 tab first.");
                statusLabel.setStyle("-fx-text-fill: #dc2626;");
                return;
            }
            if (lastDerivedKeyResult == null) {
                statusLabel.setText("No derived key yet. Derive a key in BIP32 tab first.");
                statusLabel.setStyle("-fx-text-fill: #dc2626;");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Paper Wallet PDF");
            fileChooser.setInitialFileName("paper-wallet.pdf");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File file = fileChooser.showSaveDialog(generatePdfBtn.getScene().getWindow());

            if (file != null) {
                generatePdfBtn.setDisable(true);
                statusLabel.setText("Generating PDF...");
                statusLabel.setStyle("-fx-text-fill: #64748b;");

                runBackground(() -> {
                    try {
                        byte[] pdf = paperWalletService.generatePdf(
                                lastMasterKeyResult.mnemonic(),
                                lastDerivedKeyResult.address(),
                                lastDerivedKeyResult.privateKey(),
                                lastDerivedKeyResult.path(),
                                lastDerivedKeyResult.publicKey(),
                                lastPassphrase,
                                lastMasterKeyResult.masterPrivateKey(),
                                lastMasterKeyResult.masterPublicKey());

                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            fos.write(pdf);
                        }

                        Platform.runLater(() -> {
                            statusLabel.setText("PDF saved to: " + file.getAbsolutePath());
                            statusLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                            generatePdfBtn.setDisable(false);
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Error: " + ex.getMessage());
                            statusLabel.setStyle("-fx-text-fill: #dc2626;");
                            generatePdfBtn.setDisable(false);
                        });
                    }
                });
            }
        });

        card.getChildren().addAll(title, description, generatePdfBtn, statusLabel);
        root.getChildren().add(card);
        return root;
    }

    // ======================== HELPERS ========================

    private VBox createCard() {
        VBox card = new VBox(10);
        card.setStyle(CARD_STYLE);
        return card;
    }

    private TextField createTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-padding: 8; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #d1d5db;");
        return tf;
    }

    private TextArea createResultArea(int rows) {
        TextArea ta = new TextArea();
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(rows);
        ta.setFont(MONO_FONT_SMALL);
        ta.setStyle("-fx-control-inner-background: #f1f5f9; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #e2e8f0;");
        return ta;
    }

    private Button createButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(BUTTON_STYLE);
        btn.setOnMouseEntered(e -> btn.setStyle(BUTTON_HOVER_STYLE));
        btn.setOnMouseExited(e -> btn.setStyle(BUTTON_STYLE));
        return btn;
    }

    private void runBackground(Runnable task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
