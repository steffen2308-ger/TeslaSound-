# TeslaSound

Experimentelle Android-App für virtuelle Motorgeräusche über die normale Bluetooth-Medienausgabe. GPS liefert die Geschwindigkeit, Smartphone-Sensoren die Beschleunigung. Kein Fahrzeugzugriff, keine Internetverbindung, keine Speicherung von Standortdaten.

Bedienung im Stand: Handy sicher befestigen, Tesla als Bluetooth-Medienquelle auswählen, Standortfreigabe (genau) erteilen und Sound starten. Lautstärke moderat einstellen. Nicht während der Fahrt bedienen. Kein Ersatz für Fahrzeugwarnsignale oder AVAS. Bei fehlendem GPS verstummt die Fahr-Simulation. Der Demo-Modus ist nur für den Stand gedacht.

Der Debug-Build ist für Android 8+ vorgesehen. Build mit JDK 17, Gradle 8.9 und Android SDK 35: gradle assembleDebug. APK: app/build/outputs/apk/debug/app-debug.apk. Alternativ GitHub Actions APK-Build starten, Artefakt herunterladen und entpacken. Debug-APK ist nur für Tests gedacht. Bluetooth-Latenz, Sensorverhalten und Audio-Routing müssen am Fahrzeug getestet werden.

Inoffizielles Projekt ohne Verbindung zu Tesla.

## Version 0.4.0
17 Fahrzeugprofile mit eigens gezeichneten, anklickbaren Seitenansichten und gespeicherter Auswahl. Neue Fahrzeuge: Veneno, RS6 Avant, Chiron, M3 Competition, AMG GT3, LFA, Käfer 1300, Revuelto. Motorgeräusche sind synthetische Annäherungen, wenn keine Samples verfügbar sind. Eigene gleichmäßige PCM-WAV-Aufnahmen (44,1 kHz, 16 Bit, mono/stereo) können je Fahrzeug und Drehzahl lokal importiert werden. Importierte Samples bleiben auf dem Gerät; Neustart des Sounds lädt sie. Nur rechtmäßig nutzbare Aufnahmen verwenden.

## Version 0.5.0
Alle Bedienelemente stehen oberhalb der Fahrzeuggalerie. Fotorealistisch generierte Fahrzeugbilder werden lokal aus assets/vehicles/0.jpg bis 16.jpg geladen. Fehlen die Fotos in einem reinen Quellcode-Build, werden die bisherigen Zeichnungen als Ersatz gezeigt. Das App-Icon ist als drawable-nodpi/teslasound_icon.png hinterlegt. Die hochauflösenden Bilder werden zusätzlich als separates Grafikpaket bereitgestellt; zur vollständigen Reproduktion vor dem Build in app/src/main/ kopieren (assets und res). Der APK-Build kann auch nachträglich mit den Bilddateien gepackt und erneut signiert werden. Die Motorsynthese bleibt in dieser Version unverändert.
