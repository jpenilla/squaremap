#!/usr/bin/env bash
# build.sh — build squaremap Paper plugin (web UI + paper jar)
# Usage: ./build.sh [--skip-frontend] [--with-javadoc]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

SKIP_FRONTEND=0
WITH_JAVADOC=0
for arg in "$@"; do
  case "$arg" in
    --skip-frontend) SKIP_FRONTEND=1 ;;
    --with-javadoc)  WITH_JAVADOC=1 ;;
    -h|--help)
      cat <<'EOF'
Usage: ./build.sh [options]

  Builds the web frontend (bun) then the Paper plugin jar.

Options:
  --skip-frontend   Skip bun install / bun run build
  --with-javadoc    Do not skip javadoc tasks (default: skip; often broken on forks)
  -h, --help        Show this help

On a fresh clone this script checks for required tools and prints install
hints if anything is missing.
EOF
      exit 0
      ;;
    *)
      echo "Unknown option: $arg (try --help)" >&2
      exit 1
      ;;
  esac
done

# ---------------------------------------------------------------------------
# Colours (only if stdout is a TTY)
# ---------------------------------------------------------------------------
if [[ -t 1 ]]; then
  C_RED=$'\033[0;31m'
  C_GREEN=$'\033[0;32m'
  C_YELLOW=$'\033[0;33m'
  C_CYAN=$'\033[0;36m'
  C_BOLD=$'\033[1m'
  C_RESET=$'\033[0m'
else
  C_RED= C_GREEN= C_YELLOW= C_CYAN= C_BOLD= C_RESET=
fi

info()  { printf '%s==>%s %s\n' "$C_CYAN" "$C_RESET" "$*"; }
ok()    { printf '%s==>%s %s\n' "$C_GREEN" "$C_RESET" "$*"; }
warn()  { printf '%s==>%s %s\n' "$C_YELLOW" "$C_RESET" "$*"; }
fail()  { printf '%s==>%s %s\n' "$C_RED" "$C_RESET" "$*" >&2; exit 1; }

# ---------------------------------------------------------------------------
# Dependency checks
# ---------------------------------------------------------------------------
MISSING=0

need_cmd() {
  local name="$1"
  local hint="$2"
  if command -v "$name" >/dev/null 2>&1; then
    ok "Found $name: $(command -v "$name")"
  else
    warn "Missing: $name"
    printf '    %s\n' "$hint"
    MISSING=1
  fi
}

info "Checking dependencies..."

# Java (JDK 21+)
if command -v java >/dev/null 2>&1; then
  JAVA_VER="$(java -version 2>&1 | head -1 || true)"
  ok "Found java: $JAVA_VER"
else
  warn "Missing: java (JDK 21 or later required)"
  printf '    Install a JDK 21+ and ensure `java` is on PATH.\n'
  printf '    Example (Temurin): https://adoptium.net/\n'
  MISSING=1
fi

# Bun (needed unless --skip-frontend)
if [[ "$SKIP_FRONTEND" -eq 0 ]]; then
  # Prefer PATH; also check the default install location
  if ! command -v bun >/dev/null 2>&1 && [[ -x "${HOME}/.bun/bin/bun" ]]; then
    export PATH="${HOME}/.bun/bin:${PATH}"
  fi

  if command -v bun >/dev/null 2>&1; then
    ok "Found bun: $(bun --version 2>/dev/null || command -v bun)"
  else
    warn "Missing: bun"
    cat <<'EOF'
    Bun is required to build the web UI.

    Install (Linux/macOS):
      curl -fsSL https://bun.com/install | bash

    Then add to PATH (and put this in ~/.bashrc or ~/.zshrc):
      export BUN_INSTALL="$HOME/.bun"
      export PATH="$BUN_INSTALL/bin:$PATH"

    Open a new terminal (or `source` your rc file) and re-run ./build.sh
EOF
    MISSING=1
  fi
fi

# gradlew must exist
if [[ ! -x "$ROOT/gradlew" ]]; then
  if [[ -f "$ROOT/gradlew" ]]; then
    chmod +x "$ROOT/gradlew"
  else
    fail "gradlew not found in $ROOT — run this script from the squaremap repo root"
  fi
fi
ok "Found gradlew"

if [[ "$MISSING" -ne 0 ]]; then
  fail "Install the missing tools above, then re-run ./build.sh"
fi

# ---------------------------------------------------------------------------
# Frontend
# ---------------------------------------------------------------------------
if [[ "$SKIP_FRONTEND" -eq 0 ]]; then
  if [[ ! -d "$ROOT/web" ]] || [[ ! -f "$ROOT/web/package.json" ]]; then
    fail "web/ directory or package.json missing — is this the squaremap repo?"
  fi

  info "Installing web dependencies (bun install)..."
  (
    cd "$ROOT/web"
    bun install
  )
  ok "bun install complete"

  info "Building web frontend (bun run build)..."
  (
    cd "$ROOT/web"
    bun run build
  )
  ok "Frontend build complete"
else
  warn "Skipping frontend (--skip-frontend)"
fi

# ---------------------------------------------------------------------------
# Paper plugin
# ---------------------------------------------------------------------------
GRADLE_ARGS=(:squaremap-paper:shadowJar)
if [[ "$WITH_JAVADOC" -eq 0 ]]; then
  GRADLE_ARGS+=(-x javadoc -x javadocJar)
  info "Building Paper plugin (javadoc skipped; pass --with-javadoc to include)..."
else
  info "Building Paper plugin (including javadoc)..."
fi

# Ensure bun is on PATH for Gradle's buildFrontend task as well
if [[ "$SKIP_FRONTEND" -eq 0 ]] && [[ -d "${HOME}/.bun/bin" ]]; then
  export PATH="${HOME}/.bun/bin:${PATH}"
fi

./gradlew "${GRADLE_ARGS[@]}"

# ---------------------------------------------------------------------------
# Point at the jar to copy
# ---------------------------------------------------------------------------
echo
ok "Build finished."

shopt -s nullglob
CANDIDATES=(
  "$ROOT/paper/build/libs"/squaremap-paper-*.jar
)
shopt -u nullglob

PLUGIN_JAR=""
for f in "${CANDIDATES[@]:-}"; do
  base="$(basename "$f")"
  case "$base" in
    *-sources.jar|*-javadoc.jar) continue ;;
    *) PLUGIN_JAR="$f"; break ;;
  esac
done

if [[ -n "$PLUGIN_JAR" && -f "$PLUGIN_JAR" ]]; then
  printf '\n%sPlugin jar:%s\n  %s\n\n' "$C_BOLD" "$C_RESET" "$PLUGIN_JAR"
  printf 'Copy that file into your server plugins/ folder.\n'
  printf 'Do %snot%s use *-sources.jar or *-javadoc.jar.\n' "$C_BOLD" "$C_RESET"
else
  warn "Could not locate squaremap-paper-*.jar under paper/build/libs/"
  printf '  Check the Gradle output above for the actual path.\n'
fi
