/*
 * Çevrimdışı çalışmayı sağlayan servis çalışanı.
 * İlk açılışta bütün dosyaları telefona kaydeder, sonra internetsiz çalışır.
 * Dosyalar değişince SURUM numarasını artırmak yeterlidir.
 */
const SURUM = 'arac-denetim-v3';

const DOSYALAR = [
  './',
  './index.html',
  './manifest.webmanifest',
  './icon-192.png',
  './icon-512.png'
];

self.addEventListener('install', function (e) {
  e.waitUntil(
    caches.open(SURUM)
      .then(function (c) { return c.addAll(DOSYALAR); })
      .then(function () { return self.skipWaiting(); })
  );
});

self.addEventListener('activate', function (e) {
  e.waitUntil(
    caches.keys()
      .then(function (adlar) {
        return Promise.all(adlar.map(function (ad) {
          return ad === SURUM ? null : caches.delete(ad);
        }));
      })
      .then(function () { return self.clients.claim(); })
  );
});

self.addEventListener('fetch', function (e) {
  if (e.request.method !== 'GET') return;

  e.respondWith(
    caches.match(e.request).then(function (bellekten) {
      if (bellekten) {
        // Arka planda sessizce güncelle, ama hemen bellekteki sürümü ver
        fetch(e.request).then(function (yanit) {
          if (yanit && yanit.ok) {
            caches.open(SURUM).then(function (c) { c.put(e.request, yanit); });
          }
        }).catch(function () { /* internet yok, sorun değil */ });
        return bellekten;
      }
      return fetch(e.request).catch(function () {
        return caches.match('./index.html');
      });
    })
  );
});
