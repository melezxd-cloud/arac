package com.vakit.namaz;

/**
 * Türkiye'nin 81 ili ve il merkezlerinin koordinatları.
 * Vakitler koordinata göre hesaplandığı için il adının yazılışı önemli değil.
 */
public final class Sehirler {

    public static final String[] AD = {
            "Adana", "Adıyaman", "Afyonkarahisar", "Ağrı", "Aksaray", "Amasya",
            "Ankara", "Antalya", "Ardahan", "Artvin", "Aydın", "Balıkesir",
            "Bartın", "Batman", "Bayburt", "Bilecik", "Bingöl", "Bitlis",
            "Bolu", "Burdur", "Bursa", "Çanakkale", "Çankırı", "Çorum",
            "Denizli", "Diyarbakır", "Düzce", "Edirne", "Elazığ", "Erzincan",
            "Erzurum", "Eskişehir", "Gaziantep", "Giresun", "Gümüşhane", "Hakkari",
            "Hatay", "Iğdır", "Isparta", "İstanbul", "İzmir", "Kahramanmaraş",
            "Karabük", "Karaman", "Kars", "Kastamonu", "Kayseri", "Kilis",
            "Kırıkkale", "Kırklareli", "Kırşehir", "Kocaeli", "Konya", "Kütahya",
            "Malatya", "Manisa", "Mardin", "Mersin", "Muğla", "Muş",
            "Nevşehir", "Niğde", "Ordu", "Osmaniye", "Rize", "Sakarya",
            "Samsun", "Siirt", "Sinop", "Sivas", "Şanlıurfa", "Şırnak",
            "Tekirdağ", "Tokat", "Trabzon", "Tunceli", "Uşak", "Van",
            "Yalova", "Yozgat", "Zonguldak"
    };

    public static final double[] ENLEM = {
            37.0000, 37.7648, 38.7507, 39.7191, 38.3687, 40.6499,
            39.9334, 36.8969, 41.1105, 41.1828, 37.8560, 39.6484,
            41.6344, 37.8812, 40.2552, 40.1426, 38.8854, 38.4006,
            40.7392, 37.7206, 40.1826, 40.1553, 40.6013, 40.5506,
            37.7765, 37.9144, 40.8438, 41.6818, 38.6810, 39.7500,
            39.9000, 39.7767, 37.0662, 40.9128, 40.4603, 37.5744,
            36.4018, 39.8880, 37.7648, 41.0082, 38.4192, 37.5858,
            41.2061, 37.1759, 40.6013, 41.3887, 38.7312, 36.7184,
            39.8468, 41.7333, 39.1425, 40.8533, 37.8667, 39.4167,
            38.3552, 38.6191, 37.3212, 36.8000, 37.2153, 38.7432,
            38.6939, 37.9667, 40.9839, 37.0742, 41.0201, 40.7569,
            41.2867, 37.9333, 42.0231, 39.7477, 37.1591, 37.5164,
            40.9833, 40.3167, 41.0015, 39.1079, 38.6823, 38.4891,
            40.6500, 39.8181, 41.4564
    };

    public static final double[] BOYLAM = {
            35.3213, 38.2786, 30.5567, 43.0503, 34.0370, 35.8353,
            32.8597, 30.7133, 42.7022, 41.8183, 27.8416, 27.8826,
            32.3375, 41.1351, 40.2249, 29.9793, 40.4983, 42.1095,
            31.6089, 30.2908, 29.0665, 26.4142, 33.6134, 34.9556,
            29.0864, 40.2306, 31.1565, 26.5623, 39.2264, 39.5000,
            41.2700, 30.5206, 37.3833, 38.3895, 39.4814, 43.7408,
            36.3498, 44.0048, 30.5566, 28.9784, 27.1287, 36.9371,
            32.6204, 33.2287, 43.0975, 33.7827, 35.4787, 37.1212,
            33.5153, 27.2167, 34.1709, 29.8815, 32.4833, 29.9833,
            38.3095, 27.4289, 40.7245, 34.6333, 28.3636, 41.5064,
            34.6857, 34.6833, 37.8764, 36.2464, 40.5234, 30.3781,
            36.3300, 41.9500, 35.1531, 37.0179, 38.7969, 42.4611,
            27.5167, 36.5500, 39.7178, 39.5401, 29.4082, 43.4089,
            29.2667, 34.8147, 31.7987
    };

    /** İstanbul'un listedeki sırası; ilk açılışta varsayılan il. */
    public static final int VARSAYILAN = 39;

    private Sehirler() {
    }

    public static int sayi() {
        return AD.length;
    }

    public static String ad(int i) {
        return (i >= 0 && i < AD.length) ? AD[i] : AD[VARSAYILAN];
    }

    public static double enlem(int i) {
        return (i >= 0 && i < ENLEM.length) ? ENLEM[i] : ENLEM[VARSAYILAN];
    }

    public static double boylam(int i) {
        return (i >= 0 && i < BOYLAM.length) ? BOYLAM[i] : BOYLAM[VARSAYILAN];
    }
}
