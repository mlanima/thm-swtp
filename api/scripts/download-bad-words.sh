#!/bin/bash
set -euo pipefail

DIR="src/main/resources/bad-words"
mkdir -p "$DIR"

LANGS="ar cs da de en eo es fa fi fil fr fr-CA-u-sd-caqc hi hu it ja kab ko nl no pl pt ru sv th tlh tr zh"

for LANG in $LANGS; do
  if [ ! -f "$DIR/$LANG" ]; then
    echo "Downloading $LANG..."
    curl -sL "https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/master/$LANG" -o "$DIR/$LANG"
  fi
done

echo "Done. Downloaded $(ls -1 "$DIR" | wc -l) language files."
