# Araç Denetim Defteri

Otobüs ve diğer araçlar için çevrimdışı çalışan hasar, arıza ve denetim not defteri.

## Ne yapar?

1. Araç kodunu yazıp (ör. **K1565**) **GİRİŞ**'e basarsınız.
2. Araç daha önce kaydedildiyse **geçmişteki tüm denetim kayıtları** açılır.
   Kaydedilmediyse "Yeni araç olarak kaydedilsin mi?" diye sorar.
3. Aracın ekranında şunlar görünür:
   - Toplam kayıt sayısı, giderilmemiş (açık) sorun sayısı ve son denetim tarihi
   - Tüm kayıtlar, en yenisi en üstte. Kırmızı şerit açık sorunu, gri şerit giderilmiş sorunu gösterir.
   - Tümü / Açık / Giderilen süzgeci
4. **+ YENİ DENETİM KAYDI** ile şunlar girilir:
   - Tarih ve saat (otomatik gelir, geçmiş bir tarih de seçilebilir)
   - Tür: Hasar, Arıza, Eksik parça, Temizlik, Güvenlik, Kontrol – sorun yok, Diğer
   - Aracın bölümü: Ön, Arka, Sağ yan, Sol yan, Kapılar, Camlar, Motor ve diğerleri
   - Açıklama
   - Durum: Açık veya Giderildi
5. Bir kayda dokunarak düzenleyebilir, "Giderildi" olarak işaretleyebilir veya silebilirsiniz.
6. Araç ekranındaki **⋮** menüsünden aracın tüm geçmişini metin olarak WhatsApp, SMS veya e-posta ile gönderebilirsiniz.
7. Ana ekrandaki **⋮** menüsünden tüm verileri **yedek dosyasına** kaydedebilir, başka bir telefona aktarabilirsiniz.

İnternet izni yoktur. Tüm veriler yalnızca telefonun hafızasında saklanır.
Her telefon kendi verisini tutar.

> **Önemli:** Uygulamayı silerseniz veriler de silinir. Önce menüden **Yedek al** yapın.

---

## APK nasıl oluşturulur?

### Yol 1: Android Studio (önerilen)

1. https://developer.android.com/studio adresinden Android Studio'yu indirip kurun.
2. Bu zip dosyasını bir klasöre çıkarın.
3. Android Studio'da **Open** seçeneğine tıklayıp `AracDenetim` klasörünü seçin.
4. İlk açılışta gerekli dosyalar internetten indirilir (birkaç dakika sürer).
   Alt kısımdaki ilerleme çubuğunun bitmesini bekleyin.
5. Üst menüden **Build > Build App Bundle(s) / APK(s) > Build APK(s)** seçin.
6. Bitince sağ altta çıkan **locate** bağlantısına tıklayın.
   APK dosyası `app/build/outputs/apk/debug/app-debug.apk` konumundadır.
7. Bu dosyayı telefonlara gönderip kurun.
   Telefon "bilinmeyen kaynak" uyarısı verirse kurulum için izin verin.

### Yol 2: GitHub (bilgisayara bir şey kurmadan)

1. github.com'da ücretsiz hesap açıp yeni bir depo (repository) oluşturun.
2. Bu klasördeki tüm dosyaları, `.github` klasörü dahil, depoya yükleyin.
3. **Actions** sekmesine gidin. Derleme kendiliğinden başlar ve 3–5 dakika sürer.
4. Biten çalışmaya tıklayın ve alttaki **AracDenetim-APK** dosyasını indirin.
   Zip'in içinden `app-debug.apk` çıkar.

---

## Renkleri veya listeleri değiştirmek

- Renkler: `app/src/main/res/values/colors.xml`
- Kayıt türleri ve araç bölümleri: `app/src/main/java/com/denetim/arac/Yardim.java`
  (`TURLER` ve `BOLUMLER` listeleri)
