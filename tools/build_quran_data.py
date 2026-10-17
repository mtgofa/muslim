#!/usr/bin/env python3
"""Build app/src/main/assets/quran.db + per-page fonts from a source layout DB.

usage: build_quran_data.py <source.db> <fonts_dir_with_page_zips> <out_assets_dir> <out_pack_dir>

The surah-title font goes into the APK assets; the 604 page fonts go to <out_pack_dir>, which
tools/release.sh zips into the downloadable "Mushaf pack" release asset.
"""
import os, re, sqlite3, sys, unicodedata, zipfile, shutil

src, fonts_dir, out, pack = sys.argv[1:5]
os.makedirs(out, exist_ok=True)
dbp = os.path.join(out, "quran.db")
if os.path.exists(dbp):
    os.remove(dbp)
s = sqlite3.connect(src)
d = sqlite3.connect(dbp)
d.executescript("""
CREATE TABLE surah(id INTEGER PRIMARY KEY, name TEXT, name_en TEXT, ayahs INTEGER, mecca INTEGER, page INTEGER);
CREATE TABLE ayah(sura INTEGER, ayah INTEGER, text TEXT, plain TEXT, page INTEGER, juz INTEGER, PRIMARY KEY(sura, ayah));
CREATE TABLE glyph(id INTEGER PRIMARY KEY, page INTEGER, line INTEGER, type INTEGER, code TEXT, sura INTEGER, ayah INTEGER);
CREATE INDEX glyph_page ON glyph(page);
CREATE TABLE juz(id INTEGER PRIMARY KEY, sura INTEGER, ayah INTEGER, page INTEGER);
CREATE TABLE quarter(id INTEGER PRIMARY KEY, sura INTEGER, ayah INTEGER, page INTEGER);
""")

# ---- glyphs: re-number ayah-end marks into the page font's consecutive code range
rows = s.execute('SELECT page_number,line_number,glyph_type_id,glyph_code,sura_number,ayah_number '
                 'FROM TB_Glyph WHERE mushaf_id=2 ORDER BY page_number,"order"').fetchall()
fixed, last, page, bad = [], None, None, 0
for p, ln, t, code, su, ay in rows:
    if p != page:
        page, last = p, None
    if t == 1:
        if last is not None and ord(code[0]) != last + 1:
            bad += 1
        last = ord(code[-1])
    elif t == 2:
        code = chr(last + 1)
        last += 1
    fixed.append((p, ln, t, code, su, ay))
print("glyph rows", len(fixed), "sequence breaks", bad)
d.executemany("INSERT INTO glyph(page,line,type,code,sura,ayah) VALUES(?,?,?,?,?,?)", fixed)

# ---- first page of every ayah
first_page = {}
for p, ln, t, code, su, ay in fixed:
    if t in (1, 2) and (su, ay) not in first_page:
        first_page[(su, ay)] = p

# ---- juz / quarters (type 1 = juz, 3..5 + 2 = hizb quarters)
jh = s.execute("SELECT type,surah_number,ayah_number FROM TB_JuzaHizb ORDER BY surah_number,ayah_number,type").fetchall()
juz = [(su, ay) for t, su, ay in jh if t == 1]
quarters = sorted({(su, ay) for t, su, ay in jh if t in (2, 3, 4, 5)})
print("juz", len(juz), "quarters", len(quarters))
d.executemany("INSERT INTO juz VALUES(?,?,?,?)", [(i + 1, su, ay, first_page[(su, ay)]) for i, (su, ay) in enumerate(juz)])
d.executemany("INSERT INTO quarter VALUES(?,?,?,?)", [(i + 1, su, ay, first_page[(su, ay)]) for i, (su, ay) in enumerate(quarters)])

def juz_of(su, ay):
    j = 0
    for i, k in enumerate(juz):
        if (su, ay) >= k:
            j = i + 1
    return j

DIAC = re.compile(r"[ؐ-ًؚ-ٰٟۖ-ۭـ]")
def plain(t):
    t = DIAC.sub("", t)
    for a, b in (("أ", "ا"), ("إ", "ا"), ("آ", "ا"), ("ٱ", "ا"), ("ى", "ي"), ("ة", "ه"), ("ؤ", "و"), ("ئ", "ي")):
        t = t.replace(a, b)
    return t

ay_rows = s.execute("SELECT SuraID,AyahID,AyahText FROM TB_Quran WHERE TranslationID=1 ORDER BY SuraID,AyahID").fetchall()
d.executemany("INSERT INTO ayah VALUES(?,?,?,?,?,?)",
              [(su, ay, tx, plain(tx), first_page[(su, ay)], juz_of(su, ay)) for su, ay, tx in ay_rows])
print("ayahs", len(ay_rows))

for sid, name, en, n, rev in s.execute("SELECT ID,name,englishName,numberOfAyahs,revelationType FROM TB_Surah"):
    name = name.replace("سُورَةُ", "").strip()
    d.execute("INSERT INTO surah VALUES(?,?,?,?,?,?)", (sid, name, en, n, 1 if rev == "Meccan" else 0, first_page[(sid, 1)]))
d.commit()
d.execute("VACUUM")
d.close()

# ---- fonts
fo = os.path.join(out, "mushaf")
os.makedirs(fo, exist_ok=True)
os.makedirs(pack, exist_ok=True)
for fn in sorted(os.listdir(fonts_dir)):
    m = re.match(r"QCF4_tajweed_(\d+)\.zip", fn)
    with zipfile.ZipFile(os.path.join(fonts_dir, fn)) as z:
        ttf = [n for n in z.namelist() if n.endswith(".ttf") and not n.startswith("__")][0]
        name = "p%03d.ttf" % int(m.group(1)) if m else "header.ttf"
        with z.open(ttf) as a, open(os.path.join(fo if name == "header.ttf" else pack, name), "wb") as b:
            shutil.copyfileobj(a, b)
print("page fonts", len(os.listdir(pack)))
