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
import edu.hawaii.jmotif.sax.LargeWindowAlgorithm;
import edu.hawaii.jmotif.sax.SAXFactory;
import edu.hawaii.jmotif.sax.datastructures.DiscordRecords;
import edu.hawaii.jmotif.sax.trie.TrieException;
import edu.hawaii.jmotif.timeseries.TSException;
import edu.hawaii.jmotif.util.StackTrace;

public class ECG15_SAX_Trie {

  final static Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private static final String dataFName = "/home/psenin/git/jmotif-sequitur.git/data/PAKDD/chfdbchf15_1.csv";

  public static void main(String[] args) throws TSException, TrieException, IOException {

    double[] ts = loadData(dataFName);
    for (int i = 3; i < 7; i++) {
      Date start = new Date();
      DiscordRecords discords = SAXFactory.series2Discords(ts, 160, i, 1,
          new LargeWindowAlgorithm());
      Date end = new Date();
      System.out.println("Discord found in "
          + SAXFactory.timeToString(start.getTime(), end.getTime()));
      System.out.println(discords.toString());
    }

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
