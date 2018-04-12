/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.cmd;

import static com.yahoo.sketches.Files.appendStringToFile;

import java.io.File;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * @author Lee Rhodes
 */
public class CommandLineTest {
  public static final String LS = System.getProperty("line.separator");
  String dataFileName1 = "data1.txt";
  String dataFileName2 = "data2.txt";
  String ranksFileName = "ranks.txt";
  String valuesFileName = "values.txt";
  String freqDataFileName = "freqData.txt";
  String serFileName1 = "ser1.bin";
  String serFileName2 = "ser2.bin";


  @Test  //enable/disable here for visual checking
  public void outputHelp() {
    String[] line = new String[] {""};
    CommandLine.main(line);

    println("");
    line = new String[] {"freq", "-help"};
    CommandLine.main(line);

    println("");
    line = new String[] {"hll", "-help"};
    CommandLine.main(line);

    println("");
    line = new String[] {"quant", "-help"};
    CommandLine.main(line);

    println("");
    line = new String[] {"rsamp", "-help"};
    CommandLine.main(line);

    println("");
    line = new String[] {"theta", "-help"};
    CommandLine.main(line);

    println("");
    line = new String[] {"vsamp", "-help"};
    CommandLine.main(line);
  }

  @AfterClass
  public void deleteFiles() {
    deleteFile(dataFileName1);
    deleteFile(dataFileName2);
    deleteFile(ranksFileName);
    deleteFile(valuesFileName);
    deleteFile(freqDataFileName);
    deleteFile(serFileName1);
    deleteFile(serFileName2);
  }

  //TEST UNIQUES
  @Test
  public void checkTheta() {
    deleteFile(serFileName1);
    deleteFile(serFileName2);
    println("Creating Data Files...");

    createUniquesFile(1, 20000, dataFileName1);
    createUniquesFile(15001, 20000, dataFileName2);

    println("\nUpdating Theta Sketch 1:");
    callMain("theta -k 4096 -d " + dataFileName1 + " -o " + serFileName1);

    println("\nUpdating Theta Sketch 2:");
    callMain("theta -k 4096 -d " + dataFileName2 + " -o " + serFileName2);

    println("\nMerge Theta Sketches 1 and 2:");
    callMain("theta -k 4096 -s " + serFileName1 + " " + serFileName2);

    println("\nIntersect Theta Sketches 1 and 2:");
    callMain("theta -k 4096 -i -s " + serFileName1 + " " + serFileName2);

    println("\nTheta Sketch 1 minus Theta Sketch 2 and Summarize:");
    callMain("theta -k 4096 -p -m -s " + serFileName1 + " " + serFileName2);
  }

  @Test
  public void checkHll() {
    deleteFile(serFileName1);
    deleteFile(serFileName2);
    println("Creating Data Files...");

    createUniquesFile(0, 20000, dataFileName1);
    createUniquesFile(15000, 20000, dataFileName2); //overlap is 500

    println("\nUpdating HLL Sketch 1:");
    callMain("hll -lgk 12 -d " + dataFileName1 + " -o " + serFileName1);

    println("\nUpdating HLL Sketch 2:");
    callMain("hll -lgk 12 -d " + dataFileName2 + " -o " + serFileName2);

    println("\nMerge HLL Sketch 1 and 2 and Summarize:");
    callMain("hll -lgk 12 -p -s " + serFileName1 + " " + serFileName2);
  }

  //TEST QUANTILES
  @Test
  public void checkQuantiles() {
    deleteFile(serFileName1);
    deleteFile(serFileName2);
    println("Creating Data Files...");

    createUniquesFile(0, 20000, dataFileName1);

    println("\nUpdating Quantiles Sketch 1, no options, default deciles");
    callMain("quant -k 256 -d " + dataFileName1 + " -o " + serFileName1);

    println("\nUpdating Quantiles Sketch 2, options: median");
    callMain("quant -k 256 -m -d " + dataFileName1 + " -o " + serFileName2);

    println("\nMerge Quantiles Sketch 1 and 2, options: summarize, default deciles");
    callMain("quant -k 256 -p -s " + serFileName1 + " " + serFileName2);

    println("\nQuery Histograms from Sketch 1, options: lin & log histograms");
    callMain("quant -k 256 -s " + serFileName1 + " -b 30 -h -lh 1");

    println("\nQuery specific values from ranks from Sketch 1");
    callMain("quant -k 256 -s " + serFileName1 + " -V 1 10000 20000");

    println("\nQuery specific ranks from values from Sketch 1");
    callMain("quant -k 256 -s " + serFileName1 + " -R 0 .5 1");

    createRanksFile(ranksFileName);
    println("\nQuery specific values from ranks from file from Sketch 1");
    callMain("quant -k 256 -s " + serFileName1 + " -r " + ranksFileName);

    createValuesFile(valuesFileName);
    println("\nQuery specific ranks from values from file from Sketch 1");
    callMain("quant -k 256 -s " + serFileName1 + " -v " + valuesFileName);
  }

  //TEST FREQUENT ITEMS
  @Test
  public void checkFreqItems() {
    deleteFile(serFileName1);
    deleteFile(serFileName2);
    println("Creating Data Files...");

    createFreqDataFile(freqDataFileName);

    println("\nUpdating Freq Items Sketch 1.");
    callMain("freq -k 256 -w -e -n -t -T -d " + freqDataFileName + " -o " + serFileName1);
  }

  //TEST Reservoir Samples
  @Test
  public void checkReservior() {
    deleteFile(serFileName1);
    deleteFile(serFileName2);
    println("Creating Data Files...");

    createUniquesFile(0, 20000, dataFileName1);

    println("\nUpdating Reservior Sketch 1 with summary");
    callMain("rsamp -k 25  -d " + dataFileName1 + " -o " + serFileName1);

    println("\nUpdating Reservior Sketch 2 with summary");
    callMain("rsamp -k 25 -p -d " + dataFileName1 + " -o " + serFileName2);

    println("\nMerge Reservior Sketch 1 and 2 with summary");
    callMain("rsamp -k 25 -p -s " + serFileName1 + " " + serFileName2);
  }

  private static void createUniquesFile(int start, int len, String fileName) {
    File file = new File(fileName);
    if (file.exists()) { return; }
    for (int i = 0; i < len; i++) {
      appendStringToFile(Integer.toString(i + start) + LS, fileName);
    }
  }

  private static void createRanksFile(String fileName) {
    File file = new File(fileName);
    if (file.exists()) { return; }
    appendStringToFile("0.0" + LS, fileName);
    appendStringToFile("0.5" + LS, fileName);
    appendStringToFile("1.0", fileName);
  }

  private static void createValuesFile(String fileName) {
    File file = new File(fileName);
    if (file.exists()) { return; }
    appendStringToFile("0" + LS, fileName);
    appendStringToFile("10000" + LS, fileName);
    appendStringToFile("20000", fileName);
  }

  private static void createFreqDataFile(String fileName) {
    File file = new File(fileName);
    if (file.exists()) { return; }
    for (int i = 1; i <= 19975; i++) {
      appendStringToFile("1\t" + i + LS, fileName);
    }
    for (int i = 19976; i <= 20000; i++) {
      appendStringToFile(i + "\t" + i + LS, fileName);
    }
  }

  private static void callMain(String s) {
    String[] cl = s.split(" +");
    CommandLine.main(cl);
  }

  private static void deleteFile(String fileName) {
    File file = new File(fileName);
    if (file.exists()) { file.delete(); }
  }

  @Test
  public void printlnTest() {
    println("PRINTING: "+this.getClass().getName());
  }

  /**
   * @param s value to print
   */
  static void println(String s) {
    System.out.println(s); //disable here
  }
}