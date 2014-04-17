package com.squareup.wire;

import java.util.Random;

public class WireInputBenchmark {

  private static final int ITERS = 1000;

  public static void main(String[] args) throws Exception {
    byte[] buffer = new byte[1024*1024];
    int offset = 0;
    int written = 0;
    Random random = new Random(1);
    while (offset < buffer.length - 10) {
      int count = WireOutput.writeVarint(random.nextInt(1000) - 500, buffer, offset);
      offset += count;
      written++;
    }

    long total = 0L;
    for (int times = 0; times < 10; times++) {
      long start = System.currentTimeMillis();
      for (int iter = 0; iter < ITERS; iter++) {
        WireInput input = WireInput.newInstance(buffer);
        for (int i = 0; i < written; i++) {
          int value = input.readVarint32();
        }
      }
      long end = System.currentTimeMillis();
      total += end - start;
      System.out.println("Read " + written * ITERS + " varints in " + (end - start) + " ms");
    }
    System.out.println(total);
  }
}
