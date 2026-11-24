# TeslaSound

Experimentelle Android-App für virtuelle Motorgeräusche über die normale Bluetooth-Medienausgabe. GPS liefert die Geschwindigkeit, Smartphone-Sensoren die Beschleunigung. Kein Fahrzeugzugriff, keine Internetverbindung, keine Speicherung von Standortdaten.

Bedienung im Stand: Handy sicher befestigen, Tesla als Bluetooth-Medienquelle auswählen, Standortfreigabe (genau) erteilen und Sound starten. Lautstärke moderat einstellen. Nicht während der Fahrt bedienen. Kein Ersatz für Fahrzeugwarnsignale oder AVAS. Bei fehlendem GPS verstummt die Fahr-Simulation. Der Demo-Modus ist nur für den Stand gedacht.

Der Debug-Build ist für Android 8+ vorgesehen. Build mit JDK 17, Gradle 8.9 und Android SDK 35: gradle assembleDebug. APK: app/build/outputs/apk/debug/app-debug.apk. Alternativ GitHub Actions APK-Build starten, Artefakt herunterladen und entpacken. Debug-APK ist nur für Tests gedacht. Bluetooth-Latenz, Sensorverhalten und Audio-Routing müssen am Fahrzeug getestet werden.

Inoffizielles Projekt ohne Verbindung zu Tesla.
