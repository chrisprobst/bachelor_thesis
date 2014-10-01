package de.probst.ba.gui;

import de.probst.ba.core.media.database.DataBase;
import de.probst.ba.core.media.database.DataInfo;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class StreamerGUI extends JFrame {

    private final DataBase dataBase;
    private final long delay;

    public StreamerGUI(DataBase dataBase, long delay) {
        Objects.requireNonNull(dataBase);
        this.dataBase = dataBase;
        this.delay = delay;
        initUI();
    }

    Surface surface;

    private void initUI() {
        setTitle("Leecher streaming demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        surface = new Surface();
        add(surface);
        setLocationRelativeTo(null);
        pack();
        setSize(640, 480);
        setVisible(true);
    }

    public void startTimers() {
        surface.startTimers();
    }

    class Surface extends JPanel {

        private long firstId = 0;
        private DataInfo dataInfo;
        private double x = 20;

        private void doDrawing(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            if (dataInfo != null) {
                g2d.setColor(Color.GRAY);
                for (int i = 0; i < firstId; i++) {
                    g2d.fillRect(i * 100 + 20, 100, 100, 100);
                }
                g2d.setColor(Color.GREEN);
                g2d.fillRect((int) (firstId * 100 + 20), 100, (int) (100 * dataInfo.getPercentage()), 100);

                g2d.setColor(Color.BLACK);
                g2d.translate(x, 100);
                g2d.drawLine(0, 0, 0, 100);
            } else {
                g2d.drawString("Too slow for streaming", 100, 100);
            }
        }

        private void startTimers() {
            Timer timer = new Timer(100, e -> {
                do {
                    DataInfo old = dataInfo;
                    dataInfo = dataBase.getDataInfo()
                                       .values()
                                       .stream()
                                       .filter(d -> d.getId() == firstId)
                                       .findFirst()
                                       .orElse(null);
                    if (dataInfo == null) {
                        dataInfo = old;
                        break;
                    }

                    if (dataInfo.isCompleted()) {
                        firstId++;
                    } else {
                        break;
                    }
                } while (true);


                double offset = 100 / 10 / 10;
                x += offset;
                repaint();
            });
            timer.setInitialDelay((int) delay);
            timer.start();
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            doDrawing(g);
        }


    }
}