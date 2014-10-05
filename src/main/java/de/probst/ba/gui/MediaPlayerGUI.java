package de.probst.ba.gui;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MediaPlayerGUI extends Application {

    private static final String MEDIA_URL = "http://localhost:8000/neuseeland.mp4";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("java-buddy.blogspot.com/");

        Group root = new Group();
        Scene scene = new Scene(root, 540, 210);

        // create media player
        Media media = new Media(MEDIA_URL);
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setAutoPlay(true);
        mediaPlayer.setOnError(() -> System.out.println(mediaPlayer.getError()));
        mediaPlayer.setOnPlaying(() -> {
            System.out.println(mediaPlayer.getTotalDuration());
            System.out.println(mediaPlayer.getMedia().getMetadata());
            mediaPlayer.seek(Duration.seconds(500));
        });


        // create mediaView and add media player to the viewer
        MediaView mediaView = new MediaView(mediaPlayer);
        ((Group) scene.getRoot()).getChildren().add(mediaView);

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
