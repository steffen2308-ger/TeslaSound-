package de.teslasound.app;

/**
 * Automatic acceleration run from the current speed to the vehicle's top speed.
 * Acceleration is traction-limited at low speed and power-limited (P/v) above that;
 * air resistance grows with v² and balances engine power exactly at top speed;
 * the gearbox shifts at the vehicle's typical RPM and, with a manual gearbox,
 * the car briefly stops pulling while the clutch is in.
 */
final class AutoRun {
    private final int profile;
    private double time, hold;
    private boolean done;
    AutoRun(int profile){this.profile=profile;}
    boolean done(){return done;}
    double acceleration(EngineAudio engine,double speed,double dt){
        double vmax=EngineAudio.VMAX_KMH[profile]/3.6, a0=EngineAudio.RUN_ACCEL[profile];
        double power=EngineAudio.RUN_POWER[profile];
        double a=power>0?Math.max(0,Math.min(a0,power/Math.max(speed,1))-power/(vmax*vmax*vmax)*speed*speed)
                        :a0*Math.max(0,1-Math.pow(Math.min(1,speed/vmax),2.5));
        if(engine.shifting())a*=EngineAudio.RUN_SEAMLESS[profile]?.6:.05;
        time+=dt;
        if(speed>=vmax*.97)hold+=dt;
        // Stay at top speed for a moment, then hand back to the demo slider.
        if(hold>3||time>(profile==EngineAudio.SHUTTLE?45:90))done=true;
        return a;
    }
}
