package de.teslasound.app;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.hardware.*;
import android.location.*;
import android.media.*;
import android.os.*;

public class SoundService extends Service implements SensorEventListener, LocationListener {
    static volatile boolean running=false, demo=false;
    static volatile float speed=0, acceleration=0, volume=.30f, demoSpeed=0;
    static volatile int profile=0, rpm=0, gear=1;
    static volatile boolean shifting=false, recorded=false;
    static volatile int demoShift=0;
    /** Horn button state, set by the activity while the button is held. */
    static volatile boolean horn=false;
    /** Automatic acceleration run to top speed (demo mode only). */
    static volatile boolean autoRun=false;
    private static volatile long lastFix=0;
    private volatile boolean audioRunning=false;
    private volatile AudioTrack track;
    private Thread audioThread;
    private SensorManager sensors;
    private LocationManager locations;
    private AudioManager audioManager;
    private AudioFocusRequest focus;
    private boolean routeReceiverRegistered;
    private final float[] rotation=new float[9];
    private boolean hasRotation=false;
    private float bearing=0, gpsAccel=0, sensorAccel=0;
    private boolean hasBearing=false;
    private float previousSpeed=0;
    private long previousFix=0, previousSensor=0;
    private final BroadcastReceiver noisy=new BroadcastReceiver(){
        public void onReceive(Context c,Intent i){stopSelf();}
    };
    static boolean gpsFresh(){return lastFix!=0 && SystemClock.elapsedRealtime()-lastFix<5000;}
    private static float clamp(float x,float a,float b){return Math.max(a,Math.min(b,x));}
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onCreate(){
        super.onCreate();
        sensors=(SensorManager)getSystemService(SENSOR_SERVICE);
        locations=(LocationManager)getSystemService(LOCATION_SERVICE);
        audioManager=(AudioManager)getSystemService(AUDIO_SERVICE);
        NotificationManager nm=getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("sound","TeslaSound",NotificationManager.IMPORTANCE_LOW));
        if(Build.VERSION.SDK_INT>=33)registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        routeReceiverRegistered=true;
    }
    @Override public int onStartCommand(Intent i,int flags,int id){
        if(i==null || "STOP".equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
        boolean newDemo=i.getBooleanExtra("demo",false);
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,SoundService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(this,"sound").setContentTitle("TeslaSound")
            .setContentText(newDemo?"Demo im Stand":"Fahr-Sound aktiv").setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(open).addAction(android.R.drawable.ic_media_pause,"Stopp",stop).setOngoing(true).build();
        try{
            if(Build.VERSION.SDK_INT>=29)startForeground(1,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK|(newDemo?0:ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION));
            else startForeground(1,n);
            locations.removeUpdates(this); sensors.unregisterListener(this);
            lastFix=0; previousFix=0; previousSensor=0; hasBearing=false; hasRotation=false; gpsAccel=0;sensorAccel=0; acceleration=0; speed=0;
            demo=newDemo;
            if(!demo){
                locations.requestLocationUpdates(LocationManager.GPS_PROVIDER,200,0,this,Looper.getMainLooper());
                Sensor r=sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
                Sensor a=sensors.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
                if(r!=null)sensors.registerListener(this,r,SensorManager.SENSOR_DELAY_GAME);
                if(a!=null)sensors.registerListener(this,a,SensorManager.SENSOR_DELAY_GAME);
            }
            if(!audioRunning){
                focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setOnAudioFocusChangeListener(change->{if(change<0)stopSelf();}).build();
                if(audioManager.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){stopSelf();return START_NOT_STICKY;}
                running=true; audioRunning=true;
                audioThread=new Thread(this::play,"TeslaSound-Audio");audioThread.start();
            }
        }catch(RuntimeException ex){android.util.Log.e("TeslaSound","Start failed",ex);stopSelf();}
        return START_NOT_STICKY;
    }
    @Override public void onLocationChanged(Location l){
        long t=l.getElapsedRealtimeNanos()/1000000L;
        if(!l.hasSpeed() || (l.hasSpeedAccuracy() && l.getSpeedAccuracyMetersPerSecond()>2.5f) || (l.hasAccuracy()&&l.getAccuracy()>35) || SystemClock.elapsedRealtime()-t>3000)return;
        float raw=clamp(l.getSpeed(),0,90);
        if(previousFix!=0 && t>previousFix && t-previousFix<4000){
            float a=clamp((raw-previousSpeed)/((t-previousFix)/1000f),-8,8);
            gpsAccel=.6f*gpsAccel+.4f*a;
            speed=.25f*speed+.75f*raw;
        }else{speed=raw;gpsAccel=0;}
        if(raw<.4f)speed=0;
        previousSpeed=raw;previousFix=t;lastFix=t;
        hasBearing=l.hasBearing()&&raw>2 && (!l.hasBearingAccuracy()||l.getBearingAccuracyDegrees()<35);
        if(hasBearing)bearing=(float)Math.toRadians(l.getBearing());
        if(!hasBearing || !hasRotation || previousSensor==0 || SystemClock.elapsedRealtimeNanos()-previousSensor>500000000L)acceleration=gpsAccel;
    }
    @Override public void onSensorChanged(SensorEvent e){
        if(e.sensor.getType()==Sensor.TYPE_ROTATION_VECTOR){SensorManager.getRotationMatrixFromVector(rotation,e.values);hasRotation=true;return;}
        if(e.sensor.getType()!=Sensor.TYPE_LINEAR_ACCELERATION||!hasRotation||!hasBearing||!gpsFresh())return;
        float east=rotation[0]*e.values[0]+rotation[1]*e.values[1]+rotation[2]*e.values[2];
        float north=rotation[3]*e.values[0]+rotation[4]*e.values[1]+rotation[5]*e.values[2];
        float forward=clamp((float)(east*Math.sin(bearing)+north*Math.cos(bearing)),-8,8);
        sensorAccel=.88f*sensorAccel+.12f*forward;
        acceleration=.75f*sensorAccel+.25f*gpsAccel;previousSensor=e.timestamp;
    }
    @Override public void onAccuracyChanged(Sensor s,int a){}
    @Override public void onProviderEnabled(String p){}
    @Override public void onProviderDisabled(String p){lastFix=0;}
    @Override public void onStatusChanged(String p,int s,Bundle b){}
    private void play(){
        final int rate=44100;
        AudioTrack out=null;
        try{
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
            int min=AudioTrack.getMinBufferSize(rate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
            if(min<=0)throw new IllegalStateException("Audio buffer unsupported");
            out=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(Math.max(min,2048)).setTransferMode(AudioTrack.MODE_STREAM).build();
            track=out;if(!audioRunning)return;out.play();
            short[] pcm=new short[256];
            EngineAudio engine=new EngineAudio(getAssets(),rate,new java.io.File(getFilesDir(),"engines"));
            Horn hornSound=new Horn(getAssets(),rate);
            AutoRun run=null;int runProfile=-1;
            double simSpeed=0,simAccel=0,previousDemo=0, demoDip=0;
            int lastDemoShift=demoShift;
            boolean wasValid=false, wasDemo=demo;
            while(audioRunning){
                boolean valid=demo||gpsFresh();
                double dt=pcm.length/(double)rate;
                if(wasDemo!=demo || (valid&&!wasValid))engine.reset();
                wasDemo=demo;wasValid=valid;
                if(demo&&autoRun){
                    if(run==null||runProfile!=profile){run=new AutoRun(profile);runProfile=profile;}
                    previousDemo=simSpeed;
                    double a=run.acceleration(engine,simSpeed,dt);
                    simSpeed+=a*dt;simAccel=a;speed=(float)simSpeed;
                    if(run.done()){autoRun=false;run=null;runProfile=-1;engine.forceGear(0);}
                }else if(demo){
                    if(run!=null){run=null;runProfile=-1;engine.forceGear(0);}
                    if(lastDemoShift!=demoShift){lastDemoShift=demoShift;demoDip=.48; }
                    previousDemo=simSpeed;
                    double target=demoSpeed;
                    if(demoDip>0){demoDip-=dt;target=Math.max(0,simSpeed-1.1);}
                    simSpeed+=Math.max(-3.5*dt,Math.min(4.0*dt,(target-simSpeed)*dt*2.6));
                    simAccel=.83*simAccel+.17*(simSpeed-previousDemo)/dt;
                    speed=(float)simSpeed;
                }else{autoRun=false;if(run!=null){run=null;runProfile=-1;engine.forceGear(0);}simSpeed=valid?speed:0;simAccel=valid?acceleration:0;lastDemoShift=demoShift;demoDip=0;}
                engine.render(pcm,profile,simSpeed,simAccel,valid?volume:0,dt);
                // The horn works even without GPS, like a real one.
                hornSound.mix(pcm,profile,horn,volume);
                rpm=(int)engine.rpm();gear=engine.gear();shifting=engine.shifting();recorded=engine.hasRecording();
                if(!audioRunning || out.write(pcm,0,pcm.length)<0)break;
            }
        }catch(RuntimeException e){android.util.Log.e("TeslaSound","Audio failed",e);}
        finally{if(out!=null){try{out.stop();}catch(RuntimeException ignored){}out.release();}track=null;if(audioRunning)stopSelf();}
    }
    @Override public void onTaskRemoved(Intent rootIntent){stopSelf();super.onTaskRemoved(rootIntent);}
    @Override public void onDestroy(){
        audioRunning=false;running=false;rpm=0;gear=1;shifting=false;recorded=false;autoRun=false;horn=false;
        if(locations!=null)locations.removeUpdates(this);
        if(sensors!=null)sensors.unregisterListener(this);
        if(routeReceiverRegistered)unregisterReceiver(noisy);
        AudioTrack t=track;if(t!=null){try{t.pause();t.flush();}catch(RuntimeException ignored){}}
        if(audioThread!=null){try{audioThread.join(500);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
        if(focus!=null)audioManager.abandonAudioFocusRequest(focus);
        stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();
    }
}
