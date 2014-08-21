package de.probst.ba.core.gui;

import de.probst.ba.core.diag.RecordDiagnostic;
import de.probst.ba.core.media.DataInfo;
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
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.dialog.Dialogs;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 18.08.14.
 */
public class RecordViewer extends Application {

    public static final int WIDTH = 1300;
    public static final int HEIGHT = 850;
    public static final int SLIDER_HEIGHT = 150;

    public static final double PEER_RADIUS = 46;

    // Initialized once
    private List<RecordDiagnostic.Record> rawRecords;
    private RecordDiagnostic.Record start;
    private RecordDiagnostic.Record end;
    private Map<SocketAddress, Point2D> peerPositions = new HashMap<>();

    // Dynamically created
    private List<RecordDiagnostic.Record> filteredRecords;
    private List<Map<SocketAddress, DataInfo>> peerDataInfo;

    // Gui stuff
    private Font timeFont = Font.font("monospace");
    private Canvas canvas = new Canvas(WIDTH, HEIGHT);
    private Slider slider = new Slider();
    private CheckBox collectedCheckBox = new CheckBox("Data Info");
    private CheckBox downloadRejectedCheckBox = new CheckBox("Rejected downloads");
    private CheckBox downloadRequestedCheckBox = new CheckBox("Requested downloads");
    private CheckBox downloadProgressedCheckBox = new CheckBox("Progressed downloads");
    private CheckBox downloadStartedCheckBox = new CheckBox("Started downloads");
    private CheckBox downloadSucceededCheckBox = new CheckBox("Succeeded downloads");
    private CheckBox downloadFailedCheckBox = new CheckBox("Failed downloads");
    private CheckBox clearCheckBox = new CheckBox("Clear canvas");

    private void initPeers(File file) throws IOException, ClassNotFoundException {
        rawRecords = IOUtil.deserialize(file);

        if (rawRecords.size() > 0) {
            int index = 0;
            RecordDiagnostic.Record tmp = rawRecords.get(index);
            if (tmp.getRecordType() == RecordDiagnostic.RecordType.Start) {
                start = tmp;
                rawRecords.remove(index);
            }
        }

        if (rawRecords.size() > 0) {
            int index = rawRecords.size() - 1;
            RecordDiagnostic.Record tmp = rawRecords.get(index);
            if (tmp.getRecordType() == RecordDiagnostic.RecordType.End) {
                end = tmp;
                rawRecords.remove(index);
            }
        }

        List<PeerId> peers = rawRecords.stream()
                .map(RecordDiagnostic.Record::getLocalPeerId)
                .distinct()
                .collect(Collectors.toList());

        System.out.println("Deserialized records: " + rawRecords.size());

        double target = Math.PI * 2;
        double step = target / peers.size();
        double originX = WIDTH / 2;
        double originY = HEIGHT / 2;
        double angle = 0;

        for (PeerId nextPeerId : peers) {
            double x = originX + Math.sin(angle) * (WIDTH - 240) / 2;
            double y = originY + Math.cos(angle) * (HEIGHT - 120) / 2;

            peerPositions.put(nextPeerId.getAddress(), new Point2D(x, y));
            angle += step;
        }
    }

    private boolean isValidRecord(RecordDiagnostic.Record record) {
        return (record.getRecordType() == RecordDiagnostic.RecordType.Collected && collectedCheckBox.isSelected()) ||
                (record.getRecordType() == RecordDiagnostic.RecordType.DownloadRejected && downloadRejectedCheckBox.isSelected()) ||
                (record.getRecordType() == RecordDiagnostic.RecordType.DownloadRequested && downloadRequestedCheckBox.isSelected()) ||
                (record.getRecordType() == RecordDiagnostic.RecordType.DownloadProgressed && downloadProgressedCheckBox.isSelected()) ||
                (record.getRecordType() == RecordDiagnostic.RecordType.DownloadStarted && downloadStartedCheckBox.isSelected()) ||
                (record.getRecordType() == RecordDiagnostic.RecordType.DownloadSucceeded && downloadSucceededCheckBox.isSelected()) ||
                (record.getRecordType() == RecordDiagnostic.RecordType.DownloadFailed && downloadFailedCheckBox.isSelected());
    }

    private void renderArrow(GraphicsContext gc, Point2D a, Point2D b, double offset, double backOff) {
        if (a == null || b == null) {
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
                                         RecordDiagnostic.Record record) {

        gc.setStroke(Color.GREEN);
        gc.setLineWidth(1);

        Point2D remote = peerPositions.get(record.getRemotePeerId().getAddress());
        Point2D local = peerPositions.get(record.getLocalPeerId().getAddress());
        renderArrow(gc, remote, local, PEER_RADIUS, PEER_RADIUS);
    }

    private void renderDownloadRequested(GraphicsContext gc,
                                         RecordDiagnostic.Record record) {
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(3);

        Point2D local = peerPositions.get(record.getLocalPeerId().getAddress());
        Point2D remote = peerPositions.get(record.getTransfer().getRemotePeerId().getAddress());
        renderArrow(gc, local, remote, PEER_RADIUS, PEER_RADIUS);
    }

    private void renderDownloadRejected(GraphicsContext gc,
                                        RecordDiagnostic.Record record) {
        gc.setStroke(Color.RED);
        gc.setLineWidth(3);

        Point2D local = peerPositions.get(record.getLocalPeerId().getAddress());
        Point2D remote = peerPositions.get(record.getTransfer().getRemotePeerId().getAddress());
        renderArrow(gc, remote, local, PEER_RADIUS, PEER_RADIUS);
    }

    private void renderDownloadStarted(GraphicsContext gc, RecordDiagnostic.Record record) {
        gc.setStroke(Color.DARKCYAN);
        gc.setLineWidth(3);

        Point2D local = peerPositions.get(record.getLocalPeerId().getAddress());
        Point2D remote = peerPositions.get(record.getTransfer().getRemotePeerId().getAddress());
        renderArrow(gc, remote, local, PEER_RADIUS, PEER_RADIUS);
    }

    private void renderDownloadProgressed(GraphicsContext gc, RecordDiagnostic.Record record) {
        gc.setStroke(Color.ORANGE);
        gc.setLineWidth(3);

        Point2D local = peerPositions.get(record.getLocalPeerId().getAddress());
        Point2D remote = peerPositions.get(record.getTransfer().getRemotePeerId().getAddress());
        renderArrow(gc, remote, local, PEER_RADIUS, PEER_RADIUS);
    }

    private void renderDownloadSucceeded(GraphicsContext gc, RecordDiagnostic.Record record) {
        gc.setStroke(Color.DARKGREEN);
        gc.setLineWidth(6);

        Point2D local = peerPositions.get(record.getLocalPeerId().getAddress());
        Point2D remote = peerPositions.get(record.getTransfer().getRemotePeerId().getAddress());
        renderArrow(gc, remote, local, PEER_RADIUS, PEER_RADIUS + 6);
        double radius = PEER_RADIUS + 3;

        gc.strokeArc(
                local.getX() - radius,
                local.getY() - radius,
                radius * 2,
                radius * 2,
                0,
                360,
                ArcType.OPEN);
    }

    private void renderPercentage(Point2D pos, DataInfo dataInfo) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double x = pos.getX();
        double y = pos.getY();
        double percentage = dataInfo.getPercentage();
        double newRadius = (PEER_RADIUS - 5);
        double length = newRadius * 2;

        // Render the percentage
        gc.setFill(Color.LAWNGREEN);
        gc.fillRect(x - 10, y + newRadius - percentage * length, 20, percentage * length);
    }

    private void renderChunks(Point2D pos, DataInfo dataInfo) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double x = pos.getX();
        double y = pos.getY();
        double angle = 360.0 / dataInfo.getChunkCount();
        double extent = 0.5;

        // Render each chunk
        dataInfo.getCompletedChunks().forEach(i -> {
            gc.setLineWidth(12);
            gc.setStroke(Color.LAWNGREEN);
            gc.strokeArc(x - 40, y - 40, 80, 80,
                    angle * i - extent, angle + extent, ArcType.CHORD);
        });
    }

    private void clearScreen(GraphicsContext gc) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
    }

    private void renderPeerRecord(int index) {
        if (index >= filteredRecords.size()) {
            return;
        }

        RecordDiagnostic.Record record = filteredRecords.get(index);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        if (clearCheckBox.isSelected()) {
            clearScreen(gc);
        }

        // Render the record name
        gc.setFont(timeFont);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeText("Type: " + record.getRecordType().toString(), 30, 110);

        gc.setStroke(Color.BLUE);
        gc.strokeText("Record time: " + record.getTimeStamp().toString(), 30, 50);

        // Render the start/end
        if (start != null) {
            gc.setStroke(Color.DARKGREEN);
            gc.strokeText(" Start time: " + start.getTimeStamp().toString(), 30, 30);
        }
        if (end != null) {
            gc.setStroke(Color.ORANGERED);
            gc.strokeText("   End time: " + end.getTimeStamp().toString(), 30, 70);
        }


        // Render the record
        if (record.getRecordType() == RecordDiagnostic.RecordType.Collected) {
            renderCollectedDataInfo(gc, record);
        } else if (record.getRecordType() == RecordDiagnostic.RecordType.DownloadRequested) {
            renderDownloadRequested(gc, record);
        } else if (record.getRecordType() == RecordDiagnostic.RecordType.DownloadStarted) {
            renderDownloadStarted(gc, record);
        } else if (record.getRecordType() == RecordDiagnostic.RecordType.DownloadSucceeded) {
            renderDownloadSucceeded(gc, record);
        } else if (record.getRecordType() == RecordDiagnostic.RecordType.DownloadRejected) {
            renderDownloadRejected(gc, record);
        } else if (record.getRecordType() == RecordDiagnostic.RecordType.DownloadProgressed) {
            renderDownloadProgressed(gc, record);
        }

        // Render all peers afterwards
        for (Map.Entry<SocketAddress, Point2D> peer : peerPositions.entrySet()) {
            double x = peer.getValue().getX();
            double y = peer.getValue().getY();

            // Render inner arc
            gc.setFill(Color.LIGHTSTEELBLUE);
            gc.fillArc(
                    x - PEER_RADIUS,
                    y - PEER_RADIUS,
                    PEER_RADIUS * 2,
                    PEER_RADIUS * 2,
                    0,
                    360,
                    ArcType.OPEN);

            // Render the percentage
            if (peerDataInfo.get(index) != null) {
                renderPercentage(
                        peer.getValue(),
                        peerDataInfo.get(index).get(peer.getKey()));
            }

            // Render outer arc
            double outerArcThickness = 12;
            double newRadius = PEER_RADIUS - outerArcThickness / 2;
            gc.setLineWidth(outerArcThickness);
            gc.setStroke(Color.INDIANRED);
            gc.strokeArc(
                    x - newRadius,
                    y - newRadius,
                    newRadius * 2,
                    newRadius * 2,
                    0,
                    360,
                    ArcType.OPEN);

            // Render node name
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.2);
            gc.strokeText(peer.getKey().toString(), x - 30, y + 3);

            // Render the data info
            if (peerDataInfo.get(index) != null) {
                renderChunks(
                        peer.getValue(),
                        peerDataInfo.get(index).get(peer.getKey()));
            }
        }
    }

    private void setupData() {
        filteredRecords = rawRecords.stream()
                .filter(this::isValidRecord)
                .collect(Collectors.toList());
        peerDataInfo = new ArrayList<>(filteredRecords.size());

        // Init peer data info
        Map<SocketAddress, DataInfo> last = null;
        for (RecordDiagnostic.Record record : filteredRecords) {
            if (record.getRecordType() == RecordDiagnostic.RecordType.DownloadStarted ||
                    record.getRecordType() == RecordDiagnostic.RecordType.DownloadProgressed ||
                    record.getRecordType() == RecordDiagnostic.RecordType.DownloadSucceeded) {

                if (last != null) {
                    last = new HashMap<>(last);
                    last.merge(record.getLocalPeerId().getAddress(),
                            record.getTransfer().getCompletedDataInfo(),
                            DataInfo::union);
                    peerDataInfo.add(last);
                } else {
                    last = new HashMap<>();
                    for (SocketAddress addr : peerPositions.keySet()) {
                        last.put(addr, record.getLocalPeerId().getAddress().equals(addr) ?
                                record.getTransfer().getCompletedDataInfo() :
                                record.getTransfer().getDataInfo().empty());
                    }

                    peerDataInfo.add(last);
                }
            } else if (last != null) {
                peerDataInfo.add(last);
            } else {
                peerDataInfo.add(null);
            }
        }


        if (filteredRecords.size() > 0) {

            // Setup slider
            slider.setMin(0);
            slider.setMax(filteredRecords.size() - 1);
            slider.setValue(0);
            slider.setShowTickLabels(true);
            slider.setShowTickMarks(true);
            if (filteredRecords.size() > 1) {
                slider.setMajorTickUnit((filteredRecords.size() - 1) / 2.0);
            }
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

        Scene scene = new Scene(borderPane,
                WIDTH, HEIGHT + SLIDER_HEIGHT);
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
                initPeers(file);
                setupGui(primaryStage);
                setupData();
            } catch (Exception e) {
                e.printStackTrace();

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