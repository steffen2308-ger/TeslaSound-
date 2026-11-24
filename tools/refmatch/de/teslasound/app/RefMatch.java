package de.teslasound.app;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/**
 * Compares the long-term spectrum (third-octave bands, level-normalised) of real recordings
 * with the synthesizer and searches exhaust tone parameters that bring them closer.
 * Usage: java de.teslasound.app.RefMatch MODE ref1.wav [ref2.wav ...]   -> one JSON line on stdout
 */
public class RefMatch {
    static final int SR=44100, N=4096, HOP=2048;
    static final double[] CENTERS={50,63,80,100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000,2500,3150,4000,5000,6300,8000,10000};

    public static void main(String[] args)throws Exception{
        int mode=Integer.parseInt(args[0]);
        List<double[]> frames=new ArrayList<>();
        for(int i=1;i<args.length;i++)frames.addAll(loudFrames(bandFrames(readWav(args[i])),.55));
        double[] ref=normalise(mean(frames));
        EngineSynth.Profile cur=EngineSynth.PROFILES[mode];
        double[] baseCurve=synthCurve(mode,cur);
        double base=dist(ref,baseCurve);
        double bestD=base;EngineSynth.Profile best=cur;double[] bestCurve=baseCurve;
        double[] f={.5,.7,1,1.4,2},br={.3,.5,.7,.9,1.1};
        for(double a:f)for(double b:f)for(double c:br){
            double lo=cur.mufLo*a,hi=Math.min(16000,Math.max(lo*1.15,cur.mufHi*b));
            EngineSynth.Profile p=cur.tone(lo,hi,c,cur.sharp);
            double[] curve=synthCurve(mode,p);double d=dist(ref,curve);
            if(d<bestD){bestD=d;best=p;bestCurve=curve;}
        }
        EngineSynth.Profile stage=best;
        for(double s:new double[]{.6,.8,1.25,1.6}){
            EngineSynth.Profile p=stage.tone(stage.mufLo,stage.mufHi,stage.bright,cur.sharp*s);
            double[] curve=synthCurve(mode,p);double d=dist(ref,curve);
            if(d<bestD){bestD=d;best=p;bestCurve=curve;}
        }
        StringBuilder o=new StringBuilder();
        o.append("{\"mode\":").append(mode).append(",\"frames\":").append(frames.size())
         .append(String.format(Locale.ROOT,",\"base\":%.2f,\"best\":%.2f",base,bestD))
         .append(String.format(Locale.ROOT,",\"current\":{\"mufLo\":%.0f,\"mufHi\":%.0f,\"bright\":%.2f,\"sharp\":%.0f}",cur.mufLo,cur.mufHi,cur.bright,cur.sharp))
         .append(String.format(Locale.ROOT,",\"params\":{\"mufLo\":%.0f,\"mufHi\":%.0f,\"bright\":%.2f,\"sharp\":%.0f}",best.mufLo,best.mufHi,best.bright,best.sharp))
         .append(",\"bands\":").append(arr(CENTERS)).append(",\"ref\":").append(arr(ref))
         .append(",\"synthBase\":").append(arr(baseCurve)).append(",\"synthBest\":").append(arr(bestCurve)).append("}");
        System.out.println(o);
    }

    /** Idle, full-load sweep to near redline, part-load cruise and overrun: a typical recording mix. */
    static double[] synthCurve(int mode,EngineSynth.Profile p){
        EngineAudio.Spec spec=EngineAudio.SPECS[mode];
        EngineSynth s=new EngineSynth(mode,spec,SR,p);
        int n=SR*12;double[] y=new double[n];
        for(int i=0;i<n;i++){
            double t=i/(double)SR,rpm,load;
            if(t<1.5){rpm=spec.idle;load=.2;}
            else if(t<7.5){rpm=spec.idle+(t-1.5)/6*.95*(spec.redline-spec.idle);load=.95;}
            else if(t<9.5){rpm=spec.idle+.6*(spec.redline-spec.idle);load=.5;}
            else{rpm=spec.idle+(.8-(t-9.5)/2.5*.5)*(spec.redline-spec.idle);load=.08;}
            y[i]=s.next(rpm,load,false)+s.accessory();
        }
        return normalise(mean(loudFrames(bandFrames(y),.2)));
    }

    static double[] readWav(String path)throws IOException{
        ByteBuffer b=ByteBuffer.wrap(Files.readAllBytes(Paths.get(path))).order(ByteOrder.LITTLE_ENDIAN);
        int pos=12,dataStart=-1,dataLen=0;
        while(pos+8<=b.limit()){
            String id=new String(new byte[]{b.get(pos),b.get(pos+1),b.get(pos+2),b.get(pos+3)},"US-ASCII");
            int len=b.getInt(pos+4);
            if(id.equals("data")){dataStart=pos+8;dataLen=Math.min(len,b.limit()-dataStart);break;}
            pos+=8+len+(len&1);
        }
        if(dataStart<0)throw new IOException("no data chunk: "+path);
        double[] x=new double[dataLen/2];
        for(int i=0;i<x.length;i++)x[i]=b.getShort(dataStart+2*i)/32768.0;
        return x;
    }

    /** Power per third-octave band for each Hann-windowed frame; last element holds total power. */
    static List<double[]> bandFrames(double[] x){
        List<double[]> out=new ArrayList<>();
        double[] win=new double[N];for(int i=0;i<N;i++)win[i]=.5-.5*Math.cos(2*Math.PI*i/N);
        double[] re=new double[N],im=new double[N];
        for(int start=0;start+N<=x.length;start+=HOP){
            for(int i=0;i<N;i++){re[i]=x[start+i]*win[i];im[i]=0;}
            fft(re,im);
            double[] bands=new double[CENTERS.length+1];
            for(int k=1;k<N/2;k++){
                double f=k*(double)SR/N,p=re[k]*re[k]+im[k]*im[k];
                for(int j=0;j<CENTERS.length;j++){
                    if(f>=CENTERS[j]*Math.pow(2,-1/6.0)&&f<CENTERS[j]*Math.pow(2,1/6.0)){bands[j]+=p;bands[CENTERS.length]+=p;break;}
                }
            }
            out.add(bands);
        }
        return out;
    }
    /** Keeps frames above the given energy quantile: drops silence, ambience and gaps. */
    static List<double[]> loudFrames(List<double[]> frames,double quantile){
        if(frames.isEmpty())return frames;
        double[] e=new double[frames.size()];for(int i=0;i<e.length;i++)e[i]=frames.get(i)[CENTERS.length];
        double[] sorted=e.clone();Arrays.sort(sorted);
        double th=sorted[(int)Math.min(sorted.length-1,quantile*sorted.length)];
        List<double[]> out=new ArrayList<>();
        for(int i=0;i<e.length;i++)if(e[i]>=th&&e[i]>0){
            // Normalise each frame so loud passages do not dominate the average shape.
            double[] f=frames.get(i).clone();for(int j=0;j<f.length;j++)f[j]/=e[i];out.add(f);
        }
        return out;
    }
    static double[] mean(List<double[]> frames){
        double[] m=new double[CENTERS.length];
        for(double[] f:frames)for(int j=0;j<m.length;j++)m[j]+=f[j];
        for(int j=0;j<m.length;j++)m[j]/=Math.max(1,frames.size());
        return m;
    }
    static double[] normalise(double[] p){
        double[] db=new double[p.length];double avg=0;
        for(int j=0;j<p.length;j++){db[j]=10*Math.log10(p[j]+1e-12);avg+=db[j];}
        avg/=p.length;for(int j=0;j<p.length;j++)db[j]-=avg;
        return db;
    }
    static double dist(double[] a,double[] b){double s=0;for(int j=0;j<a.length;j++)s+=(a[j]-b[j])*(a[j]-b[j]);return Math.sqrt(s/a.length);}
    static String arr(double[] a){StringBuilder s=new StringBuilder("[");for(int i=0;i<a.length;i++){if(i>0)s.append(',');s.append(String.format(Locale.ROOT,"%.1f",a[i]));}return s.append(']').toString();}

    static void fft(double[] re,double[] im){
        int n=re.length;
        for(int i=1,j=0;i<n;i++){int bit=n>>1;for(;(j&bit)!=0;bit>>=1)j^=bit;j^=bit;if(i<j){double t=re[i];re[i]=re[j];re[j]=t;t=im[i];im[i]=im[j];im[j]=t;}}
        for(int len=2;len<=n;len<<=1){
            double ang=-2*Math.PI/len,wr=Math.cos(ang),wi=Math.sin(ang);
            for(int i=0;i<n;i+=len){double cr=1,ci=0;
                for(int k=0;k<len/2;k++){
                    int a=i+k,b=i+k+len/2;
                    double xr=re[b]*cr-im[b]*ci,xi=re[b]*ci+im[b]*cr;
                    re[b]=re[a]-xr;im[b]=im[a]-xi;re[a]+=xr;im[a]+=xi;
                    double t=cr*wr-ci*wi;ci=cr*wi+ci*wr;cr=t;
                }}
        }
    }
}
