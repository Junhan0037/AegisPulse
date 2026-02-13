#!/usr/bin/env bash

set -euo pipefail

# 프로젝트 표준 훅(.githooks)을 Git hooksPath로 연결한다.
# 이후 pre-push 훅에서 단일 목적 변경 검증이 자동 실행된다.

if ! command -v git >/dev/null 2>&1; then
  echo "[install-git-hooks] ERROR: git 명령을 찾을 수 없습니다." >&2
  exit 1
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "[install-git-hooks] ERROR: Git 저장소 루트에서 실행해야 합니다." >&2
  exit 1
fi

git config core.hooksPath .githooks
echo "[install-git-hooks] OK: core.hooksPath=.githooks"
echo "[install-git-hooks] INFO: pre-push 시 단일 목적 변경 검증이 실행됩니다."
