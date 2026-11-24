# Copyright 2026 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Unit tests for the auto-fix patch generator and --verify-applied boundary.

These tests exercise the security invariants in `create_pr.py` without making
network calls: path sandboxing, idempotent metadata sanitization, working-tree
verification against `pr_metadata.json`, and no-op edit filtering.
"""

from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest import mock

# Ensure `.github/scripts` is importable regardless of the working directory.
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import create_pr  # noqa: E402


def _init_temp_git_repo(root: str) -> None:
    """Initializes a minimal git repository with one committed Kotlin source file."""
    subprocess.run(["git", "init", "-q"], cwd=root, check=True)
    subprocess.run(["git", "config", "user.name", "Test"], cwd=root, check=True)
    subprocess.run(["git", "config", "user.email", "test@example.com"], cwd=root, check=True)

    sample = os.path.join(root, "maps-compose/src/main/java/Sample.kt")
    os.makedirs(os.path.dirname(sample), exist_ok=True)
    with open(sample, "w", encoding="utf-8") as f:
        f.write("package test\n\nfun sample() = 1\n")

    subprocess.run(["git", "add", "."], cwd=root, check=True)
    subprocess.run(["git", "commit", "-q", "-m", "init"], cwd=root, check=True)


class SanitizationTest(unittest.TestCase):
    """Verifies metadata sanitizers are strict and idempotent across generate -> publish."""

    def test_validate_single_line_is_idempotent(self):
        cases = [
            ("fix: resolve crash", "fix: resolve crash"),
            ("- -fix: strip leading flags", "fix: strip leading flags"),
            ("  multi\nline\tmessage  ", "multi line message"),
            ("a" * 199 + " tail", "a" * 199),
        ]
        for raw, expected in cases:
            once = create_pr.validate_single_line(raw, "commit_message")
            self.assertEqual(once, expected)
            self.assertEqual(create_pr.validate_single_line(once, "commit_message"), once)

    def test_validate_single_line_rejects_empty_and_dashes_only(self):
        for bad in ("", "   ", "---", "- - -", None, 42):
            with self.assertRaises(ValueError):
                create_pr.validate_single_line(bad, "pr_title")

    def test_sanitize_pr_body_is_idempotent_and_filters_threats(self):
        raw = (
            "Fixes #42 and closes #999.\n"
            "Thanks @alice and @org/team for reporting.\n"
            "See https://developer.android.com/jetpack/compose."
        )
        once = create_pr.sanitize_pr_body(raw, "42")
        self.assertIn("Fixes #42", once)
        self.assertIn("closes issue 999", once)
        self.assertIn("`@alice`", once)
        self.assertIn("`@org/team`", once)
        self.assertEqual(create_pr.sanitize_pr_body(once, "42"), once)

    def test_sanitize_pr_body_rejects_unapproved_domain(self):
        with self.assertRaises(ValueError):
            create_pr.sanitize_pr_body("See https://evil.example.com/phish", "42")


class PathValidationTest(unittest.TestCase):
    """Verifies source-tree isolation for editable paths."""

    def test_allows_main_kotlin_paths_and_rejects_disallowed_paths(self):
        with tempfile.TemporaryDirectory() as tmp:
            repo_root = os.path.realpath(tmp)
            allowed = "maps-compose/src/main/java/Foo.kt"
            self.assertEqual(create_pr.validate_source_path(allowed, repo_root), allowed)

            disallowed = [
                "maps-compose/src/test/java/FooTest.kt",
                "maps-compose/src/main/generated/baselineProfiles/Foo.kt",
                "maps-compose/build.gradle.kts",
                ".github/workflows/auto-fix.yml",
                "maps-compose/src/main/../../evil.kt",
                "maps-compose/src/main/java/-Flag.kt",
            ]
            for path in disallowed:
                with self.assertRaises(SystemExit):
                    create_pr.validate_source_path(path, repo_root)


class VerifyAppliedPatchTest(unittest.TestCase):
    """Verifies --verify-applied against a real git working tree."""

    def _write_metadata(self, directory: str, overrides: dict | None = None) -> str:
        data = {
            "branch_name": "auto-fix/issue-42",
            "commit_message": "fix: update sample",
            "pr_title": "fix: update sample",
            "pr_body": "Updates sample.\n\nFixes #42",
            "files": ["maps-compose/src/main/java/Sample.kt"],
        }
        if overrides:
            data.update(overrides)
        path = os.path.join(directory, "pr_metadata.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f)
        return path

    def test_accepts_matching_modified_and_created_files(self):
        with tempfile.TemporaryDirectory() as repo_tmp, tempfile.TemporaryDirectory() as meta_tmp:
            repo_root = os.path.realpath(repo_tmp)
            _init_temp_git_repo(repo_root)

            modified = os.path.join(repo_root, "maps-compose/src/main/java/Sample.kt")
            with open(modified, "w", encoding="utf-8") as f:
                f.write("package test\n\nfun sample() = 2\n")

            created = os.path.join(repo_root, "maps-compose/src/main/java/NewFile.kt")
            with open(created, "w", encoding="utf-8") as f:
                f.write("package test\n\nfun helper() = 3\n")

            metadata_path = self._write_metadata(
                meta_tmp,
                {
                    "files": [
                        "maps-compose/src/main/java/Sample.kt",
                        "maps-compose/src/main/java/NewFile.kt",
                    ]
                },
            )
            with mock.patch.dict(os.environ, {"ISSUE_NUMBER": "42"}):
                create_pr.verify_applied_patch(metadata_path, repo_root)

    def test_rejects_unexpected_extra_file(self):
        with tempfile.TemporaryDirectory() as repo_tmp, tempfile.TemporaryDirectory() as meta_tmp:
            repo_root = os.path.realpath(repo_tmp)
            _init_temp_git_repo(repo_root)

            with open(os.path.join(repo_root, "maps-compose/src/main/java/Sample.kt"), "w", encoding="utf-8") as f:
                f.write("package test\n\nfun sample() = 2\n")
            with open(os.path.join(repo_root, "extra.txt"), "w", encoding="utf-8") as f:
                f.write("sneaky\n")

            metadata_path = self._write_metadata(meta_tmp)
            with mock.patch.dict(os.environ, {"ISSUE_NUMBER": "42"}):
                with self.assertRaises(SystemExit):
                    create_pr.verify_applied_patch(metadata_path, repo_root)

    def test_rejects_missing_change_from_noop_edit(self):
        with tempfile.TemporaryDirectory() as repo_tmp, tempfile.TemporaryDirectory() as meta_tmp:
            repo_root = os.path.realpath(repo_tmp)
            _init_temp_git_repo(repo_root)

            # Sample.kt is listed in metadata but unchanged on disk.
            metadata_path = self._write_metadata(meta_tmp)
            with mock.patch.dict(os.environ, {"ISSUE_NUMBER": "42"}):
                with self.assertRaises(SystemExit):
                    create_pr.verify_applied_patch(metadata_path, repo_root)

    def test_rejects_executable_file(self):
        with tempfile.TemporaryDirectory() as repo_tmp, tempfile.TemporaryDirectory() as meta_tmp:
            repo_root = os.path.realpath(repo_tmp)
            _init_temp_git_repo(repo_root)

            sample = os.path.join(repo_root, "maps-compose/src/main/java/Sample.kt")
            with open(sample, "w", encoding="utf-8") as f:
                f.write("package test\n\nfun sample() = 2\n")
            os.chmod(sample, 0o755)

            metadata_path = self._write_metadata(meta_tmp)
            with mock.patch.dict(os.environ, {"ISSUE_NUMBER": "42"}):
                with self.assertRaises(SystemExit):
                    create_pr.verify_applied_patch(metadata_path, repo_root)

    def test_rejects_unsanitized_metadata(self):
        with tempfile.TemporaryDirectory() as repo_tmp, tempfile.TemporaryDirectory() as meta_tmp:
            repo_root = os.path.realpath(repo_tmp)
            _init_temp_git_repo(repo_root)

            sample = os.path.join(repo_root, "maps-compose/src/main/java/Sample.kt")
            with open(sample, "w", encoding="utf-8") as f:
                f.write("package test\n\nfun sample() = 2\n")

            metadata_path = self._write_metadata(meta_tmp, {"pr_title": "--unsanitized"})
            with mock.patch.dict(os.environ, {"ISSUE_NUMBER": "42"}):
                with self.assertRaises(SystemExit):
                    create_pr.verify_applied_patch(metadata_path, repo_root)

    def test_isolated_cli_rejects_tampered_script_and_shadowed_stdlib(self):
        """Mirrors the publish job: copy create_pr.py outside the repo and run with -I."""
        script_src = os.path.abspath(create_pr.__file__)
        with (
            tempfile.TemporaryDirectory() as repo_tmp,
            tempfile.TemporaryDirectory() as runner_tmp,
        ):
            repo_root = os.path.realpath(repo_tmp)
            _init_temp_git_repo(repo_root)

            # Copy the trusted validator out of the repository before applying changes.
            trusted_copy = os.path.join(runner_tmp, "verify_create_pr.py")
            shutil.copy2(script_src, trusted_copy)

            # Apply a valid Kotlin edit plus hostile files in the working tree:
            # a modified create_pr.py that always exits 0 and a shadowing json.py.
            sample = os.path.join(repo_root, "maps-compose/src/main/java/Sample.kt")
            with open(sample, "w", encoding="utf-8") as f:
                f.write("package test\n\nfun sample() = 2\n")

            tampered_script = os.path.join(repo_root, ".github/scripts/create_pr.py")
            os.makedirs(os.path.dirname(tampered_script), exist_ok=True)
            with open(tampered_script, "w", encoding="utf-8") as f:
                f.write("import sys; sys.exit(0)\n")

            with open(os.path.join(repo_root, "json.py"), "w", encoding="utf-8") as f:
                f.write("raise AssertionError('shadowed stdlib json imported!')\n")

            metadata_path = self._write_metadata(runner_tmp)
            env = {**os.environ, "ISSUE_NUMBER": "42"}
            result = subprocess.run(
                [sys.executable, "-I", trusted_copy, "--verify-applied", metadata_path],
                cwd=repo_root,
                env=env,
                capture_output=True,
                text=True,
            )
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("Unexpected:", result.stderr)
            self.assertNotIn("shadowed stdlib json imported", result.stderr)

            # Removing the unauthorized files lets the isolated validator succeed.
            os.remove(tampered_script)
            os.remove(os.path.join(repo_root, "json.py"))
            ok_result = subprocess.run(
                [sys.executable, "-I", trusted_copy, "--verify-applied", metadata_path],
                cwd=repo_root,
                env=env,
                capture_output=True,
                text=True,
            )
            self.assertEqual(ok_result.returncode, 0, ok_result.stderr)


class NoOpEditFilteringTest(unittest.TestCase):
    """Verifies main() omits unchanged files so pr_metadata.json matches git diff."""

    def test_partial_noop_records_only_changed_file(self):
        with tempfile.TemporaryDirectory() as repo_tmp:
            repo_root = os.path.realpath(repo_tmp)
            _init_temp_git_repo(repo_root)

            other = os.path.join(repo_root, "maps-compose/src/main/java/Other.kt")
            with open(other, "w", encoding="utf-8") as f:
                f.write("package test\n\nfun other() = 10\n")

            responses = [
                json.dumps({
                    "files_to_modify": [
                        "maps-compose/src/main/java/Sample.kt",
                        "maps-compose/src/main/java/Other.kt",
                    ],
                    "files_to_create": [],
                    "explanation": "Update Sample only.",
                }),
                # Identical to Sample.kt -> should be skipped.
                "package test\n\nfun sample() = 1\n",
                # Actually modifies Other.kt -> should be recorded.
                "package test\n\nfun other() = 20\n",
                json.dumps({
                    "commit_message": "fix: update other",
                    "pr_title": "fix: update other",
                    "pr_body": "Updates Other.\n\nFixes #42",
                }),
            ]

            prev_cwd = os.getcwd()
            os.chdir(repo_root)
            try:
                env = {
                    "GEMINI_API_KEY": "fake-key",
                    "ISSUE_TITLE": "Fix bug",
                    "ISSUE_BODY": "Details",
                    "ISSUE_NUMBER": "42",
                }
                with mock.patch.dict(os.environ, env), mock.patch.object(
                    create_pr, "get_gemini_response", side_effect=responses
                ):
                    create_pr.main()

                with open(os.path.join(repo_root, "pr_metadata.json"), encoding="utf-8") as f:
                    metadata = json.load(f)
                self.assertEqual(metadata["files"], ["maps-compose/src/main/java/Other.kt"])
            finally:
                os.chdir(prev_cwd)

    def test_full_noop_aborts_without_writing_metadata(self):
        with tempfile.TemporaryDirectory() as repo_tmp:
            repo_root = os.path.realpath(repo_tmp)
            _init_temp_git_repo(repo_root)

            responses = [
                json.dumps({
                    "files_to_modify": ["maps-compose/src/main/java/Sample.kt"],
                    "files_to_create": [],
                    "explanation": "No-op edit.",
                }),
                # Identical to Sample.kt -> nothing changed.
                "package test\n\nfun sample() = 1\n",
            ]

            prev_cwd = os.getcwd()
            os.chdir(repo_root)
            try:
                env = {
                    "GEMINI_API_KEY": "fake-key",
                    "ISSUE_TITLE": "Fix bug",
                    "ISSUE_BODY": "Details",
                    "ISSUE_NUMBER": "42",
                }
                with mock.patch.dict(os.environ, env), mock.patch.object(
                    create_pr, "get_gemini_response", side_effect=responses
                ):
                    with self.assertRaises(SystemExit) as ctx:
                        create_pr.main()
                    self.assertEqual(ctx.exception.code, 1)
                self.assertFalse(os.path.exists(os.path.join(repo_root, "pr_metadata.json")))
            finally:
                os.chdir(prev_cwd)

    def test_create_noop_on_existing_file_is_skipped(self):
        with tempfile.TemporaryDirectory() as repo_tmp:
            repo_root = os.path.realpath(repo_tmp)
            _init_temp_git_repo(repo_root)

            responses = [
                json.dumps({
                    "files_to_modify": [],
                    "files_to_create": ["maps-compose/src/main/java/Sample.kt"],
                    "explanation": "Re-create Sample with identical content.",
                }),
                "package test\n\nfun sample() = 1\n",
            ]

            prev_cwd = os.getcwd()
            os.chdir(repo_root)
            try:
                env = {
                    "GEMINI_API_KEY": "fake-key",
                    "ISSUE_TITLE": "Fix bug",
                    "ISSUE_BODY": "Details",
                    "ISSUE_NUMBER": "42",
                }
                with mock.patch.dict(os.environ, env), mock.patch.object(
                    create_pr, "get_gemini_response", side_effect=responses
                ):
                    with self.assertRaises(SystemExit) as ctx:
                        create_pr.main()
                    self.assertEqual(ctx.exception.code, 1)
                self.assertFalse(os.path.exists(os.path.join(repo_root, "pr_metadata.json")))
            finally:
                os.chdir(prev_cwd)


if __name__ == "__main__":
    unittest.main()
