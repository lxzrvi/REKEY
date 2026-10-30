#!/usr/bin/env bash
set -euo pipefail

report_dir="app/build/emulator"
mkdir -p "$report_dir"

collect_logs() {
    local original_status=$?
    adb logcat -d -v threadtime > "$report_dir/logcat.txt" 2>&1 || true
    adb logcat -d -b crash -v threadtime > "$report_dir/crash.txt" 2>&1 || true
    adb exec-out screencap -p > "$report_dir/foundation.png" 2>/dev/null || true
    exit "$original_status"
}
trap collect_logs EXIT

./gradlew --no-daemon --stacktrace --max-workers=2 :app:connectedDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n io.github.lxzrvi.rekey/.MainActivity
adb shell pidof io.github.lxzrvi.rekey > "$report_dir/app-pid.txt"
adb exec-out screencap -p > "$report_dir/foundation.png"
adb logcat -d -b crash -v threadtime > "$report_dir/crash.txt"
if grep -q 'FATAL EXCEPTION' "$report_dir/crash.txt"; then
    echo 'Fatal Android runtime exception detected.' >&2
    cat "$report_dir/crash.txt" >&2
    exit 1
fi
printf 'Connected UI tests and explicit launch smoke check passed.\n'
