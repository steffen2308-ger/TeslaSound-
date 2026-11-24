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
        new Spec(850,4700,130,.48,new double[]{305,183,125,90},new double[]{0,.25,.50,.75},new double[]{1,.83,1,.83},new double[]{168,375,825,1470},new double[]{.46,.41,.28,.19},.19,.18),
        // 5: GT3 flat six, 6: modern muscle V8, 7: Ferrari V12, 8: supercharged Hellcat
        new Spec(900,9000,240,.15,new double[]{355,245,182,143,115,94,79},new double[]{0,1.0/6,2.0/6,3.0/6,4.0/6,5.0/6},new double[]{1,.86,.96,.89,.99,.88},new double[]{145,315,720,1520},new double[]{.43,.49,.34,.23},.12,.08),
        new Spec(650,7400,210,.27,new double[]{365,248,179,132,102,79},new double[]{0,.125,.25,.375,.5,.625,.75,.875},new double[]{1,.35,.93,.58,.47,1,.43,.88},new double[]{69,135,268,540},new double[]{.78,.49,.30,.14},.13,.08),
        new Spec(850,8900,260,.13,new double[]{378,259,193,151,120,99,82},new double[]{0,1.0/12,2.0/12,3.0/12,4.0/12,5.0/12,6.0/12,7.0/12,8.0/12,9.0/12,10.0/12,11.0/12},new double[]{1,.94,.99,.97,.96,1,.95,.99,.96,1,.97,.95},new double[]{175,440,970,1920},new double[]{.26,.43,.47,.29},.10,.04),
        new Spec(720,6500,215,.19,new double[]{355,238,176,139,111,90,74,60},new double[]{0,.125,.25,.375,.5,.625,.75,.875},new double[]{1,.47,.92,.64,.55,1,.50,.89},new double[]{72,147,335,670},new double[]{.72,.45,.27,.16},.13,.09),
        new Spec(950,8500,245,0.18,new double[]{390,260,191,150,121,99,83},new double[]{0.0/12,1.0/12,2.0/12,3.0/12,4.0/12,5.0/12,6.0/12,7.0/12,8.0/12,9.0/12,10.0/12,11.0/12},new double[]{1,0.92,0.98,0.89,1,0.95,0.98,0.92,1,0.9,0.98,0.95},new double[]{205,520,1100,2400},new double[]{0.28,0.45,0.48,0.27},0.12,0.06),
        new Spec(700,6800,190,0.21,new double[]{320,217,159,123,99,81,67,55},new double[]{0.0/8,1.0/8,2.0/8,3.0/8,4.0/8,5.0/8,6.0/8,7.0/8},new double[]{1,0.52,0.94,0.62,0.58,1,0.55,0.91},new double[]{72,148,300,610},new double[]{0.72,0.43,0.23,0.12},0.12,0.06),
        new Spec(800,6700,180,0.16,new double[]{300,210,155,120,96,78,65},new double[]{0.0/16,1.0/16,2.0/16,3.0/16,4.0/16,5.0/16,6.0/16,7.0/16,8.0/16,9.0/16,10.0/16,11.0/16,12.0/16,13.0/16,14.0/16,15.0/16},new double[]{1,0.85,0.97,0.83,0.95,0.9,1,0.88,0.98,0.85,0.96,0.87,1,0.9,0.96,0.85},new double[]{65,155,360,780},new double[]{0.65,0.47,0.32,0.17},0.16,0.08),
        new Spec(750,7200,190,0.2,new double[]{355,240,177,137,108,88,72,60},new double[]{0.0/6,1.0/6,2.0/6,3.0/6,4.0/6,5.0/6},new double[]{1,0.95,0.99,0.91,0.98,0.93},new double[]{120,265,560,1260},new double[]{0.52,0.43,0.3,0.16},0.13,0.08),
        new Spec(1100,7500,220,0.12,new double[]{385,275,211,166,133,110},new double[]{0.0/8,1.0/8,2.0/8,3.0/8,4.0/8,5.0/8,6.0/8,7.0/8},new double[]{1,0.45,0.91,0.62,0.53,1,0.48,0.89},new double[]{83,182,410,880},new double[]{0.7,0.52,0.34,0.18},0.16,0.11),
        new Spec(950,9000,250,0.15,new double[]{390,270,205,162,133,109},new double[]{0.0/10,1.0/10,2.0/10,3.0/10,4.0/10,5.0/10,6.0/10,7.0/10,8.0/10,9.0/10},new double[]{1,0.95,0.98,0.93,0.99,0.94,1,0.95,0.97,0.94},new double[]{220,520,1150,2450},new double[]{0.3,0.44,0.52,0.32},0.08,0.04),
        new Spec(850,4300,100,0.46,new double[]{250,148,99,70},new double[]{0.0/4,1.0/4,2.0/4,3.0/4},new double[]{1,0.77,0.95,0.74},new double[]{100,228,510,1090},new double[]{0.58,0.47,0.32,0.2},0.21,0.2),
        new Spec(850,9500,260,0.14,new double[]{410,292,219,169,135,111,94,80},new double[]{0.0/12,1.0/12,2.0/12,3.0/12,4.0/12,5.0/12,6.0/12,7.0/12,8.0/12,9.0/12,10.0/12,11.0/12},new double[]{1,0.97,0.99,0.94,1,0.96,0.98,0.95,1,0.97,0.99,0.95},new double[]{180,465,1050,2200},new double[]{0.29,0.43,0.51,0.31},0.1,0.05)
    };
    private static final class Loop {
        final double base;final float[] data;double pos;
        Loop(double base,float[] data){this.base=base;this.data=data;}
        double next(double rpm){int i=(int)pos;int j=(i+1)%data.length;double x=data[i]+(data[j]-data[i])*(pos-i);pos+=Math.max(.48,Math.min(2.35,rpm/base));if(pos>=data.length)pos-=data.length;return x;}
    }
    private final int rate;
    private double turbo=0,turboPhase=0,turboAir=0,previousLoad=0;
    private final Gearbox gearbox=new Gearbox();
    private final List<List<Loop>> recordings=new ArrayList<>();
    private final float[][] pulse=new float[SPECS.length][];
    private final float[] ring=new float[2048];
    private final Random rand=new Random(13421);
    private int current=-1,cursor=0,fireIndex=0;
    private double cycle=0,amp=0,lowNoise=0,noiseHigh=0,dc=0,level=0,revFlutter=0,shiftSound=0,blowerPhase=0,blowerLoad=0,prevRpm=0;
    private boolean previousShift=false;
    EngineAudio(AssetManager assets,int rate, File imported){
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
            File folder=new File(imported,Integer.toString(k));
            File[] custom=folder.listFiles((f,name)->name.endsWith(".pcm"));
            if(custom!=null)for(File file:custom)try{
                double rpm=Double.parseDouble(file.getName().replace(".pcm",""));
                byte[] bytes=java.nio.file.Files.readAllBytes(file.toPath());
                ByteBuffer bb=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
                float[] samples=new float[bytes.length/2];for(int i=0;i<samples.length;i++)samples[i]=bb.getShort()/32768f;
                if(samples.length>2000){list.removeIf(l->l.base==rpm);list.add(new Loop(rpm,samples));}
            }catch(Exception ex){android.util.Log.w("TeslaSound","Custom recording failed",ex);}
            list.sort(Comparator.comparingDouble(l->l.base));
        }
    }
    void reset(){gearbox.reset();current=-1;cycle=0;fireIndex=0;Arrays.fill(ring,0);shiftSound=0;previousShift=false;prevRpm=0;blowerLoad=0;turbo=0;turboAir=0;previousLoad=0;}
    double rpm(){return gearbox.rpm;}
    int gear(){return gearbox.gear;}
    boolean shifting(){return gearbox.shiftLeft>0;}
    boolean hasRecording(){return current>=0&&!recordings.get(current).isEmpty();}
    void render(short[] out,int mode,double speed,double acceleration,double volume,double dt){
        mode=Math.max(0,Math.min(SPECS.length-1,mode));
        Spec p=SPECS[mode];
        if(current!=mode){gearbox.reset();current=mode;cycle=0;fireIndex=0;Arrays.fill(ring,0);previousShift=false;shiftSound=0;amp=0;prevRpm=0;blowerLoad=0;turbo=0;turboAir=0;previousLoad=0;}
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
        if(prevRpm<=0)prevRpm=rpm;
        double rpmStep=(rpm-prevRpm)/out.length;
        double runningRpm=prevRpm;
        prevRpm=rpm;
        double recWeight=lo==null?0:.82;
        if(lo!=null && hi!=null){double range=Math.max(lo.base/rpm,rpm/hi.base);recWeight*=Math.max(0,1-Math.max(0,range-1.5)*.75);}
        for(int i=0;i<out.length;i++){
            // Small combustion variability and crank-speed flutter; 720-degree engine cycle.
            double white=rand.nextDouble()*2-1;
            revFlutter=.9995*revFlutter+.0005*white;
            runningRpm+=rpmStep;
            double rateCycle=runningRpm/(120*rate);
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
            double x=(1-recWeight)*synth+recWeight*samples;
            if(mode==10||mode==11||mode==12){
                double wanted=shifting?0:load*Math.sqrt(Math.max(0,norm));
                turbo+=(wanted-turbo)*(wanted>turbo?.00014:.00035);
                turboPhase+=2*Math.PI*(900+turbo*3800)/rate;if(turboPhase>2*Math.PI)turboPhase-=2*Math.PI;
                turboAir+=.18*(white-turboAir);
                x+=turbo*turbo*(.10*Math.sin(turboPhase)+.14*(white-turboAir));
                if(shifting)x+=turbo*.16*(white-turboAir);
            }
            if(mode==8){
                // Roots-type blower: load-dependent narrow high-frequency whine, with sidebands.
                blowerLoad+=.0014*((shifting?0:load)-blowerLoad);
                blowerPhase+=2*Math.PI*(runningRpm/60*18.7)/rate;
                if(blowerPhase>2*Math.PI)blowerPhase-=2*Math.PI;
                x+=blowerLoad*blowerLoad*(.10+.18*norm)*(.78*Math.sin(blowerPhase)+.17*Math.sin(2*blowerPhase)+.05*Math.sin(3*blowerPhase));
            }
            // DC rejection and soft saturation, plus clutch/ignition cut and brief exhaust transient.
            dc+=.0014*(x-dc);x-=dc;
            amp+=.0019*(targetAmp-amp);
            double transientSound=shiftSound*(noiseHigh*.10+lowNoise*.23)*(mode==0||mode==1||mode>=5?.9:.4);
            shiftSound*=.9994;
            double y=(x*1.6*amp+transientSound*volume);
            y=y/(1+Math.abs(y)*.55);
            out[i]=(short)(Math.max(-.97,Math.min(.97,y))*32767);
        }
    }
}
