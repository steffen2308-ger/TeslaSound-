package de.teslasound.app;

/**
 * Vehicle-specific horn. Electric disc horns are modelled as a buzzing diaphragm (rich,
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
    Horn(int rate){this.rate=rate;}

    /** Adds the horn to a block of audio; pressed is the button state. */
    void mix(short[] pcm,int profile,boolean pressed,double volume){
        Voice v=VOICES[Math.max(0,Math.min(VOICES.length-1,profile))];
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
