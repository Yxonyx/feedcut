# Kapu

Saját Android app. Instagram Reels, YouTube Shorts és TikTok feed ellen, csak ezen a telefonon.

A Playről telepített Basta ettől független. A kettő ne fusson egyszerre: a Bastánál kapcsold ki a kisegítő szolgáltatást.

## Mi van a kezedben

- Nincs `INTERNET` jogosultság. Távoli szabály, analitika, fiók és bolt nincs.
- A szabályfájl a telefon belső tárhelyén van: `files/rules.json`. Az appban a Szabályfájl gomb ezt mutatja.
- A Kapu csak a bekapcsolt csomagok eseményét kéri a rendszertől. A többi app képernyőjét nem olvassa.
- Szöveget és képet nem néz. Csak a látható nézet `view-id` mezőjét hasonlítja a jeleidhez. Kivétel az `allowTabs`: ott az alsó fülsor feliratát olvassa (például „Marketplace, 4. lap”), más nézetét nem.
- Üres jel: az appot békén hagyja. `*`: az egész app, amint megnyílik.
- `allowTabs`: a fülsoron csak ezek a fülek maradhatnak kijelölve, a többiről átkattint az elsőre. A Facebookban `["Marketplace"]`: a Hírfolyam, a Reels és a többi fül helyett a Marketplace nyílik meg. A ☰ menü és a Csoportok szabad, mert ott nincs alsó fülsor. A facebook.com böngészőben ettől még elérhető.
- `back` a rendszer Vissza gombja. `home` a kezdőképernyőre visz.
- A felhőmentés ki van kapcsolva. Törléskor a szabályok és a számláló is mennek.

A gyári jelek nyilvános felületnevek (`clips_viewer`, `reel_watch_player`, `video_player_progress`). Ha egy app frissítése után nem fog, a jelet itt cseréled, nem egy idegen szerver.

## Fordítás

Android Studio: File, Open, ez a mappa, majd Run.

Parancssorból:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

A telepíthető csomag: `app/build/outputs/apk/debug/app-debug.apk`.

Android 13-tól a telepített appnál előbb engedélyezd a korlátozott beállításokat (App infó, három pont). Utána a Kisegítő lehetőségekben kapcsold be a Kaput. Xiaomin az App infóban az Akkumulátorkímélő legyen Nincs korlátozás, és az automatikus indítás Be. Enélkül a HyperOS pár perc után lefagyasztja a Kaput (GreezeManager), és a Kapu nem kap eseményt.

A Kapu frissítése után a már futó Facebookot egyszer zárd be, különben nem küld neki eseményt.
