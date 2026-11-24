package de.teslasound.app;

import java.util.Arrays;
import java.util.Random;

/**
 * Space Shuttle launch sound. A rocket has no combustion cycle, so unlike the piston engines
 * this model is built from turbulent jet noise:
 * - deep roar: several noise bands below ~400 Hz with slow turbulent amplitude drift,
 * - crackle: random shock-wave spikes (steep N-waves) whose rate and size grow with thrust,
 *   the characteristic tearing sound of large solid rocket boosters,
 * - ignition sequence: main engine start rumble, then booster ignition bang,
 * - ground reflections: a few long, low-passed echoes.
 * Driving maps to thrust: pulling away starts the launch, acceleration sets thrust,
 * standing still for a few seconds shuts the engines down.
 */
final class RocketAudio {
    private final int rate;
    private final Random random=new Random(1981);
    private double thrust=0, target=0, sequence=-1, still=0;
    private boolean launched=false, srb=false;
    // Roar filter states (state-variable band-passes) and turbulence drift.
    private final double[] low=new double[4], band=new double[4], drift=new double[4];
    private static final double[] ROAR_HZ={28,55,110,240}, ROAR_Q={.9,.8,.8,.9}, ROAR_GAIN={1.0,.95,.70,.40};
    private double hiss=0, hissLp=0;
    // Crackle: discrete shock events written into a ring, then softened slightly.
    private final double[] ring=new double[2048];
    private int cursor=0;
    private double crackLp=0;
    // Ignition bang envelope and echo delay line.
    private double bang=0, bangLow=0, bangBand=0;
    private final double[] echo;
    private int echoCursor=0;
    private double echoLp=0, dc=0, volume=0, pump=0;

    RocketAudio(int rate){this.rate=rate;echo=new double[(int)(rate*1.6)];}

    void reset(){
        thrust=target=0;sequence=-1;still=0;launched=srb=false;
        Arrays.fill(low,0);Arrays.fill(band,0);Arrays.fill(drift,0);Arrays.fill(ring,0);Arrays.fill(echo,0);
        hiss=hissLp=crackLp=bang=bangLow=bangBand=echoLp=dc=volume=pump=0;cursor=echoCursor=0;
        random.setSeed(1981);
    }
    /** Thrust in percent, shown instead of RPM. */
    int thrustPercent(){return (int)Math.round(thrust*100);}

    private static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}

    /** Called once per audio block with the current driving state. */
    void control(double speed,double acceleration,double dt){
        if(!launched){
            if(speed>.5||acceleration>.4){launched=true;sequence=0;}
        }else{
            still=speed<.3&&acceleration<.15?still+dt:0;
            if(still>4){launched=false;srb=false;sequence=-1;}
        }
        if(launched){
            sequence+=dt;
            // Main engines spool up for ~3 s, then the boosters light.
            if(sequence<3)target=.25*sequence/3;
            else{
                if(!srb){srb=true;bang=1;}
                target=clamp(.78+.09*acceleration,.55,1);
            }
        }else target=0;
    }

    double next(double outputVolume){
        thrust+=(target-thrust)*(target>thrust?(srb?3.0:1.2):0.6)/rate;
        volume+=.0018*(outputVolume-volume)*44100.0/rate;
        double t=thrust, x=0;
        // Turbulent roar.
        for(int k=0;k<4;k++){
            drift[k]+=(.6/rate)*((random.nextDouble()*2-1)*3-drift[k]);
            double f=2*Math.sin(Math.PI*ROAR_HZ[k]*(1+.15*drift[k])/rate);
            double n=random.nextDouble()*2-1;
            low[k]+=f*band[k];double high=n-low[k]-ROAR_Q[k]*band[k];band[k]+=f*high;
            double level=ROAR_GAIN[k]*(1+.35*drift[k])*(k<2?t:t*t);
            x+=band[k]*level*(k==0?1.6:1.0);
        }
        // Broadband jet hiss, mostly heard with the boosters burning.
        double n=random.nextDouble()*2-1;
        hissLp+=.18*(n-hissLp);
        x+=(hissLp-.5*hiss)*.18*t*t;hiss=hissLp;
        // Crackle: Poisson shock events, heavy-tailed amplitudes, only with boosters.
        double crackRate=srb?(60+420*t*t):0;
        if(random.nextDouble()<crackRate/rate){
            double amp=Math.pow(random.nextDouble(),-0.45)*.12*t;
            amp=Math.min(amp,1.2);
            int width=2+random.nextInt(Math.max(1,(int)(rate*.0006)));
            for(int j=0;j<width*3;j++){
                // Steep rise then slower negative phase: an N-wave.
                double v=j<width?1-j/(double)width:-(.55*Math.exp(-(j-width)/(double)width));
                ring[(cursor+j)&2047]+=amp*v;
            }
        }
        double c=ring[cursor];ring[cursor]=0;cursor=(cursor+1)&2047;
        crackLp+=.55*(c-crackLp);
        x+=crackLp;
        // Booster ignition: a short, very low thump.
        if(bang>1e-4){
            double f=2*Math.sin(Math.PI*45/rate);
            double nb=(random.nextDouble()*2-1)*bang;
            bangLow+=f*bangBand;double high=nb-bangLow-.6*bangBand;bangBand+=f*high;
            x+=bangBand*2.2;
            bang*=Math.exp(-1/(rate*.35));
        }
        // Turbopump whine during main engine start.
        if(launched&&!srb){pump+=2*Math.PI*(900+1700*thrust)/rate;if(pump>2*Math.PI)pump-=2*Math.PI;x+=.03*Math.sin(pump)*thrust*4;}
        // Ground reflections: low-passed echoes at 0.45 s, 0.9 s and 1.5 s.
        double e=.32*echo[(echoCursor+echo.length-(int)(rate*.45))%echo.length]
                +.20*echo[(echoCursor+echo.length-(int)(rate*.9))%echo.length]
                +.12*echo[(echoCursor+echo.length-(int)(rate*1.5))%echo.length];
        echoLp+=.08*(e-echoLp);
        echo[echoCursor]=x;echoCursor=(echoCursor+1)%echo.length;
        x+=echoLp;
        dc+=.0008*(x-dc);x-=dc;
        double y=x*2.8*volume;
        if(outputVolume==0&&volume<1e-5)return 0;
        return y/(1+.6*Math.abs(y));
    }
}
