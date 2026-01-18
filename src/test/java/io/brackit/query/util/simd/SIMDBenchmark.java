/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.brackit.query.util.simd;

import java.util.Arrays;
import java.util.Random;

/**
 * Simple benchmark for SIMD operations.
 * Run with: mvn test-compile exec:java -Dexec.mainClass="io.brackit.query.util.simd.SIMDBenchmark"
 * Or from IDE as a Java application.
 *
 * @author Brackit Project Team
 */
public class SIMDBenchmark {

  private static final int WARMUP_ITERATIONS = 5;
  private static final int MEASUREMENT_ITERATIONS = 10;
  private static final int OPS_PER_ITERATION = 1000;

  public static void main(String[] args) {
    System.out.println("SIMD Benchmark");
    System.out.println("==============");
    System.out.println("Vector lengths: byte=" + VectorOps.getByteVectorLength() + ", int=" + VectorOps
                                                                                                       .getIntVectorLength()
        + ", long=" + VectorOps.getLongVectorLength() + ", double=" + VectorOps.getDoubleVectorLength());
    System.out.println();

    int[] sizes = { 64, 256, 1024, 4096, 16384 };

    for (int size : sizes) {
      System.out.println("=== Size: " + size + " ===");
      benchmarkStringEquals(size);
      benchmarkStringCompare(size);
      benchmarkSumLong(size);
      benchmarkSumDouble(size);
      benchmarkMinLong(size);
      benchmarkMaxLong(size);
      benchmarkCountGreaterThan(size);
      System.out.println();
    }
  }

  private static void benchmarkStringEquals(int size) {
    byte[] strA = new byte[size];
    byte[] strB = new byte[size];
    new Random(42).nextBytes(strA);
    System.arraycopy(strA, 0, strB, 0, size);

    // Warmup
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        VectorOps.stringEquals(strA, strB);
        Arrays.equals(strA, strB);
      }
    }

    // SIMD measurement
    long simdStart = System.nanoTime();
    boolean simdResult = false;
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        simdResult = VectorOps.stringEquals(strA, strB);
      }
    }
    long simdTime = System.nanoTime() - simdStart;

    // Scalar measurement
    long scalarStart = System.nanoTime();
    boolean scalarResult = false;
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        scalarResult = Arrays.equals(strA, strB);
      }
    }
    long scalarTime = System.nanoTime() - scalarStart;

    printResults("stringEquals", simdTime, scalarTime, simdResult == scalarResult);
  }

  private static void benchmarkStringCompare(int size) {
    byte[] strA = new byte[size];
    byte[] strB = new byte[size];
    new Random(42).nextBytes(strA);
    System.arraycopy(strA, 0, strB, 0, size);
    strB[size - 1] = (byte) (strA[size - 1] + 1); // Make B slightly larger

    // Warmup
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        VectorOps.stringCompare(strA, strB);
        Arrays.compare(strA, strB);
      }
    }

    // SIMD measurement
    long simdStart = System.nanoTime();
    int simdResult = 0;
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        simdResult = VectorOps.stringCompare(strA, strB);
      }
    }
    long simdTime = System.nanoTime() - simdStart;

    // Scalar measurement
    long scalarStart = System.nanoTime();
    int scalarResult = 0;
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        scalarResult = Arrays.compare(strA, strB);
      }
    }
    long scalarTime = System.nanoTime() - scalarStart;

    printResults("stringCompare", simdTime, scalarTime, Integer.signum(simdResult) == Integer.signum(scalarResult));
  }

  private static void benchmarkSumLong(int size) {
    long[] values = new long[size];
    for (int i = 0; i < size; i++) {
      values[i] = i;
    }

    // Warmup
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        VectorOps.sumLong(values, 0, size);
        scalarSumLong(values);
      }
    }

    // SIMD measurement
    long simdStart = System.nanoTime();
    long simdResult = 0;
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        simdResult = VectorOps.sumLong(values, 0, size);
      }
    }
    long simdTime = System.nanoTime() - simdStart;

    // Scalar measurement
    long scalarStart = System.nanoTime();
    long scalarResult = 0;
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        scalarResult = scalarSumLong(values);
      }
    }
    long scalarTime = System.nanoTime() - scalarStart;

    printResults("sumLong", simdTime, scalarTime, simdResult == scalarResult);
  }

  private static void benchmarkSumDouble(int size) {
    double[] values = new double[size];
    for (int i = 0; i < size; i++) {
      values[i] = i * 0.1;
    }

    // Warmup
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        VectorOps.sumDouble(values, 0, size);
        scalarSumDouble(values);
      }
    }

    // SIMD measurement
    long simdStart = System.nanoTime();
    double simdResult = 0;
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        simdResult = VectorOps.sumDouble(values, 0, size);
      }
    }
    long simdTime = System.nanoTime() - simdStart;

    // Scalar measurement
    long scalarStart = System.nanoTime();
    double scalarResult = 0;
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        scalarResult = scalarSumDouble(values);
      }
    }
    long scalarTime = System.nanoTime() - scalarStart;

    printResults("sumDouble", simdTime, scalarTime, Math.abs(simdResult - scalarResult) < 0.01);
  }

  private static void benchmarkMinLong(int size) {
    long[] values = new long[size];
    Random rng = new Random(42);
    for (int i = 0; i < size; i++) {
      values[i] = rng.nextLong();
    }

    // Warmup
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        VectorOps.minLong(values, 0, size);
        scalarMinLong(values);
      }
    }

    // SIMD measurement
    long simdStart = System.nanoTime();
    long simdResult = 0;
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        simdResult = VectorOps.minLong(values, 0, size);
      }
    }
    long simdTime = System.nanoTime() - simdStart;

    // Scalar measurement
    long scalarStart = System.nanoTime();
    long scalarResult = 0;
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        scalarResult = scalarMinLong(values);
      }
    }
    long scalarTime = System.nanoTime() - scalarStart;

    printResults("minLong", simdTime, scalarTime, simdResult == scalarResult);
  }

  private static void benchmarkMaxLong(int size) {
    long[] values = new long[size];
    Random rng = new Random(42);
    for (int i = 0; i < size; i++) {
      values[i] = rng.nextLong();
    }

    // Warmup
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        VectorOps.maxLong(values, 0, size);
        scalarMaxLong(values);
      }
    }

    // SIMD measurement
    long simdStart = System.nanoTime();
    long simdResult = 0;
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        simdResult = VectorOps.maxLong(values, 0, size);
      }
    }
    long simdTime = System.nanoTime() - simdStart;

    // Scalar measurement
    long scalarStart = System.nanoTime();
    long scalarResult = 0;
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        scalarResult = scalarMaxLong(values);
      }
    }
    long scalarTime = System.nanoTime() - scalarStart;

    printResults("maxLong", simdTime, scalarTime, simdResult == scalarResult);
  }

  private static void benchmarkCountGreaterThan(int size) {
    long[] values = new long[size];
    for (int i = 0; i < size; i++) {
      values[i] = i;
    }
    long threshold = size / 2;

    // Warmup
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        VectorOps.countGreaterThan(values, 0, size, threshold);
        scalarCountGreaterThan(values, threshold);
      }
    }

    // SIMD measurement
    long simdStart = System.nanoTime();
    int simdResult = 0;
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        simdResult = VectorOps.countGreaterThan(values, 0, size, threshold);
      }
    }
    long simdTime = System.nanoTime() - simdStart;

    // Scalar measurement
    long scalarStart = System.nanoTime();
    int scalarResult = 0;
    for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
      for (int j = 0; j < OPS_PER_ITERATION; j++) {
        scalarResult = scalarCountGreaterThan(values, threshold);
      }
    }
    long scalarTime = System.nanoTime() - scalarStart;

    printResults("countGreaterThan", simdTime, scalarTime, simdResult == scalarResult);
  }

  // Scalar implementations for comparison
  private static long scalarSumLong(long[] values) {
    long sum = 0;
    for (long v : values) {
      sum += v;
    }
    return sum;
  }

  private static double scalarSumDouble(double[] values) {
    double sum = 0;
    for (double v : values) {
      sum += v;
    }
    return sum;
  }

  private static long scalarMinLong(long[] values) {
    long min = Long.MAX_VALUE;
    for (long v : values) {
      if (v < min)
        min = v;
    }
    return min;
  }

  private static long scalarMaxLong(long[] values) {
    long max = Long.MIN_VALUE;
    for (long v : values) {
      if (v > max)
        max = v;
    }
    return max;
  }

  private static int scalarCountGreaterThan(long[] values, long threshold) {
    int count = 0;
    for (long v : values) {
      if (v > threshold)
        count++;
    }
    return count;
  }

  private static void printResults(String name, long simdTimeNs, long scalarTimeNs, boolean correct) {
    double simdMs = simdTimeNs / 1_000_000.0;
    double scalarMs = scalarTimeNs / 1_000_000.0;
    double speedup = (double) scalarTimeNs / simdTimeNs;
    String correctStr = correct ? "OK" : "MISMATCH";

    System.out.printf("  %-20s: SIMD=%8.3fms  Scalar=%8.3fms  Speedup=%.2fx  [%s]%n",
                      name,
                      simdMs,
                      scalarMs,
                      speedup,
                      correctStr);
  }
}
