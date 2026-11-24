"""Download generated horn variants, pick the cleanest per vehicle, write 44.1 kHz mono PCM assets.

Usage: python3 tools/prepare_horns.py URLFILE OUTDIR
URLFILE lines: "<vehicle index> <url>". Picks the variant with one steady, tonal honk.
"""
import subprocess, sys, pathlib, urllib.request, json
import numpy as np

SR = 44100

def load(url):
    raw = urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent': 'TeslaSound'}), timeout=60).read()
    out = subprocess.run(['ffmpeg', '-v', 'error', '-i', 'pipe:0', '-ac', '1', '-ar', str(SR), '-f', 'f32le', 'pipe:1'],
                         input=raw, capture_output=True, check=True).stdout
    return np.frombuffer(out, dtype=np.float32).astype(np.float64)

def analyse(x, mode):
    win = SR // 50
    n = len(x) // win
    if n < 10:
        return None
    env = np.sqrt((x[:n * win].reshape(n, win) ** 2).mean(axis=1))
    mx = env.max()
    if mx <= 0:
        return None
    active = env > 0.25 * mx
    # contiguous segments, bridging gaps shorter than 60 ms
    segs, start, gap = [], None, 0
    for i, a in enumerate(active):
        if a:
            if start is None: start = i
            gap = 0
        elif start is not None:
            gap += 1
            if gap > 3:
                segs.append((start, i - gap)); start = None; gap = 0
    if start is not None: segs.append((start, n - 1 - gap))
    if not segs:
        return None
    if mode == 18:   # alarm: repeating tones are expected, use the whole active span
        s0, s1 = segs[0][0], segs[-1][1]
    else:
        s0, s1 = max(segs, key=lambda s: s[1] - s[0])
    dur = (s1 - s0 + 1) / 50
    seg = x[s0 * win:(s1 + 1) * win]
    # spectral flatness 100 Hz - 6 kHz (low = tonal horn, high = noise)
    spec = np.abs(np.fft.rfft(seg * np.hanning(len(seg)))) ** 2
    f = np.fft.rfftfreq(len(seg), 1 / SR)
    band = spec[(f > 100) & (f < 6000)] + 1e-12
    flat = float(np.exp(np.log(band).mean()) / band.mean())
    stab = float(env[s0:s1 + 1].std() / (env[s0:s1 + 1].mean() + 1e-9))
    peak_hz = float(f[(f > 100) & (f < 6000)][np.argmax(band)])
    score = min(dur, 1.8) * (1 - min(flat * 4, 0.9)) / (1 + 2 * stab) * (1.0 if (len(segs) == 1 or mode == 18) else 0.55)
    return dict(score=score, dur=dur, flat=flat, stab=stab, segs=len(segs), peak_hz=peak_hz, s0=s0 * win, s1=(s1 + 1) * win)

def main():
    urls = {}
    for line in open(sys.argv[1]):
        if line.strip():
            m, u = line.split(maxsplit=1); urls.setdefault(int(m), []).append(u.strip())
    out = pathlib.Path(sys.argv[2]); out.mkdir(parents=True, exist_ok=True)
    report, preview = {}, []
    for mode in sorted(urls):
        best = None
        for k, u in enumerate(urls[mode]):
            try:
                x = load(u); a = analyse(x, mode)
            except Exception as ex:
                print(mode, k, 'failed', ex); continue
            if a is None: continue
            a['variant'] = k
            print(mode, k, {kk: round(v, 3) if isinstance(v, float) else v for kk, v in a.items() if kk not in ('s0', 's1')}, flush=True)
            if best is None or a['score'] > best[1]['score']:
                best = (x, a)
        if best is None:
            continue
        x, a = best
        s0 = max(0, a['s0'] - int(.03 * SR)); s1 = min(len(x), a['s1'] + int(.15 * SR))
        if mode == 18: s1 = min(s1, s0 + int(2.6 * SR))
        y = x[s0:s1].copy()
        y -= y.mean()
        y *= 0.89 / (np.abs(y).max() + 1e-9)
        fade = int(.005 * SR); y[:fade] *= np.linspace(0, 1, fade); y[-fade:] *= np.linspace(1, 0, fade)
        (out / f'{mode}.pcm').write_bytes((y * 32767).astype('<i2').tobytes())
        report[mode] = {k: v for k, v in a.items() if k not in ('s0', 's1')}
        preview += [y, np.zeros(int(.5 * SR))]
    (out / 'selection.json').write_text(json.dumps(report, indent=1))
    pv = (np.concatenate(preview) * 32767).astype('<i2').tobytes()
    import wave
    with wave.open('horn-preview.wav', 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pv)

if __name__ == '__main__':
    main()
