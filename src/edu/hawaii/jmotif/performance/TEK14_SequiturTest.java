package edu.hawaii.jmotif.performance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import edu.hawaii.jmotif.gi.sequitur.SequiturFactory;
import edu.hawaii.jmotif.sax.SAXFactory;
import edu.hawaii.jmotif.sax.trie.TrieException;
import edu.hawaii.jmotif.timeseries.TSException;
import edu.hawaii.jmotif.util.StackTrace;

public class TEK14_SequiturTest {

  final static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private static final String dataFName = "/home/psenin/git/jmotif-sequitur.git/data/PAKDD/TEK14.txt";

  private static final String OUT_FNAME = "/home/psenin/git/jmotif-sequitur.git/data/PAKDD/performance/TEK14_Squitur_coverage.csv";

  public static void main(String[] args) throws TSException, TrieException, IOException {

    double[] ts = loadData(dataFName);

    // mark start
    Date start = new Date();

    int[] coverageCurve = new int[ts.length];
    int[] windows = { 300 };
    int[] paas = { 8 };
    int[] alphabets = { 8 };

    for (int w : windows) {
      for (int p : paas) {
        for (int a : alphabets) {
          coverageCurve = SequiturFactory.series2RulesDensity(ts, w, p, a);
          // for (SequiturDiscordRecord r : dr) {
          // for (int i = r.getStart(); i < r.getEnd(); i++) {
          // coverageCurve[i]++;
          // }
          // }
        }
      }
    }

    int minValue = Integer.MAX_VALUE;
    int minPosition = -1;
    for (int i = coverageCurve.length - 100; i >= 0; i--) {
      if (coverageCurve[i] < minValue) {
        minValue = coverageCurve[i];
        minPosition = i;
      }
    }

    Date end = new Date();
    System.out.println("Discord position: " + minPosition + ", minimal distance: " + minValue);
    System.out.println("Found in " + SAXFactory.timeToString(start.getTime(), end.getTime()));

    BufferedWriter bw = new BufferedWriter(new FileWriter(new File(OUT_FNAME)));
    for (int i : coverageCurve) {
      bw.write(i + "\n");
    }
    bw.close();
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
