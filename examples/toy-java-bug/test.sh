#!/usr/bin/env bash
set -euo pipefail

rm -rf build
mkdir -p build
javac -d build src/Calculator.java test/CalculatorTest.java
java -cp build CalculatorTest
