#!/bin/bash
set -a
source "$(dirname "$0")/.env"
set +a

# Java 17 필요 (Gradle 8.x가 Java 25 미지원)
export JAVA_HOME=/Users/sanghyeok/Library/Java/JavaVirtualMachines/ms-17.0.16/Contents/Home

cd "$(dirname "$0")"
./gradlew bootRun --no-daemon
