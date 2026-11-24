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
    private TextView status, engineStatus;
    private boolean demoMode;
    private TextView selectedCar;
    private VehicleCard[] cards;
    private int importProfile;
    private SeekBar volume, demoSpeed;
    private final Runnable refresh = new Runnable() {
        public void run() {
            if (!SoundService.running) status.setText("Bereit");
            else if (SoundService.demo) status.setText(String.format(java.util.Locale.GERMANY,"DEMO  |  %.0f km/h",SoundService.speed * 3.6f));
            else if (!SoundService.gpsFresh()) status.setText("Warte auf genaues GPS …");
            else status.setText(String.format(java.util.Locale.GERMANY,"%.0f km/h  |  %+.2f m/s²",SoundService.speed * 3.6f,SoundService.acceleration));
            engineStatus.setText(!SoundService.running?"Motor aus":SoundService.profile==EngineAudio.SHUTTLE?String.format(java.util.Locale.GERMANY,"Schub %d %%\n%s",SoundService.rpm,SoundService.rpm==0?"Startbereit – losfahren zum Zünden":"Raketensimulation"):String.format(java.util.Locale.GERMANY,"%d U/min   •   Gang %d%s\n%s",SoundService.rpm,SoundService.gear,SoundService.shifting?"  •  SCHALTEN":"",SoundService.recorded?"Aufnahme + Motorsimulation":"Motorsimulation"));
            handler.postDelayed(this, 150);
        }
    };
    private TextView label(String s, int size) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.WHITE); return t;
    }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12),dp(16),dp(12),dp(16)); root.setBackgroundColor(0xff15191f);
        ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(scroll);
        scroll.setClipToPadding(false);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        scroll.requestApplyInsets();
        root.setBackgroundColor(0xff10151b);
        root.addView(label("TESLASOUND",28));
        root.addView(label("Virtueller Motorsound • Bluetooth",16));
        status=label("Bereit",25); status.setGravity(Gravity.CENTER); status.setPadding(0,dp(12),0,dp(8)); root.addView(status);
        engineStatus=label("Motor aus",18); engineStatus.setGravity(Gravity.CENTER); root.addView(engineStatus);
        if(!SoundService.running)SoundService.profile=Math.max(0,Math.min(VehicleCard.NAMES.length-1,getPreferences(0).getInt("vehicle",0)));
        selectedCar=label("",20);root.addView(selectedCar);
        selectedCar.setText("Ausgewählt: "+VehicleCard.NAMES[SoundService.profile]);
        Button custom=new Button(this);custom.setText("Eigene Motoraufnahme importieren");root.addView(custom);custom.setOnClickListener(v->{
            if(SoundService.running){Toast.makeText(this,"Sound zuerst stoppen.",Toast.LENGTH_SHORT).show();return;}
            importProfile=SoundService.profile;
            Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.setType("audio/*");pick.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(pick,7);
        });
        root.addView(label("Sound-Lautstärke",16)); volume=new SeekBar(this); volume.setMax(100); volume.setProgress((int)(SoundService.volume*100)); root.addView(volume);
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean u){SoundService.volume=p/100f;}
            public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
        });
        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);
        Button start=new Button(this); start.setText("FAHR-SOUND STARTEN"); controls.addView(start,new LinearLayout.LayoutParams(0,-2,2)); start.setOnClickListener(v->{demoMode=false; requestStart();});
        Button stop=new Button(this); stop.setText("STOPP");controls.addView(stop,new LinearLayout.LayoutParams(0,-2,1));stop.setOnClickListener(v->stopService(new Intent(this,SoundService.class)));
        root.addView(controls);
        Button demo=new Button(this); demo.setText("DEMO IM STAND STARTEN"); root.addView(demo); demo.setOnClickListener(v->{demoMode=true; requestStart();});
        root.addView(label("Demo-Geschwindigkeit (0–160 km/h)",15)); demoSpeed=new SeekBar(this); demoSpeed.setMax(160); demoSpeed.setProgress((int)(SoundService.demoSpeed*3.6f)); root.addView(demoSpeed);
        demoSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean u){SoundService.demoSpeed=p/3.6f;}
            public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
        });
        Button testShift=new Button(this); testShift.setText("DEMO: KURZER GASEINBRUCH");
        root.addView(testShift); testShift.setOnClickListener(v->{if(SoundService.demo && SoundService.running)SoundService.demoShift++; else Toast.makeText(this,"Nur im Stand-Demo verfügbar",Toast.LENGTH_SHORT).show();});
        Button info=new Button(this); info.setText("Audioquellen / Lizenzen"); root.addView(info);
        info.setOnClickListener(v->{try(java.io.InputStream in=getAssets().open("audio-credits.txt")){
            java.io.ByteArrayOutputStream b2=new java.io.ByteArrayOutputStream(); byte[] buf=new byte[4096]; int n;
            while((n=in.read(buf))!=-1)b2.write(buf,0,n);
            new android.app.AlertDialog.Builder(this).setTitle("Audioquellen").setMessage(b2.toString("UTF-8")).setPositiveButton("OK",null).show();
        }catch(java.io.IOException ex){Toast.makeText(this,"Quellen nicht verfügbar",Toast.LENGTH_SHORT).show();}});
        TextView note=label("Vor der Fahrt einstellen. Handy sicher befestigen. Tesla: Bluetooth als Medienquelle wählen. Bei fehlendem GPS bleibt der Fahr-Sound stumm. Nicht während der Fahrt bedienen.",14); note.setPadding(0,24,0,0); root.addView(note);
        TextView garageTitle=label("GARAGE · "+VehicleCard.NAMES.length+" FAHRZEUGE",20);garageTitle.setPadding(0,dp(18),0,dp(5));root.addView(garageTitle);
        root.addView(label("Tippe auf ein Auto, um seinen Motorsound zu wählen.",13));
        cards=new VehicleCard[VehicleCard.NAMES.length];
        for(int i=0;i<cards.length;i+=2){
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
            for(int j=i;j<Math.min(i+2,cards.length);j++){
                final int index=j;
                VehicleCard card=new VehicleCard(this,j);cards[j]=card;
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(136),1);lp.setMargins(dp(3),dp(4),dp(3),dp(4));
                row.addView(card,lp);card.setOnClickListener(v->selectCar(index));
            }
            if(i+1==cards.length)row.addView(new android.view.View(this),new LinearLayout.LayoutParams(0,dp(136),1));
            root.addView(row);
        }
        selectCar(SoundService.profile);

    }
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private void selectCar(int index){
        if(index<0||index>=VehicleCard.NAMES.length)return;
        SoundService.profile=index;getPreferences(0).edit().putInt("vehicle",index).apply();
        selectedCar.setText("Ausgewählt: "+VehicleCard.NAMES[index]);
        if(cards!=null)for(VehicleCard card:cards)if(card!=null)card.setSelectedCar(card.index==index);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=7||result!=RESULT_OK||data==null||data.getData()==null)return;
        final android.net.Uri uri=data.getData();final int chosen=importProfile;
        EditText input=new EditText(this);input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);input.setHint("z. B. 2500");
        new android.app.AlertDialog.Builder(this).setTitle("Drehzahl der Aufnahme (U/min)")
            .setMessage("PCM-WAV, 16 Bit, 44,1 kHz, mono oder stereo, mit möglichst gleichmäßiger Drehzahl. Nur Aufnahmen verwenden, für die du die Rechte hast.")
            .setView(input).setNegativeButton("Abbrechen",null).setPositiveButton("Importieren",(dialog,which)->{
                final int rpm;try{rpm=Integer.parseInt(input.getText().toString());if(rpm<400||rpm>22000)throw new NumberFormatException();}catch(Exception ex){Toast.makeText(this,"Ungültige Drehzahl",Toast.LENGTH_LONG).show();return;}
                new Thread(()->{try(java.io.InputStream in=getContentResolver().openInputStream(uri)){
                    WavImport.save(in,new java.io.File(getFilesDir(),"engines"),chosen,rpm);
                    runOnUiThread(()->Toast.makeText(this,"Aufnahme gespeichert. Beim nächsten Start aktiv.",Toast.LENGTH_LONG).show());
                }catch(Exception ex){runOnUiThread(()->Toast.makeText(this,"Import fehlgeschlagen: "+ex.getMessage(),Toast.LENGTH_LONG).show());}}).start();
            }).show();
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
    @Override protected void onStop(){stopService(new Intent(this,SoundService.class));super.onStop();}
    @Override protected void onDestroy(){stopService(new Intent(this,SoundService.class));super.onDestroy();}
}
