package de.probst.ba.core.gui;

import de.probst.ba.core.diag.RecordDiagnostic;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.util.IOUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.dialog.Dialogs;

import java.io.File;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 18.08.14.
 */
public class RecordViewer extends Application {

    public static int width = 1300;
    public static int height = 850;
    private List<RecordDiagnostic.Record> rawRecords;
    private List<RecordDiagnostic.Record> filteredRecords;
    private List<PeerId> peers;
    private Map<SocketAddress, Point2D> peerPositions = new HashMap<>();
    private Canvas canvas = new Canvas(width, height);
    private Slider slider = new Slider();
    private CheckBox collectedCheckBox = new CheckBox("Data Info");
    private CheckBox downloadRejectedCheckBox = new CheckBox("Rejected downloads");
    private CheckBox downloadRequestedCheckBox = new CheckBox("Requested downloads");
    private CheckBox downloadProgressedCheckBox = new CheckBox("Progressed downloads");
    private CheckBox downloadStartedCheckBox = new CheckBox("Started downloads");
    private CheckBox downloadSucceededCheckBox = new CheckBox("Succeeded downloads");
    private CheckBox downloadFailedCheckBox = new CheckBox("Failed downloads");
    private CheckBox clearCheckBox = new CheckBox("Clear canvas");

    private void initPeers() {
        peers = rawRecords.stream()
                .map(RecordDiagnostic.Record::getLocalPeerId)
                .distinct()
                .collect(Collectors.toList());

        double target = Math.PI * 2;
        double step = target / peers.size();
        double originX = width / 2;
        double originY = height / 2;
        double angle = 0;

        for (PeerId nextPeerId : peers) {
            double x = originX + Math.sin(angle) * (width - 240) / 2;
            double y = originY + Math.cos(angle) * (height - 120) / 2;

            peerPositions.put(nextPeerId.getAddress(), new Point2D(x, y));
            angle += step;
        }
    }

    private boolean isValidRecord(RecordDiagnostic.Record record) {
        return (record instanceof RecordDiagnostic.CollectedRecord && collectedCheckBox.isSelected()) ||
                (record instanceof RecordDiagnostic.DownloadRejectedRecord && downloadRejectedCheckBox.isSelected()) ||
                (record instanceof RecordDiagnostic.DownloadRequestedRecord && downloadRequestedCheckBox.isSelected()) ||
                (record instanceof RecordDiagnostic.DownloadProgressedRecord && downloadProgressedCheckBox.isSelected()) ||
                (record instanceof RecordDiagnostic.DownloadStartedRecord && downloadStartedCheckBox.isSelected()) ||
                (record instanceof RecordDiagnostic.DownloadSucceededRecord && downloadSucceededCheckBox.isSelected()) ||
                (record instanceof RecordDiagnostic.DownloadFailedRecord && downloadFailedCheckBox.isSelected());
    }

    private void renderArrow(GraphicsContext gc, Point2D a, Point2D b, double offset, double backOff) {
        if (a == null ||
                b == null) {
            return;
        }

        Point2D direction = b.subtract(a);
        Point2D normDirection = direction.normalize();
        double length = direction.magnitude() - backOff - offset;
        Point2D newA = a.add(normDirection.multiply(offset));
        Point2D newB = newA.add(normDirection.multiply(length));

        Point2D ortho = new Point2D(
                -normDirection.getY(),
                normDirection.getX());

        Point2D c = newB.subtract(normDirection.multiply(15));
        Point2D c1 = c.add(ortho.multiply(10));
        Point2D c2 = c.subtract(ortho.multiply(10));

        gc.strokeLine(newA.getX(), newA.getY(),
                newB.getX(), newB.getY());

        gc.strokeLine(newB.getX(), newB.getY(),
                c1.getX(), c1.getY());

        gc.strokeLine(newB.getX(), newB.getY(),
                c2.getX(), c2.getY());
    }

    private void renderCollectedDataInfo(GraphicsContext gc,
                                         RecordDiagnostic.CollectedRecord record) {

        gc.setStroke(Color.GREEN);
        gc.setLineWidth(2);

        Point2D remote = peerPositions.get(record.getRemotePeerId().getAddress());
        Point2D local = peerPositions.get(record.getLocalPeerId().getAddress());
        renderArrow(gc, remote, local, 46, 46);
    }

    private void renderDownloadRequested(GraphicsContext gc,
                                         RecordDiagnostic.DownloadRequestedRecord record) {
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(3);

        Point2D local = peerPositions.get(record.getLocalPeerId().getAddress());
        Point2D remote = peerPositions.get(record.getTransfer().getRemotePeerId().getAddress());
        renderArrow(gc, local, remote, 46, 46);
    }

    private void renderDownloadRejected(GraphicsContext gc,
                                        RecordDiagnostic.DownloadRejectedRecord record) {
        gc.setStroke(Color.RED);
        gc.setLineWidth(3);

        Point2D local = peerPositions.get(record.getLocalPeerId().getAddress());
        Point2D remote = peerPositions.get(record.getTransfer().getRemotePeerId().getAddress());
        renderArrow(gc, remote, local, 46, 46);
    }

    private void renderDownloadStarted(GraphicsContext gc, RecordDiagnostic.DownloadStartedRecord record) {
        gc.setStroke(Color.DARKCYAN);
        gc.setLineWidth(3);

        Point2D local = peerPositions.get(record.getLocalPeerId().getAddress());
        Point2D remote = peerPositions.get(record.getTransfer().getRemotePeerId().getAddress());
        renderArrow(gc, remote, local, 46, 46);
    }

    private void renderDownloadProgressed(GraphicsContext gc, RecordDiagnostic.DownloadProgressedRecord record) {
        gc.setStroke(Color.ORANGE);
        gc.setLineWidth(3);

        Point2D local = peerPositions.get(record.getLocalPeerId().getAddress());
        Point2D remote = peerPositions.get(record.getTransfer().getRemotePeerId().getAddress());
        renderArrow(gc, remote, local, 46, 46);
    }


    private void renderDownloadSucceeded(GraphicsContext gc, RecordDiagnostic.DownloadSucceededRecord record) {
        gc.setStroke(Color.DARKGREEN);
        gc.setLineWidth(6);

        Point2D local = peerPositions.get(record.getLocalPeerId().getAddress());
        Point2D remote = peerPositions.get(record.getTransfer().getRemotePeerId().getAddress());
        renderArrow(gc, remote, local, 46, 52);
        gc.strokeArc(local.getX() - 47, local.getY() - 47, 94, 94, 0, 360, ArcType.OPEN);
    }

    private void clearScreen(GraphicsContext gc) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
    }

    private void renderPeerRecord(int index) {
        if (index < filteredRecords.size()) {
            renderPeers(filteredRecords.get(index));
        }
    }

    private void renderPeers(RecordDiagnostic.Record... records) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        if (clearCheckBox.isSelected()) {
            clearScreen(gc);
        }

        for (RecordDiagnostic.Record record : records) {
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            gc.strokeText(record.getClass().getSimpleName(), 30, 30);

            if (record instanceof RecordDiagnostic.CollectedRecord) {
                renderCollectedDataInfo(gc, (RecordDiagnostic.CollectedRecord) record);
            } else if (record instanceof RecordDiagnostic.DownloadRequestedRecord) {
                renderDownloadRequested(gc, (RecordDiagnostic.DownloadRequestedRecord) record);
            } else if (record instanceof RecordDiagnostic.DownloadStartedRecord) {
                renderDownloadStarted(gc, (RecordDiagnostic.DownloadStartedRecord) record);
            } else if (record instanceof RecordDiagnostic.DownloadSucceededRecord) {
                renderDownloadSucceeded(gc, (RecordDiagnostic.DownloadSucceededRecord) record);
            } else if (record instanceof RecordDiagnostic.DownloadRejectedRecord) {
                renderDownloadRejected(gc, (RecordDiagnostic.DownloadRejectedRecord) record);
            } else if (record instanceof RecordDiagnostic.DownloadProgressedRecord) {
                renderDownloadProgressed(gc, (RecordDiagnostic.DownloadProgressedRecord) record);
            }
        }

        for (Map.Entry<SocketAddress, Point2D> peer : peerPositions.entrySet()) {
            double x = peer.getValue().getX();
            double y = peer.getValue().getY();


            gc.setFill(Color.LIGHTBLUE);
            gc.fillArc(x - 46, y - 46, 92, 92, 0, 360, ArcType.OPEN);

            gc.setLineWidth(6);
            gc.setStroke(Color.CORNFLOWERBLUE);
            gc.strokeArc(x - 43, y - 43, 86, 86, 0, 360, ArcType.OPEN);

            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.2);
            gc.strokeText(peer.getKey().toString(), x - 34, y + 3);
        }
    }

    private void setupData() {
        filteredRecords = rawRecords.stream()
                .filter(this::isValidRecord)
                .collect(Collectors.toList());

        if (filteredRecords.size() > 0) {

            // Setup slider
            slider.setMin(0);
            slider.setMax(filteredRecords.size() - 1);
            slider.setValue(0);
            slider.setShowTickLabels(true);
            slider.setShowTickMarks(true);
            slider.setMajorTickUnit((filteredRecords.size() - 1) / 2.0);
            slider.setBlockIncrement(1);

            renderPeerRecord(0);
        } else {
            // Reset slider
            slider.setMin(0);
            slider.setMax(0);
            slider.setValue(0);
            slider.setShowTickLabels(false);
            slider.setShowTickMarks(false);
            slider.setBlockIncrement(1);

            if (clearCheckBox.isSelected()) {
                clearScreen(canvas.getGraphicsContext2D());
            }
        }
    }

    private final ChangeListener<Boolean> guiSetupListener = (observable, oldValue, newValue) -> setupData();
    private final ChangeListener<Number> sliderUpdateListener = (observable, oldValue, newValue) -> {
        if (oldValue.intValue() != newValue.intValue()) {
            renderPeerRecord(newValue.intValue());
        }
    };
    private final ChangeListener<Boolean> clearListener =
            (observable, oldValue, newValue) -> renderPeerRecord(slider.valueProperty().intValue());

    private void setupGui(Stage primaryStage) {

        HBox menuBar = new HBox();
        menuBar.setPadding(new Insets(20, 20, 20, 20));
        menuBar.getChildren().add(collectedCheckBox);
        menuBar.getChildren().add(downloadRequestedCheckBox);
        menuBar.getChildren().add(downloadRejectedCheckBox);
        menuBar.getChildren().add(downloadProgressedCheckBox);
        menuBar.getChildren().add(downloadStartedCheckBox);
        menuBar.getChildren().add(downloadSucceededCheckBox);
        menuBar.getChildren().add(downloadFailedCheckBox);
        menuBar.getChildren().add(clearCheckBox);
        clearCheckBox.setSelected(true);

        slider.setPadding(new Insets(20, 20, 20, 20));

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(menuBar);
        borderPane.setCenter(canvas);
        borderPane.setBottom(slider);

        // Link all check boxes
        collectedCheckBox.selectedProperty().addListener(guiSetupListener);
        collectedCheckBox.setPadding(new Insets(5, 5, 5, 5));

        downloadRequestedCheckBox.selectedProperty().addListener(guiSetupListener);
        downloadRequestedCheckBox.setPadding(new Insets(5, 5, 5, 5));

        downloadRejectedCheckBox.selectedProperty().addListener(guiSetupListener);
        downloadRejectedCheckBox.setPadding(new Insets(5, 5, 5, 5));

        downloadProgressedCheckBox.selectedProperty().addListener(guiSetupListener);
        downloadProgressedCheckBox.setPadding(new Insets(5, 5, 5, 5));

        downloadStartedCheckBox.selectedProperty().addListener(guiSetupListener);
        downloadStartedCheckBox.setPadding(new Insets(5, 5, 5, 5));

        downloadSucceededCheckBox.selectedProperty().addListener(guiSetupListener);
        downloadSucceededCheckBox.setPadding(new Insets(5, 5, 5, 5));

        downloadFailedCheckBox.selectedProperty().addListener(guiSetupListener);
        downloadFailedCheckBox.setPadding(new Insets(5, 5, 5, 5));

        clearCheckBox.setPadding(new Insets(5, 5, 5, 5));
        clearCheckBox.selectedProperty().addListener(clearListener);

        slider.valueProperty().addListener(sliderUpdateListener);

        Scene scene = new Scene(borderPane, width, height + 150);
        primaryStage.setTitle("Records viewer 0.1");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void start(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(primaryStage);

        if (file == null) {
            Dialogs.create()
                    .message("No file selected")
                    .showInformation();

            Platform.exit();
        } else {
            try {
                rawRecords = IOUtil.deserialize(file);
                System.out.println("Deserialized records: " + rawRecords.size());
                initPeers();

                setupGui(primaryStage);
                setupData();
            } catch (Exception e) {
                Dialogs.create()
                        .message("Failed to load file: " + e.getMessage())
                        .showInformation();

                Platform.exit();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}