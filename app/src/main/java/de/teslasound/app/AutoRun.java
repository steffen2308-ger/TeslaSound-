package de.teslasound.app;

/**
 * Automatic acceleration run from the current speed to the vehicle's top speed, driven
 * like a real driver would:
 * - upshift early, at the RPM where this car is normally shifted (RUN_SHIFT_RPM),
 * - stay in gear or shift down only when the next/current gear can no longer pull,
 *   so the engine only revs high on the way to top speed.
 * Engine power scales with RPM (roughly constant torque below the power peak);
 * launch acceleration is capped by traction; drag grows with v² and balances the
 * available power exactly at top speed. With a manual gearbox the car stops pulling
 * while the clutch is in.
 */
final class AutoRun {
    private final int profile;
    private final EngineAudio.Spec spec;
    private final double drag, vmax, a0, power;
    private double time, hold, sinceDownshift=99;
    private boolean done;

    AutoRun(int profile){
        this.profile=profile;spec=EngineAudio.SPECS[profile];
        vmax=EngineAudio.VMAX_KMH[profile]/3.6;a0=EngineAudio.RUN_ACCEL[profile];power=EngineAudio.RUN_POWER[profile];
        // Best power share any gear can deliver at top speed sets the drag that limits it there.
        double best=.2;
        for(double r:spec.ratios){double rpm=vmax*r;if(rpm<=spec.redline)best=Math.max(best,share(rpm));}
        drag=power*best/(vmax*vmax*vmax);
    }
    boolean done(){return done;}
    private double share(double rpm){return Math.max(.2,Math.min(1,rpm/(spec.redline*.92)));}
    /** Net acceleration in a gear at speed v. */
    private double net(double v,int gear){
        double rpm=Math.max(spec.idle,v*spec.ratios[gear-1]);
        if(rpm>spec.redline*1.01)return -1;
        return Math.min(a0,power*share(rpm)/Math.max(v,1))-drag*v*v;
    }

    double acceleration(EngineAudio engine,double speed,double dt){
        double a;
        if(power>0){
            int gear=Math.max(1,engine.gear()),want=gear;
            double rpm=engine.rpm();
            double here=net(speed,gear);
            sinceDownshift+=dt;
            if(gear<spec.ratios.length){
                double up=net(speed,gear+1);
                // Everyday upshift, unless the next gear could not pull; after a downshift for
                // more power, keep the gear until the engine nears the limiter (no hunting).
                if(rpm>=EngineAudio.RUN_SHIFT_RPM[profile]&&up>.15&&sinceDownshift>4)want=gear+1;
                else if(rpm>=spec.redline*.97)want=gear+1;
            }
            if(want==gear&&gear>1&&here<.15){
                double downRpm=speed*spec.ratios[gear-2];
                double down=net(speed,gear-1);
                // Shift down only into the engine's power band, and only if that clearly helps.
                if(downRpm<=spec.redline*.92&&down>here+.1){want=gear-1;sinceDownshift=0;}
            }
            engine.forceGear(want);
            a=Math.max(0,here);
            if(engine.shifting())a*=EngineAudio.RUN_SEAMLESS[profile]?.6:.05;
        }else a=a0*Math.max(0,1-Math.pow(Math.min(1,speed/vmax),2.5));
        time+=dt;
        if(speed>=vmax*.95)hold+=dt;
        // Stay at top speed for a moment, then hand back to the demo slider.
        if(hold>3||time>(profile==EngineAudio.SHUTTLE?45:120))done=true;
        return a;
    }
}
