#!/usr/bin/env python3
"""Create, save, and reopen one clean Forge world without running unrelated tests."""
from pathlib import Path
import errno
import os
import pty
import select
import signal
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
RUN = ROOT / "run"
WORLD = RUN / "minemon-1-63-reopen"


def run_server(label: str) -> str:
    master, slave = pty.openpty()
    process = subprocess.Popen(
        ["./gradlew", "--no-daemon", "runServer"], cwd=ROOT,
        stdin=slave, stdout=slave, stderr=slave, start_new_session=True,
    )
    os.close(slave)
    output: list[str] = []
    deadline = time.monotonic() + 360
    ready = False
    stopped = False
    while time.monotonic() < deadline:
        readable, _, _ = select.select([master], [], [], 1)
        if readable:
            try:
                chunk = os.read(master, 65536).decode("utf-8", errors="replace")
            except OSError as error:
                if error.errno == errno.EIO:
                    chunk = ""
                else:
                    raise
            if chunk:
                output.append(chunk)
                print(f"[{label}] {chunk}", end="", flush=True)
                combined = "".join(output)
                if not ready and "Done (" in combined:
                    ready = True
                    os.write(master, b"stop\n")
                if ready and "Stopping server" in combined:
                    stopped = True
        if process.poll() is not None:
            break
    if process.poll() is None:
        os.killpg(process.pid, signal.SIGINT)
        try:
            process.wait(timeout=30)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait(timeout=10)
        raise SystemExit(f"{label}: server failed to stop cleanly after receiving stop")
    os.close(master)
    combined = "".join(output)
    if not ready:
        raise SystemExit(f"{label}: server did not finish opening the world; inspect preceding log output")
    if not stopped:
        raise SystemExit(f"{label}: server opened, but its graceful save/stop was not observed")
    if process.returncode != 0:
        raise SystemExit(f"{label}: runServer exited with code {process.returncode}")
    return combined

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
