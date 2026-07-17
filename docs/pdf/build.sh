#!/usr/bin/env bash
# README.md から README.pdf を生成する。
# pandoc(Typstエンジン) + カスタムTypstヘッダ + シンタックスハイライトを適用。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$ROOT_DIR"

pandoc README.md \
  -o README.pdf \
  --pdf-engine=typst \
  --include-in-header=docs/pdf/header.typ \
  --syntax-highlighting=docs/pdf/theme.theme \
  -V mainfont="Hiragino Sans" \
  -V monofont="Menlo" \
  -V fontsize=10pt \
  -V margin-x=2cm \
  -V margin-y=2cm

echo "generated: $ROOT_DIR/README.pdf"
