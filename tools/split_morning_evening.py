#!/usr/bin/env python3
"""Add separate «أذكار الصباح» (id 201) and «أذكار المساء» (id 202) chapters to
content/adhkar.json, derived from Hisn al-Muslim's combined chapter 27, each with its own wording
(أصبحنا / أمسينا …) and only the adhkar said at that time. Safe to run again.
"""
import json, re, sys

PATH = sys.argv[1] if len(sys.argv) > 1 else "content/adhkar.json"
MORNING, EVENING, SOURCE = 201, 202, 27

data = json.load(open(PATH, encoding="utf-8"))
data = [c for c in data if c["id"] not in (MORNING, EVENING)]
src = next(c for c in data if c["id"] == SOURCE)["items"]


def clean(t):
    t = re.sub(r"\[[^\]]*\]", "", t)  # the "[وإذا أمسى قال: …]" notes
    return " ".join(t.split())


# the evening wording of the adhkar that differ, by index in chapter 27
EVENING_TEXT = {
    2: "((أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لاَ إِلَهَ إلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، رَبِّ أَسْأَلُكَ خَيْرَ مَا فِي هَذِهِ اللَّيْلَةِ وَخَيْرَ مَا بَعْدَهَا، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِي هَذِهِ اللَّيْلَةِ وَشَرِّ مَا بَعْدَهَا، رَبِّ أَعُوذُ بِكَ مِنَ الْكَسَلِ وَسُوءِ الْكِبَرِ، رَبِّ أَعُوذُ بِكَ مِنْ عَذَابٍ فِي النَّارِ وَعَذَابٍ فِي الْقَبْرِ)).",
    3: "((اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ الْمَصِيرُ)).",
    5: "((اللَّهُمَّ إِنِّي أَمْسَيْتُ أُشْهِدُكَ، وَأُشْهِدُ حَمَلَةَ عَرْشِكَ، وَمَلاَئِكَتِكَ، وَجَمِيعَ خَلْقِكَ، أَنَّكَ أَنْتَ اللَّهُ لَا إِلَهَ إِلاَّ أَنْتَ وَحْدَكَ لاَ شَرِيكَ لَكَ، وَأَنَّ مُحَمَّداً عَبْدُكَ وَرَسُولُكَ)) (أربعَ مَرَّاتٍ).",
    6: "((اللَّهُمَّ مَا أَمْسَى بِي مِنْ نِعْمَةٍ أَوْ بِأَحَدٍ مِنْ خَلْقِكَ فَمِنْكَ وَحْدَكَ لاَ شَرِيكَ لَكَ، فَلَكَ الْحَمْدُ وَلَكَ الشُّكْرُ)).",
    14: "((أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ رَبِّ الْعَالَمِينَ، اللَّهُمَّ إِنِّي أَسْأَلُكَ خَيْرَ هَذِهِ اللَّيْلَةِ: فَتْحَهَا، وَنَصْرَهَا، وَنُورَهَا، وَبَرَكَتَهَا، وَهُدَاهَا، وَأَعُوذُ بِكَ مِنْ شَرِّ مَا فِيهَا وَشَرِّ مَا بَعْدَهَا)).",
    15: "((أَمْسَيْنَا عَلَى فِطْرَةِ الْإِسْلاَمِ، وَعَلَى كَلِمَةِ الْإِخْلاَصِ، وَعَلَى دِينِ نَبِيِّنَا مُحَمَّدٍ صلى الله عليه وسلم، وَعَلَى مِلَّةِ أَبِينَا إِبْرَاهِيمَ، حَنِيفاً مُسْلِماً وَمَا كَانَ مِنَ الْمُشرِكِينَ)).",
}
MORNING_ONLY = {18, 19, 20}
EVENING_ONLY = {22}
REPEAT_FIX = {8: 7}  # «حسبي الله…» is said seven times

assert len(src) == 24, len(src)
morning, evening = [], []
for i, d in enumerate(src):
    rep = REPEAT_FIX.get(i, d["repeat"])
    if i not in EVENING_ONLY:
        morning.append({"text": clean(d["text"]), "repeat": rep})
    if i not in MORNING_ONLY:
        evening.append({"text": EVENING_TEXT.get(i, clean(d["text"])), "repeat": rep})

data += [
    {"id": MORNING, "title": "أذكار الصباح", "items": morning},
    {"id": EVENING, "title": "أذكار المساء", "items": evening},
]
with open(PATH, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
print(len(morning), "morning,", len(evening), "evening")
