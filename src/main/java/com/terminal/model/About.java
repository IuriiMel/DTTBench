package com.terminal.model;

import com.terminal.Main;
import com.terminal.config.BuildInfo;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class About {

    private static final DateTimeFormatter BUILD_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void showAboutDialog() {
        String version = BuildInfo.getVersion();
        String buildTime = formatBuildTime(BuildInfo.getBuildTime());
        boolean isNative = System.getProperty("org.graalvm.nativeimage.imagecode") != null;

        String javaVersion = System.getProperty("java.version");
        String vmName = System.getProperty("java.vm.name");
        String os = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String arch = System.getProperty("os.arch");
        String author = " https://github.com/IuriiMel";
        String copyright = buildTime.substring(0, 4);

        String runtimeInfo = """
                Version: %s
                Build time: %s

                Java: %s
                VM: %s
                Mode: %s

                OS: %s (%s)
                Arch: %s

                Launch Time: %s
                
                © %s
                """.formatted(
                version,
                buildTime,
                javaVersion,
                vmName,
                isNative ? "Native Image" : "JVM",
                os,
                osVersion,
                arch,
                Main.getLaunchTime(),
                copyright + author
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("DTTBench");
        alert.setGraphic(null);
        alert.getDialogPane().setStyle( "-fx-font-family: 'Consolas';" + "-fx-font-size: 12px;");

        Label desc = new Label("A toolkit for test automation.");
        desc.setWrapText(true);

        VBox mainBox = new VBox(8);
        mainBox.setPadding(new Insets(10));
        mainBox.getChildren().addAll(
                new Label("DTTBench"),
                desc,
                new Separator(),
                new Label("Version: " + version),
                new Label("Build: " + buildTime)
        );

        TextArea runtimeArea = new TextArea(runtimeInfo);
        runtimeArea.setFont(Font.font("Consolas", 12));
        runtimeArea.setEditable(false);
        runtimeArea.setWrapText(false);
        runtimeArea.setPrefRowCount(12);

        VBox expandable = new VBox(runtimeArea);
        expandable.setPadding(new Insets(10));
        expandable.setFillWidth(true);

        alert.getDialogPane().setContent(mainBox);
        alert.getDialogPane().setExpandableContent(expandable);
        alert.getDialogPane().setExpanded(false);

        alert.setOnShown(e -> {
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(Main.getIco());
        });

        alert.showAndWait();
    }

    public static String formatBuildTime(String raw) {
        if (raw.contains("T")) {
            return Instant.parse(raw).atZone(ZoneId.systemDefault()).format(BUILD_TIME_FORMAT);
        }
        return LocalDateTime.parse(raw, BUILD_TIME_FORMAT).format(BUILD_TIME_FORMAT);
    }
}
