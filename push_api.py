#!/usr/bin/env python3
"""Push current working-tree changes to a remote branch via the GitHub Git Database API.
Usage: python3 push_api.py <branch> "<commit message>"   (run from repo root)
Pushes ALL changes vs origin/main (tracked modifications + untracked files, excluding build dirs).
"""
import sys, os, base64, subprocess

sys.path.insert(0, "/home/hatch/workspace/skills/github/bin")
from gh import call

REPO = "/repos/kurupdevs/mogscan"
BASE = "origin/main"

def git(*args):
    return subprocess.run(["git"] + list(args), capture_output=True, text=True, check=True).stdout.strip()

def main():
    branch, message = sys.argv[1], sys.argv[2]
    # changed tracked files vs base
    changed = git("diff", "--name-only", BASE).split()
    untracked = git("ls-files", "--others", "--exclude-standard").split()
    files = [f for f in changed + untracked
             if f and not f.startswith("build/") and "/build/" not in f and not f.startswith(".git")]
    if not files:
        print("nothing to push")
        return
    print(f"pushing {len(files)} files to {branch}")

    # remote base: branch head if exists else origin/main
    try:
        ref = call("GET", REPO + f"/git/refs/heads/{branch}")
        parent = ref["object"]["sha"]
    except SystemExit:
        parent = git("rev-parse", BASE)
    base_tree = call("GET", REPO + f"/git/commits/{parent}")["tree"]["sha"]

    entries = []
    for path in files:
        full = os.path.join(os.getcwd(), path)
        if not os.path.exists(full):
            entries.append({"path": path, "mode": "100644", "type": "blob", "sha": None})
            continue
        with open(full, "rb") as f:
            data = f.read()
        blob = call("POST", REPO + "/git/blobs", {"content": base64.b64encode(data).decode(), "encoding": "base64"})
        entries.append({"path": path, "mode": "100644", "type": "blob", "sha": blob["sha"]})

    tree = call("POST", REPO + "/git/trees", {"base_tree": base_tree, "tree": entries})
    commit = call("POST", REPO + "/git/commits", {
        "message": message,
        "tree": tree["sha"],
        "parents": [parent],
        "author": {"name": "kurupdevs", "email": "kurupdevs@users.noreply.github.com"},
    })
    try:
        call("PATCH", REPO + f"/git/refs/heads/{branch}", {"sha": commit["sha"]})
    except SystemExit:
        call("POST", REPO + "/git/refs", {"ref": f"refs/heads/{branch}", "sha": commit["sha"]})
    print("pushed:", commit["sha"][:8], "->", branch)

main()
