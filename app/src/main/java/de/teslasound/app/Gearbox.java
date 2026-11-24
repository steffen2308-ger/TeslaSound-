package de.teslasound.app;

/** Gearbox and short lift/re-acceleration detector; all speeds are m/s. */
final class Gearbox {
    int gear=1;
    double rpm, load, shiftLeft, clock, lastShift=-10, lastSpeed, smoothSpeed;
    double pullUntil=-1, dipAt=-1, peakSpeed, dipMinimum;
    double duration=.3, fromRpm;
    boolean initialized;
    private static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
    void reset(){gear=1;rpm=0;load=0;shiftLeft=0;clock=0;lastShift=-10;lastSpeed=0;smoothSpeed=0;pullUntil=-1;dipAt=-1;peakSpeed=0;dipMinimum=0;initialized=false;}
    void update(EngineAudio.Spec p,double v,double accel,double dt){
        clock+=dt;
        if(!initialized){lastSpeed=v;smoothSpeed=v;rpm=p.idle;initialized=true;while(gear<p.ratios.length && v*p.ratios[gear-1]>p.redline*.91)gear++;}
        smoothSpeed+=(v-smoothSpeed)*Math.min(1,dt*12);
        double dv=v-lastSpeed;lastSpeed=v;
        double shaft=smoothSpeed*p.ratios[gear-1];
        double targetLoad=clamp(.24+accel*.24+(smoothSpeed>3?.12:0),.06,1);
        load+=(targetLoad-load)*Math.min(1,dt*(targetLoad>load?7:11));
        // A recently accelerating vehicle briefly loses speed, then accelerates again.
        if(accel>.50 && dipAt<0){pullUntil=clock+1.2;peakSpeed=v;}
        if(dipAt<0 && clock<pullUntil && v>3.5 && (accel<-.35 || dv<-.16)){
            dipAt=clock;peakSpeed=Math.max(peakSpeed,v);dipMinimum=v;
        }
        boolean recovery=false;
        if(dipAt>=0){
            dipMinimum=Math.min(dipMinimum,v);
            double elapsed=clock-dipAt;
            if(elapsed>1.55 || peakSpeed-dipMinimum>3.0 || accel<-3.6){dipAt=-1;pullUntil=-1;}
            else if(elapsed>.11 && (accel>.35 || v-dipMinimum>.20) && (peakSpeed-dipMinimum>.10 || elapsed>.20)){
                recovery=true;dipAt=-1;pullUntil=-1;
            }
        }
        if(shiftLeft>0)shiftLeft=Math.max(0,shiftLeft-dt);
        boolean ready=clock-lastShift>1.05 && shiftLeft==0;
        if(v<.7){gear=1;dipAt=-1;pullUntil=-1;}
        else if(ready && recovery && shaft>p.idle*1.10 && gear<p.ratios.length){
            shift(p,gear+1);
        }else if(ready && gear<p.ratios.length && shaft>p.redline*(accel>1.1?.91:.78) && accel>-.25){
            shift(p,gear+1);
        }else if(ready && gear>1 && (shaft<p.idle*1.19 || (accel<-.5 && shaft<p.redline*.30)) && v*p.ratios[gear-2]<p.redline*.76){
            shift(p,gear-1);
        }
        double target=clamp(v*p.ratios[gear-1]+Math.max(0,accel)*p.slip,p.idle,p.redline);
        if(shiftLeft>0){
            double progress=1-shiftLeft/duration;
            target=fromRpm+(target-fromRpm)*clamp(progress*2.2,0,1);
        }
        rpm+=(target-rpm)*Math.min(1,dt*(shiftLeft>0?17:8));
    }
    private void shift(EngineAudio.Spec p,int next){fromRpm=rpm;gear=next;duration=p.shift;shiftLeft=duration;lastShift=clock;}
}
