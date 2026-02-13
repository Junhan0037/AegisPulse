#!/usr/bin/env bash

set -euo pipefail

# 변경 파일이 "단일 목적(한 단계/한 기능)" 범위를 만족하는지 점검한다.
# - 인자 미지정: Git staged 파일 우선, 없으면 working tree 변경 파일을 검사
# - 인자 지정: 전달된 파일 목록을 그대로 검사 (CI/로컬 디버깅 용도)

readonly SCRIPT_NAME="$(basename "$0")"

print_usage() {
  cat <<'EOF'
Usage:
  scripts/verify-single-purpose-change.sh [file1 file2 ...]

Examples:
  scripts/verify-single-purpose-change.sh
  scripts/verify-single-purpose-change.sh src/main/java/com/aegispulse/service/ServiceController.java
EOF
}

is_git_repo() {
  git rev-parse --is-inside-work-tree >/dev/null 2>&1
}

collect_changed_files() {
  if [[ "$#" -gt 0 ]]; then
    printf '%s\n' "$@"
    return 0
  fi

  if ! command -v git >/dev/null 2>&1 || ! is_git_repo; then
    echo "[${SCRIPT_NAME}] ERROR: Git 저장소가 아니므로 자동 변경 파일 수집이 불가합니다." >&2
    echo "[${SCRIPT_NAME}] HINT: 파일 경로를 인자로 전달해 실행하세요." >&2
    return 1
  fi

  # pre-push 시점에는 staged 기준 검사가 가장 명확하다.
  local staged
  staged="$(git diff --cached --name-only --diff-filter=ACMR)"
  if [[ -n "${staged}" ]]; then
    printf '%s\n' "${staged}"
    return 0
  fi

  # staged가 없다면 working tree 변경까지 포함해 점검한다.
  local changed
  changed="$(git diff --name-only --diff-filter=ACMR HEAD)"
  if [[ -n "${changed}" ]]; then
    printf '%s\n' "${changed}"
    return 0
  fi

  return 0
}

classify_file() {
  local path="$1"
  local lower
  lower="$(printf '%s' "${path}" | tr '[:upper:]' '[:lower:]')"

  # 문서/운영 안내 파일은 기능 구현 범위와 분리하여 docs로 분류한다.
  case "${lower}" in
    etc/*.md|*.md|docs/*)
      echo "docs"
      return 0
      ;;
  esac

  # build, wrapper, 공통 설정은 보조 변경(shared)으로 허용한다.
  case "${lower}" in
    build.gradle|settings.gradle|gradlew|gradlew.bat|gradle/*|src/main/resources/application*.properties)
      echo "shared"
      return 0
      ;;
  esac

  # Stage 0: 공통 인프라/설정/예외/필터/트레이싱 영역
  case "${lower}" in
    *"/config/"*|*"/common/"*|*"/exception/"*|*trace*|*"/infra/"*)
      echo "stage0"
      return 0
      ;;
  esac

  # Stage 1: 서비스/라우트 관리
  case "${lower}" in
    *service*|*route*)
      echo "stage1"
      return 0
      ;;
  esac

  # Stage 2: 템플릿/정책 엔진
  case "${lower}" in
    *policy*|*template*)
      echo "stage2"
      return 0
      ;;
  esac

  # Stage 3: consumer/key 관리
  case "${lower}" in
    *consumer*|*apikey*|*api-key*|*key*)
      echo "stage3"
      return 0
      ;;
  esac

  # Stage 4: 메트릭/대시보드
  case "${lower}" in
    *metric*|*dashboard*)
      echo "stage4"
      return 0
      ;;
  esac

  # Stage 5: 알림
  case "${lower}" in
    *alert*|*alarm*)
      echo "stage5"
      return 0
      ;;
  esac

  # Stage 6: 감사로그
  case "${lower}" in
    *audit*)
      echo "stage6"
      return 0
      ;;
  esac

  # Stage 7: 격리 모드
  case "${lower}" in
    *isolation*|*circuit*|*quarantine*)
      echo "stage7"
      return 0
      ;;
  esac

  # Stage 8은 릴리스 통합 단계라 파일 패턴 강제보다 문서/검증 결과가 핵심이다.
  echo "unknown"
}

is_stage_category() {
  case "$1" in
    stage0|stage1|stage2|stage3|stage4|stage5|stage6|stage7)
      return 0
      ;;
    *)
      return 1
      ;;
  esac
}

main() {
  # macOS 기본 bash(3.x) 호환을 위해 mapfile 대신 while-read를 사용한다.
  local changed_files=()
  while IFS= read -r line; do
    [[ -z "${line}" ]] && continue
    changed_files+=("${line}")
  done < <(collect_changed_files "$@")

  if [[ "${#changed_files[@]}" -eq 0 ]]; then
    echo "[${SCRIPT_NAME}] INFO: 검사할 변경 파일이 없습니다."
    exit 0
  fi

  local categories_seen=""
  local stage_seen=""
  local unknown_files=""
  local unknown_count=0

  echo "[${SCRIPT_NAME}] 변경 파일 분류 결과:"
  for file in "${changed_files[@]}"; do
    [[ -z "${file}" ]] && continue
    category="$(classify_file "${file}")"
    printf '  - %s => %s\n' "${file}" "${category}"
    if [[ " ${categories_seen} " != *" ${category} "* ]]; then
      categories_seen="${categories_seen} ${category}"
    fi
    if is_stage_category "${category}"; then
      if [[ " ${stage_seen} " != *" ${category} "* ]]; then
        stage_seen="${stage_seen} ${category}"
      fi
    fi
    if [[ "${category}" == "unknown" ]]; then
      unknown_files="${unknown_files}"$'\n'"${file}"
      unknown_count=$((unknown_count + 1))
    fi
  done

  local stage_count=0
  local stage
  for stage in ${stage_seen}; do
    stage_count=$((stage_count + 1))
  done

  # 핵심 규칙:
  # 1) stage는 최대 1개만 허용
  # 2) unknown 파일이 있으면 목적 분류가 불가능하므로 실패 처리
  if [[ "${stage_count}" -gt 1 ]]; then
    echo "[${SCRIPT_NAME}] ERROR: 여러 Stage 변경이 혼합되었습니다:${stage_seen}" >&2
    echo "[${SCRIPT_NAME}] HINT: 한 브랜치/커밋에는 한 Stage 목적만 포함하세요." >&2
    exit 1
  fi

  if [[ "${unknown_count}" -gt 0 ]]; then
    echo "[${SCRIPT_NAME}] ERROR: 분류 불가 파일이 존재합니다." >&2
    while IFS= read -r file; do
      [[ -z "${file}" ]] && continue
      printf '  - %s\n' "${file}" >&2
    done <<< "${unknown_files}"
    echo "[${SCRIPT_NAME}] HINT: 스크립트 패턴에 파일 목적(Stage)을 반영하세요." >&2
    exit 1
  fi

  echo "[${SCRIPT_NAME}] PASS: 단일 목적 변경 규칙을 만족합니다."
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  print_usage
  exit 0
fi

main "$@"
