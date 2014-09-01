package de.probst.ba.core.gui;

import javafx.application.Application;

/**
 * Created by chrisprobst on 31.08.14.
 */
public abstract class StreamingDemo extends Application {
/*
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
                    Algorithms.intelligentBrain(),
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
                    Algorithms.intelligentBrain(),
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
    }*/
}
