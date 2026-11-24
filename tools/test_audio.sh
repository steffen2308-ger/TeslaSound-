set -euo pipefail
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/android/content/res" "$tmp/android/util" "$tmp/de/teslasound/app"
cat > "$tmp/android/content/res/AssetManager.java" <<'EOF'
package android.content.res;
public class AssetManager { public String[] list(String s){return new String[0];} public java.io.InputStream open(String s)throws java.io.IOException{throw new java.io.IOException();} }
EOF
cat > "$tmp/android/util/Log.java" <<'EOF'
package android.util;
public class Log { public static int w(String a,String b,Throwable t){return 0;} }
EOF
cp app/src/main/java/de/teslasound/app/{EngineAudio,Gearbox,TrabantAudio}.java "$tmp/de/teslasound/app/"
cat > "$tmp/de/teslasound/app/Test.java" <<'EOF'
package de.teslasound.app;
public class Test {
 public static void main(String[] args){
 EngineAudio e=new EngineAudio(new android.content.res.AssetManager(),44100,new java.io.File("/no-samples"));
 short[] x=new short[256];
 for(int mode=0;mode<17;mode++){
   e.reset();long energy=0;int max=0;
   for(int i=0;i<1500;i++){
     double t=i*256.0/44100, speed=Math.min(24,t*3);
     e.render(x,mode,speed,3,.5,256.0/44100);
     if(!Double.isFinite(e.rpm())||e.gear()<1)throw new AssertionError("bad engine "+mode);
     for(short v:x){energy+=(long)v*v;max=Math.max(max,Math.abs((int)v));}
   }
   if(energy==0||max>=32767)throw new AssertionError("invalid audio "+mode);
   for(int i=0;i<300;i++)e.render(x,mode,0,0,0,256.0/44100);
   for(short v:x)if(v!=0)throw new AssertionError("not muted "+mode);
 }
 // At high RPM, check firing cadence and render 1-second blocks without a loop envelope.
 TrabantAudio direct=new TrabantAudio(44100);
 for(int rpm:new int[]{800,1800,2600,3500,4500}){
   direct.reset();int previous=0;double min=Double.MAX_VALUE,max=0;
   for(int sec=0;sec<6;sec++){
     double sum=0;
     for(int i=0;i<44100;i++){double v=direct.next(rpm,.65,.5,false,1);if(!Double.isFinite(v))throw new AssertionError("NaN");sum+=v*v;}
     int count=direct.events()-previous;previous=direct.events();
     if(Math.abs(count-2*rpm/60.0)>3)throw new AssertionError("firing frequency "+rpm+": "+count);
     if(sec>0){double rms=Math.sqrt(sum/44100);min=Math.min(min,rms);max=Math.max(max,rms);}
   }
   if(min<.005||max/min>1.25)throw new AssertionError("unstable envelope "+rpm+" "+max/min);
   System.out.printf("Trabant %d RPM: RMS variation %.2f, firing count verified%n",rpm,max/min);
 }
 System.out.println("17 engine profiles: audio generated, finite RPM and mute verified");
 }
}
EOF
javac -d "$tmp/out" $(find "$tmp" -name '*.java')
java -cp "$tmp/out" de.teslasound.app.Test
cat > "$tmp/de/teslasound/app/Preview.java" <<'EOF'
package de.teslasound.app;
public class Preview {
 public static void main(String[] args)throws Exception{
  EngineAudio e=new EngineAudio(new android.content.res.AssetManager(),44100,new java.io.File("/no-samples"));
  short[] x=new short[256];double dt=256.0/44100;
  try(java.io.DataOutputStream out=new java.io.DataOutputStream(new java.io.BufferedOutputStream(new java.io.FileOutputStream(args[0])))){
   for(int i=0;i<Math.round(34/dt);i++){
    double t=i*dt;
    double speed=t<3?0:t<18?(t-3)*2.1:t<25?31.5:Math.max(0,31.5-(t-25)*3.5);
    double accel=t<3?0:t<18?2.1:t<25?0:-3.5;
    e.render(x,4,speed,accel,.8,dt);
    for(short v:x)out.writeShort(Short.reverseBytes(v));
   }
  }
 }
}
EOF
javac -cp "$tmp/out" -d "$tmp/out" "$tmp/de/teslasound/app/Preview.java"
java -cp "$tmp/out" de.teslasound.app.Preview "$tmp/trabant.pcm"
ffmpeg -v error -y -f s16le -ar 44100 -ac 1 -i "$tmp/trabant.pcm" Trabant-Synth-Preview.wav
