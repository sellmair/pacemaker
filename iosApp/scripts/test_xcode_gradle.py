import importlib.util
import os
from pathlib import Path
import subprocess
import unittest
from unittest.mock import patch


MODULE_PATH = Path(__file__).with_name("xcode_gradle.py")
SPEC = importlib.util.spec_from_file_location("xcode_gradle", MODULE_PATH)
xcode_gradle = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(xcode_gradle)


class FakePath:
    def __init__(self, exists):
        self.exists = exists

    def is_file(self):
        return self.exists

    def __str__(self):
        return "/mock/gradle_run.py"


class XcodeGradleFallbackTest(unittest.TestCase):
    def test_missing_wrapper_without_agent_workflow_runs_normal_xcode_task_directly(self):
        with patch.object(xcode_gradle, "WRAPPER", FakePath(False)), \
             patch.object(xcode_gradle, "run_gradle_direct", return_value=23) as direct, \
             patch.dict(os.environ, {}, clear=True):
            self.assertEqual(xcode_gradle.main(), 23)
        direct.assert_called_once()

    def test_missing_wrapper_with_workflow_key_fails_closed_without_direct_gradle(self):
        with patch.object(xcode_gradle, "WRAPPER", FakePath(False)), \
             patch.object(xcode_gradle, "run_gradle_direct") as direct, \
             patch.dict(os.environ, {"PACEMAKER_GRADLE_WORKFLOW": "parent-owned"}, clear=True):
            self.assertEqual(xcode_gradle.main(), 1)
        direct.assert_not_called()

    def test_managed_wrapper_failure_never_falls_back_to_direct_gradle(self):
        failed = subprocess.CompletedProcess(args=[], returncode=17, stdout="managed failure")
        with patch.object(xcode_gradle, "WRAPPER", FakePath(True)), \
             patch.object(xcode_gradle, "invoke", return_value=failed), \
             patch.object(xcode_gradle, "run_gradle_direct") as direct, \
             patch.dict(os.environ, {"PACEMAKER_GRADLE_WORKFLOW": "parent-owned"}, clear=True):
            self.assertEqual(xcode_gradle.main(), 17)
        direct.assert_not_called()


if __name__ == "__main__":
    unittest.main()
