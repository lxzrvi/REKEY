#!/usr/bin/env bash
set -euo pipefail

# Ephemeral GitHub-runner checks only. Do not create deliverable files or screenshots.
print_failure_logs() {
    local original_status=$?
    if (( original_status != 0 )); then
        printf '\n--- Recent emulator logcat (job logs only) ---\n' >&2
        adb logcat -d -v threadtime | tail -n 300 >&2 || true
    fi
    exit "$original_status"
}
trap print_failure_logs EXIT

./gradlew --no-daemon --stacktrace --max-workers=4 :app:connectedDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n io.github.lxzrvi.rekey/.MainActivity
# Allow the intentional branded splash to finish before checking the live process.
adb shell 'for i in 1 2 3 4 5 6; do pidof io.github.lxzrvi.rekey >/dev/null && break; sleep 1; done'
printf 'App process PID: '
adb shell pidof io.github.lxzrvi.rekey
crash_log="$(adb logcat -d -b crash -v threadtime)"
if grep -q 'FATAL EXCEPTION' <<< "$crash_log"; then
    printf '%s\n' "$crash_log" >&2
    echo 'Fatal Android runtime exception detected.' >&2
    exit 1
fi
printf 'Connected UI tests and explicit launch passed. No runtime artifacts uploaded.\n'
