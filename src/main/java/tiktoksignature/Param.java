// Param — one ordered key/value pair, the Java equivalent of a single entry in
// the Python params/data/cookie dicts used by SignerPy.sign.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

public final class Param {

    public final String key;
    public final String value;

    public Param(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
