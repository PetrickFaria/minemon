#!/usr/bin/env python3
"""Create, save, and reopen one clean Forge world without running unrelated tests."""
from pathlib import Path
from queue import Queue, Empty
from threading import Thread
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
RUN = ROOT / "run"
WORLD = RUN / "minemon-1-63-reopen"


def run_server(label: str) -> str:
    process = subprocess.Popen(
        ["./gradlew", "--no-daemon", "runServer"], cwd=ROOT,
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True, bufsize=1,
    )
    lines: Queue[str | None] = Queue()

    def read_output() -> None:
        assert process.stdout is not None
        for line in process.stdout:
            lines.put(line)
        lines.put(None)

    Thread(target=read_output, daemon=True).start()
    output: list[str] = []
    deadline = time.monotonic() + 360
    ready = False
    while time.monotonic() < deadline:
        try:
            line = lines.get(timeout=1)
        except Empty:
            if process.poll() is not None:
                break
            continue
        if line is None:
            break
        output.append(line)
        print(f"[{label}] {line}", end="", flush=True)
        if "Done (" in line:
            ready = True
            assert process.stdin is not None
            process.stdin.write("stop\n")
            process.stdin.flush()
            break
    if not ready:
        process.terminate()
        try:
            process.wait(timeout=20)
        except subprocess.TimeoutExpired:
            process.kill()
        raise SystemExit(f"{label}: server did not finish opening the world; inspect preceding log output")
    try:
        process.wait(timeout=90)
    except subprocess.TimeoutExpired:
        process.terminate()
        try:
            process.wait(timeout=20)
        except subprocess.TimeoutExpired:
            process.kill()
        raise SystemExit(f"{label}: server did not save and stop cleanly")
    if process.returncode != 0:
        raise SystemExit(f"{label}: runServer exited with code {process.returncode}")
    return "".join(output)


RUN.mkdir(exist_ok=True)
(RUN / "eula.txt").write_text("eula=true\n", encoding="utf-8")
(RUN / "server.properties").write_text(
    "level-name=minemon-1-63-reopen\n"
    "level-type=minecraft:flat\n"
    "level-seed=163\n"
    "generate-structures=false\n"
    "spawn-animals=false\n"
    "spawn-monsters=false\n"
    "online-mode=false\n",
    encoding="utf-8",
)
if WORLD.exists():
    import shutil
    shutil.rmtree(WORLD)
first = run_server("create-save")
level_dat = WORLD / "level.dat"
if not level_dat.is_file():
    raise SystemExit("create-save: level.dat was not written")
second = run_server("reopen-save")
if "Preparing level" not in second and "Preparing spawn area" not in second:
    raise SystemExit("reopen-save: server log did not confirm loading the saved world")
if not level_dat.is_file():
    raise SystemExit("reopen-save: level.dat disappeared")
print("PASS WORLD REOPEN: new Forge world created, saved, and opened again.")
