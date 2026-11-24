"""Find and download real engine recordings from Wikimedia Commons for spectral comparison.

The recordings are only analysed in CI; they are not packaged into the app.
Usage: python3 tools/fetch_references.py OUTDIR
"""
import json, pathlib, subprocess, sys, urllib.parse, urllib.request

UA = 'TeslaSoundReferenceAnalysis/1.0 (https://github.com/steffen2308-ger/TeslaSound-)'
# mode -> (search queries, groups of words; a title must contain all words of at least one group)
CARS = {
    0: (['Formula 1 V8 engine sound', 'Formula One car engine sound', 'F1 car sound'], [['formula'], ['f1 ']]),
    1: (['Chevrolet Camaro engine sound', 'Chevrolet Corvette C3 sound', 'V8 engine sound'], [['camaro'], ['chevelle'], ['corvette'], ['v8']]),
    2: (['diesel engine car sound', 'TDI engine sound', 'diesel engine idle'], [['diesel'], ['tdi']]),
    3: (['four cylinder engine sound car', 'Honda Civic engine sound', 'Volkswagen Golf engine sound'], [['civic'], ['cylinder'], ['golf']]),
    5: (['Porsche 911 GT3 RS engine and exhaust sound', 'Porsche 911 GT3 sound'], [['gt3']]),
    6: (['Ford Mustang GT engine and exhaust sound', 'Ford Mustang sound'], [['mustang']]),
    7: (['Ferrari F12 engine and exhaust sound', 'Ferrari 812 sound'], [['f12'], ['812']]),
    8: (['Challenger Hellcat sound', 'Launching sound Challenger', 'Dodge Challenger SRT sound'], [['hellcat'], ['challenger']]),
    9: (['Lamborghini Veneno sound', 'Lamborghini Aventador engine and exhaust sound'], [['veneno'], ['aventador']]),
    10: (['Audi RS6 engine and exhaust sound', 'Audi RS 6 sound'], [['rs6'], ['rs 6']]),
    11: (['Bugatti Chiron sound', 'Bugatti Veyron engine and exhaust sound'], [['chiron'], ['veyron']]),
    12: (['BMW M3 engine and exhaust sound', 'BMW M4 engine and exhaust sound'], [['bmw', 'm3'], ['bmw', 'm4']]),
    13: (['Mercedes AMG GT3 sound', 'Mercedes-AMG GT engine and exhaust sound', 'SLS AMG GT3 sound'], [['amg']]),
    14: (['Lexus LFA engine and exhaust sound', 'Lexus LFA sound'], [['lfa']]),
    15: (['Volkswagen Beetle engine sound', 'VW Käfer Motor', 'Volkswagen Type 1 engine sound'], [['beetle'], ['käfer'], ['kafer'], ['type 1']]),
    16: (['Lamborghini Revuelto sound', 'Lamborghini Aventador SVJ sound', 'Lamborghini Aventador engine and exhaust sound'], [['revuelto'], ['aventador']]),
}
AUDIO_MIME = ('audio/', 'video/webm', 'video/ogg', 'application/ogg')


def get(url, timeout=60):
    req = urllib.request.Request(url, headers={'User-Agent': UA})
    with urllib.request.urlopen(req, timeout=timeout) as f:
        return f.read()


def search(query):
    params = {'action': 'query', 'format': 'json', 'generator': 'search', 'gsrnamespace': 6,
              'gsrsearch': query, 'gsrlimit': 15, 'prop': 'imageinfo', 'iiprop': 'url|mime|size'}
    data = json.loads(get('https://commons.wikimedia.org/w/api.php?' + urllib.parse.urlencode(params)))
    pages = sorted(data.get('query', {}).get('pages', {}).values(), key=lambda p: p.get('index', 99))
    return [(p['title'], p['imageinfo'][0]) for p in pages if p.get('imageinfo')]


def main():
    out = pathlib.Path(sys.argv[1]); out.mkdir(parents=True, exist_ok=True)
    chosen = {}
    for mode, (queries, groups) in CARS.items():
        picks = []
        for q in queries:
            try:
                results = search(q)
            except Exception as ex:
                print(f'mode {mode}: search failed for {q!r}: {ex!r}', flush=True); continue
            for title, info in results:
                low = title.lower()
                if not info['mime'].startswith(AUDIO_MIME) or info['size'] > 80e6: continue
                if not any(all(w in low for w in g) for g in groups): continue
                if any(title == p['title'] for p in picks): continue
                picks.append({'title': title, 'url': info['url'], 'mime': info['mime']})
                if len(picks) >= 2: break
            if len(picks) >= 2: break
        files = []
        for i, p in enumerate(picks):
            raw = out / f'{mode}_{i}.src'; wav = out / f'{mode}_{i}.wav'
            try:
                raw.write_bytes(get(p['url'], timeout=180))
                subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', str(raw), '-t', '120', '-vn', '-ac', '1',
                                '-ar', '44100', '-c:a', 'pcm_s16le', str(wav)], check=True)
                files.append(wav.name); p['file'] = wav.name
            except Exception as ex:
                print(f'mode {mode}: download/convert failed for {p["title"]}: {ex!r}', flush=True)
            finally:
                raw.unlink(missing_ok=True)
        chosen[mode] = [p for p in picks if 'file' in p]
        print(f'mode {mode}: ' + (', '.join(p['title'] for p in chosen[mode]) or 'no recording found'), flush=True)
    (out / 'sources.json').write_text(json.dumps(chosen, indent=1, ensure_ascii=False))


if __name__ == '__main__':
    main()
