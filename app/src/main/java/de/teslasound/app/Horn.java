package de.teslasound.app;

/**
 * Vehicle-specific horn. Uses a recorded-style horn sample per vehicle from assets/horns/N.pcm
 * (16-bit mono, 44.1 kHz): the attack plays once, the steady middle part loops seamlessly while
 * the button is held, and releasing fades out quickly like a real horn. Without a sample the
 * procedural model below is used.
 *
 * Procedural model: Electric disc horns are modelled as a buzzing diaphragm (rich,
 * slightly clipped tone with a short pitch rise on attack) that excites the horn's trumpet
 * resonance. Most cars use a high/low pair a major third apart; the Trabant and the Beetle
 * have a single thin 6/12-volt horn, the bus a lower, louder pair. The Space Shuttle has no
 * horn; it gets the cockpit master alarm instead.
 */
final class Horn {
    private static final class Voice {
        final double[] hz; final double buzz, formant, level, jitter; final boolean alarm;
        Voice(double[] hz,double buzz,double formant,double level,double jitter,boolean alarm){
            this.hz=hz;this.buzz=buzz;this.formant=formant;this.level=level;this.jitter=jitter;this.alarm=alarm;
        }
    }
    private static Voice pair(double lo,double hi,double buzz,double formant,double level){return new Voice(new double[]{lo,hi},buzz,formant,level,.002,false);}
    private static final Voice[] VOICES={
        /* 0 Formel 1       */ pair(520,650,3.0,2900,.85),   // real F1 cars have no horn: small high twin
        /* 1 V8 klassisch   */ pair(360,450,4.5,2200,1.00),
        /* 2 Diesel         */ pair(410,510,3.5,2500,.90),
        /* 3 Benziner       */ pair(400,500,3.0,2600,.85),
        /* 4 Trabant        */ new Voice(new double[]{530},6.0,3100,.55,.010,false),
        /* 5 GT3 RS         */ pair(420,525,3.5,2700,.90),
        /* 6 Mustang        */ pair(380,470,4.0,2300,1.00),
        /* 7 Ferrari F12    */ pair(470,590,3.5,2900,.95),
        /* 8 Hellcat        */ pair(370,460,4.5,2200,1.00),
        /* 9 Veneno         */ pair(440,555,3.5,2800,.95),
        /*10 RS6            */ pair(400,500,3.5,2500,.95),
        /*11 Chiron         */ pair(430,540,3.0,2700,.95),
        /*12 M3             */ pair(410,505,3.5,2500,.95),
        /*13 AMG GT3        */ pair(460,575,3.0,2800,.90),
        /*14 LFA            */ pair(440,550,3.0,2800,.90),
        /*15 Käfer          */ new Voice(new double[]{330},5.5,2400,.65,.008,false),
        /*16 Revuelto       */ pair(450,565,3.5,2800,.95),
        /*17 Stadtbus       */ pair(300,375,5.0,1900,1.15),
        /*18 Space Shuttle  */ new Voice(new double[]{700,1000},2.0,3000,.75,0,true),
    };

    private final int rate;
    private final double[] phase=new double[2];
    private double env=0, onTime=0, low=0, band=0, alarmClock=0, jitter=0;
    private final java.util.Random random=new java.util.Random(7);
    private final float[][] samples=new float[VOICES.length][];
    private final int[] loopStart=new int[VOICES.length], loopEnd=new int[VOICES.length], begin=new int[VOICES.length];
    private double pos=0, sampleEnv=0;
    private boolean playing=false;
    private static final int SAMPLE_RATE=44100;

    Horn(int rate){this.rate=rate;}
    Horn(android.content.res.AssetManager assets,int rate){
        this.rate=rate;
        for(int m=0;m<VOICES.length;m++){
            try(java.io.InputStream in=assets.open("horns/"+m+".pcm")){
                java.io.ByteArrayOutputStream b=new java.io.ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
                while((n=in.read(buf))!=-1)b.write(buf,0,n);
                java.nio.ByteBuffer bb=java.nio.ByteBuffer.wrap(b.toByteArray()).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                float[] x=new float[bb.remaining()/2];for(int i=0;i<x.length;i++)x[i]=bb.getShort()/32768f;
                if(x.length>SAMPLE_RATE/4)prepare(m,x);
            }catch(java.io.IOException|RuntimeException ignored){}
        }
    }
    /** Finds where the horn starts and a steady stretch to loop. */
    private void prepare(int m,float[] x){
        int win=SAMPLE_RATE/50;int frames=x.length/win;double[] env=new double[frames];double max=0;
        for(int f=0;f<frames;f++){double s=0;for(int i=f*win;i<(f+1)*win;i++)s+=x[i]*x[i];env[f]=Math.sqrt(s/win);max=Math.max(max,env[f]);}
        if(max<=0)return;
        int first=0;while(first<frames&&env[first]<.08*max)first++;
        int last=frames-1;while(last>first&&env[last]<.35*max)last--;
        int active=last-first+1;
        int ls=(first+(int)(active*.35))*win, le=(first+(int)(active*.80))*win;
        if(le-ls<SAMPLE_RATE*3/20){ls=Math.max(first*win,le-SAMPLE_RATE*3/20);}
        samples[m]=x;begin[m]=Math.max(0,first*win-win);loopStart[m]=ls;loopEnd[m]=Math.min(x.length-1,le);
    }
    private double at(float[] x,double p){int i=(int)p;if(i<0||i+1>=x.length)return 0;double f=p-i;return x[i]+(x[i+1]-x[i])*f;}

    /** Adds the horn to a block of audio; pressed is the button state. */
    void mix(short[] pcm,int profile,boolean pressed,double volume){
        int m=Math.max(0,Math.min(VOICES.length-1,profile));
        float[] x=samples[m];
        if(x==null){synth(pcm,m,pressed,volume);return;}
        if(!pressed&&!playing)return;
        if(pressed&&!playing){playing=true;pos=begin[m];}
        if(pressed)sampleEnv=1;
        double step=SAMPLE_RATE/(double)rate, release=Math.exp(-1/(rate*.06));
        int ls=loopStart[m],le=loopEnd[m],len=le-ls,xf=Math.min(len/3,SAMPLE_RATE/25);
        for(int i=0;i<pcm.length;i++){
            if(!pressed)sampleEnv*=release;
            double y;
            if(pressed&&pos>=le-xf){
                // Crossfade the end of the loop into its start so holding the button sounds seamless.
                double w=(pos-(le-xf))/xf;
                y=at(x,pos)*(1-w)+at(x,pos-len)*w;
                if(pos>=le)pos-=len;
            }else y=at(x,pos);
            pos+=step;
            if(pos>=x.length-1||sampleEnv<1e-4){playing=false;break;}
            int s=pcm[i]+(int)Math.round(y*sampleEnv*volume*.95*32767);
            pcm[i]=(short)Math.max(-31782,Math.min(31782,s));
        }
    }

    private void synth(short[] pcm,int profile,boolean pressed,double volume){
        Voice v=VOICES[profile];
        if(!pressed&&env<1e-4){env=0;onTime=0;return;}
        double attack=Math.exp(-1/(rate*.012)),release=Math.exp(-1/(rate*.045));
        double f=2*Math.sin(Math.PI*v.formant/rate);
        for(int i=0;i<pcm.length;i++){
            env=pressed?1-(1-env)*attack:env*release;
            onTime=pressed?onTime+1.0/rate:onTime;
            // Diaphragm comes up to speed during the first ~30 ms.
            double rise=1-.06*Math.exp(-onTime/.03);
            jitter+=.002*((random.nextDouble()*2-1)-jitter);
            double src=0;
            if(v.alarm){
                // Master alarm: alternating tones, 2.5 per second.
                alarmClock+=1.0/rate;
                double hz=((int)(alarmClock*5)%2==0)?v.hz[0]:v.hz[1];
                phase[0]+=2*Math.PI*hz/rate;if(phase[0]>2*Math.PI)phase[0]-=2*Math.PI;
                src=Math.tanh(v.buzz*Math.sin(phase[0]));
            }else{
                for(int k=0;k<v.hz.length;k++){
                    double hz=v.hz[k]*rise*(1+v.jitter*jitter*(k==0?1:-1));
                    phase[k]+=2*Math.PI*hz/rate;if(phase[k]>2*Math.PI)phase[k]-=2*Math.PI;
                    // Clipped diaphragm motion plus the second harmonic of its asymmetric stroke.
                    src+=Math.tanh(v.buzz*Math.sin(phase[k]))+.25*Math.sin(2*phase[k]);
                }
                src/=v.hz.length;
            }
            // Trumpet resonance.
            low+=f*band;double high=src-low-.35*band;band+=f*high;
            double y=(.55*src+.35*band)*env*v.level*volume*.9;
            int s=pcm[i]+(int)Math.round(y*32767);
            pcm[i]=(short)Math.max(-31782,Math.min(31782,s));
        }
        if(!pressed&&env<1e-4){env=0;onTime=0;alarmClock=0;}
    }
}
