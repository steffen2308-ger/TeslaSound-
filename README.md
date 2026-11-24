# TeslaSound

Experimentelle Android-App für virtuelle Motorgeräusche über die normale Bluetooth-Medienausgabe. GPS liefert die Geschwindigkeit, Smartphone-Sensoren die Beschleunigung. Kein Fahrzeugzugriff, keine Internetverbindung, keine Speicherung von Standortdaten.

Bedienung im Stand: Handy sicher befestigen, Tesla als Bluetooth-Medienquelle auswählen, Standortfreigabe (genau) erteilen und Sound starten. Lautstärke moderat einstellen. Nicht während der Fahrt bedienen. Kein Ersatz für Fahrzeugwarnsignale oder AVAS. Bei fehlendem GPS verstummt die Fahr-Simulation. Der Demo-Modus ist nur für den Stand gedacht.

Der Debug-Build ist für Android 8+ vorgesehen. Build mit JDK 17, Gradle 8.9 und Android SDK 35: gradle assembleDebug. APK: app/build/outputs/apk/debug/app-debug.apk. Alternativ GitHub Actions APK-Build starten, Artefakt herunterladen und entpacken. Debug-APK ist nur für Tests gedacht. Bluetooth-Latenz, Sensorverhalten und Audio-Routing müssen am Fahrzeug getestet werden.

Inoffizielles Projekt ohne Verbindung zu Tesla.

## Version 0.4.0
17 Fahrzeugprofile mit eigens gezeichneten, anklickbaren Seitenansichten und gespeicherter Auswahl. Neue Fahrzeuge: Veneno, RS6 Avant, Chiron, M3 Competition, AMG GT3, LFA, Käfer 1300, Revuelto. Motorgeräusche sind synthetische Annäherungen, wenn keine Samples verfügbar sind. Eigene gleichmäßige PCM-WAV-Aufnahmen (44,1 kHz, 16 Bit, mono/stereo) können je Fahrzeug und Drehzahl lokal importiert werden. Importierte Samples bleiben auf dem Gerät; Neustart des Sounds lädt sie. Nur rechtmäßig nutzbare Aufnahmen verwenden.

## Version 0.5.0
Alle Bedienelemente stehen oberhalb der Fahrzeuggalerie. Die 17 fotorealistisch KI-generierten Fahrzeugbilder sind direkt in assets/vehicles/0.jpg bis 16.jpg enthalten. Das neue TeslaSound-App-Icon ist in drawable-nodpi/teslasound_icon.png hinterlegt. Alle Bilder sind offline verfügbar. Die Motorsynthese bleibt in dieser Version unverändert. Eigene PCM-WAV-Aufnahmen lassen sich weiterhin importieren. Die Fahrzeugbilder sind illustrative KI-Renderings, keine Herstellerfotos.

## 0.5.1
Trabant-specific pulse synthesis, no continuous synthetic broadband noise on any profile. Optional licensed Trabant sample acquisition and JVM audio smoke test; no device listening test.
