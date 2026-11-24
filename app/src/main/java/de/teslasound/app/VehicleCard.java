package de.teslasound.app;
import android.content.Context;
import android.graphics.*;
import android.view.View;

final class VehicleCard extends View {
    static final String[] NAMES={"Formel 1","V8 klassisch","Diesel","Benziner","Trabant","Porsche 911 GT3 RS","Mustang GT Muscle","Ferrari F12","Dodge Hellcat","Lamborghini Veneno","Audi RS6 Avant","Bugatti Chiron","BMW M3 Competition","Mercedes-AMG GT3","Lexus LFA","VW Käfer 1300","Lamborghini Revuelto"};
    static final String[] ENGINES={"V8 · 18.000 RPM","V8 · 6.500 RPM","R4 Diesel","R4 Benzin","R2 Zweitakt","Boxer 6 · 9.000 RPM","V8 · 7.400 RPM","V12 · 8.900 RPM","V8 Kompressor","V12 · 8.500 RPM","V8 Biturbo","W16 · 4 Turbos","R6 Biturbo","V8 Rennmotor","V10 · 9.000 RPM","Boxer 4 · luftgekühlt","V12 Hybrid · 9.500 RPM"};
    static final int[] COLORS={0xffe33f36,0xffb35236,0xffa0a9b4,0xff53a0b7,0xff92bfaa,0xffe9ebea,0xff386ac9,0xffd82e37,0xff772d3c,0xffc2c7ce,0xff647a87,0xff2b65b8,0xff98b75c,0xffb3bbc4,0xffe6e6e3,0xffc7a15f,0xffef8a38};
    // Model-specific hand-drawn side profiles, normalized to a 300 x 100 canvas.
    static final String[] BODY={
        "M12 70 L16 62 L53 61 L75 43 L112 45 L128 59 L181 58 L191 39 L225 39 L232 62 L277 61 L288 66 L288 77 L12 77 Z",
        "M14 72 L19 55 L68 48 L108 29 L161 28 L195 47 L259 47 L280 56 L286 73 L273 82 L22 82 Z",
        "M15 74 L21 56 L60 50 L103 26 L185 26 L221 50 L271 54 L284 67 L282 82 L19 82 Z",
        "M14 73 L24 56 L74 49 L113 24 L171 23 L210 48 L263 53 L282 65 L280 81 L22 81 Z",
        "M19 72 L23 48 L61 47 L83 25 L172 25 L194 46 L266 48 L278 59 L278 82 L23 82 Z",
        "M15 72 L22 58 L65 48 Q91 21 143 20 Q187 21 222 47 L263 48 Q284 54 286 73 L276 82 L22 82 Z",
        "M13 72 L19 54 L82 49 L122 24 L172 25 L209 50 L272 50 L286 63 L282 82 L22 82 Z",
        "M13 73 L19 59 L87 49 L126 25 L167 22 L208 46 L265 53 L285 65 L280 82 L23 82 Z",
        "M15 72 L19 48 L76 46 L113 27 L183 27 L217 46 L274 46 L285 59 L283 82 L20 82 Z",
        "M10 75 L22 63 L73 52 L111 23 L158 24 L205 51 L250 44 L271 57 L291 73 L283 82 L15 82 Z",
        "M14 73 L20 53 L67 48 L106 23 L216 25 L257 46 L280 54 L287 72 L283 82 L20 82 Z",
        "M15 73 Q24 56 71 51 Q105 21 151 22 Q198 21 231 49 L266 51 Q285 57 287 73 L278 82 L21 82 Z",
        "M14 72 L20 52 L77 47 L118 23 L177 24 L218 48 L268 51 L285 64 L283 82 L22 82 Z",
        "M11 74 L18 57 L88 45 L127 24 L170 24 L205 48 L263 51 L287 67 L283 83 L18 83 Z",
        "M13 74 L19 57 L89 48 L130 25 L175 25 L220 47 L268 50 L285 67 L279 82 L20 82 Z",
        "M25 74 Q30 55 61 49 Q77 11 139 12 Q188 11 215 46 Q260 48 273 71 L269 81 L26 81 Z",
        "M11 74 L21 64 L79 49 L121 22 L161 24 L213 48 L260 48 L288 65 L284 82 L16 82 Z"
    };
    final int index;
    private boolean chosen;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Bitmap photo;
    VehicleCard(Context c,int i){super(c);index=i;photo=loadPhoto(c,i);setClickable(true);setFocusable(true);setContentDescription(NAMES[i]+", "+ENGINES[i]);}
    private static Bitmap loadPhoto(Context context,int index){
        try(java.io.InputStream in=context.getAssets().open("vehicles/"+index+".jpg")){
            return BitmapFactory.decodeStream(in);
        }catch(java.io.IOException|RuntimeException ex){return null;}
    }
    void setSelectedCar(boolean value){chosen=value;setSelected(value);invalidate();}
    private void color(int c){p.setColor(c);p.setStyle(Paint.Style.FILL);}
    private void rect(Canvas c,float x,float y,float x2,float y2,int color){color(color);c.drawRect(x,y,x2,y2,p);}
    private void text(Canvas c,String t,float y,float size,int color,float max){p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);color(color);if(p.measureText(t)>max)p.setTextSize(size*max/p.measureText(t));c.drawText(t,150,y,p);}
    private Path path(String commands){
        String[] a=commands.replaceAll("([MLQZ])", " $1 ").trim().split("\\s+");Path path=new Path();int k=0;
        while(k<a.length){String cmd=a[k++];if(cmd.equals("Z")){path.close();continue;}float x=Float.parseFloat(a[k++]),y=Float.parseFloat(a[k++]);if(cmd.equals("M"))path.moveTo(x,y);else if(cmd.equals("L"))path.lineTo(x,y);else if(cmd.equals("Q"))path.quadTo(x,y,Float.parseFloat(a[k++]),Float.parseFloat(a[k++]));}return path;
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        float scale=Math.min(getWidth()/300f,getHeight()/220f);
        // background fills view; drawing itself is centered and aspect-ratio preserving
        color(chosen?0xff1e3347:0xff222831);canvas.drawRoundRect(1,1,getWidth()-1,getHeight()-1,13,13,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(chosen?3:1);p.setColor(chosen?0xff67c7ff:0xff46515d);canvas.drawRoundRect(2,2,getWidth()-2,getHeight()-2,13,13,p);p.setStyle(Paint.Style.FILL);
        canvas.save();canvas.translate((getWidth()-300*scale)/2,(getHeight()-220*scale)/2);canvas.scale(scale,scale);
        if(photo!=null){
            float maxW=286,maxH=121;
            float ratio=Math.min(maxW/photo.getWidth(),maxH/photo.getHeight());
            float w=photo.getWidth()*ratio,h=photo.getHeight()*ratio;
            RectF dest=new RectF(150-w/2,77-h/2,150+w/2,77+h/2);
            p.setColor(Color.WHITE);p.setStyle(Paint.Style.FILL);canvas.drawBitmap(photo,null,dest,p);
        }else{
        canvas.save();canvas.translate(0,29);
        color(0x66000000);canvas.drawOval(13,77,287,94,p);
        Path b=path(BODY[index]);color(COLORS[index]);canvas.drawPath(b,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.4f);p.setColor(0xffd4dce2);canvas.drawPath(b,p);p.setStyle(Paint.Style.FILL);
        if(index==0){
            rect(canvas,11,55,42,61,0xff14181d);rect(canvas,246,33,276,40,0xff14181d);rect(canvas,264,39,270,61,0xff14181d);
            color(0xff1a242d);canvas.drawOval(125,45,159,57,p);
        }else{
            Path glass;
            if(index==15)glass=path("M77 47 Q97 20 137 20 Q171 20 195 47 Z");
            else if(index==4)glass=path("M68 44 L89 31 L164 31 L183 44 Z");
            else if(index==10)glass=path("M82 47 L112 29 L213 31 L247 47 Z");
            else if(index==5||index==11)glass=path("M82 48 Q111 27 149 27 Q182 27 216 48 Z");
            else if(index==9||index==16)glass=path("M92 48 L122 29 L156 29 L199 48 Z");
            else glass=path("M88 47 L126 30 L170 30 L204 47 Z");
            color(0xff233844);canvas.drawPath(glass,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xff92a5b2);canvas.drawPath(glass,p);p.setStyle(Paint.Style.FILL);
            rect(canvas,149,30,153,48,0xff495760);
            color(0xffe1f1fc);canvas.drawRoundRect(20,57,38,62,2,2,p);
            color(0xffef4348);canvas.drawRoundRect(264,55,281,61,2,2,p);
            if(index==5||index==9||index==13){rect(canvas,238,29,274,34,0xff19212a);rect(canvas,254,34,258,52,0xff19212a);}
            if(index==9||index==16||index==11||index==14){color(0xff212932);canvas.drawPath(path("M201 57 L246 53 L228 68 L204 68 Z"),p);}
            if(index==11){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(0xffc5ceda);canvas.drawArc(148,25,249,79,70,230,false,p);p.setStyle(Paint.Style.FILL);}
            if(index==10){rect(canvas,111,21,220,24,0xff20262c);}
            if(index==8){rect(canvas,42,44,78,49,0xff262c33);}
            if(index==13){rect(canvas,17,81,283,85,0xff101317);}
            if(index==15){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xfff3dfb2);canvas.drawOval(44,49,98,89,p);canvas.drawOval(213,49,267,89,p);p.setStyle(Paint.Style.FILL);}
        }
        for(int cx:new int[]{index==0?64:67,index==0?235:237}){
            int radius=index==0?21:18;color(0xff0b0f13);canvas.drawCircle(cx,79,radius,p);color(0xff7f8b97);canvas.drawCircle(cx,79,radius*.66f,p);color(0xff2a3540);canvas.drawCircle(cx,79,radius*.47f,p);color(0xffcad2d8);canvas.drawCircle(cx,79,radius*.19f,p);
        }
        canvas.restore();
        }
        text(canvas,NAMES[index],157,22,0xfff5f7f9,276);text(canvas,ENGINES[index],180,16,0xffb9c9d7,276);
        if(chosen){color(0xff67c7ff);canvas.drawCircle(276,18,11,p);p.setColor(0xff122335);p.setStrokeWidth(2.5f);p.setStyle(Paint.Style.STROKE);canvas.drawLine(270,18,275,23,p);canvas.drawLine(275,23,282,14,p);p.setStyle(Paint.Style.FILL);}
        canvas.restore();
    }
}
