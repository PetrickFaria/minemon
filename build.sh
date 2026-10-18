#!/usr/bin/env sh
set -eu
java -version 2>&1 | grep -q '17\.' || { echo 'Use Java 17.' >&2; exit 1; }
if [ -x ./gradlew ]; then
  ./gradlew --no-daemon build
elif command -v gradle >/dev/null 2>&1; then
  gradle --no-daemon build
else
  echo 'No Gradle Wrapper is available and Gradle is not installed. Use the GitHub Actions workflow or install Gradle 7.5.1.' >&2
  exit 1
fi
