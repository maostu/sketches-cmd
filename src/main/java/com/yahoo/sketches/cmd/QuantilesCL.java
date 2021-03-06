/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.cmd;

import static com.yahoo.sketches.Util.TAB;
import static java.lang.Math.log10;
import static java.lang.Math.pow;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.quantiles.DoublesSketch;
import com.yahoo.sketches.quantiles.DoublesSketchBuilder;
import com.yahoo.sketches.quantiles.DoublesUnion;
import com.yahoo.sketches.quantiles.DoublesUnionBuilder;
import com.yahoo.sketches.quantiles.UpdateDoublesSketch;

  public class QuantilesCL extends SketchCommandLineParser<UpdateDoublesSketch> {

    private static final int DEFAULT_NUM_BINS = 10;

    QuantilesCL() {
      super();
      // input options
      options.addOption(Option.builder("k")
          .desc("parameter k")
          .hasArg()
          .build());
      // output options
      options.addOption(Option.builder("r")
          .longOpt("rank2value")
          .desc("query values with ranks from list DOUBLES")
          .hasArgs() //unlimited
          .argName("DOUBLES")
          .build());
      options.addOption(Option.builder("R")
          .longOpt("rank2value-file")
          .desc("query values with ranks from FILE")
          .hasArg()
          .argName("FILE")
          .build());
      options.addOption(Option.builder("v")
          .longOpt("value2rank")
          .desc("query ranks with values from list DOUBLES")
          .hasArgs() //unlimited
          .argName("DOUBLES")
          .build());
      options.addOption(Option.builder("V")
          .longOpt("value2rank-file")
          .desc("query ranks with values from FILE")
          .hasArg()
          .argName("FILE")
          .build());
      options.addOption(Option.builder("b")
          .longOpt("number-histogram-bars")
          .desc("number of bars in the histogram")
          .hasArg()
          .argName("INT")
          .build());
      options.addOption(Option.builder("h")
          .longOpt("query-histogram")
          .desc("query histogram")
          .build());
      options.addOption(Option.builder("lh")
          .longOpt("query-loghistogram")
          .hasArg()
          .argName("Zero Substitution")
          .desc("query log scale histogram")
          .build());
    }

  @Override
  protected void showHelp() {
        final HelpFormatter helpf = new HelpFormatter();
        helpf.setOptionComparator(null);
        helpf.printHelp("ds quant", options);
  }

  protected UpdateDoublesSketch buildSketch() {
    final DoublesSketchBuilder builder = DoublesSketch.builder();
    if (cl.hasOption("k")) {
      builder.setK(Integer.parseInt(cl.getOptionValue("k")));
    }
    return builder.build();
  }

  @Override
  protected void updateSketch(final BufferedReader br) {
    final UpdateDoublesSketch sketch = buildSketch();
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        final double item = Double.parseDouble(itemStr);
        sketch.update(item);
      }
      sketchList.add(sketch);
    } catch (final IOException | NumberFormatException e ) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
  }

  @Override
  protected UpdateDoublesSketch deserializeSketch(final byte[] bytes) {
    return UpdateDoublesSketch.heapify(Memory.wrap(bytes));  //still questionable
  }

  @Override
  protected byte[] serializeSketch(final UpdateDoublesSketch sketch) {
    return sketch.toByteArray();
  }

  @Override
  protected void mergeSketches() {
    final DoublesUnionBuilder builder = DoublesUnion.builder();
    if (cl.hasOption("k")) {
      builder.setMaxK(Integer.parseInt(cl.getOptionValue("k")));
    }
    final DoublesUnion union = builder.build();
    for (UpdateDoublesSketch sketch: sketchList) {
      union.update(sketch);
    }
    sketchList.add(union.getResult());
  }

  @Override
  protected void queryCurrentSketch() {
    if (sketchList.size() > 0) {
      final UpdateDoublesSketch sketch = sketchList.get(sketchList.size() - 1);
      boolean optionChosen = false;

      if (cl.hasOption("h")) { //Histogram
        optionChosen = true;
        int splitPoints = DEFAULT_NUM_BINS - 1;
        if (cl.hasOption("b")) {
          splitPoints = Integer.parseInt(cl.getOptionValue("b")) - 1;
        }
        final long n = sketch.getN();
        final double[] splitsArr = getEvenSplits(sketch, splitPoints);
        final double[] histArr = sketch.getPMF(splitsArr);
        println("\nValue" + TAB + "Freq");
        final double min = sketch.getMinValue();
        String splitVal = String.format("%,f", min);
        String freqVal = String.format("%,d", (long)(histArr[0] * n));
        println(splitVal + TAB + freqVal);
        for (int i = 0; i < splitsArr.length; i++) {
          splitVal = String.format("%,f", splitsArr[i]  );
          freqVal = String.format("%,d", (long)(histArr[i + 1] * n));
          println(splitVal + TAB + freqVal);
        }
      }

      if (cl.hasOption("lh")) { //log Histogram
        optionChosen = true;
        final double zeroSub = Double.parseDouble(cl.getOptionValue("lh"));
        int splitPoints = DEFAULT_NUM_BINS - 1;
        if (cl.hasOption("b")) {
          splitPoints = Integer.parseInt(cl.getOptionValue("b")) - 1;
        }
        final long n = sketch.getN();
        final double[] splitsArr = getLogSplits(sketch, splitPoints, zeroSub);
        final double[] histArr = sketch.getPMF(splitsArr);
        println("\nValue" + TAB + "Freq");
        final double min = sketch.getMinValue();
        String splitVal = String.format("%,f", min);
        String freqVal = String.format("%,d", (long)(histArr[0] * n));
        println(splitVal + TAB + freqVal);
        for (int i = 0; i < splitsArr.length; i++) {
          splitVal = String.format("%,f", splitsArr[i] );
          freqVal = String.format("%,d", (long)(histArr[i + 1] * n));
          println(splitVal + TAB + freqVal);
        }
      }

      if (cl.hasOption("r")) { //ranks to value from list
        optionChosen = true;
        println("\nRank + TAB + Value");
        final String[] ranks = cl.getOptionValues("r");
        println("\nRank" + TAB + "Value");
        for (String rank : ranks) {
          final String quant = String.format("%.2f", sketch.getQuantile(Double.parseDouble(rank)));
          println(rank + TAB + quant);
        }
      }

      if (cl.hasOption("R")) { //ranks to value from file
        optionChosen = true;
        final String[] ranks = queryFileReader(cl.getOptionValue("R"));
        println("\nRank" + TAB + "Value");
        for (String rank: ranks) {
            final String quant = String.format("%.2f", sketch.getQuantile(Double.parseDouble(rank)));
            println(rank + TAB + quant);
        }
      }

      if (cl.hasOption("v")) { //values to ranks from list
        optionChosen = true;
        final String[] values = cl.getOptionValues("v");
        final double[] valuesArray = Arrays.stream(values).mapToDouble(Double::parseDouble).toArray();
        Arrays.sort(valuesArray);
        final double[] cdf =  sketch.getCDF(valuesArray);
        println("\nValue" + TAB + "Rank");
        for (int i = 0; i < valuesArray.length ; i++) {
          println(String.format("%.2f", valuesArray[i]) + TAB + String.format("%.6f",cdf[i]));
        }
      }

      if (cl.hasOption("V")) { //values to ranks from file
        optionChosen = true;
        final String[] items = queryFileReader(cl.getOptionValue("V"));
        final double[] valuesArray = Arrays.stream(items).mapToDouble(Double::parseDouble).toArray();
        final double[] cdf =  sketch.getCDF(valuesArray);
        println("\nValue" + TAB + "Rank");
        for (int i = 0; i < valuesArray.length ; i++) {
          println(String.format("%.2f", valuesArray[i]) + TAB + String.format("%.6f",cdf[i]));
        }
      }

      // print deciles if no other option chosen
      if (!optionChosen) {
        final double[] ranks = new double[] {0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
        println("Print deciles as default:");
        println("\nRank" + TAB + "Value");
        final double[] values = sketch.getQuantiles(ranks);
        for (int i = 0; i < values.length; i++) {
          println(String.format("%.1f", ranks[i]) + TAB + values[i]);
        }
      }
    }
  }

  private static double[] getEvenSplits(final DoublesSketch sketch, final int splitPoints) {
    final double min = sketch.getMinValue();
    final double max = sketch.getMaxValue();
    return getSplits(min, max, splitPoints);
  }

  private static double[] getLogSplits(final DoublesSketch sketch, final int splitPoints,
      final double zeroSub) {
    double min = sketch.getMinValue();
    min = (min == 0) ? zeroSub : min;
    if (min < 0) {
      throw new IllegalArgumentException(
          "Log Histogram cannot be produced with negative values in the stream.");
    }
    final double max = sketch.getMaxValue();
    final double logMin = log10(min);
    final double logMax = log10(max);
    final double[] logArr = getSplits(logMin, logMax, splitPoints);
    final double[] expArr = new double[logArr.length];
    for (int i = 0; i < logArr.length; i++) {
      expArr[i] = pow(10.0, logArr[i]);
    }
    return expArr;
  }

  private static double[] getSplits(final double min, final double max, final int splitPoints) {
    final double range = max - min;
    final double delta = range / (splitPoints + 1);
    final double[] splits = new double[splitPoints];
    for (int i = 0; i < splitPoints; i++) {
      splits[i] = min + (delta * (i + 1));
    }
    return splits;
  }

}
