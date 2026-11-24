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

## 0.7.0
- Two new vehicles: city bus (inline-six turbodiesel, automatic, air-brake release when stopping) and Space Shuttle (new RocketAudio: main engine start, booster ignition, turbulent roar with crackle, ground echoes; acceleration sets thrust, standing still for 4 s shuts down).
- Gearbox: upshift point depends on acceleration (gentle driving around a quarter of the rev range, full throttle near the redline). A shift is recognised when acceleration that had been building suddenly drops away (like pressing the clutch), if the RPM is in a sensible range.
- Horn button (hold): vehicle-typical horn, e.g. single thin horn for Trabant and Beetle, low loud pair for the bus, master alarm for the Space Shuttle. Works without GPS.
- Automatic acceleration run to top speed in demo mode, with vehicle-typical 0-100 km/h time, shift RPM, power-to-weight ratio and top speed (approximate published figures).

## 0.7.1
- Real gear ratios (gear ratios × final drive / tyre circumference) for Käfer 1300 (3.80/2.06/1.26/0.89, final 4.375), Trabant 601 (23.5 km/h per 1000 rpm in 4th), petrol and diesel four-cylinders, Camaro Z28, Mustang GT, GT3 RS, Aventador/Veneno, RS6, M3, LFA and city bus.
- Automatic run now drives like a real driver: early upshifts at the car's everyday RPM (diesel 2000, most petrol 2500, sports engines 3000-4000, Trabant 3300), engine power scales with RPM, downshift only when the current gear can no longer pull near top speed. Trabant 0-100 km/h corrected to about 40 s.

## 0.7.2
Horns are now recorded-style samples per vehicle (generated with ElevenLabs Sound Effects from vehicle-specific descriptions, four variants each; the cleanest single steady honk was picked automatically by tools/prepare_horns.py, see analysis/horn_selection.json). Tapping gives a short honk, holding loops the steady part seamlessly, releasing fades out. The procedural horn remains as a fallback.

## 0.7.3
Clutch/converter slip now only acts while pulling away (engine held at a launch RPM until the drivetrain catches up); afterwards RPM is exactly speed × ratio. Previously the slip was added permanently while accelerating, so fast cars (e.g. Chiron) showed too high RPM and the automatic run shifted far too early (Chiron 1→2 at 13 km/h, now 30 km/h).

## 0.7.4
- Gearing checked for all vehicles and corrected with published data: Lexus LFA (official max speed per gear 83/123/167/218/277 km/h), Porsche GT3 RS (Porsche ratios 3.75/2.381/1.7/1.344/1.114/0.957/0.844, final 4.188), Ferrari F12 (3.08/2.19/1.63/1.29/1.03/0.84/0.69, final 4.38, limiter 8700), Dodge Hellcat (8HP90, axle 2.62, limiter 6200), Lamborghini Veneno/Aventador (ISR ratios, overall reduction so that 350 km/h is reached in 7th at the power peak; previously 7th would have reached 486 km/h).
- Automatic run now accelerates briskly but not flat out: the driver aims for about half of the car's maximum acceleration (at most 5 m/s²), weak cars are therefore driven near full throttle, strong cars gently. Trabant tuned to its real 0-100 km/h of about 40 s.

## 0.7.5
Shift gaps: Formula 1 shifts in 50 ms (was 145 ms) and dual-clutch/sequential gearboxes (GT3 RS, F12, Veneno, Chiron, AMG GT3, Revuelto) in 60-100 ms with only a partial ignition cut, so drive is barely interrupted. Manual gearboxes keep the real clutch pause. RPM follows the new gear as fast as the gearbox shifts.
