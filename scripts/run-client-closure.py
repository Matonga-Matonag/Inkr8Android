"""Run client closure JVM checks with the workspace JDK/SDK and preserve raw evidence."""
import argparse
import datetime
import json
import os
import pathlib
import shutil
import subprocess
import sys
import time

parser = argparse.ArgumentParser()
parser.add_argument("--project", choices=("base", "proposal"), default="base")
parser.add_argument("--all", action="store_true", dest="all_tests")
parser.add_argument("--coverage", action="store_true", help="Generate reports after preserving the test result, including red suites")
args = parser.parse_args()
if args.coverage and not args.all_tests:
    parser.error("--coverage requires --all")
root = pathlib.Path(__file__).resolve().parents[3]
project = root / "work" / ("frontend" if args.project == "base" else "propuesta_frontend")
out = root / "outputs/cierre/cliente"
out.mkdir(parents=True, exist_ok=True)
label = args.project + ("-all" if args.all_tests else "-negative")
if args.coverage:
    label += "-coverage"
env = os.environ.copy()
env["JAVA_HOME"] = str(root / "work/tools/jdk-21.0.9+10")
env["ANDROID_HOME"] = str(root / "work/tools/android-sdk")
env["ANDROID_SDK_ROOT"] = env["ANDROID_HOME"]
env["PATH"] = env["JAVA_HOME"] + os.pathsep + env["JAVA_HOME"] + "/bin" + os.pathsep + env["PATH"]
env["CI"] = "true"
cmd = [str(project / "gradlew.bat"), "--no-daemon", "--max-workers=2", "--console=plain", ":app:testDebugUnitTest", "--rerun"]
if args.all_tests:
    cmd += [":domain:test", "--rerun", "--continue"]
else:
    cmd += ["--tests", "com.inkr8.presentation.ClienteNegativeFlowTest"]
metadata = {"command": cmd, "cwd": str(project), "JAVA_HOME": env["JAVA_HOME"], "ANDROID_HOME": env["ANDROID_HOME"],
            "start_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(), "scope": "JVM; no Android device or rendered Compose screen"}
started = time.perf_counter()
with (out / (label + ".log")).open("w", encoding="utf-8") as log:
    log.write(json.dumps(metadata, ensure_ascii=False) + "\n")
    log.flush()
    code = subprocess.run(cmd, cwd=project, env=env, stdout=log, stderr=subprocess.STDOUT, shell=False).returncode
    metadata["tests_exit_code"] = code
    if args.coverage:
        # Reporting uses the exec data from the run above; exclusions avoid rerunning tests.
        # A failed test exit remains failed and is never turned into an approval by this step.
        report_cmd = [str(project / "gradlew.bat"), "--no-daemon", "--max-workers=2", "--console=plain",
                      ":app:jacocoDebugUnitTestReport", ":domain:jacocoTestReport",
                      "-x", ":app:testDebugUnitTest", "-x", ":domain:test"]
        metadata["coverage_command"] = report_cmd
        log.write("\nREPORT_ONLY_AFTER_TEST_RESULT: " + json.dumps(report_cmd) + "\n")
        log.flush()
        report_code = subprocess.run(report_cmd, cwd=project, env=env, stdout=log, stderr=subprocess.STDOUT, shell=False).returncode
        metadata["coverage_exit_code"] = report_code
        if code == 0:
            code = report_code
metadata.update(exit_code=code, duration_seconds=time.perf_counter() - started,
                end_utc=datetime.datetime.now(datetime.timezone.utc).isoformat())
evidence = out / (label + "-xml")
evidence.mkdir(exist_ok=True)
for module, folder in (("app", "testDebugUnitTest"), ("domain", "test")):
    if module == "domain" and not args.all_tests:
        continue
    for path in (project / module / "build/test-results" / folder).glob("TEST*.xml"):
        shutil.copy2(path, evidence / path.name)
if args.coverage:
    for module in ("app", "domain"):
        for path in (project / module / "build/reports/jacoco").glob("**/*.xml"):
            target = out / (label + "-jacoco") / module / path.name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, target)
(out / (label + ".json")).write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
sys.stdout.reconfigure(encoding="utf-8")
print(json.dumps(metadata, ensure_ascii=False))
print((out / (label + ".log")).read_text(encoding="utf-8", errors="replace")[-3500:])
sys.exit(code)
