package com.terminal.plotter;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineHelper;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngWriter;
import com.terminal.model.AlertService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Plotter {

    public enum TimeMode { ABSOLUTE, RELATIVE }

    private final Map<String, List<Long>> timestampsMap = new LinkedHashMap<>();
    private final Map<String, List<Double>> valuesMap = new LinkedHashMap<>();
    private final Map<String, XYChart.Series<Number, Number>> seriesMap = new LinkedHashMap<>();
    private final Map<XYChart.Data<Number, Number>, Tooltip> tooltipMap = new LinkedHashMap<>();
    private Map<String, String> unitsMap = new LinkedHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(Plotter.class);

    private static final int MAX_POINTS_PER_SERIES = 10000;
    private String title = "Plot";
    private TimeMode timeMode = TimeMode.RELATIVE;

    private static final String ICON_PATH = "/com/terminal/images/ico.png";

    private Stage stage;
    private LineChart<Number, Number> chart;
    private NumberAxis xAxis;
    private NumberAxis yAxis;
    private Label statusLabel;
    private ToggleButton modeButton;

    // Zoom bounds
    private double xLowerBound, xUpperBound;
    private double yLowerBound, yUpperBound;

    // Drag state
    private boolean isDragging = false;
    private double dragStartX, dragStartY;
    private double dragStartXLower, dragStartXUpper;
    private double dragStartYLower, dragStartYUpper;

    // Rubber band
    private Rectangle rubberBand;
    private double rubberBandStartX, rubberBandEndX;
    private double rubberBandStartY, rubberBandEndY;
    private Pane plotArea;

    private long dayStartMs = System.currentTimeMillis();
    private double currentXTickUnit = 1.0;

    private boolean isAutoScaling = true;

    // for ABSOLUTE time mode
    private final StringConverter<Number> absoluteTimeFormatter = new StringConverter<>() {

        private final SimpleDateFormat formatSec = new SimpleDateFormat("HH:mm:ss");
        private final SimpleDateFormat formatMs = new SimpleDateFormat("HH:mm:ss.SSS");

        @Override
        public String toString(Number n) {
            if (n == null) return "";
            long actualTime = dayStartMs + n.longValue();

            if (currentXTickUnit < 500.0) {
                return formatMs.format(new Date(actualTime));
            } else {
                return formatSec.format(new Date(actualTime));
            }
        }
        @Override
        public Number fromString(String s) {
            return 0;
        }
    };

    // === Public API ===
    public void addData(String seriesName, double value, String units) {
        if (units != null && !units.isEmpty()) {
            unitsMap.put(seriesName, units);
        }
        addData(seriesName, System.currentTimeMillis(), value);
    }

    public void addData(double value) {
        addData("default", value, "");
    }

    public void addData(String seriesName, double value) {
        addData(seriesName, value, "");
    }

    private void addData(String seriesName, long timestamp, double value) {
        synchronized (this) {
            List<Long> times = timestampsMap.computeIfAbsent(seriesName, k -> new ArrayList<>());
            List<Double> values = valuesMap.computeIfAbsent(seriesName, k -> new ArrayList<>());
            times.add(timestamp);
            values.add(value);
            if (times.size() > MAX_POINTS_PER_SERIES) {
                times.removeFirst();
                values.removeFirst();
            }
        }
        if (stage != null && stage.isShowing()) {
            Platform.runLater(this::refreshChart);
        }
    }

    public void clear() {
        timestampsMap.clear();
        valuesMap.clear();
        unitsMap.clear();
        tooltipMap.clear();
        xLowerBound = 0;
        xUpperBound = 0;
        yLowerBound = 0;
        yUpperBound = 0;
        if (stage != null && stage.isShowing()) {
            Platform.runLater(() -> {
                chart.getData().clear();
                seriesMap.clear();
                refreshChart();
            });
        }
    }

    public void setTimeMode(TimeMode mode) {
        timeMode = mode;
        xLowerBound = 0;
        xUpperBound = 0;
        yLowerBound = 0;
        yUpperBound = 0;
        if (stage != null && stage.isShowing()) {
            Platform.runLater(this::refreshChart);
        }
    }

    public void setTitle(String title) {
        String t = title;
        if (stage != null && stage.isShowing()) {
            Platform.runLater(() -> stage.setTitle(t));
        }
    }

    public synchronized void plotGraph() {
        if(stage != null){
            stage.toFront();
            return;
        }
        show();
    }

    // === Internal UI ===
    private void show() {
        stage = new Stage();
        stage.setTitle(title);

        stage.setOnCloseRequest(event -> {
            stage = null;
            chart = null;
            xAxis = null;
            yAxis = null;
            plotArea = null;
            statusLabel = null;
            modeButton = null;
            rubberBand = null;
            seriesMap.clear();
            isDragging = false;
        });

        try {
            var iconStream = Plotter.class.getResourceAsStream(ICON_PATH);
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            } else {
                File iconFile = new File(ICON_PATH);
                if (iconFile.exists()) {
                    stage.getIcons().add(new Image(iconFile.toURI().toString()));
                }
            }
        }
        catch (Exception e) {
            String message = "Failed to load ico";
            log.error(message, e);
        }

        BorderPane root = new BorderPane();
        root.setTop(buildToolbar());
        buildChart();
        root.setCenter(chart);

        statusLabel = new Label("Points: 0");
        statusLabel.setPadding(new Insets(5));
        HBox statusBox = new HBox(statusLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        root.setBottom(statusBox);

        Scene scene = new Scene(root, 1100, 650);
        stage.setScene(scene);
        stage.show();

        refreshChart();
    }

    private ToolBar buildToolbar() {
        modeButton = new ToggleButton(timeMode == TimeMode.ABSOLUTE ? "Absolute" : "Relative");
        modeButton.setSelected(timeMode == TimeMode.ABSOLUTE);
        modeButton.setOnAction(e -> {
            timeMode = modeButton.isSelected() ? TimeMode.ABSOLUTE : TimeMode.RELATIVE;
            modeButton.setText(timeMode == TimeMode.ABSOLUTE ? "Absolute" : "Relative");
            setTimeMode(timeMode);
        });

        Button zoomIn = new Button("Zoom +");
        zoomIn.setOnAction(e -> zoom(0.5, 0.5));

        Button zoomOut = new Button("Zoom −");
        zoomOut.setOnAction(e -> zoom(2.0, 2.0));

        Button resetZoom = new Button("Reset");
        resetZoom.setOnAction(e -> resetZoom());

        Button exportPng = new Button("PNG");
        exportPng.setOnAction(e -> exportPng());

        Button exportCsv = new Button("CSV");
        exportCsv.setOnAction(e -> exportCsv());

        return new ToolBar(modeButton, new Separator(), zoomIn, zoomOut, resetZoom, new Separator(), exportPng, exportCsv, new Separator());
    }

    private void buildChart() {
        xAxis = new NumberAxis();
        yAxis = new NumberAxis();
        String unit = unitsMap.values().stream().findFirst().orElse("");
        yAxis.setLabel(unit);
        yAxis.setForceZeroInRange(false);

        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setLegendVisible(true);
        chart.setVerticalGridLinesVisible(true);
        chart.setHorizontalGridLinesVisible(true);

        chart.setStyle(".chart-symbol { -fx-padding: 3px; -fx-background-radius: 3px; }");

        chart.setOnScroll((ScrollEvent e) -> {
            isAutoScaling = false;
            double factor = e.getDeltaY() > 0 ? 0.8 : 1.25;
            zoom(factor, factor, e.getX(), e.getY());
        });
        chart.setOnMousePressed((MouseEvent e) -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                isAutoScaling = false;
                isDragging = true;
                dragStartX = e.getX();
                dragStartY = e.getY();
                dragStartXLower = xLowerBound;
                dragStartXUpper = xUpperBound;
                dragStartYLower = yLowerBound;
                dragStartYUpper = yUpperBound;
            } else if (e.getButton() == MouseButton.PRIMARY) {
                rubberBandStartX = e.getX();
                rubberBandStartY = e.getY();

                if (plotArea == null) {
                    plotArea = (Pane) chart.lookup(".chart-plot-background").getParent();
                }
                if (rubberBand == null) {
                    rubberBand = new Rectangle();
                    rubberBand.setFill(Color.color(0, 0, 1, 0.08));
                    rubberBand.setStroke(Color.BLUE);
                    rubberBand.setStrokeWidth(1.5);
                    rubberBand.setMouseTransparent(true);
                    plotArea.getChildren().add(rubberBand);
                }

                double px = plotArea.sceneToLocal(chart.localToScene(e.getX(), 0)).getX();
                double py = plotArea.sceneToLocal(chart.localToScene(0, e.getY())).getY();
                rubberBand.setX(px);
                rubberBand.setY(py);
                rubberBand.setWidth(0);
                rubberBand.setHeight(0);
                rubberBand.setVisible(true);
            }
        });
        chart.setOnMouseDragged((MouseEvent e) -> {
            if (isDragging) {
                double dx = e.getX() - dragStartX;
                double dy = e.getY() - dragStartY;

                double plotWidth = chart.getWidth() - chart.getYAxis().getWidth()
                        - chart.getPadding().getLeft() - chart.getPadding().getRight();
                double plotHeight = chart.getHeight() - chart.getXAxis().getHeight()
                        - chart.getPadding().getTop() - chart.getPadding().getBottom();

                if (plotWidth > 0) {
                    double xRange = dragStartXUpper - dragStartXLower;
                    xLowerBound = dragStartXLower - dx * xRange / plotWidth;
                    xUpperBound = dragStartXUpper - dx * xRange / plotWidth;
                }

                if (plotHeight > 0) {
                    double yRange = dragStartYUpper - dragStartYLower;
                    yLowerBound = dragStartYLower + dy * yRange / plotHeight;
                    yUpperBound = dragStartYUpper + dy * yRange / plotHeight;
                }

                applyBounds();
            } else if (rubberBand != null && rubberBand.isVisible()) {
                rubberBandEndX = e.getX();
                rubberBandEndY = e.getY();

                double psx = plotArea.sceneToLocal(chart.localToScene(rubberBandStartX, 0)).getX();
                double pex = plotArea.sceneToLocal(chart.localToScene(rubberBandEndX, 0)).getX();
                double psy = plotArea.sceneToLocal(chart.localToScene(0, rubberBandStartY)).getY();
                double pey = plotArea.sceneToLocal(chart.localToScene(0, rubberBandEndY)).getY();

                rubberBand.setX(Math.min(psx, pex));
                rubberBand.setY(Math.min(psy, pey));
                rubberBand.setWidth(Math.abs(pex - psx));
                rubberBand.setHeight(Math.abs(pey - psy));
            }
        });
        chart.setOnMouseReleased((MouseEvent e) -> {
            if (isDragging) {
                isDragging = false;
                chart.setCursor(Cursor.DEFAULT);
            } else if (rubberBand != null && rubberBand.isVisible()) {
                isAutoScaling = false;
                rubberBand.setVisible(false);
                if (rubberBandEndX > rubberBandStartX) {
                    // from left to right → zoom
                    double x1 = Math.min(rubberBandStartX, rubberBandEndX);
                    double x2 = Math.max(rubberBandStartX, rubberBandEndX);
                    double y1 = Math.min(rubberBandStartY, rubberBandEndY);
                    double y2 = Math.max(rubberBandStartY, rubberBandEndY);

                    if (Math.abs(x2 - x1) > 10 && Math.abs(y2 - y1) > 10) {
                        double plotWidth = chart.getWidth() - chart.getYAxis().getWidth()
                                - chart.getPadding().getLeft() - chart.getPadding().getRight();
                        double plotHeight = chart.getHeight() - chart.getXAxis().getHeight()
                                - chart.getPadding().getTop() - chart.getPadding().getBottom();

                        double xRange = xUpperBound - xLowerBound;
                        double yRange = yUpperBound - yLowerBound;

                        double xr1 = Math.max(0, Math.min(1, (x1 - chart.getYAxis().getWidth()) / plotWidth));
                        double xr2 = Math.max(0, Math.min(1, (x2 - chart.getYAxis().getWidth()) / plotWidth));
                        double yr1 = Math.max(0, Math.min(1, (y1 - chart.getPadding().getTop()) / plotHeight));
                        double yr2 = Math.max(0, Math.min(1, (y2 - chart.getPadding().getTop()) / plotHeight));

                        double newXLower = xLowerBound + xRange * xr1;
                        xUpperBound = xLowerBound + xRange * xr2;
                        xLowerBound = newXLower;

                        // Y inverse
                        double newYUpper = yUpperBound - yRange * yr1;
                        yLowerBound = yUpperBound - yRange * yr2;
                        yUpperBound = newYUpper;

                        applyBounds();
                    }
                } else {
                    // from right to left → reset
                    resetZoom();
                }
            }
        });
    }

    private synchronized void refreshChart() {
        if (valuesMap.isEmpty() || valuesMap.values().stream().allMatch(List::isEmpty)) {
            chart.getData().clear();
            seriesMap.clear();
            statusLabel.setText("Points: 0");
            return;
        }
        // 1. Get the beginning of the day
        long firstTs = timestampsMap.values().stream().filter(list -> !list.isEmpty()).mapToLong(list -> list.getFirst())
                .min().orElse(System.currentTimeMillis());
        this.dayStartMs = java.time.LocalDate.ofInstant(java.time.Instant.ofEpochMilli(firstTs), java.time.ZoneId.systemDefault())
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        // === Data ===
        chart.getData().clear();
        seriesMap.clear();

        double xMin = Double.MAX_VALUE, xMax = -Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE, yMax = -Double.MAX_VALUE;
        int totalPoints = 0;

        for (String seriesName : valuesMap.keySet()) {
            List<Long> timestamps = timestampsMap.get(seriesName);
            List<Double> values = valuesMap.get(seriesName);
            if (timestamps == null || values == null || timestamps.isEmpty()) continue;

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(seriesName);

            long startTs = timestamps.get(0);

            for (int i = 0; i < values.size(); i++) {
                long ts = timestamps.get(i);
                double x = timeMode == TimeMode.ABSOLUTE ? (ts - dayStartMs) : (ts - startTs) / 1000.0;
                double y = values.get(i);

                XYChart.Data<Number, Number> data = new XYChart.Data<>(x, y);
                series.getData().add(data);

                if (x < xMin) xMin = x;
                if (x > xMax) xMax = x;
                if (y < yMin) yMin = y;
                if (y > yMax) yMax = y;
            }
            seriesMap.put(seriesName, series);
            chart.getData().add(series);
            totalPoints += values.size();
        }

        if (isAutoScaling) {
            double minVisibleRange = timeMode == TimeMode.ABSOLUTE ? 5000.0 : 5.0;

            double xPad = Math.max((xMax - xMin) * 0.1, minVisibleRange * 0.1);
            xLowerBound = Math.max(0, xMin - xPad);
            xUpperBound = Math.max(xLowerBound + minVisibleRange, xMax + xPad);

            double yPad = Math.max((yMax - yMin) * 0.05, 1);
            yLowerBound = yMin - yPad;
            yUpperBound = yMax + yPad;
        }

        double xRange = Math.max(0, xUpperBound - xLowerBound);
        this.currentXTickUnit = calculateTickUnit(xRange, timeMode == TimeMode.ABSOLUTE);

        if (timeMode == TimeMode.ABSOLUTE) {
            xAxis.setLabel("Time (Today)");
            xAxis.setTickLabelFormatter(absoluteTimeFormatter);
        } else {
            xAxis.setLabel("Time (s)");
            xAxis.setTickLabelFormatter(new StringConverter<>() {
                @Override
                public String toString(Number n) {
                    if (n == null) return "";
                    return formatRelativeTime(n.doubleValue(), currentXTickUnit);
                }
                @Override
                public Number fromString(String s) { return 0; }
            });
        }

        String unit = unitsMap.values().size() == 1 ? unitsMap.values().stream().findFirst().orElse("") : "";
        yAxis.setLabel(unit);
        yAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number n) {
                if (n == null) return "";
                double v = n.doubleValue();
                if (unit.isEmpty()) {
                    return formatDouble(v);
                }
                return formatDouble(v) + " " + unit;
            }
            @Override
            public Number fromString(String s) { return 0; }
        });

        applyBounds();
        attachTooltips(new SimpleDateFormat("HH:mm:ss.SSS"));

        statusLabel.setText("Points: " + (timestampsMap.values().stream().mapToInt(List::size).sum()) +
                " | Mode: " + timeMode +
                " | X: " + formatDouble(xUpperBound - xLowerBound) +
                " | Y: " + formatDouble(yUpperBound - yLowerBound));
    }

    private void attachTooltips(SimpleDateFormat absFmt) {
        Platform.runLater(() -> {
            for (XYChart.Series<Number, Number> series : chart.getData()) {
                String seriesName = series.getName();
                String unit = unitsMap.getOrDefault(seriesName, "");
                for (XYChart.Data<Number, Number> data : series.getData()) {
                    if (data.getNode() == null) continue;

                    double x = data.getXValue().doubleValue();
                    double y = data.getYValue().doubleValue();

                    String xStr = timeMode == TimeMode.ABSOLUTE
                            ? absFmt.format(new Date((long) x))
                            : String.format("%.3f s", x);
                    String yStr = unit.isEmpty()
                            ? formatDouble(y)
                            : formatDouble(y) + " " + unit;

                    tooltipMap.computeIfAbsent(data, d -> {
                        Tooltip tooltip = new Tooltip(series.getName() + "\nX: " + xStr + "\nY: " + yStr);
                        tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: #333; -fx-text-fill: white;");
                        tooltip.setShowDelay(new Duration(300));
                        tooltip.setHideDelay(new Duration(300));
                        d.getNode().setOnMouseEntered(ev ->
                                d.getNode().setStyle("-fx-background-color: red, white; -fx-background-insets: 0, 2; -fx-padding: 5px;")
                        );
                        d.getNode().setOnMouseExited(ev ->
                                d.getNode().setStyle("")
                        );
                        Tooltip.install(d.getNode(), tooltip);
                        return tooltip;
                    });

                }
            }
        });
    }

    private void applyBounds() {
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(xLowerBound);
        xAxis.setUpperBound(xUpperBound);

        double xRange = Math.max(0, xUpperBound - xLowerBound);
        currentXTickUnit = calculateTickUnit(xRange, timeMode == TimeMode.ABSOLUTE);
        xAxis.setTickUnit(currentXTickUnit);

        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(yLowerBound);
        yAxis.setUpperBound(yUpperBound);
    }

    private double calculateTickUnit(double range, boolean isAbsolute) {
        if (range <= 0) return isAbsolute ? 1000.0 : 1.0;

        double targetUnit = range / 10.0;
        double minSafeUnit = range / 20.0;

        if (isAbsolute) {
            double[] niceUnits = {
                    10, 50, 100, 500,
                    1000, 5000, 10000, 30000,
                    60000, 300000, 600000, 1800000,
                    3600000, 14400000, 86400000
            };
            for (double unit : niceUnits) {
                if (unit >= targetUnit) return Math.max(unit, minSafeUnit);
            }
            return Math.max(86400000.0, minSafeUnit);
        } else {
            double[] niceUnits = {
                    0.01, 0.02, 0.05, 0.1, 0.2, 0.5,
                    1, 2, 5, 10, 15, 30,
                    60, 120, 300, 600, 1800,
                    3600, 7200, 14400, 28800, 86400
            };
            for (double unit : niceUnits) {
                if (unit >= targetUnit) return Math.max(unit, minSafeUnit);
            }
            return Math.max(86400.0, minSafeUnit);
        }
    }

    private static String formatDouble(double v) {
        if (v == (long) v) return String.valueOf((long) v);
        return String.format(Locale.US, "%.4f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private String formatRelativeTime(double value, double tickUnit) {
        if (value == (long) value && tickUnit >= 1.0) {
            return String.valueOf((long) value);
        }

        int decimals;
        if (tickUnit >= 10.0)      decimals = 0;
        else if (tickUnit >= 1.0)  decimals = 1;
        else if (tickUnit >= 0.1)  decimals = 2;
        else if (tickUnit >= 0.01) decimals = 3;
        else                       decimals = 4;

        String formatted = String.format(java.util.Locale.US, "%." + decimals + "f", value);

        return formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private void zoom(double xFactor, double yFactor) {
        zoom(xFactor, yFactor, chart.getWidth() / 2.0, chart.getHeight() / 2.0);
    }

    private void zoom(double xFactor, double yFactor, double pivotScreenX, double pivotScreenY) {
        double plotWidth = chart.getWidth() - chart.getYAxis().getWidth() - chart.getPadding().getLeft() - chart.getPadding().getRight();
        double plotHeight = chart.getHeight() - chart.getXAxis().getHeight() - chart.getPadding().getTop() - chart.getPadding().getBottom();
        double xRange = xUpperBound - xLowerBound;
        double newXRange = xRange * xFactor;
        double xPivot = plotWidth > 0 ? Math.max(0, Math.min(1, (pivotScreenX - chart.getYAxis().getWidth()) / plotWidth)) : 0.5;
        double xPivotData = xLowerBound + xRange * xPivot;
        xLowerBound = xPivotData - newXRange * xPivot;
        xUpperBound = xPivotData + newXRange * (1 - xPivot);
        double yRange = yUpperBound - yLowerBound;
        double newYRange = yRange * yFactor;
        double yPivot = plotHeight > 0 ? Math.max(0, Math.min(1, (pivotScreenY - chart.getPadding().getTop()) / plotHeight)) : 0.5;
        double yPivotData = yUpperBound - yRange * yPivot;
        yLowerBound = yPivotData - newYRange * (1 - yPivot);
        yUpperBound = yPivotData + newYRange * yPivot;

        applyBounds();
    }

    private void resetZoom() {
        isAutoScaling = true;
        xLowerBound = 0;
        xUpperBound = 0;
        yLowerBound = 0;
        yUpperBound = 0;
        refreshChart();
    }

    private void exportPng() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save as PNG");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
            fileChooser.setInitialFileName("plot.png");
            File file = fileChooser.showSaveDialog(stage);
            if (file == null) return;

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.WHITE);

            WritableImage image = new WritableImage((int) chart.getWidth(), (int) chart.getHeight());

            chart.snapshot(params, image);
            int width = (int) image.getWidth();
            int height = (int) image.getHeight();

            int[] pixels = new int[width * height];
            image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                ImageInfo info = new ImageInfo(width, height, 8, true);
                PngWriter png = new PngWriter(fos, info);

                png.getMetadata().setText("Software", "DTTBench via pngj");
                png.getMetadata().setText("Creation time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

                ImageLineInt line = new ImageLineInt(info);

                for (int y = 0; y < height; y++) {
                    int offset = y * width;
                    for (int x = 0; x < width; x++) {
                        int argb = pixels[offset + x];

                        int a = (argb >> 24) & 0xFF;
                        int r = (argb >> 16) & 0xFF;
                        int g = (argb >> 8) & 0xFF;
                        int b = argb & 0xFF;

                        ImageLineHelper.setPixelRGBA8(line, x, r, g, b, a);
                    }
                    png.writeRow(line);
                }
                png.end();
            }
        } catch (Throwable e) {
            String message = "Unable to save png";
            log.error(message, e);
            AlertService.alertError(e, "Error", message);
        }
    }

    private void exportCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save as CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fc.setInitialFileName("data.csv");
        File f = fc.showSaveDialog(stage);
        if (f == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(f, StandardCharsets.UTF_8))) {
            pw.println("series,timestamp_ms,datetime,value,unit");
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            for (String seriesName : valuesMap.keySet()) {
                List<Long> timestamps = timestampsMap.get(seriesName);
                List<Double> values = valuesMap.get(seriesName);
                if (timestamps == null || values == null) continue;
                String unit = unitsMap.getOrDefault(seriesName, "");
                for (int i = 0; i < values.size(); i++) {
                    long ts = timestamps.get(i);
                    pw.printf("%s,%d,%s,%.6f, %s%n",
                            seriesName, ts, fmt.format(new Date(ts)), values.get(i), unit);
                }
            }
        } catch (IOException e) {
            String message = "Unable to save csv";
            log.error(message, e);
            AlertService.alertError(e, "Error", "Unable to save csv");
        }
    }
}