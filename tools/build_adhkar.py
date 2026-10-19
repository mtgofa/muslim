#!/usr/bin/env python3
"""Download the Hisn al-Muslim collection (hisnmuslim.com public JSON API) into
content/adhkar.json as [{id, title, items: [{text, repeat}]}].
Pushing content/ to GitHub updates installed apps without a release."""
import json, sys, time, urllib.request

OUT = sys.argv[1] if len(sys.argv) > 1 else "content/adhkar.json"

def get(url):
    for attempt in range(4):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Linux; Android 14) Muslem"})
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.loads(r.read().decode("utf-8-sig"))
        except Exception as e:
            time.sleep(1 + attempt)
    raise SystemExit(f"failed: {url}")

index = get("http://www.hisnmuslim.com/api/ar/husn_ar.json")["العربية"]
chapters = []
for ch in index:
    data = get(ch["TEXT"])
    items = next(iter(data.values()))
    chapters.append({
        "id": ch["ID"],
        "title": ch["TITLE"].strip(),
        "items": [{"text": it["ARABIC_TEXT"].strip(), "repeat": int(it.get("REPEAT") or 1)} for it in items],
    })
chapters.sort(key=lambda c: c["id"])
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(chapters, f, ensure_ascii=False, separators=(",", ":"))
print(len(chapters), "chapters,", sum(len(c["items"]) for c in chapters), "adhkar")

# separate morning and evening chapters, built from chapter 27
import os, subprocess
subprocess.run([sys.executable, os.path.join(os.path.dirname(os.path.abspath(__file__)), "split_morning_evening.py"), OUT], check=True)
