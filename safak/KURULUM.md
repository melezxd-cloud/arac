# Şafak Sayar Pro – Kurulum ve Yayınlama

Paket adı: `com.safaksayar.pro` · Sürüm: 1.0.0 (kod 1)
Hedef: Android 16 (API 36) · En düşük: Android 8.0 (API 26)
AdMob uygulama kimliği ve banner kimliği koda yazılı.

## 1. Projeyi aç (ilk sefer 10–15 dk)
1. **Android Studio**'nun güncel sürümünü kur: https://developer.android.com/studio
2. Bu zip'i bir klasöre çıkar.
3. Android Studio > **File > Open** > `SafakSayarPro` klasörünü seç.
4. Alttaki çubukta **Gradle sync** bitene kadar bekle (internet gerekir, gerekli her şeyi kendisi indirir).
   "Android SDK 36 yüklü değil" uyarısı çıkarsa uyarıdaki **Install** bağlantısına tıkla.

## 2. Telefonda dene
1. Telefonda **Ayarlar > Telefon hakkında > Yapım numarası**'na 7 kez dokun (geliştirici seçenekleri açılır).
2. **Geliştirici seçenekleri > USB hata ayıklama**'yı aç, telefonu USB ile bağla.
3. Android Studio'da üstteki yeşil ▶ **Run** düğmesine bas.

Bu deneme sürümünde altta **"Test Ad"** yazan Google test reklamı görünür, bu normal.
Gerçek reklam sadece yayın (release) sürümünde çıkar. **Kendi gerçek reklamına asla tıklama.**

## 3. Play Store için imzalı paket (AAB)
1. **Build > Generate Signed App Bundle or APK** > **Android App Bundle** > Next.
2. **Create new...** ile bir anahtar deposu (keystore) oluştur. Şifreleri bir yere yaz.
   ⚠️ Bu `.jks` dosyasını ve şifreleri **yedekle**. Kaybedersen uygulamayı güncelleyemezsin.
3. **release** seç > Create. Çıktı: `app/release/app-release.aab`

Sadece APK istersen aynı menüde **APK** seç (Play Store AAB ister, APK telefona elle kurmak içindir).

## 4. Gizlilik politikası
`gizlilik-politikasi.html` dosyasındaki `[E-POSTA ADRESİNİZİ BURAYA YAZIN]` kısmını değiştir ve
ücretsiz bir yerde yayınla (Google Sites, GitHub Pages vb.). Çıkan web adresi Play Console'a girilecek.

## 5. Play Console'da doldurulacaklar
- **Uygulama oluştur**: ad "Şafak Sayar Pro", Uygulama, Ücretsiz.
- **Reklam içeriyor**: Evet.
- **Gizlilik politikası**: 4. adımdaki web adresi.
- **Hedef kitle**: 18 yaş ve üzeri.
- **İçerik derecelendirmesi**: anketi doldur (şiddet vb. yok).
- **Veri güvenliği**: "Veri toplanıyor: Evet" → *Cihaz veya diğer kimlikler* (reklam kimliği), *Uygulama etkinliği / etkileşimler*, *Uygulama bilgileri ve performansı* → amaç: **Reklam veya pazarlama**, **Analiz**, **Sahtekârlık önleme**. Aktarım sırasında şifrelenir: Evet. Kullanıcı silme talebinde bulunabilir: veriler cihazda tutulur.
- **İzinler**: Bildirim (kalan gün bildirimi için), açılışta çalıştırma (gece yarısı güncellemesini telefon açılınca yeniden kurmak için).
- **Mağaza girişi**: kısa açıklama, uzun açıklama, ikon 512×512, öne çıkan görsel 1024×500, en az 2 ekran görüntüsü.

Yeni bir **kişisel** geliştirici hesabındaysan Google, üretime çıkmadan önce kapalı testte belirli sayıda
test kullanıcısının uygulamayı belirli bir süre kullanmasını isteyebilir. Play Console'daki "Kontrol Paneli"
sana tam şartı gösterir.

## 6. Yayından sonra AdMob
AdMob > Uygulamalar > Şafak Sayar Pro > **Mağaza ekle** ile Play Store sayfasını bağla.
"Sınırlı reklam sunumu" uyarısı inceleme bitince kalkar.

## 7. Güncelleme yayınlarken
`app/build.gradle.kts` içinde `versionCode`'u 1 artır (2, 3, ...) ve `versionName`'i değiştir (1.0.1 ...),
sonra 3. adımı **aynı keystore** ile tekrarla.

## Dosya haritası
| Dosya | Ne işe yarar |
|---|---|
| `data/SafakCalculator.kt` | Şafak hesabı (TSK kuralları, takvim ayı, rapor/ceza, elle çıkış) |
| `data/Plates.kt` | 81 il plakası |
| `ui/MainScreen.kt` | Ana ekran: halka, plaka, saat, kartlar, aşama |
| `ui/SettingsScreen.kt` | Dişliden açılan ayarlar |
| `notify/SafakNotifier.kt` | Durum çubuğu sayısı, kalıcı bar, özel gün bildirimleri |
| `ads/Ads.kt` | AdMob banner ve reklam izni |
| `app/build.gradle.kts` | Sürüm numarası, banner kimlikleri |
