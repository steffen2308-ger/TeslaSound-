"""Prepare RPM-tagged loop samples from explicitly CC0 Freesound recordings.
Public preview audio only, no authenticated download or third-party tracking.
The app has a procedural fallback when previews cannot be retrieved."""
import html, re, urllib.request, concurrent.futures, subprocess, tempfile, pathlib, json, math
import numpy as np
import urllib.parse
SOURCES=[(4,412818,'blukotek','two-stroke-trabant-engine-02',[800,1200,1800,2400,3200,4100],2)]
DIRECT={}
RATE=44100
DEST=pathlib.Path('app/src/main/assets/engines')
def fetch(url):
    r=urllib.request.Request(url,headers={'User-Agent':'TeslaSound/0.3 (https://github.com/steffen2308-ger/TeslaSound-; engine audio sample preparation)'})
    with urllib.request.urlopen(r,timeout=12) as f:return f.read(12000000)
def urls(sid,user,slug):
    page='https://www.soundsofchanges.eu/sound/trabant-601-tramp-engine-running/'
    try:
        raw=html.unescape(fetch(page).decode('utf-8','replace')).replace('\\/','/')
        candidates=re.findall(r'[^"\s<>]+?\.(?:wav|mp3)(?:\?[^"\s<>]*)?',raw,re.I)
        for u in dict.fromkeys(candidates):
            u=urllib.parse.urljoin(page,u)
            if 'soundsofchanges.eu' in urllib.parse.urlparse(u).netloc and ('trabant' in u.lower() or 'tramp' in u.lower()):
                yield u
    except Exception as ex:print('Museum recording unavailable',str(ex),flush=True)

    for page in [f'https://freesound.org/people/{user}/sounds/{sid}/',f'https://freesound.org/embed/sound/iframe/{sid}/simple/medium/',f'https://soundspool.com/sounds/{slug}']:
        try:
            raw=html.unescape(fetch(page).decode('utf-8','replace')).replace('\\/','/').replace('\\u002F','/')
            found=re.findall(r'(?:https?:)?//(?:cdn|www)\\.freesound\\.org/previews/[^"\\s<>]+?\\.(?:mp3|ogg)',raw,re.I)
            found += re.findall(r'https?://[^"\\s<>]+?'+str(sid)+r'[^"\\s<>]*?\\.(?:mp3|ogg)',raw,re.I)
            for u in dict.fromkeys(found):
                if str(sid)+'_' not in u and str(sid)+'-' not in u:continue
                yield 'https:'+u if u.startswith('//') else u
        except Exception as ex: print('Preview page unavailable',sid,str(ex),flush=True)
def prep(source):
    k,sid,user,slug,targets,fires=source
    for url in ([DIRECT[k]] if k in DIRECT else urls(sid,user,slug)):
        try:
            with tempfile.TemporaryDirectory() as d:
                p=pathlib.Path(d)/'source';p.write_bytes(fetch(url))
                data=subprocess.check_output(['ffmpeg','-v','error','-i',str(p),'-af','highpass=f=38,lowpass=f=4200','-ac','1','-ar',str(RATE),'-f','f32le','-'],timeout=25)
            x=np.frombuffer(data,dtype='<f4').astype(float)
            if len(x)<RATE*.7:continue
            source_url=url
            break
        except Exception as ex:print('Preview decode failed',sid,str(ex),flush=True)
    else:return {'profile':k,'id':sid,'status':'unavailable'}
    # Search stationary windows; prefer coherent firing frequencies and minimal drift.
    n=22050; window=np.hanning(n); bins=np.fft.rfftfreq(n,1/RATE)
    candidates=[]
    for start in range(0,len(x)-n,RATE//4):
        y=x[start:start+n].copy();y-=y.mean()
        rms=np.sqrt(np.mean(y*y))
        if rms<.006:continue
        spec=np.abs(np.fft.rfft(y*window)); total=np.sum(spec[(bins>25)&(bins<5000)])+1e-9
        for rpm in targets:
            f=rpm*fires/60
            band=np.where((bins>f*.78)&(bins<f*1.22))[0]
            if len(band)==0:continue
            ix=band[np.argmax(spec[band])];hz=bins[ix]
            if ix>0 and ix+1<len(spec):
                a,b,c=np.log(spec[ix-1:ix+2]+1e-9);den=a-2*b+c
                if abs(den)>1e-8:hz+=(.5*(a-c)/den)*(RATE/n)
            estimated=hz*60/fires
            lag=max(1,round(RATE/hz))
            corr=np.dot(y[:-lag],y[lag:])/(np.linalg.norm(y[:-lag])*np.linalg.norm(y[lag:])+1e-9)
            clarity=spec[ix]/total
            quality=max(0,corr)*.75+min(.35,clarity*2)-abs(math.log(estimated/rpm))*.25
            if corr>.16:candidates.append((rpm,quality,start,estimated,hz))
    dest=DEST/str(k);dest.mkdir(parents=True,exist_ok=True)
    chosen=[]
    for target in targets:
        opts=[c for c in candidates if c[0]==target]
        if not opts:continue
        _,quality,start,rpm,hz=max(opts,key=lambda c:c[1])
        if any(abs(math.log(rpm/r))<.12 for r in chosen):continue
        y=x[start:start+n].copy();y-=y.mean()
        # Seam crossfade across 25 ms, loop duration near an integer number of firing cycles.
        cross=round(RATE*.025)
        length=int(round((n-cross)*hz/RATE)*RATE/hz)
        length=min(n-cross,max(3000,length))
        y=y[:length+cross];fade=np.linspace(0,1,cross)
        z=y[cross:].copy();z[-cross:]=z[-cross:]*(1-fade)+y[:cross]*fade
        peak=np.max(np.abs(z))+1e-8;rms=np.sqrt(np.mean(z*z))+1e-8
        z*=min(.82/peak,.20/rms)
        (dest/(str(int(rpm))+'.pcm')).write_bytes((np.clip(z,-1,1)*32767).astype('<i2').tobytes());chosen.append(rpm)
    return {'profile':k,'id':sid,'status':'recorded' if chosen else 'no stable loops','rpm':[round(r) for r in chosen],'source':source_url}
if __name__=='__main__':
    DEST.mkdir(parents=True,exist_ok=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=1) as pool:
        results=list(pool.map(prep,SOURCES))
    (DEST/'sample-report.json').write_text(json.dumps(results,indent=2))
    print(json.dumps(results,indent=2),flush=True)
