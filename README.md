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

## 0.5.2
Stop audio when activity goes into background or task is removed. Private phase-aligned Trabant samples may be packaged separately; public repository contains no unlicensed source audio.

## 0.5.3
Trabant private recordings: retain full unaveraged samples, avoid continuous overlap and synthetic/table fallback when a recording is present. Limit pitch ratio to 1.55 until native high-RPM recordings are available. Existing foreground exit behavior remains.

## 0.5.4
Dedicated real-time Trabant two-stroke synthesizer. Two combustion events per crank revolution, variable individual pressure pulses, fixed exhaust resonances, no loop playback or broadband hiss for the Trabant. Existing imported audio for other profiles is unchanged. A generated driving audio preview accompanies the CI artifact.

## 0.5.5
Trabant synthesizer: feedback delay line models the expansion chamber (~140 Hz pipe resonance), separate kernels per cylinder for an audible twin character, short filtered noise burst gated to each combustion event (no continuous noise), sharper blowdown transient. Level reduced at high RPM to stay below clipping.

## 0.6.0
New physical-style synthesizer for all 16 other vehicles (EngineSynth): every cylinder bank feeds its own exhaust pipe, so the firing order shapes the sound (crossplane V8 burble, even V10/V12 scream, boxer beat). Per-cylinder differences and lumpy idle, diesel knock, valvetrain ticks, intake roar under load, overrun pops after lifting off and on upshifts, turbo whine with blow-off, roots blower, cooling fan (Beetle), hybrid e-motor and straight-cut gear whine. Levels calibrated per vehicle. Imported recordings still take priority within their RPM range.

## 0.6.1
Engine tone tuned against real recordings from Wikimedia Commons: CI downloads them (they are not packaged), compares third-octave spectra with the synthesizer and searches muffler cutoffs, brightness, pulse sharpness and the new per-combustion exhaust flow noise. Mean tone deviation dropped from about 5.9 dB to 4.5 dB. Results and sources: analysis/report.md. Rerun by pushing to the reference-analysis branch.
