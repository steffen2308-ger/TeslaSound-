#!/usr/bin/env bash
# Compare all vehicle profiles with downloaded reference recordings.
# Usage: tools/run_refmatch.sh REFDIR OUTDIR
set -euo pipefail
refs=$1; outdir=$2
mkdir -p "$outdir"
classes=$(mktemp -d)
javac -d "$classes" $(find tools/refmatch -name '*.java') \
  app/src/main/java/de/teslasound/app/{EngineAudio,EngineSynth,Gearbox,TrabantAudio}.java
: > "$outdir/results.jsonl"
for mode in 0 1 2 3 5 6 7 8 9 10 11 12 13 14 15 16; do
  files=$(ls "$refs"/${mode}_*.wav 2>/dev/null | tr '\n' ' ' || true)
  if [ -z "$files" ]; then echo "mode $mode: no reference"; continue; fi
  echo "$mode $files"
done | grep -v "no reference" | xargs -P "$(nproc)" -L 1 bash -c \
  'java -cp "'"$classes"'" de.teslasound.app.RefMatch "$0" "$@" || echo "{\"mode\":$0,\"error\":true}"' \
  >> "$outdir/results.jsonl"
python3 tools/refmatch_report.py "$refs/sources.json" "$outdir/results.jsonl" > "$outdir/report.md"
cat "$outdir/report.md"
