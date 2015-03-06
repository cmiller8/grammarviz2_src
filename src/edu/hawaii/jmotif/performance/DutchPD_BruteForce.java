package edu.hawaii.jmotif.performance;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import edu.hawaii.jmotif.distance.EuclideanDistance;
import edu.hawaii.jmotif.sax.SAXFactory;
import edu.hawaii.jmotif.sax.trie.TrieException;
import edu.hawaii.jmotif.timeseries.TSException;
import edu.hawaii.jmotif.timeseries.TSUtils;
import edu.hawaii.jmotif.util.StackTrace;

public class DutchPD_BruteForce {

  final static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private static final String dataFName = "data/dutch_power_demand.txt";

  public static void main(String[] args) throws TSException, TrieException, IOException {

    // load data
    double[] ts = loadData(dataFName);
    System.out.println("loaded " + ts.length + " points");

    // mark start
    Date start = new Date();

    int window = 700;

    double bestSoFarDistance = 0.0;
    int bestSoFarPosition = 0;

    double secondBestSoFarDistance = 0.0;
    int secondBestSoFarPosition = 0;

    int distCounter = 0;

    for (int i = 0; i < ts.length - window; i++) { // outer loop

      if (i % 1000 == 0) {
        System.out.println(i);
      }

      double[] cw = TSUtils.subseries(ts, i, window);
      double cwNNDist = Double.MAX_VALUE;

      for (int j = 1; j < ts.length - window - 1; j++) {

        if (Math.abs(i - j) > window) {
          double[] currentSubsequence = TSUtils.subseries(ts, j, window);
          double dist = EuclideanDistance.distance(cw, currentSubsequence);
          distCounter++;
          // if (dist < bestSoFarDistance) {
          // cwNNDist = dist;
          // break;
          // }

          if (dist < cwNNDist) {
            cwNNDist = dist;
          }

        }
      }

      // System.out.println(i + ", " + cwNNDist);

      if (Double.MAX_VALUE != cwNNDist && cwNNDist > bestSoFarDistance) {
        bestSoFarDistance = cwNNDist;
        bestSoFarPosition = i;
      }

    }
    Date firstDiscord = new Date();

    System.out.println("Best pos: " + bestSoFarPosition + ", best distance: " + bestSoFarDistance);

    System.out.println("First discord found in "
        + SAXFactory.timeToString(start.getTime(), firstDiscord.getTime()) + " distance calls: "
        + distCounter);

    for (int i = 0; i < ts.length - window; i++) { // outer loop

      double[] cw = TSUtils.subseries(ts, i, window);
      double cwNNDist = Double.MAX_VALUE;

      for (int j = 1; j < ts.length - window - 1; j++) {

        if (Math.abs(i - bestSoFarPosition) <= window) {
          break;
        }

        if (Math.abs(i - j) > window) {
          double[] currentSubsequence = TSUtils.subseries(ts, j, window);
          double dist = EuclideanDistance.distance(cw, currentSubsequence);

          if (dist < secondBestSoFarDistance) {
            cwNNDist = dist;
            break;
          }

          if (dist < cwNNDist) {
            cwNNDist = dist;
          }

        }
      }

      // System.out.println(i + ", " + cwNNDist);

      if (Double.MAX_VALUE != cwNNDist && cwNNDist > secondBestSoFarDistance) {
        secondBestSoFarDistance = cwNNDist;
        secondBestSoFarPosition = i;
      }

    }

    Date secondDiscord = new Date();

    System.out.println("Best pos: " + bestSoFarPosition + ", best distance: " + bestSoFarDistance);

    System.out.println("Second best pos: " + secondBestSoFarPosition + ", second best distance: "
        + secondBestSoFarDistance);

    System.out.println("First discord found in "
        + SAXFactory.timeToString(start.getTime(), firstDiscord.getTime()));
    System.out.println("Second discord found in "
        + SAXFactory.timeToString(firstDiscord.getTime(), secondDiscord.getTime()));
    System.out.println("Total time spent for two: "
        + SAXFactory.timeToString(start.getTime(), secondDiscord.getTime()));

  }

  private static double[] loadData(String fname) {

    double ts[] = new double[1];

    Path path = Paths.get(fname);

    ArrayList<Double> data = new ArrayList<Double>();

    try {

      BufferedReader reader = Files.newBufferedReader(path, DEFAULT_CHARSET);

      String line = null;
      long lineCounter = 0;
      while ((line = reader.readLine()) != null) {
        String[] lineSplit = line.trim().split("\\s+");
        for (int i = 0; i < lineSplit.length; i++) {
          double value = new BigDecimal(lineSplit[i]).doubleValue();
          data.add(value);
        }
        lineCounter++;
      }
      reader.close();
    }
    catch (Exception e) {
      System.err.println(StackTrace.toString(e));
    }
    finally {
      assert true;
    }

    if (!(data.isEmpty())) {
      ts = new double[data.size()];
      for (int i = 0; i < data.size(); i++) {
        ts[i] = data.get(i);
      }
    }

    return ts;

  }

}
