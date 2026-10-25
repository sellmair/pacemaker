from __future__ import annotations

import json
import os
from pathlib import Path
import subprocess
import sys
from typing import Optional


ROOT = Path(__file__).resolve().parents[2]
WRAPPER = ROOT / ".agents" / "skills" / "gradle-run" / "scripts" / "gradle_run.py"
QUESTION = "Does the Xcode Kotlin phase package the Apple framework with Xcode's build environment?"
GRADLE_TASK = ["./gradlew", ":app:embedAndSignAppleFrameworkForXcode"]


def invoke(arguments: list[str], env: dict[str, str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, str(WRAPPER), *arguments],
        cwd=ROOT,
        env=env,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        check=False,
    )


def run_gradle_direct(env: dict[str, str]) -> int:
    return subprocess.run(GRADLE_TASK, cwd=ROOT, env=env, check=False).returncode


def main() -> int:
    env = os.environ.copy()
    if not WRAPPER.is_file():
        if "PACEMAKER_GRADLE_WORKFLOW" in env:
            print("Managed Gradle wrapper is unavailable for the supplied workflow.", file=sys.stderr)
            return 1
        return run_gradle_direct(env)

    workflow: Optional[str] = env.get("PACEMAKER_GRADLE_WORKFLOW")
    owns_workflow = not workflow

    if owns_workflow:
        created = invoke(["create"], env)
        sys.stdout.write(created.stdout)
        if created.returncode != 0:
            return created.returncode
        try:
            payload = json.loads(created.stdout)
            workflow = payload["workflow"]
        except (json.JSONDecodeError, KeyError, TypeError):
            print("Could not read the managed Gradle workflow ID from create output.", file=sys.stderr)
            return 1

    assert workflow is not None
    command = [
        "run",
        "--workflow",
        workflow,
        "--scope",
        "targeted",
        "--question",
        QUESTION,
        "--",
        *GRADLE_TASK,
    ]
    result = invoke(command, env)
    sys.stdout.write(result.stdout)
    if result.returncode != 0:
        print("Managed Gradle workflow retained for diagnosis: " + workflow, file=sys.stderr)
        return result.returncode

    if owns_workflow:
        finished = invoke(["finish", "--workflow", workflow], env)
        sys.stdout.write(finished.stdout)
        if finished.returncode != 0:
            print("Managed Gradle workflow retained: " + workflow, file=sys.stderr)
            return finished.returncode

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
