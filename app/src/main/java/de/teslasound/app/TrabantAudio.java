package de.teslasound.app;

import java.util.Arrays;
import java.util.Random;

/**
 * Procedural two-stroke parallel twin for the Trabant 601. Each cylinder fires once
 * per crank revolution; combustion pulses excite a feedback delay line that models
 * the tuned expansion chamber, the source of the characteristic "bratt". Cylinders
 * use slightly different kernels (asymmetric Y-pipe) so the engine reads as a real
 * twin rather than a doubled single. No sample loops, no continuous noise bed; all
 * sound comes from discrete firing events and their pipe reflections.
 */
final class TrabantAudio {
    private final int rate;
    private final double[] ring=new double[4096];
    private final double[][] kernelsA=new double[3][]; // cylinder A kernels
    private final double[][] kernelsB=new double[3][]; // cylinder B kernels (slightly shifted modes)
    private final Random random=new Random(601);

    // Expansion-chamber feedback delay, tuned near 140 Hz fundamental pipe mode.
    private final double[] pipe;
    private int pipeCursor=0;
    private double pipeDamp=0;

    // Combustion-noise burst state (gated to each firing, decays within ~15 ms).
    private double burstEnergy=0, burstFilter=0;

    private double firingPhase=1, intervalScale=1, volume=0, dc=0, lp=0;
    private double mixture=0.8;
    private int cursor=0, cylinder=0, consecutiveMiss=0;
    private int events=0;

    TrabantAudio(int rate){
        this.rate=rate;
        // Pipe fundamental around 140 Hz; a real Trabi exhaust is roughly 1.2 m.
        pipe=new double[Math.max(4,(int)(rate/140.0))];
        for(int l=0;l<3;l++){
            double load=l*.5;
            int n=(int)(rate*.030);
            kernelsA[l]=buildKernel(load,n,1.00,1.00);
            kernelsB[l]=buildKernel(load,n,0.955,0.92); // shorter branch: modes +4.5%, 8% quieter
        }
    }
    private double[] buildKernel(double load,int n,double freqScale,double levelScale){
        double[] h=new double[n];
        for(int i=0;i<n;i++){
            double t=i/(double)rate;
            // Sharper blowdown: faster rise, biexponential decay.
            double rise=1-Math.exp(-t*(3200+load*3800));
            double blow=rise*(1.25*Math.exp(-t*(210+load*130))-.52*Math.exp(-t*70));
            // Exhaust port/chamber modes, fixed in Hz (shifted per cylinder, not with RPM).
            double body=.42*mode(t,151*freqScale,95)
                       +.48*mode(t,455*freqScale,160)
                       +(.11+.09*load)*mode(t,1007*freqScale,350)
                       +(.028+.075*load)*mode(t,1615*freqScale,650);
            // Metallic port-edge tick coupled to combustion, not broadband hiss.
            double tick=(.05+.09*load)*mode(t,2380*freqScale,1200);
            double fade=t>.025?Math.max(0,(.030-t)/.005):1;
            h[i]=(blow+rise*(body+tick))*fade*levelScale;
        }
        return h;
    }
    private static double mode(double t,double hz,double decay){
        return Math.sin(2*Math.PI*hz*t)*Math.exp(-t*decay);
    }
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
    void reset(){
        Arrays.fill(ring,0);Arrays.fill(pipe,0);
        cursor=0;pipeCursor=0;pipeDamp=0;
        cylinder=0;consecutiveMiss=0;events=0;
        firingPhase=1;intervalScale=1;volume=0;dc=0;lp=0;
        burstEnergy=0;burstFilter=0;
        mixture=.8;random.setSeed(601);
    }
    int events(){return events;}
    double next(double rpm,double load,double outputVolume,boolean shifting,double shiftFraction){
        rpm=clamp(rpm,750,4700);load=clamp(load,0,1);
        double speedFactor=clamp((rpm-800)/2400,0,1);
        double step=2*rpm/(60.0*rate)*intervalScale;
        firingPhase+=step;
        if(firingPhase>=1){
            firingPhase-=1;events++;
            cylinder^=1;
            // Independent combustion fluctuations, reduced as load and RPM increase.
            double rough=(1-load)*(1-.75*speedFactor);
            intervalScale=1+(random.nextDouble()*2-1)*(.003+.027*rough);
            // Residual mixture affects only the next combustion; no repeating modulation.
            mixture=clamp(mixture+(random.nextDouble()-.5)*.30+.10*(.85-mixture),.45,1.20);
            double missProbability=(.015+.31*rough)*(shifting?1.6:1);
            boolean miss=consecutiveMiss<2&&random.nextDouble()<missProbability;
            if(miss)consecutiveMiss++;else consecutiveMiss=0;
            double force=(.58+.59*load)*(.88+.24*random.nextDouble());
            force*=miss?(.07+.16*random.nextDouble()):(.86+.17*mixture);
            if(shifting)force*=.30+.45*shiftFraction;
            double color=clamp(load*.68+speedFactor*.32,0,1)*2;
            int a=Math.min(1,(int)color);
            double b=color-a;
            double[][] set=(cylinder==0)?kernelsA:kernelsB;
            double[] h1=set[a],h2=set[a+1];
            // Fractional event timing avoids sample-grid jitter and hard edges.
            double fraction=clamp(firingPhase/Math.max(step,1e-8),0,1);
            for(int j=0;j<h1.length;j++){
                double x=(h1[j]+(h2[j]-h1[j])*b)*force;
                ring[(cursor+j)&4095]+=x*fraction;
                ring[(cursor+j+1)&4095]+=x*(1-fraction);
            }
            // Gate a short filtered-noise burst to this combustion event.
            burstEnergy=Math.min(1.0,burstEnergy+force*(.30+.35*load));
        }
        // Direct kernel output.
        double pressure=ring[cursor];ring[cursor]=0;cursor=(cursor+1)&4095;
        // Combustion noise burst (decays ~15 ms, never continuous).
        if(burstEnergy>1e-6){
            double noise=(random.nextDouble()*2-1)*burstEnergy;
            burstFilter+=.42*(noise-burstFilter);
            pressure+=burstFilter*.22;
            burstEnergy*=.9965;
        }
        // Expansion-chamber feedback delay with damping in the loop.
        // Higher load → hotter gas → slightly more HF damping (more muffled bark).
        double pipeOut=pipe[pipeCursor];
        pipeDamp+=(.35+.15*load)*(pipeOut-pipeDamp);
        double fb=.62*(.85*pipeDamp+.15*pipeOut);
        pipe[pipeCursor]=pressure+fb;
        pipeCursor++;if(pipeCursor>=pipe.length)pipeCursor=0;
        // Mix direct impulse + pipe reflections.
        double x=pressure+.55*pipeOut;
        // AC coupling at 32 Hz and a gently load-dependent exhaust bandwidth.
        dc+=.00455*(x-dc);
        x-=dc;
        lp+=(.26+.12*load)*(x-lp);
        volume+=.0018*(outputVolume-volume);
        // Avoid exaggerated high-speed level caused by increasingly overlapping pulses.
        double level=.80/(1+.70*speedFactor);
        double y=lp*level*volume;
        return outputVolume==0&&volume<.00001?0:y/(1+.50*Math.abs(y));
    }
}
