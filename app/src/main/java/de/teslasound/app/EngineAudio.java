package de.teslasound.app;

import android.content.res.AssetManager;
import java.io.*;
import java.util.*;
import java.nio.*;

final class EngineAudio {
    static final class Spec {
        final double idle,redline,slip,shift;
        final double[] ratios, angles, strengths, resonances, gains;
        final double noise, metallic;
        Spec(double idle,double redline,double slip,double shift,double[] ratios,double[] angles,double[] strengths,double[] resonances,double[] gains,double noise,double metallic){
            this.idle=idle;this.redline=redline;this.slip=slip;this.shift=shift;this.ratios=ratios;this.angles=angles;this.strengths=strengths;this.resonances=resonances;this.gains=gains;this.noise=noise;this.metallic=metallic;
        }
    }
    static final Spec[] SPECS={
        // Classic high-revving F1 V8, crossplane V8, four-cylinder diesel, petrol I4, two-stroke twin.
        new Spec(5100,18000,430,.145,new double[]{675,505,400,327,277,237,205},new double[]{0,.125,.25,.375,.5,.625,.75,.875},new double[]{1,.93,1,.96,1,.93,1,.96},new double[]{260,630,1250,2500},new double[]{.18,.34,.36,.18},.10,.04),
        new Spec(720,6500,200,.32,new double[]{335,221,155,118,91,73},new double[]{0,.125,.25,.375,.5,.625,.75,.875},new double[]{1,.43,.91,.55,.53,1,.48,.87},new double[]{78,151,310,610},new double[]{.63,.40,.21,.12},.10,.08),
        new Spec(790,4500,105,.38,new double[]{245,155,110,82,65,52},new double[]{0,.25,.50,.75},new double[]{1,.91,1,.91},new double[]{91,245,720,1840},new double[]{.53,.31,.23,.25},.15,.35),
        new Spec(820,6600,165,.34,new double[]{322,208,150,112,87,72},new double[]{0,.25,.50,.75},new double[]{1,.95,.98,.94},new double[]{125,270,590,1050},new double[]{.51,.34,.26,.13},.10,.10),
        new Spec(850,4700,130,.48,new double[]{305,183,125,90},new double[]{0,.25,.50,.75},new double[]{1,.83,1,.83},new double[]{168,375,825,1470},new double[]{.46,.41,.28,.19},.19,.18)
    };
    private static final class Loop {
        final double base;final float[] data;double pos;
        Loop(double base,float[] data){this.base=base;this.data=data;}
        double next(double rpm){int i=(int)pos;int j=(i+1)%data.length;double x=data[i]+(data[j]-data[i])*(pos-i);pos+=Math.max(.48,Math.min(2.35,rpm/base));if(pos>=data.length)pos-=data.length;return x;}
    }
    private final int rate;
    private final Gearbox gearbox=new Gearbox();
    private final List<List<Loop>> recordings=new ArrayList<>();
    private final float[][] pulse=new float[SPECS.length][];
    private final float[] ring=new float[2048];
    private final Random rand=new Random(13421);
    private int current=-1,cursor=0,fireIndex=0;
    private double cycle=0,amp=0,lowNoise=0,noiseHigh=0,dc=0,level=0,revFlutter=0,shiftSound=0;
    private boolean previousShift=false;
    EngineAudio(AssetManager assets,int rate){
        this.rate=rate;
        for(int k=0;k<SPECS.length;k++){
            List<Loop> list=new ArrayList<>();recordings.add(list);
            Spec p=SPECS[k];float[] ir=new float[420];pulse[k]=ir;
            for(int i=0;i<ir.length;i++){
                double t=i/(double)rate,x=0;
                for(int j=0;j<p.resonances.length;j++){
                    double f=p.resonances[j],decay=65+j*65;
                    x+=p.gains[j]*Math.sin(2*Math.PI*f*t)*Math.exp(-t*decay);
                }
                ir[i]=(float)(x*(1-Math.exp(-t*2600))*.56);
            }
            try{
                String[] names=assets.list("engines/"+k);
                if(names!=null)for(String name:names)if(name.endsWith(".pcm")){
                    double base=Double.parseDouble(name.substring(0,name.length()-4));
                    ByteArrayOutputStream b=new ByteArrayOutputStream();
                    try(InputStream in=assets.open("engines/"+k+"/"+name)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);}
                    ByteBuffer bytes=ByteBuffer.wrap(b.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
                    float[] x=new float[bytes.remaining()/2];for(int i=0;i<x.length;i++)x[i]=bytes.getShort()/32768f;
                    if(x.length>2000)list.add(new Loop(base,x));
                }
                list.sort(Comparator.comparingDouble(l->l.base));
            }catch(Exception e){android.util.Log.w("TeslaSound","Recording unavailable for "+k,e);}
        }
    }
    void reset(){gearbox.reset();current=-1;cycle=0;fireIndex=0;Arrays.fill(ring,0);shiftSound=0;previousShift=false;}
    double rpm(){return gearbox.rpm;}
    int gear(){return gearbox.gear;}
    boolean shifting(){return gearbox.shiftLeft>0;}
    boolean hasRecording(){return current>=0&&!recordings.get(current).isEmpty();}
    void render(short[] out,int mode,double speed,double acceleration,double volume,double dt){
        mode=Math.max(0,Math.min(SPECS.length-1,mode));
        Spec p=SPECS[mode];
        if(current!=mode){gearbox.reset();current=mode;cycle=0;fireIndex=0;Arrays.fill(ring,0);previousShift=false;shiftSound=0;amp=0;}
        gearbox.update(p,speed,acceleration,dt);
        boolean shifting=shifting();
        if(shifting&&!previousShift)shiftSound=1;
        previousShift=shifting;
        double rpm=gearbox.rpm,load=gearbox.load;
        double cut=shifting?(.10+.82*Math.pow(1-gearbox.shiftLeft/gearbox.duration,2)):1;
        double targetAmp=volume*(.20+.80*Math.sqrt(load))*cut;
        List<Loop> layers=recordings.get(mode);
        Loop lo=null,hi=null;
        double mix=0;
        if(!layers.isEmpty()){
            lo=layers.get(0);hi=layers.get(layers.size()-1);
            for(Loop l:layers){if(l.base<=rpm)lo=l;if(l.base>=rpm){hi=l;break;}}
            if(lo!=hi)mix=Math.max(0,Math.min(1,Math.log(rpm/lo.base)/Math.log(hi.base/lo.base)));
        }
        double wa=Math.sqrt(1-mix),wb=Math.sqrt(mix);
        double norm=(rpm-p.idle)/(p.redline-p.idle);
        double rateCycle=rpm/(120*rate);
        for(int i=0;i<out.length;i++){
            // Small combustion variability and crank-speed flutter; 720-degree engine cycle.
            double white=rand.nextDouble()*2-1;
            revFlutter=.9995*revFlutter+.0005*white;
            double old=cycle;
            cycle+=rateCycle*(1+revFlutter*.16+(mode==4?.012:0)*Math.sin(2*Math.PI*old));
            if(cycle>=1){cycle-=1;fireIndex=0;}
            if(cycle<old || (fireIndex<p.angles.length && cycle>=p.angles[fireIndex])){
                if(fireIndex<p.angles.length){
                    double strength=p.strengths[fireIndex]*(.78+.44*rand.nextDouble())*(.52+.62*load);
                    if(mode==4 && rand.nextDouble()<.065 && load<.6)strength*=.15;
                    if(mode==2)strength*=1.10;
                    float[] h=pulse[mode];
                    for(int j=0;j<h.length;j++)ring[(cursor+j)&2047]+=h[j]*strength;
                    fireIndex++;
                }
            }
            double combustion=ring[cursor];ring[cursor]=0;cursor=(cursor+1)&2047;
            lowNoise+=.045*(white-lowNoise);noiseHigh+=.32*(white-noiseHigh);
            double texture=lowNoise*(p.noise+load*.14)+noiseHigh*p.metallic*(.19+.30*load);
            double synth=combustion*.85+texture*(.45+norm*.55);
            double samples=0;
            if(lo!=null){samples=lo.next(rpm)*wa;if(hi!=lo)samples+=hi.next(rpm)*wb;}
            double x=lo==null?synth:(.76*samples+.24*synth);
            // DC rejection and soft saturation, plus clutch/ignition cut and brief exhaust transient.
            dc+=.0014*(x-dc);x-=dc;
            amp+=.0019*(targetAmp-amp);
            double transientSound=shiftSound*(noiseHigh*.10+lowNoise*.23)*(mode==0||mode==1?.9:.4);
            shiftSound*=.9994;
            double y=(x*1.6*amp+transientSound*volume);
            y=y/(1+Math.abs(y)*.55);
            out[i]=(short)(Math.max(-.97,Math.min(.97,y))*32767);
        }
    }
}
