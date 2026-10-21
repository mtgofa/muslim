#!/usr/bin/env python3
"""Build tafsir-muyassar.txt.gz — «التفسير الميسر» (King Fahd Complex) for every ayah — from
Quranpedia's open dump (https://quranpedia.net/dumps). The app downloads it once from the
repo's mushaf-v1 release the first time the tafsir tab is opened.

Output: gzip text, one line per ayah: "<sura>|<ayah>|<plain text>".
"""
import gzip, json, re, sys, urllib.request

OUT = sys.argv[1] if len(sys.argv) > 1 else "tafsir-muyassar.txt.gz"
BOOK = 2012  # التفسير الميسر, one entry per ayah

req = urllib.request.Request(f"https://quranpedia.net/dumps/tafsir-book-{BOOK}.json.gz", headers={"User-Agent": "Muslim build"})
with urllib.request.urlopen(req, timeout=120) as r:
    dump = json.loads(gzip.decompress(r.read()).decode("utf-8"))


def clean(t: str) -> str:
    t = re.sub(r'<div class="foot-notes">.*?</div>', "", t, flags=re.S)  # an editor's note
    t = t.replace("(*)", "")
    t = re.sub(r"<[^>]+>", "", t)
    t = t.replace(";", "؛")  # the source uses a Latin semicolon for the Arabic one
    return " ".join(t.split())


rows = sorted(dump["ayahs"], key=lambda e: (e["surah"], e["ayah"]))
assert len(rows) == 6236, len(rows)
lines = [f'{e["surah"]}|{e["ayah"]}|{clean(" ".join(c["text"] for c in e["content"]))}' for e in rows]
assert all(l.split("|", 2)[2] for l in lines)
with gzip.open(OUT, "wt", encoding="utf-8", compresslevel=9) as f:
    f.write("\n".join(lines) + "\n")
print(len(lines), "ayahs, dump version", dump["license"].get("version"))
