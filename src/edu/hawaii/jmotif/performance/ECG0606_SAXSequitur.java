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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import edu.hawaii.jmotif.distance.EuclideanDistance;
import edu.hawaii.jmotif.gi.GrammarRuleRecord;
import edu.hawaii.jmotif.gi.GrammarRules;
import edu.hawaii.jmotif.gi.sequitur.SequiturFactory;
import edu.hawaii.jmotif.logic.RuleInterval;
import edu.hawaii.jmotif.sax.NumerosityReductionStrategy;
import edu.hawaii.jmotif.sax.SAXFactory;
import edu.hawaii.jmotif.sax.datastructures.DiscordRecords;
import edu.hawaii.jmotif.sax.trie.TrieException;
import edu.hawaii.jmotif.timeseries.TSException;
import edu.hawaii.jmotif.timeseries.TSUtils;
import edu.hawaii.jmotif.util.StackTrace;

public class ECG0606_SAXSequitur {

  final static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private static final String dataFName = "data/ecg0606_1.csv";

  private static final String OUT_FNAME = "RCode/ecg0606_1_discords_density.csv";
  private static final String OUT_DISTANCES_FNAME = "RCode/ecg0606_1_distances_curve.csv";

  public static void main(String[] args) throws TSException, TrieException, IOException {

    // get the data
    //
    double[] ts = loadData(dataFName);
    int[] coverageCurve = new int[ts.length];

    Date start = new Date();

    // get a coverage density curve
    //
    int w = 100;
    int p = 9;
    int a = 5;

    int[] dr = SequiturFactory.series2RulesDensity(ts, w, p, a);
    for (int j = 0; j < dr.length; j++) {
      coverageCurve[j] = coverageCurve[j] + dr[j];
    }

    BufferedWriter bw = new BufferedWriter(new FileWriter(new File(OUT_FNAME)));
    for (int i : coverageCurve) {
      bw.write(i + "\n");
    }
    bw.close();

    // build an array of rules with their average coverage
    //
    GrammarRules rules = SequiturFactory.series2SequiturRules(ts, w, p, a,
        NumerosityReductionStrategy.EXACT, 0.05);

    ArrayList<RuleInterval> intervals = new ArrayList<RuleInterval>();

    // populate all intervals with their coverage
    //
    for (GrammarRuleRecord e : rules) {
      for (RuleInterval ri : e.getRuleIntervals()) {
        double[] subsequence = subsequence(coverageCurve, ri.getStartPos(), ri.getEndPos());
        double mean = TSUtils.mean(subsequence);
        ri.setCoverage(mean);
        ri.setId(e.ruleNumber());
        intervals.add(ri);
      }
    }
    // check if somewhere there is a ZERO coverage!
    //
    for (int i = 0; i < coverageCurve.length; i++) {
      if (0 == coverageCurve[i]) {
        int j = i;
        while ((j < coverageCurve.length) && (0 == coverageCurve[j])) {
          j++;
        }
        intervals.add(new RuleInterval(0, i, j, 0.0d));
        i = j;
      }
    }
    // run HOTSAX with this intervals set
    //
    DiscordRecords discords = SAXFactory.series2SAXSequiturAnomalies(ts, 1, intervals);

    Date end = new Date();

    System.out.println("discords search finished in : "
        + SAXFactory.timeToString(start.getTime(), end.getTime()));

    // now lets find all the distances to non-self match
    //
    double[] distances = new double[ts.length];
    double[] widths = new double[ts.length];

    Collections.sort(intervals, new Comparator<RuleInterval>() {
      @Override
      public int compare(RuleInterval c1, RuleInterval c2) {
        if (c1.getStartPos() > c2.getStartPos()) {
          return 1;
        }
        else if (c1.getStartPos() < c2.getStartPos()) {
          return -1;

        }
        return 0;
      }
    });

    for (RuleInterval ri : intervals) {

      int ruleStart = ri.getStartPos();
      int ruleEnd = ruleStart + ri.getLength();
      int window = ruleEnd - ruleStart;

      double[] cw = TSUtils.subseries(ts, ruleStart, window);

      double cwNNDist = Double.MAX_VALUE;

      for (int j = 0; j < ts.length - window - 1; j++) {

        if (Math.abs(ruleStart - j) > window) {

          double[] currentSubsequence = TSUtils.subseries(ts, j, window);
          double dist = EuclideanDistance.distance(cw, currentSubsequence);

          if (dist < cwNNDist) {
            cwNNDist = dist;
          }

        }
      }

      distances[ruleStart] = cwNNDist;
      widths[ruleStart] = ri.getLength();
    }

    bw = new BufferedWriter(new FileWriter(new File(OUT_DISTANCES_FNAME)));
    for (int i = 0; i < distances.length; i++) {
      bw.write(distances[i] + "," + widths[i] + "\n");
    }
    bw.close();

  }

  private static double[] subsequence(int[] coverageCurve, int startPos, int endPos) {
    double[] res = new double[endPos - startPos];
    for (int i = startPos; i < endPos; i++) {
      res[i - startPos] = Integer.valueOf(coverageCurve[i]).doubleValue();
    }
    return res;
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
        if (lineCounter > 16500) {
          break;
        }
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
