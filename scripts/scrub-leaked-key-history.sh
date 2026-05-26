#!/usr/bin/env bash
#
# scrub-leaked-key-history.sh
#
# PURPOSE
#   Permanently remove app/google-services.json (which contains the leaked
#   Google API key AIzaSyCbh_mUGE5srd8ubdhClcsOCdG_JDbkKbo) from the ENTIRE
#   git history of this repository.
#
# ⚠️  THIS REWRITES HISTORY AND IS DESTRUCTIVE.
#   - It changes every commit SHA after the first commit that touched the file.
#   - Everyone with a clone must re-clone (or hard-reset) afterwards.
#   - You must force-push, which can break open PRs.
#   - DO NOT run this on your only copy. Make a backup first.
#
# IMPORTANT: Removing the key from history does NOT un-leak it. The key has
# been public since Aug 2025 and must be assumed compromised. BEFORE OR AFTER
# scrubbing, you MUST restrict and/or rotate it (see KEY-REMEDIATION steps at
# the bottom of this file). Scrubbing only stops the secret scanner from
# re-flagging it and prevents future clones from seeing it.
#
# This script does NOTHING unless you pass --run. By default it prints the plan.

set -euo pipefail

FILE_TO_PURGE="app/google-services.json"

cat <<'BANNER'
=========================================================================
 Leaked-key history scrub  (DRY RUN unless --run is passed)
=========================================================================
BANNER

DO_RUN="no"
if [[ "${1:-}" == "--run" ]]; then
  DO_RUN="yes"
fi

# --- Preferred tool: git filter-repo --------------------------------------
# Install: pipx install git-filter-repo   (or)   pip install git-filter-repo
if command -v git-filter-repo >/dev/null 2>&1 || git filter-repo --help >/dev/null 2>&1; then
  echo "Tool: git filter-repo (recommended)"
  echo "Plan:"
  echo "  1. Work on a FRESH mirror clone (filter-repo refuses a dirty/working repo)."
  echo "  2. git filter-repo --path ${FILE_TO_PURGE} --invert-paths --force"
  echo "  3. Re-add the remote, force-push all branches and tags."
  if [[ "$DO_RUN" == "yes" ]]; then
    echo
    echo ">>> Running filter-repo on the CURRENT repo..."
    echo ">>> (Recommended instead: clone --mirror, run there, then push.)"
    read -r -p "Type 'SCRUB' to proceed: " confirm
    [[ "$confirm" == "SCRUB" ]] || { echo "Aborted."; exit 1; }
    git filter-repo --path "${FILE_TO_PURGE}" --invert-paths --force
    echo "Done. Now: git remote add origin <url> && git push --force --all && git push --force --tags"
  fi
  exit 0
fi

# --- Fallback tool: BFG Repo-Cleaner ---------------------------------------
# Download bfg.jar from https://rtyley.github.io/bfg-repo-cleaner/  (needs Java)
cat <<EOF
Tool: git filter-repo NOT found. Fallback = BFG Repo-Cleaner.
Plan (run against a fresh 'git clone --mirror'):
  1. git clone --mirror <repo-url> repo-mirror.git
  2. cd repo-mirror.git
  3. java -jar bfg.jar --delete-files google-services.json
  4. git reflog expire --expire=now --all && git gc --prune=now --aggressive
  5. git push --force
EOF

# ---------------------------------------------------------------------------
# KEY-REMEDIATION (do this regardless of history scrubbing) -- console only:
#
# 1. Google Cloud Console -> APIs & Services -> Credentials -> the leaked key.
# 2. Application restrictions: "Android apps" -> add package
#    com.hereliesaz.lexorcist + the release/debug signing SHA-1 fingerprints.
# 3. API restrictions: "Restrict key" -> allow ONLY the APIs the app uses
#    (e.g. Drive, Sheets, Apps Script, Generative Language) -- nothing else.
# 4. Strongly recommended: create a NEW key, swap it into a fresh
#    google-services.json (kept out of git -- it is already in .gitignore),
#    then delete the old leaked key.
# ---------------------------------------------------------------------------
