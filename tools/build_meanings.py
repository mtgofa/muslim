#!/usr/bin/env python3
"""Build content/meanings.json — word meanings (غريب القرآن) per ayah — from Quranpedia's open
dump (https://quranpedia.net/dumps, free for use inside apps). Prefers «السراج في بيان غريب
القرآن» (al-Khudairi) and falls back to «كلمات القرآن» (Makhlouf) where it has no entry.

Output: {"<sura>:<ayah>": [[word, meaning], ...], ...}
"""
import gzip, json, sys, urllib.request

OUT = sys.argv[1] if len(sys.argv) > 1 else "content/meanings.json"
PREFERRED = [491, 1424, 208, 327]  # Siraj, Kalimat al-Quran, Tadhkirat al-Arib, al-Tibyan

req = urllib.request.Request("https://quranpedia.net/dumps/meanings.json.gz", headers={"User-Agent": "Muslem build"})
with urllib.request.urlopen(req, timeout=120) as r:
    dump = json.loads(gzip.decompress(r.read()).decode("utf-8"))

out = {}
for e in dump["data"]:
    books = {m["book_info"]["id"]: m["words"] for m in e["meanings"] if m.get("words")}
    for bid in PREFERRED:
        if bid in books:
            out[f'{e["surah"]}:{e["ayah"]}'] = [[w["text"].strip(), " ".join(w["meaning"].split())] for w in books[bid]]
            break
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, separators=(",", ":"))
print(len(out), "ayahs,", sum(len(v) for v in out.values()), "words, dump version", dump["license"].get("version"))
