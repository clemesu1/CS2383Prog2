package ca.unbsj.cs2383;

import edu.princeton.cs.algs4.MinPQ;
import edu.princeton.cs.algs4.Picture;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


// fun args: src/main/resources/motherboard-152501.png 0.2 0.25 0.1 20.5 fun 200
// serious args: src/main/resources/motherboard-152501.png 0.2 0.25 0.1 20.5 serious 25
public class Prog2 {
    public static void main(String[] args) {
        Picture pix = new Picture(args[0]);

        if (args.length != 7) throw new IllegalArgumentException("7 arguments required");

        double p1 = Double.parseDouble(args[1]);
        double p2 = Double.parseDouble(args[2]);
        double p3 = Double.parseDouble(args[3]);
        double p4 = Double.parseDouble(args[4]);
        String option = args[5];
        int variable = Integer.parseInt(args[6]);

        System.out.println(option + " mode selected");

        if (option.equals("fun")) {
            simulation(pix, p1, p2, p3, p4, true, variable);
        } else if (option.equals("serious")) {
            seriousSimulation(pix, p1, p2, p3, p4, false, variable);
        }

        System.exit(0);

    }

    public static void seriousSimulation(Picture pix, double p1, double p2, double p3, double p4, boolean visualization, int v) {

        // get number of green pixels
        int ctr = 0;
        for (int y = 0; y < pix.height(); y++) {
            for (int x = 0; x < pix.width(); x++) {
                if (isGreen(pix.get(x, y))) ctr++;
            }
        }

        int[] results = new int[10];
        for (int i = 0; i < v; i++) {
            pix = new Picture("src/main/resources/motherboard-152501.png");

            int result = simulation(pix, p1, p2, p3, p4, visualization, v);

            System.out.println("Simulation " + (i + 1) + " complete");

            double p = (double) result / ctr;

            if (p >= 0 && p < 0.1) {
                results[0]++;
            } else if (p >= 0.1 && p < 0.2) {
                results[1]++;
            } else if (p >= 0.2 && p < 0.3) {
                results[2]++;
            } else if (p >= 0.3 && p < 0.4) {
                results[3]++;
            } else if (p >= 0.4 && p < 0.5) {
                results[4]++;
            } else if (p >= 0.5 && p < 0.6) {
                results[5]++;
            } else if (p >= 0.6 && p < 0.7) {
                results[6]++;
            } else if (p >= 0.7 && p < 0.8) {
                results[7]++;
            } else if (p >= 0.8 && p < 0.9) {
                results[8]++;
            } else {
                results[9]++;
            }
        }

        System.out.println("Number of simulations: " + v);
        for (int i = 0; i < results.length; i++) {
            System.out.println("The number of times that between " + i * 10 + "% and " + (i + 1) * 10 + "% of the original unburnt green pixels ended up burnt: " + results[i]);
        }

    }

    public static int simulation(Picture pix, double p1, double p2, double p3, double p4, boolean visualization, int v) {

        int xMax = pix.width();
        int yMax = pix.height();
        MinPQ<Event> pq = new MinPQ<>();
        PixelState[][] boardState = new PixelState[xMax][yMax];

        // initialize mother board state
        for (int y = 0; y < yMax; y++) {
            for (int x = 0; x < xMax; x++) {
                Color c = pix.get(x, y);
                if (isGreen(c)) boardState[x][y] = PixelState.UNBURNT;
                else boardState[x][y] = null;
            }
        }

        // Generate first 100 sparks.
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            int sparkLocationX = random.nextInt(xMax);
            int sparkLocationY = random.nextInt(yMax);
            pq.insert(new SparkEvent(0, sparkLocationX, sparkLocationY));
        }

        int displayTime = 0;
        while (!pq.isEmpty()) {
            Event currentEvent = pq.delMin();
            int currentTime = currentEvent.time;

            // if visualization mode is selected
            if (visualization && currentTime >= displayTime) {
                pix.show();

                System.out.println("Press enter to continue...");
                try {
                    System.in.read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                displayTime = currentTime + v;
            }

            int activeLocationX = currentEvent.x;
            int activeLocationY = currentEvent.y;

            // if event is a spark landing
            if (currentEvent.id == 0) {
                // determine if the landing spot is unburnt green and unlucky to catch fire
                if (isWithinBounds(activeLocationX, activeLocationY, xMax, yMax)
                        && isGreen(pix.get(activeLocationX, activeLocationY))
                        && boardState[activeLocationX][activeLocationY].equals(PixelState.UNBURNT)) {
                    // if so, set pixel’s state to igniting

                    // probability p1 that the spark will ignite.
                    double value = random.nextDouble();
                    if (value <= p1) {
                        boardState[activeLocationX][activeLocationY] = PixelState.IGNITING;
                        pix.set(activeLocationX, activeLocationY, Color.ORANGE);
                        Event e = new PixelTransitionEvent(currentTime + 10, activeLocationX, activeLocationY, PixelState.BURNING);
                        pq.insert(e);
                    }
                }
            }

            // if event is a pixel becoming burning
            if (currentEvent.id == PixelState.BURNING.ordinal()) {
                boardState[activeLocationX][activeLocationY] = PixelState.BURNING;
                pix.set(activeLocationX, activeLocationY, Color.RED);

                // determine how many sparks it throws
                int sparkCount = 0;
                double value = random.nextDouble();
                if (value <= p2) sparkCount = 1;
                if (value <= p3) sparkCount = 2;

                // determine when and where the spark(s) will land
                for (int spark = 0; spark < sparkCount; spark++) {
                    int hangTime = random.nextInt(80) + 20;
                    int direction = random.nextInt(360);
                    int distance = random.nextInt((int) (Math.round(p4) - 1)) + 1;
                    int sparkX = activeLocationX + (distance * ((int) Math.cos(direction)));
                    int sparkY = activeLocationY + (distance * ((int) Math.sin(direction)));
                    pq.insert(new SparkEvent(currentTime + hangTime, sparkX, sparkY));
                }

                // determine which of its neighbours are unburnt green
                List<NeighbourPixel> neighbours = new ArrayList<>();
                if (isWithinBounds(activeLocationX + 1, activeLocationY, xMax, yMax)
                        && boardState[activeLocationX + 1][activeLocationY] == PixelState.UNBURNT
                        && isGreen(pix.get(activeLocationX + 1, activeLocationY))) {
                    neighbours.add(new NeighbourPixel(activeLocationX + 1, activeLocationY));
                }
                if (isWithinBounds(activeLocationX - 1, activeLocationY, xMax, yMax)
                        && boardState[activeLocationX - 1][activeLocationY] == PixelState.UNBURNT
                        && isGreen(pix.get(activeLocationX - 1, activeLocationY))) {
                    neighbours.add(new NeighbourPixel(activeLocationX - 1, activeLocationY));
                }
                if (isWithinBounds(activeLocationX, activeLocationY + 1, xMax, yMax)
                        && boardState[activeLocationX][activeLocationY + 1] == PixelState.UNBURNT
                        && isGreen(pix.get(activeLocationX, activeLocationY + 1))) {
                    neighbours.add(new NeighbourPixel(activeLocationX, activeLocationY + 1));
                }
                if (isWithinBounds(activeLocationX, activeLocationY - 1, xMax, yMax)
                        && boardState[activeLocationX][activeLocationY - 1] == PixelState.UNBURNT
                        && isGreen(pix.get(activeLocationX, activeLocationY - 1))) {
                    neighbours.add(new NeighbourPixel(activeLocationX, activeLocationY - 1));
                }

                for (NeighbourPixel pixel : neighbours) {
                    // for each, set neighbour’s state to igniting
                    boardState[pixel.x][pixel.y] = PixelState.IGNITING;
                    pix.set(pixel.x, pixel.y, Color.ORANGE);

                    // create an event E for currentTime+10 for this neighbour pixel to become "burning"
                    Event e = new PixelTransitionEvent(currentTime + 10, pixel.x, pixel.y, PixelState.BURNING);
                    pq.insert(e);
                }
                Event e = new PixelTransitionEvent(currentTime + 100, activeLocationX, activeLocationY, PixelState.BURNT);
                pq.insert(e);
            }

            // if event is a pixel becoming burnt
            if (currentEvent.id == PixelState.BURNT.ordinal()) {
                boardState[activeLocationX][activeLocationY] = PixelState.BURNT;
                pix.set(activeLocationX, activeLocationY, Color.BLACK);
            }

            if (visualization) {
                System.out.println(currentEvent);
            }
        }

        if (!visualization) {
            int ctr = 0;
            for (int y = 0; y < yMax; y++) {
                for (int x = 0; x < xMax; x++) {
                    if (boardState[x][y] != null && boardState[x][y].ordinal() == 3) ctr++;
                }
            }
            return ctr;
        }

        return 0;
    }

    public static class Event implements Comparable<Event> {
        public int time; // when the event is to happen
        public int x, y; // where the event will take place
        public int id;

        @Override
        public int compareTo(Event other) {
            return Integer.compare((time), other.time);
        }
    }

    public static final class SparkEvent extends Event {

        public SparkEvent(int time, int x, int y) {
            this.time = time;
            this.x = x;
            this.y = y;
            id = 0;
        }

        @Override
        public String toString() {
            return "<spark lands at (" + x + "," + y + ") at time=" + time + ">";
        }
    }

    public static final class PixelTransitionEvent extends Event {
        PixelState futureState;

        public PixelTransitionEvent(int time, int x, int y, PixelState newState) {
            this.time = time;
            this.x = x;
            this.y = y;
            futureState = newState;
            id = newState.ordinal();
        }

        public PixelState getFutureState() {
            return futureState;
        }

        public String toString() {
            return "<pixel at (" + x + "," + y + ") will change to " + futureState + " at time=" + time + ">";
        }
    }

    private static boolean isGreen(Color c) {
        return c.getRed() == 52 && c.getGreen() == 171 && c.getBlue() == 0;
    }

    private static boolean isWithinBounds(int x, int y, int xMax, int yMax) {
        return x >= 0 && x < xMax && y >= 0 && y < yMax;
    }
}
