#!/bin/sh

#
# Generate short unique string for the current Bamboo build. Used in Bamboo pipeline.
#

set -e

branch_name=$1
build_number=$2

if [ -z "$branch_name" ] || [ -z "$build_number" ]; then
  echo "Usage: $0 <branch_name> <build_number>"
  exit 1
fi

tag=$(git tag --points-at HEAD | head -n 1)
if [ -n "$tag" ]; then
  echo "$tag"
  exit 0
fi

branch_lower=$(echo "$branch_name" | tr '[:upper:]' '[:lower:]')
branch_short=$(echo "$branch_lower" | sed -e 's/.*\///' | sed -r -e 's/[^A-Za-z0-9]+/-/g' | cut -c1-20 | sed -e 's/-$//')

if [ "$branch_name" = "$branch_short" ]; then
  # found simple result name
  result="$branch_name"
else
  # similar to git short revisions
  sha_short=$(echo "$branch_name" | sha256sum | cut -c1-7)
  result="$branch_short-$sha_short"
fi

echo "$result-$build_number"
