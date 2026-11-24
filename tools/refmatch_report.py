"""Turn RefMatch JSON lines into a readable Markdown report."""
import json, sys

NAMES = ["Formel 1", "V8 klassisch", "Diesel", "Benziner", "Trabant", "Porsche 911 GT3 RS", "Mustang GT Muscle",
         "Ferrari F12", "Dodge Hellcat", "Lamborghini Veneno", "Audi RS6 Avant", "Bugatti Chiron",
         "BMW M3 Competition", "Mercedes-AMG GT3", "Lexus LFA", "VW Käfer 1300", "Lamborghini Revuelto", "Stadtbus", "Space Shuttle"]
sources = json.load(open(sys.argv[1]))
rows = []
for l in open(sys.argv[2]):
    try: rows.append(json.loads(l))
    except ValueError: pass
rows.sort(key=lambda r: r['mode'])
print('# Abgleich mit echten Aufnahmen (Wikimedia Commons)\n')
print('Abweichung = mittlere Differenz der Klangfarbe in Terzbändern 50 Hz–10 kHz (dB), kleiner ist besser.\n')
print('| Fahrzeug | Aufnahmen | vorher | nachher | Muffler lo/hi Hz | Helligkeit | Schärfe | Rauheit |')
print('|---|---|---|---|---|---|---|---|')
for r in rows:
    m = r['mode']; src = '<br>'.join(s['title'].replace('File:', '') for s in sources.get(str(m), []))
    if r.get('error'):
        print(f'| {NAMES[m]} | {src} | Fehler | | | | | |'); continue
    p, c = r['params'], r['current']
    print(f"| {NAMES[m]} | {src} | {r['base']:.1f} | {r['best']:.1f} | {c['mufLo']:.0f}/{c['mufHi']:.0f} → {p['mufLo']:.0f}/{p['mufHi']:.0f} "
          f"| {c['bright']:.2f} → {p['bright']:.2f} | {c['sharp']:.0f} → {p['sharp']:.0f} | {c['rasp']:.2f} → {p['rasp']:.2f} |")
missing = [NAMES[int(m)] for m, v in sources.items() if not v]
if missing:
    print('\nKeine passende Aufnahme gefunden: ' + ', '.join(missing))
