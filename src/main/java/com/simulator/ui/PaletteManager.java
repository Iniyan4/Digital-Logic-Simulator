package com.simulator.ui;

import com.simulator.data.CircuitPersistence;
import com.simulator.data.TemplateManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class PaletteManager {
    private final VBox palettePane;
    private final SimulatorApp app;
    private final TemplateManager templateManager;
    private final CircuitPersistence circuitPersistence;
    private final Stage mainStage;

    public PaletteManager(SimulatorApp app, TemplateManager templateManager, CircuitPersistence circuitPersistence, Stage mainStage) {
        this.app = app;
        this.templateManager = templateManager;
        this.circuitPersistence = circuitPersistence;
        this.mainStage = mainStage;

        this.palettePane = new VBox(10);
        this.palettePane.setPadding(new Insets(10));
        this.palettePane.setStyle("-fx-background-color: #E0E0E0;");
        this.palettePane.setMinWidth(160); // Slightly wider to fit side-by-side buttons
        this.palettePane.setAlignment(Pos.TOP_CENTER); // Center align contents
    }

    public VBox getPalettePane() {
        return palettePane;
    }

    public void reloadPalette() {
        palettePane.getChildren().clear();

        //SECTION 1: TOOLS & ACTIONS (TOP)

        // 1. Navigation (Back Button)
        Button backBtn = new Button("← Menu");
        styleButton(backBtn, "-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold;");
        backBtn.setOnAction(e -> app.showWelcomeScreen());
        palettePane.getChildren().add(backBtn);

        // 2. File Operations (Save | Load) - Side by Side
        HBox fileOps = new HBox(5); // 5px spacing
        Button saveBtn = createSmallButton("Save", () -> circuitPersistence.saveCircuit(mainStage));
        Button loadBtn = createSmallButton("Load", () -> circuitPersistence.loadCircuit(mainStage));
        fileOps.getChildren().addAll(saveBtn, loadBtn);
        palettePane.getChildren().add(fileOps);

        // 3. Circuit Tools (Bundle | Table) - Side by Side
        HBox tools = new HBox(5);
        Button bundleBtn = createSmallButton("Bundle", () -> {
            templateManager.bundleSelectedCircuit(app.getSelectedItems(), app.getRootPane());
            reloadPalette();
        });
        Button tableBtn = createSmallButton("Table", app::generateTruthTable);
        tools.getChildren().addAll(bundleBtn, tableBtn);
        palettePane.getChildren().add(tools);

        // 4. Clear (Full Width)
        Button clearBtn = new Button("Clear Workspace");
        styleButton(clearBtn, "-fx-base: #ffcccc;"); // Light red tint
        clearBtn.setOnAction(e -> app.clearWorkspace());
        palettePane.getChildren().add(clearBtn);

        palettePane.getChildren().add(new Separator());

        //SECTION 2: COMPONENTS (BOTTOM)

        Label title = new Label("Components");
        title.setFont(new Font("Arial", 16));
        title.setAlignment(Pos.CENTER);
        // Center the label in the VBox
        title.setMaxWidth(Double.MAX_VALUE);
        palettePane.getChildren().add(title);

        // Standard Gates
        palettePane.getChildren().addAll(
                new PaletteIcon("Switch", "SWITCH"),
                new PaletteIcon("Clock", "CLOCK_GATE"),
                new PaletteIcon("Probe", "PROBE"),
                new PaletteIcon("AND", "AND_GATE"),
                new PaletteIcon("OR", "OR_GATE"),
                new PaletteIcon("NOT", "NOT_GATE"),
                new PaletteIcon("NAND", "NAND_GATE"),
                new PaletteIcon("NOR", "NOR_GATE"),
                new PaletteIcon("XOR", "XOR_GATE"),
                new PaletteIcon("XNOR", "XNOR_GATE"),
                new PaletteIcon("D Flip-Flop", "D_FLIP_FLOP")
        );

        // Custom Gates
        if (!templateManager.getAllTemplates().isEmpty()) {
            palettePane.getChildren().add(new Separator());

            Label customTitle = new Label("Custom Gates");
            customTitle.setFont(new Font("Arial", 14));
            customTitle.setAlignment(Pos.CENTER);
            customTitle.setMaxWidth(Double.MAX_VALUE);
            palettePane.getChildren().add(customTitle);

            for (String gateName : templateManager.getAllTemplates().keySet()) {
                palettePane.getChildren().add(
                        new PaletteIcon(gateName, "CUSTOM:" + gateName, true)
                );
            }
        }

        // Inside PaletteManager.java -> reloadPalette()
        palettePane.getChildren().add(new Separator());

        Label speedLabel = new Label("Clock Frequency: 1.0 Hz");
        speedLabel.setStyle("-fx-font-weight: bold;");

        javafx.scene.control.Slider speedSlider = new javafx.scene.control.Slider(0.2, 10.0, 1.0);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(2.0);

        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double hz = newVal.doubleValue();
            speedLabel.setText(String.format("Clock Frequency: %.1f Hz", hz));
            app.setSimulationFrequency(hz); // Send update command to application wrapper
        });

        VBox speedContainer = new VBox(5, speedLabel, speedSlider);
        speedContainer.setPadding(new Insets(5, 0, 5, 0));
        palettePane.getChildren().add(speedContainer);
    }

    // Helper to create side-by-side buttons
    private Button createSmallButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btn, Priority.ALWAYS); // Grow to fill space
        btn.setOnAction(e -> action.run());
        return btn;
    }

    // Helper to style full-width buttons
    private void styleButton(Button btn, String style) {
        btn.setMaxWidth(Double.MAX_VALUE);
        if (style != null) {
            btn.setStyle(style);
        }
    }
}