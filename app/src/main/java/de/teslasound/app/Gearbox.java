package de.teslasound.app;

/**
 * Virtual gearbox; all speeds are m/s, accelerations m/s².
 *
 * Upshifts happen where a driver would shift: early when cruising or accelerating gently,
 * late only under hard acceleration. A shift is also recognised the moment acceleration
 * that had been building suddenly falls away, as when the clutch is pressed in a real
 * car, provided the engine is in a sensible rev range for an upshift.
 */
final class Gearbox {
    int gear=1;
    double rpm, load, shiftLeft, clock, lastShift=-10, smoothSpeed;
    double accFast, accSlow, pullTime;
    double duration=.3, fromRpm;
    /** When above zero (automatic acceleration run), the gearbox selects exactly this gear. */
    int forcedGear=0;
    boolean initialized;
    private static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
    void reset(){gear=1;rpm=0;load=0;shiftLeft=0;clock=0;lastShift=-10;smoothSpeed=0;accFast=0;accSlow=0;pullTime=0;initialized=false;}

    /** Upshift point as share of the rev range above idle, depending on how hard the car accelerates. */
    static double upshiftShare(double accel){return clamp(.18+.12*accel,.18,.92);}

    void update(EngineAudio.Spec p,double v,double accel,double dt){
        clock+=dt;
        double range=p.redline-p.idle;
        if(!initialized){
            smoothSpeed=v;rpm=p.idle;initialized=true;
            while(gear<p.ratios.length && v*p.ratios[gear-1]>p.idle+upshiftShare(0)*range && v*p.ratios[gear]>p.idle*1.1)gear++;
        }
        smoothSpeed+=(v-smoothSpeed)*Math.min(1,dt*12);
        accFast+=(accel-accFast)*Math.min(1,dt/.12);
        accSlow+=(accel-accSlow)*Math.min(1,dt/.8);
        // How long the driver has been pulling; fades out slowly so a sudden drop can still be seen.
        pullTime=accFast>.5?pullTime+dt:Math.max(0,pullTime-1.5*dt);
        double targetLoad=clamp(.24+accel*.24+(smoothSpeed>3?.12:0),.06,1);
        load+=(targetLoad-load)*Math.min(1,dt*(targetLoad>load?7:11));

        if(shiftLeft>0)shiftLeft=Math.max(0,shiftLeft-dt);
        boolean ready=clock-lastShift>.9 && shiftLeft==0;
        // Upshifts need a little more spacing so one clutch event never produces two shifts.
        boolean readyUp=clock-lastShift>1.5 && shiftLeft==0;
        double shaft=smoothSpeed*p.ratios[gear-1];
        boolean canUp=gear<p.ratios.length;
        double nextRpm=canUp?smoothSpeed*p.ratios[gear]:0;
        // Clutch in: acceleration that had built up suddenly falls away.
        boolean dropout=pullTime>.5 && accSlow>.55 && accFast<Math.max(.25,.4*accSlow);

        if(v<.7){gear=1;}
        else if(forcedGear>0){
            int want=Math.max(1,Math.min(p.ratios.length,forcedGear));
            if(want!=gear && shiftLeft==0 && clock-lastShift>.25)shift(p,want);
        }
        else if(readyUp && canUp && dropout && shaft>p.idle+.20*range && nextRpm>p.idle*1.05){
            shift(p,gear+1);pullTime=0;
        }else if(readyUp && canUp && accel>-.3 && shaft>p.idle+upshiftShare(Math.max(0,accFast))*range && nextRpm>p.idle*1.1){
            shift(p,gear+1);
        }else if(ready && gear>1){
            double downRpm=smoothSpeed*p.ratios[gear-2];
            boolean lugging=shaft<p.idle*1.15;
            boolean kickdown=accFast>2.0 && shaft<p.idle+.35*range && downRpm<p.idle+.80*range;
            boolean braking=accFast<-1.0 && downRpm<p.idle+.45*range;
            if((lugging||kickdown||braking) && downRpm<p.redline*.92)shift(p,gear-1);
        }
        // The clutch (or torque converter) only slips while pulling away: the engine is held at a
        // launch RPM until the gearbox side catches up, then RPM follows speed × ratio exactly.
        double launch=p.idle+Math.max(0,accel)*p.slip;
        double target=clamp(Math.max(smoothSpeed*p.ratios[gear-1],launch),p.idle,p.redline);
        if(shiftLeft>0){
            double progress=1-shiftLeft/duration;
            target=fromRpm+(target-fromRpm)*clamp(progress*2.2,0,1);
        }
        // During a shift RPM follows the new gear as fast as the gearbox changes.
        rpm+=(target-rpm)*Math.min(1,dt*(shiftLeft>0?Math.max(17,3/duration):8));
    }
    private void shift(EngineAudio.Spec p,int next){fromRpm=rpm;gear=next;duration=p.shift;shiftLeft=duration;lastShift=clock;}
}
