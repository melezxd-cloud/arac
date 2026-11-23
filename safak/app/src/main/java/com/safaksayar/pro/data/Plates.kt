package com.safaksayar.pro.data

import java.util.Locale

/** Türkiye'nin 81 ili, plaka koduna göre sıralı (indeks = plaka). */
object Plates {
    private val IL = listOf(
        "",
        "Adana", "Adıyaman", "Afyonkarahisar", "Ağrı", "Amasya", "Ankara", "Antalya", "Artvin", "Aydın", "Balıkesir",
        "Bilecik", "Bingöl", "Bitlis", "Bolu", "Burdur", "Bursa", "Çanakkale", "Çankırı", "Çorum", "Denizli",
        "Diyarbakır", "Edirne", "Elazığ", "Erzincan", "Erzurum", "Eskişehir", "Gaziantep", "Giresun", "Gümüşhane", "Hakkari",
        "Hatay", "Isparta", "Mersin", "İstanbul", "İzmir", "Kars", "Kastamonu", "Kayseri", "Kırklareli", "Kırşehir",
        "Kocaeli", "Konya", "Kütahya", "Malatya", "Manisa", "Kahramanmaraş", "Mardin", "Muğla", "Muş", "Nevşehir",
        "Niğde", "Ordu", "Rize", "Sakarya", "Samsun", "Siirt", "Sinop", "Sivas", "Tekirdağ", "Tokat",
        "Trabzon", "Tunceli", "Şanlıurfa", "Uşak", "Van", "Yozgat", "Zonguldak", "Aksaray", "Bayburt", "Karaman",
        "Kırıkkale", "Batman", "Şırnak", "Bartın", "Ardahan", "Iğdır", "Yalova", "Karabük", "Kilis", "Osmaniye",
        "Düzce"
    )

    private val TR = Locale.forLanguageTag("tr-TR")

    /** Kalan gün 1..81 ise o günün ilini döndürür, değilse null. */
    fun cityFor(daysLeft: Int): String? = IL.getOrNull(daysLeft)?.takeIf { daysLeft in 1..81 }

    fun cityUpper(daysLeft: Int): String? = cityFor(daysLeft)?.uppercase(TR)

    fun code(daysLeft: Int): String = daysLeft.toString().padStart(2, '0')
}
