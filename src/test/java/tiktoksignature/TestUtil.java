// Test utility assertions.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

import java.util.Arrays;

public final class TestUtil {

    private TestUtil() {}

    public static void assertEquals(String name, String want, String got) {
        if (!want.equals(got)) {
            throw new AssertionError(name + " mismatch:\n  want " + want + "\n  got  " + got);
        }
    }

    public static void assertEquals(String name, int want, int got) {
        if (want != got) {
            throw new AssertionError(name + " mismatch: want " + want + " got " + got);
        }
    }

    public static void assertEquals(String name, long want, long got) {
        if (want != got) {
            throw new AssertionError(name + " mismatch: want " + want + " got " + got);
        }
    }

    public static void assertBytes(String name, byte[] want, byte[] got) {
        if (!Arrays.equals(want, got)) {
            throw new AssertionError(name + " mismatch\n  want " + Arrays.toString(want)
                    + "\n  got  " + Arrays.toString(got));
        }
    }

    public static void assertTrue(String name, boolean cond) {
        if (!cond) {
            throw new AssertionError(name + " expected true but was false");
        }
    }
}
