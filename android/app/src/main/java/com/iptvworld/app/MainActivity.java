package com.iptvworld.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import android.text.InputType;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
  private ExoPlayer player;
  private PlayerView playerView;
  private EditText urlInput, playlistInput;
  private LinearLayout channelList;
  private TextView status;
  private final ExecutorService executor=Executors.newSingleThreadExecutor();
  private final ArrayList<Channel> channels=new ArrayList<>();
  static class Channel { String name,url; Channel(String n,String u){name=n;url=u;} }

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    buildUi();
    player=new ExoPlayer.Builder(this).build();
    playerView.setPlayer(player);
    player.addListener(new Player.Listener(){
      @Override public void onPlayerError(androidx.media3.common.PlaybackException e){runOnUiThread(()->status.setText("تعذر تشغيل البث: "+e.getErrorCodeName()));}
      @Override public void onPlaybackStateChanged(int state){
        if(state==Player.STATE_READY) status.setText("يعمل الآن");
        else if(state==Player.STATE_BUFFERING) status.setText("جارٍ تحميل البث…");
      }
    });
  }
  private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density);}
  private TextView label(String t,int size){TextView x=new TextView(this);x.setText(t);x.setTextColor(Color.WHITE);x.setTextSize(size);x.setGravity(Gravity.CENTER);return x;}
  private EditText field(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setTextColor(Color.WHITE);e.setHintTextColor(0xFF9AA7BA);e.setHint(hint);e.setPadding(dp(12),dp(8),dp(12),dp(8));e.setBackgroundColor(0xFF1B2638);e.setSelectAllOnFocus(false);return e;}
  private Button btn(String t){Button b=new Button(this);b.setText(t);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF155EEF));return b;}
  private void buildUi(){
    LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(12),dp(8),dp(12),dp(8));root.setBackgroundColor(0xFF080E19);
    TextView title=label("IPTV WORLD",22);title.setTypeface(null,1);root.addView(title,new LinearLayout.LayoutParams(-1,dp(42)));
    playerView=new PlayerView(this);playerView.setUseController(true);playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS);root.addView(playerView,new LinearLayout.LayoutParams(-1,0,1.2f));
    status=label("أدخل رابط بث أو رابط قائمة M3U",13);root.addView(status,new LinearLayout.LayoutParams(-1,dp(32)));
    urlInput=field("رابط البث المباشر M3U8 / MP4");root.addView(urlInput,new LinearLayout.LayoutParams(-1,dp(50)));
    Button play=btn("▶ تشغيل الرابط");root.addView(play,new LinearLayout.LayoutParams(-1,dp(48)));
    play.setOnClickListener(v->playUrl(urlInput.getText().toString().trim()));
    LinearLayout pl=new LinearLayout(this);pl.setOrientation(0);playlistInput=field("رابط قائمة M3U");pl.addView(playlistInput,new LinearLayout.LayoutParams(0,dp(50),1));
    Button load=btn("تحميل");pl.addView(load,new LinearLayout.LayoutParams(dp(100),dp(50)));root.addView(pl);
    load.setOnClickListener(v->loadPlaylist(playlistInput.getText().toString().trim()));
    TextView ch=label("القنوات",17);ch.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);root.addView(ch,new LinearLayout.LayoutParams(-1,dp(38)));
    ScrollView scroll=new ScrollView(this);channelList=new LinearLayout(this);channelList.setOrientation(1);scroll.addView(channelList);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,0.8f));
    setContentView(root);
  }
  private void playUrl(String url){
    if(url.isEmpty()){status.setText("أدخل رابط البث أولاً");return;}
    try{player.setMediaItem(MediaItem.fromUri(UriFix(url)));player.prepare();player.play();urlInput.clearFocus();}
    catch(Exception e){status.setText("الرابط غير صالح");}
  }
  private android.net.Uri UriFix(String s){return android.net.Uri.parse(s);}
  private void loadPlaylist(String url){
    if(url.isEmpty()){status.setText("أدخل رابط قائمة M3U");return;}
    status.setText("جارٍ تحميل قائمة القنوات…");
    executor.execute(()->{
      try{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","IPTV-WORLD/1.0");
        BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"));
        ArrayList<Channel> found=new ArrayList<>();String line,name="قناة";
        while((line=r.readLine())!=null){line=line.trim();if(line.startsWith("#EXTINF")){int comma=line.lastIndexOf(',');name=comma>=0?line.substring(comma+1).trim():"قناة";}else if(!line.isEmpty()&&!line.startsWith("#")){found.add(new Channel(name,line));name="قناة";}}
        r.close();c.disconnect();
        runOnUiThread(()->{channels.clear();channels.addAll(found);channelList.removeAllViews();for(Channel item:channels){Button b=btn("▶  "+item.name);b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);channelList.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));b.setOnClickListener(v->{urlInput.setText(item.url);playUrl(item.url);});}status.setText("تم تحميل "+found.size()+" قناة");});
      }catch(Exception e){runOnUiThread(()->status.setText("فشل تحميل القائمة. تحقق من الرابط والاتصال."));}
    });
  }
  @Override protected void onStop(){super.onStop();if(player!=null)player.pause();}
  @Override protected void onDestroy(){super.onDestroy();if(player!=null)player.release();executor.shutdownNow();}
}
