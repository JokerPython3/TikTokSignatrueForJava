#!/usr/bin/env bash
# Build the TikTokSignatrueForJava library (+ example) and run its test suite
# without Maven — a plain-javac path that needs only a JDK (no external jars).
# A tiny in-repo @Test annotation stub lets the JUnit-marked test sources
# compile without downloading anything; Maven/Gradle use the real JUnit instead.
#
# Author: S1
# GitHub: github.com/JokerPython3
# Language: Java
set -euo pipefail
cd "$(dirname "$0")"

echo "==> Building @Test annotation stub (in-repo, replaces JUnit for this path)..."
rm -rf out/stub
mkdir -p out/stub
javac -d out/stub src/test/stub/org/junit/Test.java

echo "==> Building main sources (library + Example)..."
rm -rf out/main
mkdir -p out/main
javac -d out/main src/main/java/tiktoksignature/*.java src/main/java/Example.java

echo "==> Building test sources..."
rm -rf out/test
mkdir -p out/test
javac -cp "out/main:out/stub" -d out/test src/test/java/tiktoksignature/*.java

echo "==> Running tests (from project root so testdata/ is discovered)..."
java -cp "out/main:out/test" tiktoksignature.TestRunner

echo "==> Build + tests complete."