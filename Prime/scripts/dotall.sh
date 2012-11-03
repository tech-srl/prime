#!/bin/bash
pushd $1
FILES=*.dot
mkdir svgs
for f in $FILES
do
  echo "Processing $f..."
  dot -Tsvg -osvgs/$f.svg < $f
done

popd
