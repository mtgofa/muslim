#!/usr/bin/env bash
# Build a signed release APK and publish it as the latest GitHub release, which the app's
# auto-updater reads (release name carries "(code N)", body carries "sha256: <hex>").
#
#   tools/release.sh bump [notes…]   increase versionCode + patch version, then release
#   tools/release.sh [notes…]        release the current version
#   tools/release.sh pack           (re)publish the Mushaf page-font pack (prerelease tag)
set -euo pipefail
cd "$(dirname "$0")/.."

export JAVA_HOME="${JAVA_HOME:-/home/gofa/wasla-toolchain/jdk-17.0.20.1+1}"
GRADLE_FILE=app/build.gradle.kts
REPO="$(grep '^ghOwner=' gradle.properties | cut -d= -f2)/$(grep '^ghRepo=' gradle.properties | cut -d= -f2)"
PACK_TAG="$(grep '^mushafPackTag=' gradle.properties | cut -d= -f2)"

if [[ "${1:-}" == "pack" ]]; then
    [[ -d dist/mushaf-pack ]] || { echo "dist/mushaf-pack missing — run tools/build_quran_data.py first" >&2; exit 1; }
    rm -f dist/mushaf-pack.zip
    (cd dist/mushaf-pack && zip -q -9 ../mushaf-pack.zip p*.ttf)
    if gh release view "$PACK_TAG" -R "$REPO" >/dev/null 2>&1; then
        gh release upload "$PACK_TAG" dist/mushaf-pack.zip -R "$REPO" --clobber
    else
        gh release create "$PACK_TAG" dist/mushaf-pack.zip -R "$REPO" --prerelease \
            --title "Mushaf pack ($PACK_TAG)" \
            --notes "Page fonts downloaded once by the app on first opening of the Mushaf. Not an app release."
    fi
    echo "pack published: $PACK_TAG"
    exit 0
fi

if [[ "${1:-}" == "bump" ]]; then
    shift
    code=$(grep -oP 'versionCode = \K\d+' "$GRADLE_FILE")
    name=$(grep -oP 'versionName = "\K[^"]+' "$GRADLE_FILE")
    IFS=. read -r ma mi pa <<<"$name"
    sed -i "s/versionCode = $code/versionCode = $((code + 1))/; s/versionName = \"$name\"/versionName = \"$ma.$mi.$((pa + 1))\"/" "$GRADLE_FILE"
fi

code=$(grep -oP 'versionCode = \K\d+' "$GRADLE_FILE")
name=$(grep -oP 'versionName = "\K[^"]+' "$GRADLE_FILE")
notes="${*:-}"

./gradlew -q assembleRelease
mkdir -p dist
apk="dist/Muslim.apk"
cp app/build/outputs/apk/release/app-release.apk "$apk"
sha=$(sha256sum "$apk" | cut -d' ' -f1)

body="${notes:+$notes

}sha256: $sha"
gh release create "v$name" "$apk" -R "$REPO" --title "Muslim $name (code $code)" --notes "$body" --latest
echo "released v$name (code $code) — $(du -h "$apk" | cut -f1), sha256 $sha"
