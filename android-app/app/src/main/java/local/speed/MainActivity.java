package local.speed;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.content.pm.ApplicationInfo;
import android.widget.*;
import android.text.InputType;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ArrayList<ApplicationInfo> apps = new ArrayList<>();
    private Spinner picker;
    private EditText rate;
    private TextView status;
    private String selectedPackage;
    private int connectedPid;
    private Process injector;
    private static final String BASE = "/data/local/tmp/local-speed";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(28, 40, 28, 20);
        panel.setBackgroundColor(Color.rgb(245, 240, 228));
        ScrollView scroll = new ScrollView(this); scroll.addView(panel); setContentView(scroll);
        TextView title = new TextView(this); title.setText("Laya Speed"); title.setTextSize(32); panel.addView(title);
        TextView hint = new TextView(this);
        hint.setText("Root 实验版 · 当前仅支持 LayaAir\n先正常打开目标应用，再选择并连接。\n范围 0.1–20 倍；高倍率可能造成事件或结算异常。"); panel.addView(hint);
        picker = new Spinner(this); panel.addView(picker);
        button(panel, "刷新应用列表", this::refreshApps);
        rate = new EditText(this); rate.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); rate.setText("5"); panel.addView(rate);
        SeekBar slider = new SeekBar(this); slider.setMax(199); slider.setProgress(49); panel.addView(slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s,int p,boolean user) { if(user)rate.setText(String.format(Locale.US,"%.1f",(p+1)/10.0)); }
            public void onStartTrackingTouch(SeekBar s) {} public void onStopTrackingTouch(SeekBar s) {}
        });
        button(panel,"连接目标（不启动应用）",this::connect);
        button(panel,"应用倍率",()->apply(false));
        button(panel,"恢复 1 倍",()->{rate.setText("1");apply(true);});
        status = new TextView(this); status.setText("未连接。默认倍率 5，连接后仍需点击应用倍率。"); panel.addView(status);
        refreshApps();
    }
    private void button(LinearLayout panel,String text,Runnable action) {
        Button b=new Button(this);b.setText(text);b.setOnClickListener(v->action.run());panel.addView(b);
    }
    private void show(String text) { runOnUiThread(()->status.setText(text)); }
    private String root(String command) throws Exception {
        Process p=new ProcessBuilder("su","-c",command).redirectErrorStream(true).start();
        if(!p.waitFor(15,TimeUnit.SECONDS)){p.destroy();throw new IOException("Root 命令超时，请确认授权");}
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        byte[] buffer=new byte[4096];int count;
        while((count=p.getInputStream().read(buffer))!=-1)bytes.write(buffer,0,count);
        String output=new String(bytes.toByteArray(),"UTF-8").trim();
        if(p.exitValue()!=0)throw new IOException(output.isEmpty()?"命令失败":output);
        return output;
    }
    private void refreshApps() {
        apps.clear();
        for(ApplicationInfo a:getPackageManager().getInstalledApplications(0))
            if(!a.packageName.equals(getPackageName()) && (a.flags&ApplicationInfo.FLAG_SYSTEM)==0 && getPackageManager().getLaunchIntentForPackage(a.packageName)!=null)apps.add(a);
        apps.sort(Comparator.comparing(a->a.loadLabel(getPackageManager()).toString()));
        ArrayList<String> labels=new ArrayList<>();for(ApplicationInfo a:apps)labels.add(a.loadLabel(getPackageManager())+" · "+a.packageName);
        picker.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels));
    }
    private File copy(String name) throws Exception {
        File f=new File(getFilesDir(),name);
        try(InputStream in=getAssets().open(name);OutputStream out=new FileOutputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}return f;
    }
    private void connect() {
        if(apps.isEmpty())return;
        String pkg=apps.get(picker.getSelectedItemPosition()).packageName;
        if(!pkg.matches("[A-Za-z0-9_.]+")){show("无效包名");return;}
        if(connectedPid!=0){show("本次已连接。更换目标前先恢复 1 倍并重启工具。");return;}
        worker.submit(()->{try{
            show("正在请求 Root 并检查目标…");
            String pidText=root("pidof "+pkg);
            if(!pidText.matches("[0-9]+"))throw new IOException("目标未运行或有多个进程，请先打开目标应用");
            int pid=Integer.parseInt(pidText);
            String abi=root("getprop ro.product.cpu.abi");if(!abi.equals("x86_64"))throw new IOException("此构建仅支持 x86_64 模拟器");
            File bin=copy("frida-inject"), agent=copy("agent.js");
            root("mkdir -p "+BASE+" && chmod 755 "+BASE+" && cp '"+bin+"' "+BASE+"/inject && chmod 755 "+BASE+"/inject && cp '"+agent+"' "+BASE+"/agent.js && chmod 644 "+BASE+"/agent.js && echo 1 > "+BASE+"/rate && chmod 644 "+BASE+"/rate");
            injector=new ProcessBuilder("su","-c",BASE+"/inject -p "+pid+" -s "+BASE+"/agent.js").redirectErrorStream(true).start();
            new Thread(()->{try(BufferedReader r=new BufferedReader(new InputStreamReader(injector.getInputStream()))){String line;while((line=r.readLine())!=null)show(line);}catch(Exception e){show(e.toString());}}).start();
            Thread.sleep(1500);if(!injector.isAlive())throw new IOException("注入器退出，请检查 Root 和兼容性");
            connectedPid=pid;selectedPackage=pkg;show("已附加 PID "+pid+"。请点击应用倍率；设置是否生效需观察游戏。");
        }catch(Exception e){show("连接失败："+e.getMessage());}});
    }
    private void apply(boolean reset) {
        double value;
        try{value=reset?1:Double.parseDouble(rate.getText().toString());if(!Double.isFinite(value)||value<0.1||value>20)throw new Exception();}
        catch(Exception e){show("倍率请输入 0.1～20");return;}
        final double v=value;
        worker.submit(()->{try{
            if(connectedPid==0)throw new IOException("请先连接目标");
            if(!root("pidof "+selectedPackage).equals(String.valueOf(connectedPid)))throw new IOException("目标已重启，请重启工具重新连接");
            root("echo "+v+" > "+BASE+"/rate");show("已提交 "+v+" 倍，实际效果需观察游戏。恢复失败时请重启目标应用。");
        }catch(Exception e){show("设置失败："+e.getMessage());}});
    }
}
