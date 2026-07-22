#!/usr/bin/env python3
"""
Host-side test harness for the AHM background service on Termux.

The Android app connects to --android-port (default 9999) over the adb reverse
 tunnel. Local control clients connect via --control-socket and send JSON
 commands which are forwarded to Android. Responses are routed back by
 request_id.

Usage:
  ./scripts/test-server.py
  ./scripts/test-invoke.py ping
  ./scripts/test-invoke.py info
  ./scripts/test-invoke.py invoke VersionController getSystemVersionSignature /api/version GET
"""

import argparse
import asyncio
import json
import os
import signal
import subprocess
import sys
import uuid
from pathlib import Path

ANDROID_PORT = 9999
CONTROL_SOCKET_NAME = "test-server.sock"


class TestServer:
    def __init__(self, android_port: int, control_socket: Path, setup_reverse: bool):
        self.android_port = android_port
        self.control_socket = control_socket
        self.setup_reverse = setup_reverse
        self.android_reader: asyncio.StreamReader | None = None
        self.android_writer: asyncio.StreamWriter | None = None
        self.pending: dict[str, asyncio.StreamWriter] = {}
        self.android_connected = asyncio.Event()
        self.shutdown_event = asyncio.Event()

    async def run(self):
        if self.setup_reverse:
            await self._adb_reverse("add")

        # Ensure directory for control socket exists
        self.control_socket.parent.mkdir(parents=True, exist_ok=True)

        # Remove stale socket.
        if self.control_socket.exists():
            self.control_socket.unlink()

        android_server = await asyncio.start_server(
            self._handle_android, host="127.0.0.1", port=self.android_port
        )
        control_server = await asyncio.start_unix_server(
            self._handle_control, path=str(self.control_socket)
        )

        print(f"[server] Listening for Android on 127.0.0.1:{self.android_port}")
        print(f"[server] Control socket: {self.control_socket}")

        try:
            await self.shutdown_event.wait()
        finally:
            print("\n[server] Shutting down sockets and cleaning dependencies...")
            control_server.close()
            await control_server.wait_closed()
            android_server.close()
            await android_server.wait_closed()
            if self.android_writer:
                self.android_writer.close()
                try:
                    await self.android_writer.wait_closed()
                except Exception:
                    pass
            if self.control_socket.exists():
                self.control_socket.unlink()
            if self.setup_reverse:
                await self._adb_reverse("remove")
            print("[server] Shutdown complete.")

    async def _adb_reverse(self, action: str):
        spec = f"tcp:{self.android_port}"
        cmd = ["adb", "reverse"]
        if action == "remove":
            cmd += ["--remove", spec]
        else:
            cmd += [spec, spec]
        try:
            proc = await asyncio.create_subprocess_exec(
                *cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE
            )
            stdout, stderr = await proc.communicate()
            if proc.returncode == 0:
                print(f"[adb] reverse {action}: {spec}")
            else:
                print(f"[adb] reverse {action} failed: {stderr.decode().strip()}")
        except FileNotFoundError:
            print("[adb] adb not found in PATH; reverse tunnel must be set up manually.")

    async def _handle_android(self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter):
        if self.android_writer is not None:
            print("[server] Extra Android connection; closing previous one.")
            old = self.android_writer
            old.close()
            try:
                await old.wait_closed()
            except Exception:
                pass

        self.android_reader = reader
        self.android_writer = writer
        self.android_connected.set()
        addr = writer.get_extra_info("peername")
        print(f"[server] Android connected from {addr}")

        try:
            while True:
                line = await reader.readline()
                if not line:
                    break
                await self._route_android_response(line.decode("utf-8").strip())
        except asyncio.CancelledError:
            raise
        except Exception as e:
            print(f"[server] Android read error: {e}")
        finally:
            print("[server] Android disconnected.")
            self.android_writer = None
            self.android_reader = None
            self.android_connected.clear()

    async def _handle_control(self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter):
        peer = writer.get_extra_info("peername")
        print(f"[server] Control client connected: {peer}")
        try:
            while True:
                line = await reader.readline()
                if not line:
                    break
                await self._forward_control_command(line.decode("utf-8").strip(), writer)
        except asyncio.CancelledError:
            raise
        except Exception as e:
            print(f"[server] Control client error: {e}")
        finally:
            print(f"[server] Control client disconnected: {peer}")
            stale = [rid for rid, w in self.pending.items() if w is writer]
            for rid in stale:
                self.pending.pop(rid, None)
            writer.close()
            try:
                await writer.wait_closed()
            except Exception:
                pass

    async def _forward_control_command(self, raw: str, control_writer: asyncio.StreamWriter):
        if not raw:
            return
        try:
            command = json.loads(raw)
        except json.JSONDecodeError as e:
            self._write_json(control_writer, {"status": "error", "message": f"Invalid JSON: {e}"})
            return

        if "request_id" not in command:
            command["request_id"] = str(uuid.uuid4())

        rid = command["request_id"]
        self.pending[rid] = control_writer

        if self.android_writer is None or self.android_writer.is_closing():
            self.pending.pop(rid, None)
            self._write_json(
                control_writer,
                {"status": "error", "request_id": rid, "message": "Android not connected."},
            )
            return

        self._write_json(self.android_writer, command)
        print(f"[server] -> Android: {command}")

    async def _route_android_response(self, raw: str):
        if not raw:
            return
        print(f"[server] <- Android: {raw}")
        try:
            response = json.loads(raw)
        except json.JSONDecodeError:
            print(f"[server] Malformed response from Android: {raw}")
            return

        rid = response.get("request_id")
        control_writer = self.pending.pop(rid, None) if rid else None
        if control_writer is not None and not control_writer.is_closing():
            self._write_json(control_writer, response)
        else:
            print(json.dumps(response, indent=2))

    def _write_json(self, writer: asyncio.StreamWriter, obj: dict):
        line = json.dumps(obj, separators=(",", ":")) + "\n"
        writer.write(line.encode("utf-8"))

    def request_shutdown(self):
        self.shutdown_event.set()


async def async_main():
    parser = argparse.ArgumentParser(description="AHM background service test harness.")
    parser.add_argument(
        "--android-port",
        type=int,
        default=int(os.environ.get("AHM_TEST_ANDROID_PORT", ANDROID_PORT)),
        help="TCP port the Android app connects to (default: 9999).",
    )
    parser.add_argument(
        "--control-socket",
        type=Path,
        default=Path(os.environ.get("AHM_TEST_CONTROL_SOCKET", f".qoder/{CONTROL_SOCKET_NAME}")),
        help="Unix socket path for local control clients.",
    )
    parser.add_argument(
        "--no-reverse",
        action="store_true",
        help="Do not set up or tear down the adb reverse tunnel.",
    )
    args = parser.parse_args()

    # Resolve control socket relative to project root if not absolute
    if not args.control_socket.is_absolute():
        project_root = Path(__file__).resolve().parent.parent
        args.control_socket = project_root / args.control_socket

    server = TestServer(
        android_port=args.android_port,
        control_socket=args.control_socket,
        setup_reverse=not args.no_reverse,
    )

    # Background signal trap mapping
    loop = asyncio.get_running_loop()
    for sig in (signal.SIGINT, signal.SIGTERM):
        try:
            loop.add_signal_handler(sig, server.request_shutdown)
        except NotImplementedError:
            pass

    # Interrupt protection handler block
    try:
        await server.run()
    except KeyboardInterrupt:
        print("\n[server] Interrupted via keyboard shortcut.")
        server.request_shutdown()
        await server.run()


def main():
    try:
        asyncio.run(async_main())
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()

