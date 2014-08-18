package de.probst.ba.core.gui;

import de.probst.ba.core.diag.RecordDiagnostic;
import de.probst.ba.core.net.peer.PeerId;
import de.probst.ba.core.util.IOUtil;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.stage.Stage;

import java.net.SocketAddress;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by chrisprobst on 18.08.14.
 */
public class RecordViewer extends Application {

    public static int width = 1024;
    public static int height = 768;
    private List<RecordDiagnostic.Record> sortedByTimeRecords;
    private List<PeerId> peers;
    private Map<SocketAddress, Point2D> nodePositions = new HashMap<>();

    private boolean isValidRecord(RecordDiagnostic.Record record) {
        return record instanceof RecordDiagnostic.CollectedRecord ||
                record instanceof RecordDiagnostic.DownloadRejectedRecord ||
                record instanceof RecordDiagnostic.DownloadRequestedRecord ||
                record instanceof RecordDiagnostic.DownloadProgressedRecord ||
                record instanceof RecordDiagnostic.DownloadStartedRecord ||
                record instanceof RecordDiagnostic.DownloadSucceededRecord;
    }

    @Override
    public void init() throws Exception {
        sortedByTimeRecords = IOUtil.deserialize(Paths.get("/Users/chrisprobst/Desktop/records.dat"));
        sortedByTimeRecords = sortedByTimeRecords.stream()
                .filter(this::isValidRecord)
                .collect(Collectors.toList());
        peers = sortedByTimeRecords.stream()
                .map(RecordDiagnostic.Record::getLocalPeerId)
                .distinct()
                .collect(Collectors.toList());

        System.out.println("Deserialized records: " + sortedByTimeRecords.size());
    }

    private Canvas setupCanvas() {
        Canvas canvas = new Canvas(width, height);
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        renderNodes(gc, true, true);
        return canvas;
    }


    private Slider setupSlider() {
        Slider slider = new Slider();
        slider.setMin(0);
        slider.setMax(sortedByTimeRecords.size() - 1);
        slider.setValue(0);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(sortedByTimeRecords.size() / 2 - 1);
        slider.setMinorTickCount(sortedByTimeRecords.size() / 20 - 1);
        slider.setBlockIncrement(1);
        return slider;
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

        Point2D remote = nodePositions.get(record.getRemotePeerId().getAddress());
        Point2D local = nodePositions.get(record.getLocalPeerId().getAddress());
        renderArrow(gc, remote, local, 50, 50);
    }

    private void renderDownloadRequested(GraphicsContext gc,
                                         RecordDiagnostic.DownloadRequestedRecord record) {
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(3);

        Point2D local = nodePositions.get(record.getLocalPeerId().getAddress());
        Point2D remote = nodePositions.get(record.getTransfer().getRemotePeerId().getAddress());
        renderArrow(gc, local, remote, 50, 50);
    }

    private void renderDownloadRejected(GraphicsContext gc,
                                        RecordDiagnostic.DownloadRejectedRecord record) {
        gc.setStroke(Color.RED);
        gc.setLineWidth(3);

        Point2D local = nodePositions.get(record.getLocalPeerId().getAddress());
        Point2D remote = nodePositions.get(record.getTransfer().getRemotePeerId().getAddress());
        renderArrow(gc, remote, local, 50, 50);
    }

    private void renderDownloadStarted(GraphicsContext gc, RecordDiagnostic.DownloadStartedRecord record) {
        gc.setStroke(Color.CYAN);
        gc.setLineWidth(3);

        Point2D local = nodePositions.get(record.getLocalPeerId().getAddress());
        Point2D remote = nodePositions.get(record.getTransfer().getRemotePeerId().getAddress());
        renderArrow(gc, remote, local, 50, 50);
    }

    private void renderDownloadSucceeded(GraphicsContext gc, RecordDiagnostic.DownloadSucceededRecord record) {
        gc.setStroke(Color.DARKGREEN);
        gc.setLineWidth(3);

        Point2D local = nodePositions.get(record.getLocalPeerId().getAddress());

        gc.strokeArc(local.getX() - 52, local.getY() - 52, 104, 104, 0, 360, ArcType.OPEN);
    }

    private void renderNodes(GraphicsContext gc,
                             boolean initPositions,
                             boolean clear,
                             RecordDiagnostic.Record... records) {
        if (clear) {
            gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        }

        double target = Math.PI * 2;
        double step = target / peers.size();
        double originX = width / 2;
        double originY = height / 2;

        for (RecordDiagnostic.Record record : records) {
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
            }
        }

        double angle = 0;
        for (PeerId nextPeerId : peers) {

            double x = originX + Math.sin(angle) * (width - 200) / 2;
            double y = originY + Math.cos(angle) * (height - 100) / 2;

            if (initPositions) {
                nodePositions.put(nextPeerId.getAddress(), new Point2D(x, y));
            }

            gc.setStroke(Color.BLUE);
            gc.setLineWidth(3);
            gc.strokeArc(x - 50, y - 50, 100, 100, 0, 360, ArcType.OPEN);

            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
            gc.strokeText(nextPeerId.getAddress().toString(), x - 40, y);

            angle += step;
        }
    }


    @Override
    public void start(Stage primaryStage) {

        Canvas canvas = setupCanvas();
        Slider slider = setupSlider();

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(canvas);
        borderPane.setBottom(slider);

        slider.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (oldValue.intValue() != newValue.intValue()) {
                System.out.println(newValue.intValue());
                renderNodes(
                        canvas.getGraphicsContext2D(),
                        false,
                        true,
                        sortedByTimeRecords.get(newValue.intValue()));
            }
        });

        Scene scene = new Scene(borderPane, width, height + 80);

        primaryStage.setTitle("Hello World!");
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}