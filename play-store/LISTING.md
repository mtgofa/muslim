# Google Play — بيانات الإدراج (مسلم · Muslim)

## الاسم
- العربي: **مسلم**
- English: **Muslim**

## الوصف القصير (حتى 80 حرفاً)
- عربي: مواقيت الصلاة والأذان في وقته، والمصحف بألوان التجويد وأصوات القراء
- English: Prayer times, adhan on time, Mushaf with tajweed colours and reciters

## الوصف الكامل (عربي)
مسلم تطبيق متكامل للمسلم في يومه:
- مواقيت الصلاة حسب موقعك، مع أذان في وقته وتنبيه قبله وتنبيه للإقامة، وإزاحة لكل صلاة.
- المصحف الشريف بخط المدينة وألوان التجويد، مع الاستماع لعدد من القراء والتكرار والسرعة، وتكبير الخط بالإصبعين.
- معاني الكلمات والتفسير الميسر لكل آية.
- أذكار الصباح والمساء، وحصن المسلم، والأذكار اليومية، مع تذكير يومي لكل منهما.
- السبحة بعدّ وصوت واهتزاز.
- القبلة بالبوصلة.
- الورد اليومي لختمة القرآن، والتذكير به.
- تذكير الصيام (الاثنين والخميس والأيام البيض).
- تنبيه قيام الليل في الوقت الذي تختاره.
يعمل التطبيق بدون حساب ولا إعلانات، ولا يجمع بياناتك الشخصية.

## Full description (English)
Muslim is a complete companion for the Muslim day:
- Prayer times for your location, with the adhan on time, a reminder before it and one for the iqama, and adjustments for each prayer.
- The Holy Quran in the Madinah print with tajweed colours, listening to several reciters, repeat and speed control, and pinch to enlarge the text.
- Word meanings and a simplified tafsir for every ayah.
- Morning and evening adhkar, Hisn al-Muslim and daily adhkar, with a daily reminder for each.
- Tasbeeh with count, sound and vibration.
- Qibla compass.
- A daily Quran reading plan with reminders.
- Fasting reminders (Mondays, Thursdays and the white days).
- A night-prayer alarm at a time you choose.
No account, no ads, and no collection of personal data.

## الفئة
- نمط الحياة (Lifestyle)، أو الكتب والمراجع إن فضّلت.

## معلومات الاتصال
- البريد: gofait25@gmail.com
- سياسة الخصوصية: https://mtgofa.github.io/muslim/privacy-policy.html (بعد تفعيل GitHub Pages، انظر الخطوات أدناه)

## تعبئة استبيان «سلامة البيانات» (Data safety)
- البيانات التي يجمعها التطبيق: **الموقع التقريبي/الدقيق** لحساب المواقيت، **يبقى على الجهاز** ولا يُرسل للمطوّر.
- لا يجمع بيانات شخصية، ولا يشارك بيانات مع طرف ثالث، ولا يستخدم بيانات للإعلانات.
- البيانات مشفّرة أثناء النقل (HTTPS).
- يمكن للمستخدم طلب حذف البيانات بحذف التطبيق أو بيانات التطبيق.

## تصنيف المحتوى والجمهور
- التصنيف: للجميع (Everyone).
- الجمهور المستهدف: 13 سنة فأكثر (التطبيق ليس موجهاً للأطفال).
- لا يحتوي على إعلانات، ولا مشتريات داخل التطبيق.

## الأذونات التي تحتاج تبريراً في Play Console
- **USE_FULL_SCREEN_INTENT**: شاشة الأذان تظهر فوق شاشة القفل في وقت الصلاة، وهي تنبيه منبّه بطبيعته.
- **SCHEDULE_EXACT_ALARM**: لتشغيل الأذان في الدقيقة المحددة.
- **FOREGROUND_SERVICE_SPECIAL_USE**: خدمة تُبقي التطبيق مجدولاً للأذان، ويظهر لها إشعار دائم يمكن إيقافه من الإعدادات.
- **ACCESS_FINE_LOCATION / COARSE**: لحساب المواقيت واتجاه القبلة، ويمكن إدخال المدينة يدوياً بدلاً منه.

## خطوات الرفع
1. فعّل GitHub Pages: من المستودع mtgofa/muslim ← Settings ← Pages ← Deploy from a branch ← main ← المجلد /docs. ثم تصبح سياسة الخصوصية على الرابط أعلاه.
2. في Play Console أنشئ التطبيق، وحمّل `Muslim-1.0.60.aab` في الإنتاج أو الاختبار الداخلي.
3. فعّل Play App Signing (المفتاح المرفوع هو مفتاح `keystore/muslem-release.jks`).
4. ارفع الصور: `app-icon-512.png` و`feature-graphic-1024x500.png` و`screenshots/` (1080×2160).

## إجابات استبيان تصنيف المحتوى (IARC)
- العنف: لا. المحتوى الديني والتلاوة لا يتضمن عنفاً.
- المحتوى الجنسي أو الإيحاءات: لا.
- الألفاظ النابية: لا.
- المواد المخدّرة أو الكحول أو التبغ: لا.
- المقامرة: لا.
- المحتوى الذي ينشئه المستخدم أو يتيح التواصل بين المستخدمين: لا.
- يشارك الموقع الجغرافي مع الآخرين: لا (يبقى على الجهاز).
- يقدّم شراءات داخل التطبيق: لا.
- المتوقع: التصنيف للجميع (Everyone).

## خطة الاختبار المغلق (حساب شخصي)
1. في Play Console: الاختبار ← الاختبار المغلق ← مسار جديد، وأضف 12 مختبراً على الأقل بإيميلاتهم (Gmail).
2. ارفع `Muslim-1.0.60.aab` إلى هذا المسار، وأرسل للمختبرين رابط الانضمام.
3. يبقى المسار مفتوحاً 14 يوماً متصلة، ويجب أن يظل 12 مختبراً على الأقل فاعلين طوال هذه المدة.
4. بعد 14 يوماً يصبح طلب النشر في الإنتاج متاحاً.

## قائمة التأكد قبل الضغط على «نشر»
- [ ] رابط سياسة الخصوصية يعمل: https://mtgofa.github.io/muslim/privacy-policy.html
- [ ] صورة الأيقونة 512 وصورة الخلفية 1024×500 ولقطات الشاشة (4 لقطات).
- [ ] تعبئة استبيان سلامة البيانات وفق القسم أعلاه.
- [ ] تعبئة استبيان التصنيف وفق القسم أعلاه.
- [ ] شرح الأذونات الحساسة (USE_FULL_SCREEN_INTENT و FOREGROUND_SERVICE_SPECIAL_USE).
- [ ] حقوق الأصوات (أذان علي ملا وأذانات abodehq) مؤكدة، أو أُزيلت.
- [ ] التطبيق مُختبَر على هاتف Android 15.
