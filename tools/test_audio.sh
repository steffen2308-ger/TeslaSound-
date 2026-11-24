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
cp app/src/main/java/de/teslasound/app/{EngineAudio,Gearbox}.java "$tmp/de/teslasound/app/"
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
 System.out.println("17 engine profiles: audio generated, finite RPM and mute verified");
 }
}
EOF
javac -d "$tmp/out" $(find "$tmp" -name '*.java')
java -cp "$tmp/out" de.teslasound.app.Test
