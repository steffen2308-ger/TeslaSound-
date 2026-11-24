package de.teslasound.app;

import java.util.Arrays;
import java.util.Random;

/**
 * Procedural two-stroke twin, no samples, no looping waveform and no continuous noise.
 * Fixed acoustic modes are informed by spectral peaks in the reference exhaust recording,
 * not by copied waveform data. Each cylinder fires once per crank revolution.
 */
final class TrabantAudio {
    private final int rate;
    private final double[] ring=new double[4096];
    private final double[][] kernels=new double[3][];
    private final Random random=new Random(601);
    private double firingPhase=1, intervalScale=1, volume=0, dc=0, lp=0;
    private double mixture=0.8;
    private int cursor=0, cylinder=0, consecutiveMiss=0;
    private int events=0;
    TrabantAudio(int rate){
        this.rate=rate;
        for(int l=0;l<3;l++){
            double load=l*.5;
            double[] h=new double[(int)(rate*.038)];
            for(int i=0;i<h.length;i++){
                double t=i/(double)rate;
                // Asymmetric pressure release: sharp blowdown, slower rarefaction.
                double rise=1-Math.exp(-t*(2200+load*3000));
                double blow=rise*(1.15*Math.exp(-t*(190+load*120))-.48*Math.exp(-t*65));
                // Exhaust chamber/pipe modes, fixed in Hz rather than shifted with RPM.
                double body=.39*mode(t,151,90)+.46*mode(t,455,155)
                    +(.105+.085*load)*mode(t,1007,340)
                    +(.025+.07*load)*mode(t,1615,630);
                // Short metallic mechanical response coupled to combustion, not broadband hiss.
                double tick=(.045+.08*load)*mode(t,2380,1150);
                double fade=t>.033?Math.max(0,(.038-t)/.005):1;
                h[i]=(blow+rise*(body+tick))*fade;
            }
            // Normalize relative to the original impulse scale, not each load block.
            kernels[l]=h;
        }
    }
    private static double mode(double t,double hz,double decay){
        return Math.sin(2*Math.PI*hz*t)*Math.exp(-t*decay);
    }
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
    void reset(){
        Arrays.fill(ring,0);cursor=0;cylinder=0;consecutiveMiss=0;events=0;
        firingPhase=1;intervalScale=1;volume=0;dc=0;lp=0;mixture=.8;random.setSeed(601);
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
            double force=(.58+.59*load)*(.88+.24*random.nextDouble())*(cylinder==0?1:.87);
            force*=miss?(.07+.16*random.nextDouble()):(.86+.17*mixture);
            if(shifting)force*=.30+.45*shiftFraction;
            double color=clamp(load*.68+speedFactor*.32,0,1)*2;
            int a=Math.min(1,(int)color);
            double b=color-a;
            double[] h1=kernels[a],h2=kernels[a+1];
            // Fractional event timing avoids sample-grid jitter and hard edges.
            double fraction=clamp(firingPhase/Math.max(step,1e-8),0,1);
            for(int j=0;j<h1.length;j++){
                double x=(h1[j]+(h2[j]-h1[j])*b)*force;
                ring[(cursor+j)&4095]+=x*fraction;
                ring[(cursor+j+1)&4095]+=x*(1-fraction);
            }
        }
        double pressure=ring[cursor];ring[cursor]=0;cursor=(cursor+1)&4095;
        // AC coupling at 32 Hz and a gently load-dependent exhaust bandwidth.
        dc+=.00455*(pressure-dc);
        double x=pressure-dc;
        lp+=(.24+.12*load)*(x-lp);
        volume+=.0018*(outputVolume-volume);
        // Avoid exaggerated high-speed level caused by increasingly overlapping pulses.
        double level=1.14/(1+.30*speedFactor);
        double y=lp*level*volume;
        return outputVolume==0&&volume<.00001?0:y/(1+.50*Math.abs(y));
    }
}
