package de.teslasound.app;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;

public class MainActivity extends Activity {
    private final Handler handler = new Handler();
    private TextView status;
    private boolean demoMode;
    private Spinner profile;
    private SeekBar volume, demoSpeed;
    private final Runnable refresh = new Runnable() {
        public void run() {
            if (!SoundService.running) status.setText("Bereit");
            else if (SoundService.demo) status.setText(String.format(java.util.Locale.GERMANY,"DEMO  |  %.0f km/h",SoundService.speed * 3.6f));
            else if (!SoundService.gpsFresh()) status.setText("Warte auf genaues GPS …");
            else status.setText(String.format(java.util.Locale.GERMANY,"%.0f km/h  |  %+.2f m/s²",SoundService.speed * 3.6f,SoundService.acceleration));
            handler.postDelayed(this, 250);
        }
    };
    private TextView label(String s, int size) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.WHITE); return t;
    }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32,64,32,32); root.setBackgroundColor(0xff15191f);
        ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(scroll);
        root.addView(label("TESLASOUND",32));
        root.addView(label("Virtueller Motorsound • Bluetooth",16));
        status=label("Bereit",25); status.setGravity(Gravity.CENTER); status.setPadding(0,45,0,45); root.addView(status);
        root.addView(label("Motorprofil",16));
        profile=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"V8 • tief","Sport • V6","Future • elektrisch"});
        profile.setAdapter(adapter); profile.setSelection(SoundService.profile);
        profile.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> p, android.view.View v,int i,long id){SoundService.profile=i;}
            public void onNothingSelected(AdapterView<?> p){}
        }); root.addView(profile);
        root.addView(label("Sound-Lautstärke",16)); volume=new SeekBar(this); volume.setMax(100); volume.setProgress((int)(SoundService.volume*100)); root.addView(volume);
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean u){SoundService.volume=p/100f;}
            public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
        });
        Button start=new Button(this); start.setText("FAHR-SOUND STARTEN"); root.addView(start); start.setOnClickListener(v->{demoMode=false; requestStart();});
        Button stop=new Button(this); stop.setText("STOPP"); root.addView(stop); stop.setOnClickListener(v->stopService(new Intent(this,SoundService.class)));
        Button demo=new Button(this); demo.setText("DEMO IM STAND STARTEN"); root.addView(demo); demo.setOnClickListener(v->{demoMode=true; requestStart();});
        root.addView(label("Demo-Geschwindigkeit (0–160 km/h)",15)); demoSpeed=new SeekBar(this); demoSpeed.setMax(160); demoSpeed.setProgress((int)(SoundService.demoSpeed*3.6f)); root.addView(demoSpeed);
        demoSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean u){SoundService.demoSpeed=p/3.6f;}
            public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
        });
        TextView note=label("Vor der Fahrt einstellen. Handy sicher befestigen. Tesla: Bluetooth als Medienquelle wählen. Bei fehlendem GPS bleibt der Fahr-Sound stumm. Nicht während der Fahrt bedienen.",14); note.setPadding(0,24,0,0); root.addView(note);
    }
    private void requestStart(){
        if (!demoMode && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},1); return;
        }
        if (Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},2); return;
        }
        startSound();
    }
    private void startSound(){
        Intent i=new Intent(this,SoundService.class); i.putExtra("demo",demoMode); startForegroundService(i);
    }
    @Override public void onRequestPermissionsResult(int req,String[] p,int[] g){
        super.onRequestPermissionsResult(req,p,g);
        if(req==1){
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)requestStart();
            else Toast.makeText(this,"Genauer Standort für den Fahr-Sound erforderlich.",Toast.LENGTH_LONG).show();
        } else if(req==2)startSound();
    }
    @Override protected void onResume(){super.onResume();handler.post(refresh);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
}
