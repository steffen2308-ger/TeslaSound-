package de.teslasound.app;
import java.io.*;
import java.nio.*;
final class WavImport {
    static void save(InputStream in,File base,int index,int rpm)throws IOException{
        if(in==null)throw new IOException("Keine Datei");
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
        while((n=in.read(b))!=-1){if(out.size()+n>16000000)throw new IOException("Datei zu groß (max. 16 MB)");out.write(b,0,n);}
        ByteBuffer bb=ByteBuffer.wrap(out.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
        if(bb.remaining()<44||bb.getInt()!=0x46464952)throw new IOException("WAV erforderlich");bb.getInt();if(bb.getInt()!=0x45564157)throw new IOException("WAV erforderlich");
        int format=0,channels=0,rate=0,bits=0;byte[] pcm=null;
        while(bb.remaining()>=8){int tag=bb.getInt(),len=bb.getInt();if(len<0||len>bb.remaining())break;int pos=bb.position();
            if(tag==0x20746d66&&len>=16){format=bb.getShort()&65535;channels=bb.getShort()&65535;rate=bb.getInt();bb.getInt();bb.getShort();bits=bb.getShort()&65535;}
            if(tag==0x61746164){pcm=new byte[len];bb.get(pcm);}
            int next=pos+len+(len&1);if(next>bb.limit())break;bb.position(next);}
        if(format!=1||rate!=44100||bits!=16||(channels!=1&&channels!=2)||pcm==null)throw new IOException("Benötigt PCM 16 Bit / 44,1 kHz / mono oder stereo");
        int frames=Math.min(pcm.length/(2*channels),44100*8);if(frames<11025)throw new IOException("Mindestens 0,25 Sekunden");
        ByteBuffer data=ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN);double[] x=new double[frames];double mean=0;
        for(int i=0;i<frames;i++){double sample=data.getShort();if(channels==2)sample=(sample+data.getShort())/2;x[i]=sample;mean+=sample;}
        mean/=frames;for(int i=0;i<frames;i++)x[i]-=mean;
        int cross=Math.min(1102,frames/5),length=frames-cross;
        double[] y=new double[length];for(int i=0;i<length;i++)y[i]=x[i+cross];
        for(int i=0;i<cross;i++){double t=(i+1.0)/cross;y[length-cross+i]=y[length-cross+i]*(1-t)+x[i]*t;}
        double peak=1;for(double v:y)peak=Math.max(peak,Math.abs(v));
        File folder=new File(base,Integer.toString(index));if(!folder.exists()&&!folder.mkdirs())throw new IOException("Speicher nicht verfügbar");
        ByteBuffer result=ByteBuffer.allocate(length*2).order(ByteOrder.LITTLE_ENDIAN);for(double v:y)result.putShort((short)(v/peak*24000));
        File dest=new File(folder,rpm+".pcm"),tmp=new File(folder,rpm+".tmp");
        try(FileOutputStream f=new FileOutputStream(tmp)){f.write(result.array());}
        if(!tmp.renameTo(dest)){tmp.delete();throw new IOException("Speichern fehlgeschlagen");}
    }
}
