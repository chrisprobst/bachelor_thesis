package de.probst.ba.core.gui;

import de.probst.ba.core.Config;
import de.probst.ba.core.diag.CombinedDiagnostic;
import de.probst.ba.core.diag.Diagnostic;
import de.probst.ba.core.diag.DiagnosticAdapter;
import de.probst.ba.core.diag.LoggingDiagnostic;
import de.probst.ba.core.logic.brains.Brains;
import de.probst.ba.core.media.DataInfo;
import de.probst.ba.core.media.databases.DataBases;
import de.probst.ba.core.net.TransferManager;
import de.probst.ba.core.net.peer.Peer;
import de.probst.ba.core.net.peer.peers.Peers;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.IntStream;

/**
 * Created by chrisprobst on 31.08.14.
 */
public class StreamingDemo extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        int chunkCount = 50;
        int leechers = 5;
        int seeders = 1;

        // Benchmark data
        DataInfo[] dataInfo = IntStream.range(0, 1)
                .mapToObj(i -> new DataInfo(
                        i,
                        10000,
                        Optional.empty(),
                        Optional.empty(),
                        "Stream hash, part: " + i,
                        chunkCount,
                        String::valueOf)
                        .full())
                .toArray(DataInfo[]::new);


        // *********
        // SETUP GUI
        Canvas canvas = new Canvas();

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(canvas);

        Scene scene = new Scene(borderPane, 800, 600);
        primaryStage.setTitle("Streaming Demo");
        primaryStage.setScene(scene);
        primaryStage.show();

        // List of peers
        Queue<Peer> peers = new LinkedList<>();

        // The event loop group shared by all peers
        EventLoopGroup eventLoopGroup = new DefaultEventLoopGroup();

        // Create the combined diagnostic
        Diagnostic stats = new DiagnosticAdapter() {

            @Override
            public void downloadSucceeded(Peer peer, TransferManager transferManager) {
                GraphicsContext ctx = canvas.getGraphicsContext2D();


            }
        };

        Diagnostic combined = new CombinedDiagnostic(new LoggingDiagnostic(leechers * chunkCount), stats);

        // Setup all seeders
        for (int i = 0; i < seeders; i++) {
            peers.add(Peers.localPeer(
                    1000,
                    1000,
                    new LocalAddress("S-" + i),
                    DataBases.fakeDataBase(dataInfo),
                    Brains.intelligentBrain(),
                    combined,
                    Optional.of(eventLoopGroup)));
        }

        // Setup all leechers
        for (int i = 0; i < leechers; i++) {
            peers.add(Peers.localPeer(
                    1000,
                    1000,
                    new LocalAddress("L-" + i),
                    DataBases.fakeDataBase(),
                    Brains.intelligentBrain(),
                    combined,
                    Optional.of(eventLoopGroup)));
        }

        // Wait for init
        Peers.waitForInit(peers);

        // Connect every peer to every other peer
        Peers.connectGrid(peers);
    }

    public static void main(String[] args) {
        // Setup config
        Config.setAnnounceDelay(250);
        Config.setBrainDelay(200);

        // Setup logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());

        // Launch the app
        launch(args);
    }
}
