#!/usr/bin/env python3
"""Create, save, and reopen one clean Forge world without running unrelated tests."""
from pathlib import Path
import errno
import os
import pty
import select
import signal
import socket
import struct
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
RUN = ROOT / "run"
WORLD = RUN / "minemon-1-68-reopen"


RCON_PORT = 25575
RCON_PASSWORD = os.environ.get("MINEMON_TEST_RCON_PASSWORD")
if not RCON_PASSWORD:
    raise SystemExit("MINEMON_TEST_RCON_PASSWORD must be supplied through the environment")


def read_exact(sock: socket.socket, length: int) -> bytes:
    data = bytearray()
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            raise ConnectionError("RCON connection closed")
        data.extend(chunk)
    return bytes(data)


def rcon_packet(sock: socket.socket, request_id: int, packet_type: int, payload: str) -> tuple[int, int, str]:
    body = struct.pack("<ii", request_id, packet_type) + payload.encode("utf-8") + b"\0\0"
    sock.sendall(struct.pack("<i", len(body)) + body)
    length = struct.unpack("<i", read_exact(sock, 4))[0]
    packet = read_exact(sock, length)
    response_id, response_type = struct.unpack("<ii", packet[:8])
    return response_id, response_type, packet[8:-2].decode("utf-8", errors="replace")


def stop_with_rcon() -> bool:
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        try:
            with socket.create_connection(("127.0.0.1", RCON_PORT), timeout=2) as sock:
                sock.settimeout(3)
                auth_id, auth_type, _ = rcon_packet(sock, 11, 3, RCON_PASSWORD)
                if auth_id != 11 or auth_type != 2:
                    time.sleep(0.5)
                    continue
                command_id, _, _ = rcon_packet(sock, 12, 2, "stop")
                return command_id == 12
        except (OSError, ConnectionError, struct.error):
            time.sleep(0.5)
    return False


def rcon_command(command: str) -> str:
    with socket.create_connection(("127.0.0.1", RCON_PORT), timeout=3) as sock:
        sock.settimeout(3)
        auth_id, auth_type, _ = rcon_packet(sock, 11, 3, RCON_PASSWORD)
        if auth_id != 11 or auth_type != 2:
            raise RuntimeError("RCON authentication failed")
        command_id, _, response = rcon_packet(sock, 12, 2, command)
        if command_id != 12:
            raise RuntimeError(f"RCON rejected command: {command}")
        return response


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
    stop_sent = False
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
                    rcon_command("forceload add 0 0")
                    species_positions = (("alakazam", 0),)
                    if label == "create-save":
                        for slug, x in species_positions:
                            response = rcon_command(
                                f"summon minemon_gen1:{slug} {x} 5 0 "
                                "{PersistenceRequired:1b}"
                            )
                            if "Summoned new" not in response:
                                raise SystemExit(f"{label}: failed to create registered {slug}: {response}")
                    # RCON acknowledgement precedes entity insertion and chunk reload.
                    time.sleep(3)
                    for slug, _ in species_positions:
                        response = rcon_command(f"data get entity @e[type=minemon_gen1:{slug},limit=1]")
                        if "UUID" not in response:
                            raise SystemExit(f"{label}: {slug} was not registered in the loaded test chunk: {response}")
                    rcon_command("save-all flush")
                    stop_sent = stop_with_rcon()
                    if not stop_sent:
                        raise SystemExit(f"{label}: RCON did not accept the graceful stop command")
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
        raise SystemExit(f"{label}: server failed to stop cleanly after RCON stop")
    os.close(master)
    combined = "".join(output)
    if not ready:
        raise SystemExit(f"{label}: server did not finish opening the world; inspect preceding log output")
    if not stop_sent or not stopped:
        raise SystemExit(f"{label}: graceful save/stop was not observed")
    if "Saving chunks for level" not in combined:
        raise SystemExit(f"{label}: server shutdown did not report saving world chunks")
    if process.returncode != 0:
        raise SystemExit(f"{label}: runServer exited with code {process.returncode}")
    return combined

RUN.mkdir(exist_ok=True)
(RUN / "eula.txt").write_text("eula=true\n", encoding="utf-8")
(RUN / "server.properties").write_text(
    "level-name=minemon-1-68-reopen\n"
    "level-type=minecraft:flat\n"
    "level-seed=163\n"
    "generate-structures=false\n"
    "spawn-animals=false\n"
    "spawn-monsters=false\n"
    "online-mode=false\n"
    "enable-rcon=true\n"
    f"rcon.port={RCON_PORT}\n"
    f"rcon.password={RCON_PASSWORD}\n",
    encoding="utf-8",
)
if WORLD.exists():
    import shutil
    shutil.rmtree(WORLD)
first = run_server("create-save")
level_dat = WORLD / "level.dat"
if not level_dat.is_file():
    raise SystemExit("create-save: level.dat was not written")
second = run_server("reopen-save-first-time")
if "Preparing level" not in second and "Preparing spawn area" not in second:
    raise SystemExit("reopen-save-first-time: server log did not confirm loading the saved world")
if not level_dat.is_file():
    raise SystemExit("reopen-save-first-time: level.dat disappeared")
third = run_server("reopen-save-second-time")
if "Preparing level" not in third and "Preparing spawn area" not in third:
    raise SystemExit("reopen-save-second-time: server log did not confirm loading the saved world")
if not level_dat.is_file():
    raise SystemExit("reopen-save-second-time: level.dat disappeared")
print("PASS WORLD REOPEN: new Forge world with Alakazam created, saved, and reopened twice.")
(RUN / "world-reopen-pass.txt").write_text(
    "PASS WORLD REOPEN: Alakazam was registered, saved, and present after both world reopens.\n",
    encoding="utf-8",
)
