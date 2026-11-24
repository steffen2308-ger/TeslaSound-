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
        new Spec(800,4500,110,.48,new double[]{325,177,118,87},new double[]{0,.25,.50,.75},new double[]{1,.82,1,.82},new double[]{155,315,680,1450},new double[]{.46,.34,.16,.08},0,0),
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
        new Spec(850,9500,260,0.14,new double[]{410,292,219,169,135,111,94,80},new double[]{0.0/12,1.0/12,2.0/12,3.0/12,4.0/12,5.0/12,6.0/12,7.0/12,8.0/12,9.0/12,10.0/12,11.0/12},new double[]{1,0.97,0.99,0.94,1,0.96,0.98,0.95,1,0.97,0.99,0.95},new double[]{180,465,1050,2200},new double[]{0.29,0.43,0.51,0.31},0.1,0.05),
        // 17: city bus, 7.7 l inline-six turbodiesel with torque-converter automatic.
        new Spec(600,2300,150,0.50,new double[]{500,300,195,140,105,85},new double[]{0.0/6,1.0/6,2.0/6,3.0/6,4.0/6,5.0/6},new double[]{1,1,1,1,1,1},new double[]{55,120,300,700},new double[]{0.7,0.5,0.3,0.18},0.15,0.3),
        // 18: Space Shuttle, rendered by RocketAudio; single ratio so the gearbox never shifts.
        new Spec(1000,10000,0,0.30,new double[]{100},new double[]{0},new double[]{1},new double[]{100,200,400,800},new double[]{0.5,0.4,0.3,0.2},0,0)
    };
    private static final class Loop {
        final double base;final float[] data;double pos;
        double at(double phase){double f=phase*data.length;int i=Math.min(data.length-1,(int)f);return data[i]+(data[(i+1)%data.length]-data[i])*(f-i);}
        Loop(double base,float[] data){this.base=base;this.data=data;}
        double next(double rpm){return next(rpm,2.35);}
        double next(double rpm,double maxRatio){int i=(int)pos;int j=(i+1)%data.length;double x=data[i]+(data[j]-data[i])*(pos-i);pos+=Math.max(.48,Math.min(maxRatio,rpm/base));if(pos>=data.length)pos-=data.length;return x;}
    }
    private final int rate;
    private final TrabantAudio trabant;
    private final RocketAudio rocket;
    static final int BUS=17, SHUTTLE=18;
    private double turbo=0,turboPhase=0,turboAir=0,previousLoad=0;
    private final Gearbox gearbox=new Gearbox();
    private final List<List<Loop>> recordings=new ArrayList<>();
    private final List<Loop> trabiTables=new ArrayList<>();
    private final EngineSynth[] synths=new EngineSynth[SPECS.length];
    private final Random rand=new Random(13421);
    private int current=-1,cursor=0,fireIndex=0;
    private double cycle=0,amp=0,lowNoise=0,noiseHigh=0,dc=0,level=0,revFlutter=0,shiftSound=0,blowerPhase=0,blowerLoad=0,prevRpm=0;
    private boolean previousShift=false;
    private double trabiFilter=0,trabiFilter2=0,trabiCrank=0;
    private int trabiPulse=0;
    private double samplePhase=0;
    private Loop trabiActive=null,trabiPrevious=null;
    private double trabiFade=1;
    EngineAudio(AssetManager assets,int rate, File imported){
        this.rate=rate;
        trabant=new TrabantAudio(rate);
        rocket=new RocketAudio(rate);
        try{String[] files=assets.list("trabant_phase");if(files!=null)for(String name:files)if(name.endsWith(".pcm")){
            double rpm=Double.parseDouble(name.substring(0,name.length()-4));
            try(InputStream in=assets.open("trabant_phase/"+name)){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);ByteBuffer bb=ByteBuffer.wrap(b.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);float[] x=new float[bb.remaining()/2];for(int j=0;j<x.length;j++)x[j]=bb.getShort()/32768f;if(x.length==8192)trabiTables.add(new Loop(rpm,x));}
        }trabiTables.sort(Comparator.comparingDouble(l->l.base));}catch(Exception ex){android.util.Log.w("TeslaSound","Trabant phase tables unavailable",ex);}
        for(int k=0;k<SPECS.length;k++){
            List<Loop> list=new ArrayList<>();recordings.add(list);
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
    void reset(){trabant.reset();rocket.reset();for(EngineSynth e:synths)if(e!=null)e.reset();gearbox.reset();current=-1;shiftSound=0;previousShift=false;prevRpm=0;blowerLoad=0;turbo=0;turboAir=0;previousLoad=0;trabiFilter=0;trabiFilter2=0;trabiCrank=0;trabiPulse=0;samplePhase=0;dc=0;trabiActive=null;trabiPrevious=null;trabiFade=1;}
    /** Engine RPM, or thrust in percent for the Space Shuttle. */
    double rpm(){return current==SHUTTLE?rocket.thrustPercent():gearbox.rpm;}
    int gear(){return current==SHUTTLE?1:gearbox.gear;}
    boolean shifting(){return current!=SHUTTLE&&gearbox.shiftLeft>0;}
    boolean hasRecording(){return current>=0&&current!=4&&current!=SHUTTLE&&!recordings.get(current).isEmpty();}
    void render(short[] out,int mode,double speed,double acceleration,double volume,double dt){
        mode=Math.max(0,Math.min(SPECS.length-1,mode));
        Spec p=SPECS[mode];
        if(current!=mode){trabant.reset();rocket.reset();if(mode!=4&&mode!=SHUTTLE){if(synths[mode]==null)synths[mode]=new EngineSynth(mode,SPECS[mode],rate);else synths[mode].reset();}gearbox.reset();current=mode;previousShift=false;shiftSound=0;amp=0;prevRpm=0;blowerLoad=0;turbo=0;turboAir=0;previousLoad=0;trabiFilter=0;trabiFilter2=0;trabiCrank=0;trabiPulse=0;samplePhase=0;dc=0;trabiActive=null;trabiPrevious=null;trabiFade=1;}
        if(mode==SHUTTLE){
            rocket.control(speed,acceleration,dt);
            for(int i=0;i<out.length;i++)out[i]=(short)(Math.max(-.97,Math.min(.97,rocket.next(volume)))*32767);
            return;
        }
        gearbox.update(p,speed,acceleration,dt);
        boolean shifting=shifting();
        if(shifting&&!previousShift)shiftSound=1;
        previousShift=shifting;
        double rpm=gearbox.rpm,load=gearbox.load;
        double cut=shifting?(.10+.82*Math.pow(1-gearbox.shiftLeft/gearbox.duration,2)):1;
        // Torque-converter automatic: power is reduced, not cut, during a shift.
        if(mode==BUS&&shifting)cut=.55+.45*(1-gearbox.shiftLeft/gearbox.duration);
        double targetAmp=volume*(.20+.80*Math.sqrt(load))*cut;
        if(mode==4)targetAmp=volume*(.24+.76*load)*cut;
        if(mode==4){
            // Dedicated physical-style synthesizer deliberately bypasses ALL imported loops.
            double start=prevRpm>0?prevRpm:rpm;
            double step=(rpm-start)/out.length;
            double progress=shifting?1-gearbox.shiftLeft/gearbox.duration:1;
            for(int i=0;i<out.length;i++){
                start+=step;
                double y=trabant.next(start,load,targetAmp,shifting,progress);
                out[i]=(short)(Math.max(-.97,Math.min(.97,y))*32767);
            }
            prevRpm=rpm;
            return;
        }
        EngineSynth synth=synths[mode];
        synth.setSpeed(speed);
        List<Loop> layers=recordings.get(mode);
        Loop lo=null,hi=null;
        double mix=0;
        if(!layers.isEmpty()){
            lo=layers.get(0);hi=layers.get(layers.size()-1);
            for(Loop l:layers){if(l.base<=rpm)lo=l;if(l.base>=rpm){hi=l;break;}}
            if(lo!=hi)mix=Math.max(0,Math.min(1,Math.log(rpm/lo.base)/Math.log(hi.base/lo.base)));
        }
        double wa=Math.sqrt(1-mix),wb=Math.sqrt(mix);
        if(prevRpm<=0)prevRpm=rpm;
        double rpmStep=(rpm-prevRpm)/out.length;
        double runningRpm=prevRpm;
        prevRpm=rpm;
        // Imported recordings dominate within their RPM range; the synthesizer fills in beyond it.
        double recWeight=lo==null?0:.82;
        if(lo!=null && hi!=null){double range=Math.max(lo.base/rpm,rpm/hi.base);recWeight*=Math.max(0,1-Math.max(0,range-1.5)*.75);}
        for(int i=0;i<out.length;i++){
            runningRpm+=rpmStep;
            double engine=synth.next(runningRpm,load,shifting);
            double samples=0;
            if(lo!=null){samples=lo.next(runningRpm)*wa;if(hi!=lo)samples+=hi.next(runningRpm)*wb;}
            // Turbo, blower, fan etc. stay audible on top of imported recordings.
            double x=(1-recWeight)*engine+recWeight*samples+synth.accessory();
            dc+=.0014*(x-dc);x-=dc;
            amp+=.0019*(targetAmp-amp);
            double y=x*1.6*amp;
            if(volume==0 && amp<.00001)y=0;
            y=y/(1+Math.abs(y)*.55);
            out[i]=(short)(Math.max(-.97,Math.min(.97,y))*32767);
        }
    }
}
