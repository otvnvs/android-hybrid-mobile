#!/usr/bin/env python3
"""
Send a single JSON command to the AHM background service through the test server.
Optimised to dynamically track the control channel under WSL mounts.

Examples:
  ./scripts/test-invoke.py ping
  ./scripts/test-invoke.py info
  ./scripts/test-invoke.py invoke VersionController getSystemVersionSignature /api/version GET
  ./scripts/test-invoke.py '{"type":"ping"}'
"""

import argparse
import asyncio
import json
import os
import sys
import uuid
from pathlib import Path

CONTROL_SOCKET_NAME = "test-server.sock"
CONTROL_SOCKET_DEFAULT = f".qoder/{CONTROL_SOCKET_NAME}"


def build_command(args: list[str]) -> dict:
    if not args:
        raise ValueError("No command provided.")

    head = args[0]
    if head == "ping":
        return {"type": "ping"}
    if head == "info":
        return {"type": "info"}

    if head == "invoke":
        if len(args) < 3:
            raise ValueError("invoke requires at least controller and method names.")
        cmd = {
            "type": "invoke",
            "controller": args[1],
            "method": args[2],
        }
        if len(args) > 3:
            cmd["path"] = args[3]
        if len(args) > 4:
            cmd["http_method"] = args[4].upper()
        return cmd

    # Treat as raw JSON.
    return json.loads(head)


async def send_command(control_socket: Path, command: dict, timeout: float) -> dict:
    if not control_socket.exists():
        raise FileNotFoundError(
            f"Control socket not found at '{control_socket}'.\n"
            f"Please verify that your test-server.py backend is up and running."
        )

    rid = str(uuid.uuid4())
    command["request_id"] = rid

    reader, writer = await asyncio.wait_for(
        asyncio.open_unix_connection(path=str(control_socket)), timeout=timeout
    )
    try:
        payload = json.dumps(command, separators=(",", ":")) + "\n"
        writer.write(payload.encode("utf-8"))
        await writer.drain()

        response_line = await asyncio.wait_for(reader.readline(), timeout=timeout)
        if not response_line:
            raise ConnectionResetError("Server closed connection before responding.")
        response = json.loads(response_line.decode("utf-8"))

        returned_rid = response.get("request_id")
        if returned_rid != rid:
            raise RuntimeError(f"Request/response ID mismatch: {rid} != {returned_rid}")
        return response
    finally:
        writer.close()
        try:
            await writer.wait_closed()
        except Exception:
            pass


def main():
    parser = argparse.ArgumentParser(description="Invoke one command on the AHM background service.")
    parser.add_argument(
        "command",
        nargs=argparse.REMAINDER,
        help='Command: ping | info | invoke <controller> <method> [path] [http_method] | raw JSON',
    )
    parser.add_argument(
        "--socket",
        type=Path,
        default=None,  # Handled dynamically below to evaluate WSL vs native paths
        help="Path to the test server's control Unix socket.",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=10.0,
        help="Seconds to wait for a response (default: 10).",
    )
    parser.add_argument(
        "--raw",
        action="store_true",
        help="Print the raw response line instead of pretty JSON.",
    )
    args = parser.parse_args()

    # Determine dynamic socket target context
    if args.socket is None:
        env_socket = os.environ.get("AHM_TEST_CONTROL_SOCKET")
        if env_socket:
            args.socket = Path(env_socket)
        elif "/mnt/" in str(Path(__file__).resolve()):
            # Safe Fallback: We are running inside a Windows mount layer under WSL
            args.socket = Path(f"/tmp/{CONTROL_SOCKET_NAME}")
        else:
            # Running inside standard Linux system directory or native Termux container
            args.socket = Path(CONTROL_SOCKET_DEFAULT)

    # If it evaluates to a relative layout path, bind it cleanly to project root
    if not args.socket.is_absolute():
        project_root = Path(__file__).resolve().parent.parent
        args.socket = project_root / args.socket

    try:
        command = build_command(args.command)
    except (ValueError, json.JSONDecodeError) as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(2)

    try:
        response = asyncio.run(send_command(args.socket, command, args.timeout))
    except asyncio.TimeoutError:
        print("Error: Timed out waiting for response.", file=sys.stderr)
        sys.exit(1)
    except FileNotFoundError as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)

    if args.raw:
        print(json.dumps(response, separators=(",", ":")))
    else:
        print(json.dumps(response, indent=2))

    status = response.get("status")
    sys.exit(0 if status == "ok" else 1)


if __name__ == "__main__":
    main()

