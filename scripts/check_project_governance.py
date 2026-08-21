#!/usr/bin/env python3
"""Reject roadmap, release-policy and device-evidence drift before Android builds."""

from __future__ import annotations

import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
PLAN = ROOT / "docs/PROJECT_MASTER_PLAN.md"
BOARD = ROOT / "docs/PROJECT_EXECUTION_BOARD.md"
GATE = ROOT / "docs/OPENING_30MIN_RELEASE_GATE.md"
PROTOCOL = ROOT / "docs/AI_COLLABORATION_PROTOCOL.md"
POLICY = ROOT / "docs/RELEASE_POLICY.md"
MATRIX = ROOT / "docs/V162_SMOKE_TEST_MATRIX.md"
GRADLE = ROOT / "app/build.gradle.kts"
WORKFLOWS = (
    ROOT / ".github/workflows/android-build.yml",
    ROOT / ".github/workflows/android-debug-apk.yml",
)

ALLOWED_STATES = {
    "NEXT",
    "TODO",
    "IN_PROGRESS",
    "NEEDS_REPRO",
    "DEVICE_REQUIRED",
    "BLOCKED",
    "DONE",
    "DEFERRED",
}
ALLOWED_PRIORITIES = {"P0", "P1", "P2"}


class GovernanceFailure(Exception):
    """Raised when the project's sources of truth contradict one another."""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise GovernanceFailure(message)


def read(path: Path) -> str:
    require(path.is_file(), f"required governance document is missing: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def capture(pattern: str, text: str, description: str) -> re.Match[str]:
    match = re.search(pattern, text, re.MULTILINE)
    require(match is not None, f"could not read {description}")
    assert match is not None
    return match


def split_row(line: str, expected_columns: int, description: str) -> list[str]:
    values = [part.strip().strip("`") for part in line.strip().strip("|").split("|")]
    require(len(values) == expected_columns, f"{description} has {len(values)} columns instead of {expected_columns}")
    return values


def main() -> int:
    try:
        plan = read(PLAN)
        board = read(BOARD)
        gate = read(GATE)
        protocol = read(PROTOCOL)
        policy = read(POLICY)
        matrix = read(MATRIX)
        gradle = read(GRADLE)

        entry_paths = (
            "docs/PROJECT_MASTER_PLAN.md",
            "docs/PROJECT_EXECUTION_BOARD.md",
            "docs/AI_COLLABORATION_PROTOCOL.md",
            "docs/RELEASE_POLICY.md",
            "docs/OPENING_30MIN_RELEASE_GATE.md",
        )
        for entry in (ROOT / "AGENTS.md", ROOT / "START_HERE.md"):
            content = read(entry)
            for path in entry_paths:
                require(path in content, f"{entry.name} does not require {path}")

        version_name = capture(r'versionName\s*=\s*"([^"]+)"', gradle, "Gradle versionName").group(1)
        version_code = int(capture(r"versionCode\s*=\s*(\d+)", gradle, "Gradle versionCode").group(1))
        board_version = capture(
            r"当前已安装基线：`([^`]+)`\s*/\s*`versionCode\s+(\d+)`",
            board,
            "execution-board installed baseline",
        )
        require(board_version.group(1) == version_name, "execution-board versionName disagrees with Gradle")
        require(int(board_version.group(2)) == version_code, "execution-board versionCode disagrees with Gradle")
        require(version_name in plan and f"versionCode {version_code}" in plan, "master plan does not state the actual Gradle build version")

        milestone_match = capture(r"当前里程碑进度：\*\*(\d+)\s*/\s*(\d+)\*\*", plan, "current milestone progress")
        milestone_done, milestone_total = map(int, milestone_match.groups())
        overall_match = capture(r"当前总进度：\*\*(\d+)\s*/\s*(\d+)\*\*", plan, "overall roadmap progress")
        overall_done, overall_total = map(int, overall_match.groups())

        stab_section = capture(
            r"# 四、V1\.6\.2[^\n]*\n(?P<section>[\s\S]*?)(?=\n# 五、)",
            plan,
            "V1.6.2 milestone section",
        ).group("section")
        stab_tasks = re.findall(r"^## (STAB-\d{3})[^\n]*\n\n状态：`([A-Z_]+)`", stab_section, re.MULTILINE)
        require(len(stab_tasks) == milestone_total, "roadmap milestone total disagrees with actual STAB task count")
        require(sum(state == "DONE" for _, state in stab_tasks) == milestone_done, "roadmap completed STAB count disagrees with task statuses")

        main_tasks = re.findall(r"^## ((?:STAB|HIST|WORLD|STRAT|POLISH)-\d{3})\b", plan, re.MULTILINE)
        require(len(main_tasks) == overall_total, "overall roadmap total disagrees with actual mainline task count")
        require(len(main_tasks) == len(set(main_tasks)), "roadmap repeats a mainline task identifier")
        task_statuses = re.findall(
            r"^## ((?:STAB|HIST|WORLD|STRAT|POLISH)-\d{3})[^\n]*\n(?:\n)?状态：`([A-Z_]+)`",
            plan,
            re.MULTILINE,
        )
        require(len(task_statuses) == overall_total, "not every mainline roadmap task has an explicit status")
        require(sum(state == "DONE" for _, state in task_statuses) == overall_done, "overall completed progress disagrees with roadmap statuses")
        require(
            f"当前主线进度：`{milestone_done}/{milestone_total}`；全项目：`{overall_done}/{overall_total}`" in board,
            "execution-board progress disagrees with master roadmap",
        )

        row_lines = [line for line in board.splitlines() if re.match(r"^\| [A-Z]+-P[012]-\d{3} \|", line)]
        require(bool(row_lines), "execution board contains no actionable task rows")
        rows = [split_row(line, 7, "execution-board task") for line in row_lines]
        task_ids = [row[0] for row in rows]
        require(len(task_ids) == len(set(task_ids)), "execution board repeats a task identifier")
        for task_id, priority, status, target, owner, dependency, acceptance in rows:
            require(priority in ALLOWED_PRIORITIES, f"{task_id} has invalid priority {priority}")
            require(status in ALLOWED_STATES, f"{task_id} has invalid status {status}")
            require(bool(re.fullmatch(r"V\d+\.\d+\.\d+", target)), f"{task_id} has invalid target version {target}")
            require(bool(owner) and bool(dependency) and bool(acceptance), f"{task_id} has no owner, dependency or acceptance evidence")

        next_rows = [row for row in rows if row[2] == "NEXT"]
        require(len(next_rows) == 1, f"execution board needs exactly one NEXT task, found {len(next_rows)}")
        next_id = next_rows[0][0]
        declared_next = capture(r"唯一当前下一执行项：`([^`]+)`", board, "declared next execution item").group(1)
        require(next_id == declared_next, "execution board heading disagrees with its NEXT row")
        require(f"当前下一执行项：**{next_id}" in plan, "master plan does not point at the board's unique NEXT task")

        gate_lines = [line for line in gate.splitlines() if re.match(r"^\| O30-\d{2} \|", line)]
        gate_rows = [split_row(line, 5, "opening-30-minute acceptance item") for line in gate_lines]
        require(len(gate_rows) == 30, f"opening release gate has {len(gate_rows)} items instead of 30")
        require([row[0] for row in gate_rows] == [f"O30-{index:02d}" for index in range(1, 31)], "opening release-gate IDs are missing, reordered or duplicated")
        require(all(row[3] in ALLOWED_PRIORITIES for row in gate_rows), "opening release gate contains an invalid priority")
        require(all(row[4] in {"PASS", "BLOCKED", "DEVICE_REQUIRED", "NEEDS_REPRO"} for row in gate_rows), "opening release gate contains an invalid evidence status")

        smoke_rows = [line for line in matrix.splitlines() if re.match(r"^\| (?:MENU|PRO|COURT|MAP|PEOPLE|GOV|MIL|HIST|MEDIA|AI|SAVE|NAV)-\d+ \|", line)]
        require(len(smoke_rows) == 49, "formal smoke-test matrix no longer has 49 rows")
        bgm_row = next((line for line in smoke_rows if line.startswith("| MEDIA-03 |")), None)
        require(bgm_row is not None, "formal smoke-test matrix is missing MEDIA-03")
        audio_row = next((row for row in rows if row[0] == "AUDIO-P0-001"), None)
        require(audio_row is not None, "execution board is missing the formal BGM acceptance task")
        if bgm_row.rstrip().endswith("`BLOCKED` |"):
            require(audio_row[2] in {"BLOCKED", "DEVICE_REQUIRED"}, "BGM smoke-test blocker has been silently marked resolved on the execution board")

        states = dict(stab_tasks)
        require(states.get("STAB-007") != "DONE" or all(row[2] == "DONE" for row in rows if row[1] == "P0" and row[0] != "RELEASE-P0-001"), "STAB-007 cannot finish while non-release P0 tasks remain open")
        if states.get("STAB-007") != "DONE":
            require(states.get("STAB-008") == "TODO", "STAB-008 cannot start before STAB-007 is complete")
            release_row = next((row for row in rows if row[0] == "RELEASE-P0-001"), None)
            require(release_row is not None and release_row[2] == "TODO", "release preparation was started before acceptance finished")

        for phrase in ("同版本", "正式升级", "versionCode", "STAB-008", "固定"):
            require(phrase in policy, f"release policy does not define required distinction: {phrase}")
        require("不得倒退为 V1.6.0" in policy, "release policy allows the already-installed version to move backwards")
        for phrase in ("用户", "Claude", "Work", "豆包", "GameState"):
            require(phrase in protocol, f"collaboration protocol does not cover {phrase}")
        require("#68" in board and "尚未合入" in board, "independent map PR #68 is incorrectly presented as already integrated")

        for workflow in WORKFLOWS:
            content = read(workflow)
            require("integration/v1.6.2-preacceptance" in content, f"{workflow.name} does not run for the formal integration PR base")
            require("python3 ./scripts/check_project_governance.py" in content, f"{workflow.name} does not enforce project governance")

        open_p0 = [row[0] for row in rows if row[1] == "P0" and row[2] != "DONE"]
        print(f"GOVERNANCE_VERSION: {version_name}; versionCode={version_code}; development_build_only=1")
        print(f"GOVERNANCE_ROADMAP: milestone={milestone_done}/{milestone_total}; overall={overall_done}/{overall_total}")
        print(f"GOVERNANCE_BOARD: tasks={len(rows)}; unique_next={next_id}; open_p0={len(open_p0)}")
        print(f"GOVERNANCE_OPENING_GATE: checkpoints={len(gate_rows)}; device_evidence_required=1")
        print("GOVERNANCE_RELEASE: STAB-007=" + states["STAB-007"] + "; STAB-008=" + states["STAB-008"] + "; fixed_signing_required=1")
        return 0
    except (GovernanceFailure, OSError, ValueError) as exc:
        print(f"::error::Project governance audit failed: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
