package io.brackit.query.atomic;

import io.brackit.query.BrackitQueryContext;
import io.brackit.query.Query;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalTimezoneTest {
  @ParameterizedTest
  @ValueSource(strings = { "America/New_York", "America/St_Johns", "GMT-00:30", "Europe/Berlin", "Asia/Kolkata",
      "UTC" })
  public void localTimezoneStoresMagnitudeAndSign(String timezone) throws Exception {
    fork(timezone, "offset");
  }

  @ParameterizedTest
  @ValueSource(strings = { "America/New_York", "America/St_Johns", "GMT-00:30", "Europe/Berlin", "Asia/Kolkata",
      "UTC" })
  public void currentDateTimeAndTimeMatchClock(String timezone) throws Exception {
    fork(timezone, "clock");
  }

  private static void fork(String timezone, String check) throws Exception {
    // LOCAL_TIMEZONE is initialized once, so each default timezone needs a fresh JVM.
    Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                                         "-Xmx128m",
                                         "--enable-preview",
                                         "--add-modules=jdk.incubator.vector",
                                         "-Duser.timezone=" + timezone,
                                         "-cp",
                                         System.getProperty("surefire.test.class.path",
                                                            System.getProperty("java.class.path")),
                                         LocalTimezoneTest.class.getName(),
                                         check).redirectErrorStream(true).start();
    try {
      assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Timezone probe timed out: " + timezone);
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      assertEquals(0, process.exitValue(), timezone + " / " + check + ":\n" + output);
    } finally {
      if (process.isAlive()) {
        process.destroyForcibly();
      }
    }
  }

  public static void main(String[] args) {
    int offset = TimeZone.getDefault().getOffset(System.currentTimeMillis());
    int magnitude = Math.abs(offset);
    if (args[0].equals("offset")) {
      DTD timezone = AbstractTimeInstant.LOCAL_TIMEZONE;
      assertAll(() -> assertEquals(offset < 0, timezone.isNegative()),
                () -> assertEquals(0, timezone.getDays()),
                () -> assertEquals(magnitude / 3_600_000, timezone.getHours()),
                () -> assertEquals(magnitude / 60_000 % 60, timezone.getMinutes()),
                () -> assertEquals(magnitude % 60_000 * 1000, timezone.getMicros()));
    } else {
      ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(offset / 1000);
      // Construct an independent, valid DTD to catch DateTime's sign handling even
      // when the LOCAL_TIMEZONE producer is still broken.
      DTD timezone = new DTD(offset < 0, 0, (byte) (magnitude / 3_600_000), (byte) (magnitude / 60_000 % 60), 0);
      assertAll(() -> {
        Instant before = Instant.now();
        OffsetDateTime actual = OffsetDateTime.parse(new DateTime(timezone).stringValue());
        assertCurrentInstant(actual, expectedOffset, before, Instant.now());
      }, () -> {
        BrackitQueryContext context = new BrackitQueryContext();
        Instant before = Instant.now();
        DateTime dateTime = (DateTime) new Query("current-dateTime()").execute(context);
        Instant after = Instant.now();
        OffsetDateTime actual = OffsetDateTime.parse(dateTime.stringValue());
        assertCurrentInstant(actual, expectedOffset, before, after);
        Time time = (Time) new Query("current-time()").execute(context);
        assertEquals(actual.toOffsetTime(), OffsetTime.parse(time.stringValue()));
      });
    }
  }

  private static void assertCurrentInstant(OffsetDateTime actual, ZoneOffset expectedOffset, Instant before,
      Instant after) {
    assertEquals(expectedOffset, actual.getOffset());
    // DateTime reads a millisecond clock; allow its truncation of the lower precision.
    assertTrue(!actual.toInstant().isBefore(before.minusMillis(1)) && !actual.toInstant().isAfter(after),
               "Expected current instant between " + before + " and " + after + ", got " + actual);
  }
}
