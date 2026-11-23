package com.denetim.arac;

import java.util.Calendar;

/**
 * Peronlar ve her peronda o gün yapılması gereken en az denetim sayısı.
 * Sayılar amirlik denetim tablosundan alınmıştır.
 */
public final class Peron {

    /** Gün türü: tablodaki üç sütun. */
    public static final int IS_GUNU = 0;
    public static final int CUMARTESI = 1;
    public static final int PAZAR = 2;

    public static final String[] GUN_ADI = { "İş günü", "Cumartesi", "Pazar" };

    public static final String[] AD = {
            "Fenertepe Peronu",
            "Boğazköy Peronu",
            "K.S.S. Hastanesi Peronu",
            "Metrokent Peronu",
            "Halkalı Marmaray Peronu",
            "98AB Peronu",
            "Kayaşehir Peronu",
            "İ.Ü. Avcılar Kampüs",
            "Olimpiyat Metro - 31Y Hattı"
    };

    /** Tabloda bu peronun geçtiği satırın adı; tabloyla karşılaştırmak için. */
    public static final String[] TABLO_SATIRI = {
            "FENERTEPE",
            "BOĞAZKÖY",
            "K.S.S HASTANESİ",
            "METROKENT",
            "H. MARMARAY - H.PARK",
            "89B-98AB",
            "KAYAŞEHİR",
            "İST.ÜNİV.AVCILAR KAMP.",
            "31Y - OLİMPİYAT METRO"
    };

    /** [peron][gün] = tekli denetim sayısı */
    private static final int[][] TEKLI = {
            //  iş günü, cumartesi, pazar
            {  83,  58,  35 },   // Fenertepe
            {  63,  58,  47 },   // Boğazköy
            { 128,  97,  87 },   // K.S.S. Hastanesi
            {  43,  40,  35 },   // Metrokent
            {  33,  30,  28 },   // Halkalı Marmaray
            {  24,  19,  19 },   // 98AB (89B-98AB satırı)
            {  24,  15,   9 },   // Kayaşehir
            {  86,  60,  38 },   // İ.Ü. Avcılar Kampüs
            {  38,  25,  21 }    // Olimpiyat Metro - 31Y
    };

    /** [peron][gün] = çiftli denetim sayısı */
    private static final int[][] CIFTLI = {
            {  40,  30,  25 },
            {  30,  30,  25 },
            {  65,  50,  45 },
            {  25,  20,  20 },
            {  25,  25,  25 },
            {  20,  20,  20 },
            {  25,  15,   9 },
            {  45,  30,  20 },
            {  20,  20,  20 }
    };

    private Peron() {
    }

    public static int sayi() {
        return AD.length;
    }

    public static String ad(int i) {
        return (i >= 0 && i < AD.length) ? AD[i] : AD[0];
    }

    /** Bugünün tablodaki gün türü. */
    public static int bugununGunu() {
        int g = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if (g == Calendar.SATURDAY) return CUMARTESI;
        if (g == Calendar.SUNDAY) return PAZAR;
        return IS_GUNU;
    }

    /** Bir peronun belirtilen gün ve vardiya için en az denetim sayısı. */
    public static int enAzDenetim(int peron, int gun, boolean ciftli) {
        if (peron < 0 || peron >= AD.length) peron = 0;
        if (gun < 0 || gun > 2) gun = IS_GUNU;
        return ciftli ? CIFTLI[peron][gun] : TEKLI[peron][gun];
    }
}
