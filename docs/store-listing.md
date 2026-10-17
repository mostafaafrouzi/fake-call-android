# متن‌های انتشار در مارکت‌ها — تماس ساختگی / Fake Call

## کافه‌بازار و مایکت (فارسی)

**عنوان:** تماس ساختگی

**توضیح کوتاه:**
تماس ورودی ساختگی با نام و شمارهٔ دلخواه، دقیقاً در زمانی که شما تنظیم می‌کنید

**توضیح کامل:**
جلسه‌ای که تمام نمی‌شود؟ قراری که می‌خواهید مؤدبانه ترکش کنید؟ «تماس ساختگی» دقیقاً برای همین لحظه‌هاست.

یک پروفایل بسازید (نام، شماره و عکس تماس‌گیرنده)، زمان را انتخاب کنید و تمام؛ گوشی شما سر وقت با یک تماس ورودی کاملاً واقع‌گرایانه زنگ می‌خورد. اگر برای پروفایل صدای ضبط‌شده گذاشته باشید، بعد از پاسخ‌دادن همان صدا پخش می‌شود تا همه‌چیز طبیعی به نظر برسد.

امکانات:
- زمان‌بندی سریع: ۱۰ ثانیه، ۳۰ ثانیه، ۱ دقیقه، ۵ دقیقه یا زمان دلخواه
- پروفایل‌های نامحدود با عکس و صدای اختصاصی
- نمایش تمام‌صفحه روی قفل گوشی، با رینگتون و ویبره
- تاریخچهٔ تماس‌های اجراشده
- دوزبانه (فارسی و انگلیسی) با تم روشن و تاریک

تماس ساختگی کاملاً آفلاین کار می‌کند؛ بدون اینترنت، بدون حساب کاربری، بدون تبلیغ و بدون جمع‌آوری هیچ داده‌ای.

## Google Play (English)

**Title:** Fake Call

**Short description:**
A realistic fake incoming call with your chosen caller, at the exact time you set

**Full description:**
Stuck in a meeting that never ends? On a date you politely want to leave? Fake Call is made for exactly those moments.

Create a caller profile (name, number, photo), pick a delay, and your phone rings right on time with a fully realistic incoming call. If the profile has a recorded voice, it plays after you answer — so everything sounds natural.

Features:
- Quick delays: 10s, 30s, 1m, 5m, or a custom time
- Unlimited caller profiles with custom photo and voice
- Full-screen call over the lock screen, with ringtone and vibration
- Call history inside the app
- Bilingual (English and Persian), light & dark themes

Fake Call works completely offline. No internet, no account, no ads, no data collection.

## توجیه دسترسی‌ها (برای تیم بررسی مارکت‌ها)

با سلام و احترام؛
دسترسی‌های این برنامه صرفاً برای کارکرد اصلی آن (زمان‌بندی و نمایش تماس ساختگی) و فقط با اقدام صریح کاربر استفاده می‌شوند:

- **نمایش تمام‌صفحه (USE_FULL_SCREEN_INTENT):** برای نمایش صفحهٔ تماس ساختگی روی قفل گوشی در زمان مقرر؛ بدون آن، تماس زمان‌بندی‌شده دیده نمی‌شود.
- **آلارم دقیق (SCHEDULE_EXACT_ALARM):** برای زنگ خوردن تماس دقیقاً در ثانیهٔ تنظیم‌شده توسط کاربر.
- **اعلان‌ها (POST_NOTIFICATIONS):** برای نمایش تماس ورودی و وضعیت زمان‌بندی.
- **لرزش (VIBRATE) و اجرای بعد از راه‌اندازی (RECEIVE_BOOT_COMPLETED):** به‌ترتیب برای ویبرهٔ تماس و بازگردانی زمان‌بندی‌ها پس از ری‌استارت گوشی.

برنامه کاملاً متن‌باز است و هیچ داده‌ای از دستگاه خارج نمی‌شود:
https://github.com/mostafaafrouzi/fake-call-android
