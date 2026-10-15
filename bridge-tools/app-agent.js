import Java from 'frida-java-bridge';
let last=null;
setInterval(()=>{
 try {
 const value=Number(File.readAllText('/data/local/tmp/local-speed/rate').trim());
 if(!Number.isFinite(value)||value<0.1||value>20||value===last)return;
 Java.perform(()=>{
  try{Java.use('layaair.game.browser.ConchJNI').RunJS('(function(){if(typeof Laya!=="undefined"&&Laya.timer){Laya.timer.scale='+value+';if(Laya.systemTimer){Laya.systemTimer.scale='+value+';}console.log("LOCAL_SPEED timer="+Laya.timer.scale+" systemTimer="+(Laya.systemTimer?Laya.systemTimer.scale:"missing"));}else{console.log("LOCAL_SPEED unsupported");}})();');last=value;send('双计时器倍率已提交 '+value+'；实际结果需观察目标应用');}
  catch(e){send('不兼容：'+e);}
 });
 }catch(e){send('控制文件错误：'+e);}
},500);
