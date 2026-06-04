#!/bin/bash
cd /home/passwd_v2
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export ANDROID_HOME=/root/Android
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/34.0.0:$PATH
chmod +x gradlew
echo "=== Java Version ==="
java -version 2>&1
echo ""
echo "=== Android SDK ==="
ls $ANDROID_HOME/build-tools/ 2>&1
ls $ANDROID_HOME/platforms/ 2>&1
echo ""
echo "=== Compiling ==="
./gradlew assembleDebug 2>&1
echo ""
echo "=== Exit Code: $? ==="
