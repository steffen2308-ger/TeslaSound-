package de.teslasound.app;

import java.util.Arrays;
import java.util.Random;

/**
 * Procedural four-stroke engine model used for every profile except the Trabant.
 *
 * Sound is built only from discrete events, never from loops or a continuous noise bed:
 * - Each combustion emits a pressure pulse into the exhaust of its own cylinder bank.
 *   Every bank has its own pipe (damped feedback delay) with slightly different length.
 *   The bank order of the firing sequence therefore shapes the character: a crossplane
 *   V8 feeds its banks unevenly (the classic burble), a flatplane V8, V10 or V12
 *   alternates evenly (the scream), a boxer four feeds R-L-L-R (the Beetle beat).
 * - Fixed per-cylinder differences and cycle-to-cycle variation, strongest at idle.
 * - Event-gated extras: diesel knock, valvetrain ticks, intake roar under load,
 *   overrun pops after lifting off and on upshifts.
 * - Accessories with their own state: turbo whine and blow-off, roots blower,
 *   air-cooling fan, straight-cut gear whine, hybrid e-motor.
 */
final class EngineSynth {
    static final class Profile {
        final int[] banks;
        final double pipeFb, pipeMix, sharp, mufLo, mufHi, bright;
        final double rough, lope, knock, valve, intake, pops;
        final int turbos; final double turboHz, bov;
        final double blower, fan, hybrid, gearWhine;
        double rasp; // broadband exhaust flow noise per combustion (set from RASP)
        Profile(int[] banks,double pipeFb,double pipeMix,double sharp,double mufLo,double mufHi,double bright,
                double rough,double lope,double knock,double valve,double intake,double pops,
                int turbos,double turboHz,double bov,double blower,double fan,double hybrid,double gearWhine){
            this.banks=banks;this.pipeFb=pipeFb;this.pipeMix=pipeMix;this.sharp=sharp;this.mufLo=mufLo;this.mufHi=mufHi;this.bright=bright;
            this.rough=rough;this.lope=lope;this.knock=knock;this.valve=valve;this.intake=intake;this.pops=pops;
            this.turbos=turbos;this.turboHz=turboHz;this.bov=bov;this.blower=blower;this.fan=fan;this.hybrid=hybrid;this.gearWhine=gearWhine;
        }
        /** Copy with a different exhaust tone (muffler cutoffs, brightness, pulse sharpness). */
        Profile tone(double mufLo,double mufHi,double bright,double sharp,double rasp){
            Profile p=new Profile(banks,pipeFb,pipeMix,sharp,mufLo,mufHi,bright,rough,lope,knock,valve,intake,pops,turbos,turboHz,bov,blower,fan,hybrid,gearWhine);
            p.rasp=rasp;return p;
        }
    }

    // Bank sequence of the firing order (one entry per cylinder, evenly spaced over 720 degrees).
    private static final int[] FLAT8={0,1,0,1,0,1,0,1};
    private static final int[] CROSS8={0,1,0,0,1,0,1,1};   // e.g. Ford 1-5-4-2-6-3-7-8
    private static final int[] I4={0,0,0,0};
    private static final int[] ALT6={0,1,0,1,0,1};
    private static final int[] ALT10={0,1,0,1,0,1,0,1,0,1};
    private static final int[] ALT12={0,1,0,1,0,1,0,1,0,1,0,1};
    private static final int[] W16={0,1,2,3,0,1,2,3,0,1,2,3,0,1,2,3};
    private static final int[] BOXER4={0,1,1,0};             // 1-4-3-2, cylinders 1/2 right, 3/4 left

    static final Profile[] PROFILES={
        // Tone values (sharp, mufLo, mufHi, bright and RASP) tuned against Wikimedia Commons recordings, see analysis/report.md.
        //            banks   fb   mix  sharp  mufLo mufHi bright rough lope knock valve intake pops  tb  turboHz bov  blow fan hyb gear
        /* 0 F1 V8     */ new Profile(FLAT8, .45,.50,14400,2250,4750,.30,.30,.00,.00,.05,.90,.35,0,0,0,0,0,0,.05),
        /* 1 V8 klass. */ new Profile(CROSS8,.56,.62,5120,900,4600,.30,1.00,.90,.00,.10,.25,.25,0,0,0,0,0,0,0),
        /* 2 Diesel R4 */ new Profile(I4,    .40,.40,9600,375,4800,1.10,.50,.00,.90,.14,.10,.00,1,5200,0,0,0,0,0),
        /* 3 Benzin R4 */ new Profile(I4,    .45,.45,6400,550,2600,.30,.40,.00,.00,.12,.30,.00,0,0,0,0,0,0,0),
        /* 4 Trabant (own synthesizer, entry unused) */ new Profile(I4,.4,.4,3000,1000,2000,.5,.5,0,0,0,0,0,0,0,0,0,0,0,0),
        /* 5 GT3 Boxer6*/ new Profile(ALT6,  .50,.55,9600,4000,8680,.30,.35,.00,.00,.08,.90,.40,0,0,0,0,0,0,0),
        /* 6 Mustang   */ new Profile(CROSS8,.56,.62,5600,3300,3795,1.10,.70,.30,.00,.09,.40,.55,0,0,0,0,0,0,0),
        /* 7 F12 V12   */ new Profile(ALT12, .45,.48,10400,6600,16000,.30,.25,.00,.00,.06,.85,.30,0,0,0,0,0,0,0),
        /* 8 Hellcat   */ new Profile(CROSS8,.56,.62,5440,3000,8400,.30,.70,.35,.00,.09,.30,.50,0,0,0,1,0,0,0),
        /* 9 Veneno    */ new Profile(ALT12, .45,.48,10400,1200,7600,.30,.25,.00,.00,.06,.80,.60,0,0,0,0,0,0,0),
        /*10 RS6       */ new Profile(CROSS8,.50,.55,5600,800,7200,.30,.40,.10,.00,.07,.20,.60,2,4300,1,0,0,0,0),
        /*11 Chiron    */ new Profile(W16,   .45,.50,5600,2100,6000,.30,.25,.00,.00,.05,.30,.10,4,3600,1,0,0,0,0),
        /*12 M3        */ new Profile(ALT6,  .50,.55,7200,3900,12000,.30,.30,.00,.00,.08,.30,.70,2,5000,.4,0,0,0,0),
        /*13 AMG GT3   */ new Profile(CROSS8,.56,.60,7200,2800,8400,.30,.50,.25,.00,.08,.60,.80,0,0,0,0,0,0,.05),
        /*14 LFA V10   */ new Profile(ALT10, .45,.48,12000,1400,5740,.30,.30,.00,.00,.06,.90,.35,0,0,0,0,0,0,0),
        /*15 Käfer     */ new Profile(BOXER4,.50,.55,1500,2700,5400,.30,.80,.10,.00,.35,.15,.10,0,0,0,0,1,0,0),
        /*16 Revuelto  */ new Profile(ALT12, .45,.48,11200,1150,7600,.30,.25,.00,.00,.06,.80,.50,0,0,0,0,0,1,0),
        /*17 Stadtbus  */ new Profile(new int[]{0,0,0,0,0,0},.45,.45,6000,500,2500,.50,.40,.00,.70,.12,.15,.00,1,3800,0,0,0,0,0),
        /*18 Space Shuttle (RocketAudio, entry unused) */ new Profile(I4,.4,.4,3000,1000,2000,.5,.5,0,0,0,0,0,0,0,0,0,0,0,0),
    };

    // Exhaust flow noise per vehicle (tuned against reference recordings).
    private static final double[] RASP={.30,.35,.20,.35,0,.35,.35,.30,.20,.35,.35,.35,.35,.80,.30,.80,.55,.25,0};
    static{for(int i=0;i<PROFILES.length;i++)PROFILES[i].rasp=RASP[i];}
    /** Pneumatic brake hiss when coming to a stop (buses). */
    private static final boolean[] AIR_BRAKE=new boolean[PROFILES.length];
    /** Torque-converter automatic: shifts reduce combustion only mildly. */
    private static final boolean[] AUTOMATIC=new boolean[PROFILES.length];
    static{AIR_BRAKE[17]=true;AUTOMATIC[17]=true;}

    private static final int RING=4096, MASK=RING-1;
    private final int rate;
    private final Profile pr;
    private final EngineAudio.Spec spec;
    private final int cyl, nb;
    private final double[][][] kernels;   // [bank][load level][sample]
    private final double[][] rings;       // per-bank exhaust input
    private final double[][] pipes;       // per-bank pipe delay lines
    private final int[] pipeCursor, bankDelay;
    private final double[] pipeDamp, cylGain, raspEnv, raspLp;
    private final double[] mech=new double[RING];
    private final double[] knockK, valveK, crackK;
    private final Random random;
    private final long seed;
    private final boolean airBrake, automatic;
    private double speed=0, movedFor=0, air=0, airLow=0, airBand=0, airHp=0;
    private int cursor=0, firing=0;
    private double phase=1, interval=1, mixture=1, gain=1;
    private double lp1=0, lp2=0, dc=0;
    private double intakeEnv=0, intLow=0, intBand=0;
    private double crackEnv=0, crackLp=0;
    private double overrun=0, prevLoad=0; private boolean prevShift=false;
    private double spool=0, turboPhase[]=new double[4], bovEnv=0, bovLow=0, bovBand=0;
    private double blowerLoad=0, blowerPhase=0, fanPhase=0, fanAm=0, motorPhase=0, gearPhase=0;
    private double accessory=0;

    EngineSynth(int mode,EngineAudio.Spec spec,int rate){this(mode,spec,rate,PROFILES[mode]);}
    EngineSynth(int mode,EngineAudio.Spec spec,int rate,Profile profile){
        this.rate=rate;this.spec=spec;this.pr=profile;
        this.cyl=pr.banks.length;
        int maxBank=0;for(int b:pr.banks)maxBank=Math.max(maxBank,b);
        nb=maxBank+1;
        seed=7919L*(mode+1);random=new Random(seed);
        airBrake=AIR_BRAKE[mode];automatic=AUTOMATIC[mode];
        kernels=new double[nb][3][];rings=new double[nb][RING];pipes=new double[nb][];
        pipeCursor=new int[nb];bankDelay=new int[nb];pipeDamp=new double[nb];raspEnv=new double[nb];raspLp=new double[nb];
        double pipeHz=spec.resonances[0];
        for(int b=0;b<nb;b++){
            // Banks differ slightly in pipe length, header length and therefore mode tuning.
            double pipeScale=1+.06*b, fScale=b%2==0?1-.01*b:.975;
            pipes[b]=new double[Math.max(8,(int)Math.round(rate/(pipeHz*pipeScale)))];
            bankDelay[b]=(int)(rate*.0003*b);
            for(int l=0;l<3;l++)kernels[b][l]=buildKernel(l*.5,fScale);
        }
        // Fixed per-cylinder differences (manufacturing, intake runner, exhaust path).
        cylGain=new double[cyl];
        Random c=new Random(seed^0x5DEECE66DL);
        for(int i=0;i<cyl;i++)cylGain[i]=.92+.16*c.nextDouble();
        knockK=click(new double[]{2900,4100,5600},new double[]{1700,2200,2800},new double[]{1,.7,.45},.006);
        valveK=click(new double[]{3800,6200},new double[]{2600,3400},new double[]{1,.6},.004);
        crackK=click(new double[]{1800,3300,5200},new double[]{900,1400,2100},new double[]{1,.8,.6},.012);
        calibrate();
    }

    private double[] buildKernel(double load,double fScale){
        int n=(int)(rate*.022);
        double[] h=new double[n];
        double sharp=pr.sharp*(1+.6*load);
        for(int i=0;i<n;i++){
            double t=i/(double)rate;
            double rise=1-Math.exp(-t*sharp);
            // Exhaust blowdown pulse followed by a slower rarefaction.
            double blow=rise*(1.2*Math.exp(-t*(260+140*load))-.5*Math.exp(-t*80));
            double body=0;
            for(int j=0;j<spec.resonances.length;j++){
                double w=j<2?1:(.45+.55*load)*(.35+pr.bright);
                body+=spec.gains[j]*w*Math.sin(2*Math.PI*spec.resonances[j]*fScale*t)*Math.exp(-t*(70+j*95));
            }
            double fade=i>n-rate*.005?(n-i)/(rate*.005):1;
            h[i]=(.8*blow+rise*body)*fade;
        }
        return h;
    }
    private double[] click(double[] hz,double[] decay,double[] g,double len){
        double[] h=new double[(int)(rate*len)];
        for(int i=0;i<h.length;i++){
            double t=i/(double)rate,x=0;
            for(int j=0;j<hz.length;j++)x+=g[j]*Math.sin(2*Math.PI*hz[j]*t)*Math.exp(-t*decay[j]);
            h[i]=x*(1-Math.exp(-t*20000));
        }
        return h;
    }
    private static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
    private static double coef(double hz,int rate){return 1-Math.exp(-2*Math.PI*hz/rate);}

    /** Sets the gain so every profile reaches a similar loudest level across the rev range. */
    private void calibrate(){
        gain=1;
        double loudest=0;
        int n=(int)(rate*.35),skip=(int)(rate*.15);
        for(double pos:new double[]{.35,.65,.95}){
            reset();gain=1;
            double rpm=spec.idle+pos*(spec.redline-spec.idle),sum=0;
            for(int i=0;i<n;i++){double y=engine(rpm,.95,false);if(i>=skip)sum+=y*y;}
            loudest=Math.max(loudest,Math.sqrt(sum/(n-skip)));
        }
        gain=loudest>1e-6?.32/loudest:1;
        reset();
    }
    void reset(){
        for(double[] r:rings)Arrays.fill(r,0);
        for(double[] p:pipes)Arrays.fill(p,0);
        Arrays.fill(mech,0);Arrays.fill(pipeCursor,0);Arrays.fill(pipeDamp,0);Arrays.fill(raspEnv,0);Arrays.fill(raspLp,0);Arrays.fill(turboPhase,0);
        cursor=0;firing=0;phase=1;interval=1;mixture=1;lp1=lp2=dc=0;
        intakeEnv=intLow=intBand=0;crackEnv=crackLp=0;overrun=0;prevLoad=0;prevShift=false;
        spool=0;bovEnv=bovLow=bovBand=0;speed=0;movedFor=0;air=airLow=airBand=airHp=0;blowerLoad=blowerPhase=fanPhase=fanAm=motorPhase=gearPhase=0;accessory=0;
        random.setSeed(seed);
    }
    /** Accessory sound (turbo, blower, fan, e-motor, gears) of the last sample, unscaled by recordings. */
    double accessory(){return accessory;}
    /** Vehicle speed in m/s, set once per audio block; used for stop-dependent sounds. */
    void setSpeed(double metresPerSecond){
        if(airBrake){
            if(metresPerSecond>2.5)movedFor=Math.min(10,movedFor+.006);
            // Coming to a halt after driving: brake cylinders vent with a sharp "pssht".
            if(speed>=.4&&metresPerSecond<.4&&movedFor>.5){air=1;movedFor=0;}
        }
        speed=metresPerSecond;
    }

    double next(double rpm,double load,boolean shifting){
        double y=engine(rpm,load,shifting);
        accessory=accessories(rpm,load,shifting);
        return y;
    }

    private void addKernel(double[] ring,int offset,double[] h1,double[] h2,double b,double force,double frac){
        for(int j=0;j<h1.length;j++){
            double x=(h1[j]+(h2[j]-h1[j])*b)*force;
            ring[(cursor+offset+j)&MASK]+=x*frac;
            ring[(cursor+offset+j+1)&MASK]+=x*(1-frac);
        }
    }
    private void addClick(double[] h,int offset,double amp){
        for(int j=0;j<h.length;j++)mech[(cursor+offset+j)&MASK]+=h[j]*amp;
    }

    private double engine(double rpm,double load,boolean shifting){
        rpm=clamp(rpm,spec.idle*.8,spec.redline*1.02);load=clamp(load,0,1);
        double norm=clamp((rpm-spec.idle)/(spec.redline-spec.idle),0,1);
        double idleness=(1-load)*(1-norm);
        // Overrun: time since lifting off at elevated RPM.
        boolean lifted=load<.2&&norm>.3&&!shifting;
        overrun=lifted?overrun+1.0/rate:0;
        boolean shiftStart=shifting&&!prevShift;prevShift=shifting;

        double step=rpm*cyl/(120.0*rate)*interval;
        phase+=step;
        if(phase>=1){
            phase-=1;
            int k=firing;firing=(firing+1)%cyl;
            int bank=pr.banks[k];
            // Cycle-to-cycle variation: strongest at idle, nearly gone at high load.
            interval=1+(random.nextDouble()*2-1)*(.002+.018*pr.rough*idleness);
            mixture=clamp(mixture+(random.nextDouble()-.5)*.18*(.3+pr.rough*idleness)+.15*(1-mixture),.6,1.3);
            double force=cylGain[k]*(.35+.65*load)*mixture;
            // Cam overlap at idle: some cylinders burn weakly (lumpy idle).
            if(pr.lope>0&&(k%4==1||k%4==2))force*=1-pr.lope*.45*idleness*(.5+random.nextDouble());
            if(shifting)force*=automatic?.75:.18;
            if(lifted)force*=.55;
            double color=clamp(load*.65+norm*.35,0,1)*2;
            int a=Math.min(1,(int)color);double b=color-a;
            double frac=clamp(phase/Math.max(step,1e-9),0,1);
            addKernel(rings[bank],bankDelay[bank],kernels[bank][a],kernels[bank][a+1],b,force,frac);
            // Turbulent gas rushing out of the opened exhaust valve.
            raspEnv[bank]=Math.min(2,raspEnv[bank]+pr.rasp*force*(.4+.6*load));
            // Pressure-rise knock of compression ignition; loudest at idle and under load.
            if(pr.knock>0)addClick(knockK,0,pr.knock*.55*(.55+.45*load+.4*idleness)*(.8+.4*random.nextDouble()));
            // Valvetrain tick, offset from combustion, masked as load rises.
            if(pr.valve>0)addClick(valveK,(int)Math.min(3000,.37/Math.max(step,1e-9)),pr.valve*.18*(1-.6*load)*(.7+.6*random.nextDouble()));
            if(pr.intake>0&&!shifting)intakeEnv=Math.min(1.5,intakeEnv+pr.intake*force*load*(.15+norm));
            // Overrun pops: unburnt fuel igniting in the hot exhaust after lifting off or on upshifts.
            double popChance=0;
            if(lifted&&overrun<1.8)popChance=pr.pops*.09*(1-overrun/1.8)*norm;
            if(shiftStart&&random.nextDouble()<pr.pops*.8)popChance=1;
            if(popChance>0&&random.nextDouble()<popChance){
                double p=(1.0+1.2*random.nextDouble())*(.5+.5*pr.pops);
                int pb=random.nextInt(nb);
                addKernel(rings[pb],bankDelay[pb]+(int)(rate*.002),kernels[pb][2],kernels[pb][2],0,p,1);
                crackEnv=Math.min(1.2,crackEnv+.6*p);
                addClick(crackK,(int)(rate*.002),.35*p);
            }
        }
        // Per-bank exhaust: direct pulse plus reflections in its own pipe.
        double exhaust=0;
        double raspHp=coef(500,rate),raspDecay=Math.exp(-1/(rate*.004));
        for(int b=0;b<nb;b++){
            double pressure=rings[b][cursor];rings[b][cursor]=0;
            if(raspEnv[b]>1e-5){
                double noise=(random.nextDouble()*2-1)*raspEnv[b];
                raspLp[b]+=raspHp*(noise-raspLp[b]);
                pressure+=(noise-raspLp[b])*.6;
                raspEnv[b]*=raspDecay;
            }
            double[] pipe=pipes[b];
            double out=pipe[pipeCursor[b]];
            pipeDamp[b]+=(.30+.15*load)*(out-pipeDamp[b]);
            pipe[pipeCursor[b]]=pressure+pr.pipeFb*(.85*pipeDamp[b]+.15*out);
            if(++pipeCursor[b]>=pipe.length)pipeCursor[b]=0;
            exhaust+=pressure+pr.pipeMix*out;
        }
        exhaust/=Math.sqrt(nb);
        // Muffler: two-pole low-pass, opening up with load and RPM (sport valves).
        double fc=pr.mufLo+(pr.mufHi-pr.mufLo)*clamp(.6*load+.4*norm,0,1);
        double c=coef(fc,rate);
        lp1+=c*(exhaust-lp1);lp2+=c*(lp1-lp2);
        double x=lp2;
        double m=mech[cursor];mech[cursor]=0;
        x+=m;
        // Intake roar: noise excited by intake events only, band-pass rising with RPM.
        if(intakeEnv>1e-5){
            double f=2*Math.sin(Math.PI*Math.min(.45*rate,320+1100*norm)/rate);
            double noise=(random.nextDouble()*2-1)*intakeEnv;
            intLow+=f*intBand;double high=noise-intLow-.9*intBand;intBand+=f*high;
            x+=intBand*.11;
            intakeEnv*=Math.exp(-1/(rate*.012));
        }
        // Pop crackle: short bright noise burst.
        if(crackEnv>1e-5){
            double noise=(random.nextDouble()*2-1)*crackEnv;
            crackLp+=.55*(noise-crackLp);
            x+=(noise-crackLp)*.25;
            crackEnv*=Math.exp(-1/(rate*.006));
        }
        cursor=(cursor+1)&MASK;
        dc+=coef(20,rate)*(x-dc);
        return (x-dc)*gain;
    }

    private double accessories(double rpm,double load,boolean shifting){
        double norm=clamp((rpm-spec.idle)/(spec.redline-spec.idle),0,1);
        double out=0;
        if(pr.turbos>0){
            // Boost builds with load and RPM, with turbo lag; drops quickly on lift or shift.
            double target=shifting?0:clamp(load*1.2-.15,0,1)*Math.pow(norm,.6);
            double before=spool;
            spool+=(target-spool)*(target>spool?1.6:6.0)/rate;
            if(pr.bov>0&&before>.3&&target<before-.25&&bovEnv<.2)bovEnv=before*pr.bov;
            double whine=0;
            for(int t=0;t<pr.turbos;t++){
                double detune=1+(t==0?0:t==1?.013:t==2?-.011:.021);
                double f=pr.turboHz*(.35+.65*spool)*detune*(1+.002*(random.nextDouble()-.5));
                turboPhase[t]+=2*Math.PI*f/rate;if(turboPhase[t]>2*Math.PI)turboPhase[t]-=2*Math.PI;
                whine+=Math.sin(turboPhase[t])+.25*Math.sin(2*turboPhase[t]);
            }
            out+=whine/Math.sqrt(pr.turbos)*spool*spool*.045;
            if(bovEnv>1e-4){
                double f=2*Math.sin(Math.PI*2300/rate);
                double noise=(random.nextDouble()*2-1)*bovEnv;
                bovLow+=f*bovBand;double high=noise-bovLow-.7*bovBand;bovBand+=f*high;
                out+=bovBand*.10;
                bovEnv*=Math.exp(-1/(rate*.28));
            }
        }
        if(pr.blower>0){
            // Roots-type blower: narrow whine locked to crank speed, rising with load.
            blowerLoad+=.0014*((shifting?0:load)-blowerLoad)*44100.0/rate;
            blowerPhase+=2*Math.PI*(rpm/60*18.7)/rate;if(blowerPhase>2*Math.PI)blowerPhase-=2*Math.PI;
            out+=pr.blower*blowerLoad*blowerLoad*(.08+.14*norm)*(.78*Math.sin(blowerPhase)+.17*Math.sin(2*blowerPhase)+.05*Math.sin(3*blowerPhase));
        }
        if(pr.fan>0){
            // Belt-driven cooling fan of the air-cooled boxer.
            fanAm+=.002*((random.nextDouble()*2-1)-fanAm);
            fanPhase+=2*Math.PI*(rpm/60*1.8*14)/rate;if(fanPhase>2*Math.PI)fanPhase-=2*Math.PI;
            out+=pr.fan*(.010+.022*norm)*(1+3*fanAm)*(Math.sin(fanPhase)+.4*Math.sin(2*fanPhase));
        }
        if(pr.hybrid>0){
            // Electric motor and inverter whine, mainly audible at low engine speed.
            motorPhase+=2*Math.PI*(rpm/60*11)/rate;if(motorPhase>2*Math.PI)motorPhase-=2*Math.PI;
            out+=pr.hybrid*.018*load*(1-.7*norm)*(Math.sin(motorPhase)+.3*Math.sin(3*motorPhase));
        }
        if(air>1e-4){
            // Air release: bright noise, starts with a hard edge and fades over about a second.
            double f=2*Math.sin(Math.PI*3800/rate);
            double noise=(random.nextDouble()*2-1)*air;
            airLow+=f*airBand;double high=noise-airLow-.8*airBand;airBand+=f*high;
            airHp+=.3*(noise-airHp);
            out+=airBand*.10+(noise-airHp)*.05*air;
            air*=Math.exp(-1/(rate*.45));
        }
        if(pr.gearWhine>0){
            // Straight-cut race gearbox.
            gearPhase+=2*Math.PI*(rpm/60*7.3)/rate;if(gearPhase>2*Math.PI)gearPhase-=2*Math.PI;
            out+=pr.gearWhine*(.3+.7*load)*norm*Math.sin(gearPhase);
        }
        return out;
    }
}
