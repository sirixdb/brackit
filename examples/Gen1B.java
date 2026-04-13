import java.io.*;
import java.util.Random;
import java.nio.charset.StandardCharsets;

/**
 * Fast 1B-record generator using raw byte[] output (no String→UTF-8 re-encoding).
 * Target: ~48GB for 1B records, matching memory's "48GB" baseline.
 * Schema: {"age":N,"dept":"X","city":"X","active":T}
 */
public class Gen1B {
  private static final byte[][] DEPTS = toBytes(new String[] {
    "Eng", "Sales", "Mkt", "Ops", "HR", "Finance", "Legal", "Supp"
  });
  private static final byte[][] CITIES = toBytes(new String[] {
    "NYC", "LA", "SF", "ATL", "BOS", "CHI", "DEN", "DAL"
  });
  private static final byte[] P1 = "{\"age\":".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] P2 = ",\"dept\":\"".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] P3 = "\",\"city\":\"".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] P4 = "\",\"active\":".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] TRUE = "true".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] FALSE = "false".getBytes(StandardCharsets.US_ASCII);

  private static byte[][] toBytes(String[] arr) {
    byte[][] r = new byte[arr.length][];
    for (int i = 0; i < arr.length; i++) r[i] = arr[i].getBytes(StandardCharsets.US_ASCII);
    return r;
  }

  // Write ASCII int into buffer at offset; return new offset.
  private static int writeInt(byte[] buf, int off, int v) {
    if (v < 10) { buf[off++] = (byte)('0' + v); return off; }
    // v is 2-3 digits for age (18-65), but keep general for safety
    int start = off;
    while (v > 0) { buf[off++] = (byte)('0' + (v % 10)); v /= 10; }
    // reverse the digits
    for (int l = start, r = off - 1; l < r; l++, r--) {
      byte t = buf[l]; buf[l] = buf[r]; buf[r] = t;
    }
    return off;
  }

  public static void main(String[] args) throws Exception {
    long N = 1_000_000_000L;
    String path = args.length > 0 ? args[0] : "/tmp/bench_1B.json";
    Random rng = new Random(42);
    // 16MB write buffer
    final int BUF = 16 * 1024 * 1024;
    byte[] buf = new byte[BUF + 128]; // slack for a record
    int pos = 0;
    try (FileOutputStream fos = new FileOutputStream(path)) {
      buf[pos++] = (byte)'[';
      long start = System.nanoTime();
      for (long i = 0; i < N; i++) {
        if (i > 0) buf[pos++] = (byte)',';
        System.arraycopy(P1, 0, buf, pos, P1.length); pos += P1.length;
        pos = writeInt(buf, pos, 18 + rng.nextInt(48));
        System.arraycopy(P2, 0, buf, pos, P2.length); pos += P2.length;
        byte[] d = DEPTS[rng.nextInt(DEPTS.length)];
        System.arraycopy(d, 0, buf, pos, d.length); pos += d.length;
        System.arraycopy(P3, 0, buf, pos, P3.length); pos += P3.length;
        byte[] c = CITIES[rng.nextInt(CITIES.length)];
        System.arraycopy(c, 0, buf, pos, c.length); pos += c.length;
        System.arraycopy(P4, 0, buf, pos, P4.length); pos += P4.length;
        if (rng.nextBoolean()) {
          System.arraycopy(TRUE, 0, buf, pos, TRUE.length); pos += TRUE.length;
        } else {
          System.arraycopy(FALSE, 0, buf, pos, FALSE.length); pos += FALSE.length;
        }
        buf[pos++] = (byte)'}';
        if (pos >= BUF) {
          fos.write(buf, 0, pos);
          pos = 0;
        }
        if (i > 0 && i % 100_000_000L == 0) {
          long ms = (System.nanoTime() - start) / 1_000_000;
          System.err.printf("  %,d / %,d  (%,d ms)%n", i, N, ms);
        }
      }
      buf[pos++] = (byte)']';
      fos.write(buf, 0, pos);
    }
    System.err.println("done");
  }
}
