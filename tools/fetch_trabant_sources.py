"""Collect only public audio links from the three licensed source pages; no login."""
import re, html, urllib.request, urllib.parse, pathlib, json, subprocess, concurrent.futures
from html.parser import HTMLParser
D=pathlib.Path('trabant_sources');D.mkdir(exist_ok=True)
pages=[('museum','https://www.soundsofchanges.eu/sound/trabant-601-tramp-engine-running/'),('cc0_01','https://freesound.org/people/blukotek/sounds/412819/'),('cc0_02','https://freesound.org/people/blukotek/sounds/412818/')]
def get(url):
    req=urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0 (compatible; TeslaSound audio research)','Accept':'*/*'})
    with urllib.request.urlopen(req,timeout=9) as f:return f.read(24000000),f.geturl()
class P(HTMLParser):
    def __init__(self):super().__init__();self.links=[]
    def handle_starttag(self,tag,attrs):
        self.links.extend(v for k,v in attrs if v and re.search(r'\.(?:wav|mp3|ogg)(?:[?#]|$)',v,re.I))
def process(item):
    name,page=item
    urls=[]
    for u in [page]+([f'https://freesound.org/embed/sound/iframe/{page.rstrip("/").split("/")[-1]}/simple/medium/'] if name.startswith('cc0') else []):
        try:
            raw,final=get(u);raw=html.unescape(raw.decode('utf8','replace')).replace('\\/','/').replace('\\u002F','/')
            p=P();p.feed(raw)
            hits=p.links+re.findall(r'(?:https?://|//)[^"<>\n\r\'\\]+?\.(?:wav|mp3|ogg)(?:\?[^"<>\s\']*)?',raw,re.I)
            for link in hits:
                link=urllib.parse.urljoin(final,link.strip())
                if name.startswith('cc0') and page.rstrip('/').split('/')[-1] not in link:continue
                if name=='museum' and not any(x in link.lower() for x in ('trab','tramp','601')):continue
                if link not in urls:urls.append(link)
            print(name,'candidate links',urls,flush=True)
        except Exception as ex:print(name,'page error',repr(ex),flush=True)
    for u in urls:
        try:
            raw,final=get(u);src=D/(name+'.raw');src.write_bytes(raw)
            dest=D/(name+'.wav')
            subprocess.run(['ffmpeg','-v','error','-y','-i',str(src),'-t','55','-ac','1','-ar','44100','-c:a','pcm_s16le',str(dest)],check=True,timeout=20)
            src.unlink(missing_ok=True)
            if dest.stat().st_size<44100:raise ValueError('short file')
            return {'name':name,'status':'recorded','source':final,'page':page}
        except Exception as ex:print(name,'download error',u,repr(ex),flush=True);(D/(name+'.raw')).unlink(missing_ok=True);(D/(name+'.wav')).unlink(missing_ok=True)
    return {'name':name,'status':'unavailable','page':page}
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as ex:results=list(ex.map(process,pages))
(D/'report.json').write_text(json.dumps(results,indent=2))
print(json.dumps(results))
