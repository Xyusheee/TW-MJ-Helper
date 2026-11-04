package hk.mahjong.ledger;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import org.json.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.time.LocalDate;
import java.util.*;

public class MainActivity extends Activity {
    Ledger l=new Ledger();
    String[] names={"東家","南家","西家","北家"};
    JSONArray logs=new JSONArray();
    ArrayList<String> undo=new ArrayList<>();
    LinearLayout content;
    int page=0;
    String[] pages={"牌局","結算","紀錄","設定","規則"};
    static final int EXPORT=10,IMPORT=11;
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        try{String s=getPreferences(0).getString("state",null);if(s!=null)load(s);}catch(Exception e){toast("讀取紀錄失敗；請匯入備份。" );}
        draw();
    }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(1);return v;}
    TextView text(LinearLayout box,String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(30,50,55));t.setPadding(0,10,0,10);box.addView(t);return t;}
    void button(LinearLayout box,String s,Runnable action){Button b=new Button(this);b.setText(s);box.addView(b);b.setOnClickListener(v->action.run());}
    EditText input(LinearLayout box,String hint,String value,boolean decimal){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setSingleLine(true);if(decimal)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);box.addView(e);return e;}
    Spinner select(LinearLayout box,String label,String[] values,int index){text(box,label,15);Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,values));s.setSelection(index);box.addView(s);return s;}
    void draw(){
        LinearLayout root=column();root.setBackgroundColor(Color.rgb(241,247,244));root.setPadding(16,12,16,12);
        if(Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets x=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(16+x.left,12+x.top,16+x.right,12+x.bottom);return i;});
        else root.setFitsSystemWindows(true);
        setContentView(root);
        text(root,"台牌記帳 · "+pages[page],24);
        HorizontalScrollView nav=new HorizontalScrollView(this);LinearLayout row=new LinearLayout(this);nav.addView(row);root.addView(nav);
        for(int i=0;i<pages.length;i++){final int n=i;button(row,pages[i],()->{page=n;draw();});}
        ScrollView scroll=new ScrollView(this);content=column();scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(page==0)game();if(page==1)totals();if(page==2)history();if(page==3)settings();if(page==4)rules();
    }
    String m(BigDecimal x){return "$"+Ledger.money(x);}
    void game(){
        text(content,"莊家："+names[l.dealer]+"　連"+l.streak+"　莊家相關加番："+l.bonus(),18);
        text(content,"底 "+m(l.base)+" ／ 每番 "+m(l.rate)+"\n中央罰款："+m(l.pot),16);
        if(l.chain<0)text(content,"目前沒有未找數的拉數。",17);
        else{
            text(content,"拉數胡家："+names[l.chain],19);
            for(int p=0;p<4;p++)if(p!=l.chain)text(content,names[p]+" 暫欠 "+m(l.debt[p]),17);
        }
        button(content,"記錄食胡／自摸",this::winDialog);
        button(content,"找數（全數結清）",()->ask("結清目前拉數？",()->change("找數",()->l.settle(-1))));
        button(content,"手動踢半並找數",()->{
            if(l.chain<0){toast("沒有拉數");return;}
            LinearLayout b=column();Spinner p=select(b,"只減半此玩家欠現任胡家的款項",names,0);
            new AlertDialog.Builder(this).setTitle("手動踢半（不要重複使用）").setView(b).setPositiveButton("確認",(d,w)->{
                int n=p.getSelectedItemPosition();if(n==l.chain){toast("胡家不能踢自己的數");return;}change(names[n]+" 手動踢半找數",()->l.settle(n));
            }).setNegativeButton("取消",null).show();
        });
        button(content,"叫碰不碰：罰一底入中央",()->{
            new AlertDialog.Builder(this).setTitle("選擇罰款玩家").setItems(names,(d,w)->change(names[w]+" 叫碰不碰罰一底",()->l.penalty(w))).show();
        });
        button(content,"流局／手動莊家及連莊",this::dealerDialog);
        button(content,"復原上一項操作",()->{
            if(undo.isEmpty()){toast("沒有可復原操作（只保留本次開啟期間最近30項）");return;}
            ask("復原上一項操作？",()->{try{load(undo.remove(undo.size()-1));save();draw();}catch(Exception e){toast(e.toString());}});
        });
        text(content,"輸入番數不包括莊家加番。食胡後自動處理拉數、踢半及輪莊；流局請手動調整莊家。",14);
    }
    void winDialog(){
        LinearLayout box=column();Spinner winner=select(box,"胡家",names,0);Spinner loser=select(box,"出沖者（自摸時忽略）",names,1);
        EditText fan=input(box,"手牌番數（不含莊家加番）","0",true);
        Switch self=new Switch(this);self.setText("自摸");box.addView(self);
        CheckBox dice=new CheckBox(this);dice.setText("莊家已放骰，可計莊家胡牌加番");dice.setChecked(l.diceVisible);box.addView(dice);
        text(box,"未放骰：莊家胡牌不加莊番；莊家出沖／支付他人自摸仍須支付莊番。\n非莊家胡牌後自動順移一位莊家。",14);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("記錄一局").setView(box).setPositiveButton("記錄",null).setNegativeButton("取消",null).create();
        dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(q->{try{
            int f=Integer.parseInt(fan.getText().toString());if(f<0||f>10000)throw new IllegalArgumentException("番數須為0至10000整數");
            int a=winner.getSelectedItemPosition(),b=loser.getSelectedItemPosition();boolean s=self.isChecked();
            if(!s&&a==b)throw new IllegalArgumentException("胡家和出沖者不能相同");
            String desc=names[a]+(s?" 自摸 ":" 食 "+names[b]+" ")+f+"番；局前莊 "+names[l.dealer]+" 連"+l.streak;
            change(desc,()->{l.diceVisible=dice.isChecked();l.win(a,b,f,s);});dialog.dismiss();
        }catch(Exception e){toast(e.getMessage());}}));dialog.show();
    }
    void dealerDialog(){
        LinearLayout b=column();Spinner p=select(b,"莊家",names,l.dealer);EditText n=input(b,"連莊數",String.valueOf(l.streak),true);
        new AlertDialog.Builder(this).setTitle("手動調整（不新增勝負）").setView(b).setPositiveButton("儲存",(d,w)->{try{int z=Integer.parseInt(n.getText().toString());if(z<0||z>10000)throw new Exception();change("手動調整莊家／連莊",()->{l.dealer=p.getSelectedItemPosition();l.streak=z;});}catch(Exception e){toast("連莊數須為0至10000整數");}}).setNegativeButton("取消",null).show();
    }
    void totals(){
        text(content,"全期淨額（已結算＋飯錢）",20);
        for(int p=0;p<4;p++)text(content,names[p]+"："+m(l.balance[p]),18);
        text(content,"如現在全數找數（未踢半）",20);BigDecimal[] projected=l.projected();
        BigDecimal sum=BigDecimal.ZERO;for(int p=0;p<4;p++){text(content,names[p]+"："+m(projected[p]),17);sum=sum.add(projected[p]);}
        text(content,"四家淨額合計 "+m(sum)+" ＋ 中央 "+m(l.pot)+" ＝ "+m(sum.add(l.pot))+"（應為0）",15);
        button(content,"記錄飯錢／共同開支",this::mealDialog);
        if(l.pot.signum()==0){
            text(content,"按現在全數找數的簡化轉帳建議",20);
            BigDecimal[] b=projected.clone();for(int i=0;i<4;i++)for(int j=0;j<4;j++)if(b[i].signum()<0&&b[j].signum()>0){BigDecimal a=b[i].negate().min(b[j]);text(content,names[i]+" → "+names[j]+"："+m(a),17);b[i]=b[i].add(a);b[j]=b[j].subtract(a);}
            text(content,"這只是建議，不會執行付款或更改帳目。",14);
        }
    }
    BigDecimal decimal(EditText e){BigDecimal n=new BigDecimal(e.getText().toString().trim());if(n.signum()<0||n.precision()>30||n.scale()>8)throw new IllegalArgumentException("請輸入非負金額，最多8位小數／30位有效數字");return n;}
    void mealDialog(){
        LinearLayout b=column();text(b,"各人已付總額必須等於各人應付總額。正數淨額代表應收。",15);
        EditText[] paid=new EditText[4],share=new EditText[4];for(int p=0;p<4;p++){text(b,names[p],17);paid[p]=input(b,"已付","0",true);share[p]=input(b,"應攤","0",true);}
        ScrollView sc=new ScrollView(this);sc.addView(b);
        AlertDialog d=new AlertDialog.Builder(this).setTitle("飯錢").setView(sc).setPositiveButton("記錄",null).setNegativeButton("取消",null).create();
        d.setOnShowListener(v->d.getButton(-1).setOnClickListener(vv->{try{
            BigDecimal[] a=new BigDecimal[4];BigDecimal sum=BigDecimal.ZERO;for(int p=0;p<4;p++){a[p]=decimal(paid[p]).subtract(decimal(share[p]));sum=sum.add(a[p]);}
            if(sum.signum()!=0)throw new IllegalArgumentException("收支不平衡，差額："+m(sum));
            change("飯錢／共同開支",()->{for(int p=0;p<4;p++)l.balance[p]=l.balance[p].add(a[p]);});d.dismiss();
        }catch(Exception e){toast(e.getMessage());}}));d.show();
    }
    void history(){
        text(content,"年度／每月已結算淨額（包括飯錢及罰款）",18);
        EditText year=input(content,"年份",String.valueOf(LocalDate.now().getYear()),true);
        button(content,"查看指定年份",()->report(year.getText().toString()));
        text(content,"完整操作紀錄（最新在上）",19);
        for(int i=logs.length()-1;i>=0;i--){JSONObject o=logs.optJSONObject(i);text(content,o.optString("date")+"  "+o.optString("text"),15);}
        text(content,"拉數未找數前不計入已結算報表。復原會移除被復原的操作。",14);
    }
    void report(String year){
        if(!year.matches("[0-9]{4}")){toast("請輸入4位年份");return;}
        TreeMap<String,BigDecimal[]> rows=new TreeMap<>();BigDecimal[] total=Ledger.zeros();
        try{for(int i=0;i<logs.length();i++){JSONObject o=logs.getJSONObject(i);String date=o.getString("date");if(!date.startsWith(year+"-"))continue;String month=date.substring(0,7);BigDecimal[] v=rows.computeIfAbsent(month,k->Ledger.zeros());JSONArray d=o.getJSONArray("delta");for(int p=0;p<4;p++){BigDecimal x=new BigDecimal(d.getString(p));v[p]=v[p].add(x);total[p]=total[p].add(x);}}}catch(Exception e){toast(e.toString());return;}
        StringBuilder out=new StringBuilder();rows.put(year+" 全年",total);for(Map.Entry<String,BigDecimal[]> r:rows.entrySet()){out.append(r.getKey()).append("\n");for(int p=0;p<4;p++)out.append(names[p]).append(" ").append(m(r.getValue()[p])).append("\n");out.append("\n");}
        new AlertDialog.Builder(this).setTitle("已結算報表").setMessage(out.toString()).setPositiveButton("關閉",null).show();
    }
    void settings(){
        text(content,"名稱與金額",20);EditText[] ns=new EditText[4];for(int p=0;p<4;p++)ns[p]=input(content,"玩家"+(p+1),names[p],false);
        EditText base=input(content,"一底",Ledger.money(l.base),true),rate=input(content,"每番",Ledger.money(l.rate),true);
        button(content,"儲存設定",()->{try{
            BigDecimal b=decimal(base),r=decimal(rate);if((l.chain>=0||l.pot.signum()!=0)&&(b.compareTo(l.base)!=0||r.compareTo(l.rate)!=0))throw new IllegalArgumentException("請先找數及清空中央罰款才改金額");
            String[] n=new String[4];for(int p=0;p<4;p++){n[p]=ns[p].getText().toString().trim();if(n[p].isEmpty())throw new IllegalArgumentException("姓名不可留空");}
            change("更新設定",()->{names=n;l.base=b;l.rate=r;});
        }catch(Exception e){toast(e.getMessage());}});
        button(content,"匯出完整JSON備份",()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"mahjong-"+LocalDate.now()+".json");startActivityForResult(i,EXPORT);});
        button(content,"匯入JSON（覆蓋本機資料）",()->ask("匯入將覆蓋目前所有帳目及紀錄。繼續？",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,IMPORT);}));
        button(content,"清除全部資料／開新帳簿",()->ask("永久清除本機所有紀錄？請先匯出備份。",()->{undo.clear();l=new Ledger();logs=new JSONArray();names=new String[]{"東家","南家","西家","北家"};save();draw();}));
        text(content,"只在本機保存，不需帳戶或網絡。刪除應用程式會失去本機資料，請定期備份。\n預設 $10底／$2番；如你們未協議並採用規則的$10底／$10番，請在此手動改為10。",15);
    }
    void rules(){
        try(InputStream in=getAssets().open("rules.txt")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);text(content,new String(out.toByteArray(),StandardCharsets.UTF_8),16);}catch(Exception e){text(content,e.toString(),14);}
    }
    void ask(String message,Runnable action){new AlertDialog.Builder(this).setMessage(message).setPositiveButton("確認",(d,w)->action.run()).setNegativeButton("取消",null).show();}
    JSONArray amounts(BigDecimal[] a){JSONArray j=new JSONArray();for(BigDecimal n:a)j.put(Ledger.money(n));return j;}
    String state() throws Exception{
        JSONObject o=new JSONObject();o.put("version",1);o.put("names",new JSONArray(Arrays.asList(names)));o.put("base",Ledger.money(l.base));o.put("rate",Ledger.money(l.rate));o.put("balance",amounts(l.balance));o.put("debt",amounts(l.debt));o.put("pot",Ledger.money(l.pot));o.put("chain",l.chain);o.put("dealer",l.dealer);o.put("streak",l.streak);o.put("diceVisible",l.diceVisible);o.put("logs",logs);return o.toString(2);
    }
    BigDecimal[] readAmounts(JSONArray a,boolean nonnegative)throws Exception{if(a.length()!=4)throw new Exception("金額陣列長度錯誤");BigDecimal[] b=new BigDecimal[4];for(int p=0;p<4;p++){b[p]=new BigDecimal(a.getString(p));if(nonnegative&&b[p].signum()<0)throw new Exception("欠款不可為負");}return b;}
    void load(String s)throws Exception{
        JSONObject o=new JSONObject(s);if(o.getInt("version")!=1)throw new Exception("不支援備份版本");Ledger n=new Ledger();String[] nn=new String[4];JSONArray na=o.getJSONArray("names");if(na.length()!=4)throw new Exception("玩家數錯誤");for(int p=0;p<4;p++){nn[p]=na.getString(p);if(nn[p].trim().isEmpty())throw new Exception("姓名空白");}
        n.base=new BigDecimal(o.getString("base"));n.rate=new BigDecimal(o.getString("rate"));n.pot=new BigDecimal(o.getString("pot"));n.balance=readAmounts(o.getJSONArray("balance"),false);n.debt=readAmounts(o.getJSONArray("debt"),true);n.chain=o.getInt("chain");n.dealer=o.getInt("dealer");n.streak=o.getInt("streak");n.diceVisible=o.getBoolean("diceVisible");JSONArray ll=o.getJSONArray("logs");
        if(n.chain< -1||n.chain>3||n.dealer<0||n.dealer>3||n.streak<0||n.streak>10000||n.base.signum()<0||n.rate.signum()<0||n.pot.signum()<0)throw new Exception("備份數值錯誤");
        BigDecimal sum=n.pot;for(int p=0;p<4;p++){sum=sum.add(n.balance[p]);if((n.chain<0||n.chain==p)&&n.debt[p].signum()!=0)throw new Exception("拉數狀態不一致");}if(sum.signum()!=0)throw new Exception("備份帳目不平衡");
        for(int i=0;i<ll.length();i++){JSONObject entry=ll.getJSONObject(i);LocalDate.parse(entry.getString("date"));entry.getString("text");readAmounts(entry.getJSONArray("delta"),false);}
        l=n;names=nn;logs=ll;
    }
    void save(){try{if(!getPreferences(0).edit().putString("state",state()).commit())toast("儲存失敗，請立即匯出備份");}catch(Exception e){toast(e.toString());}}
    void change(String desc,Runnable operation){
        String before=null;try{
            before=state();BigDecimal[] old=l.balance.clone();operation.run();JSONObject log=new JSONObject();log.put("date",LocalDate.now().toString());log.put("text",desc);BigDecimal[] delta=new BigDecimal[4];for(int p=0;p<4;p++)delta[p]=l.balance[p].subtract(old[p]);log.put("delta",amounts(delta));logs.put(log);undo.add(before);if(undo.size()>30)undo.remove(0);save();draw();
        }catch(Exception e){if(before!=null)try{load(before);}catch(Exception ignored){}toast("未記錄："+e.getMessage());}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();try{
        if(request==EXPORT){try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(state().getBytes(StandardCharsets.UTF_8));}toast("已匯出備份");}
        if(request==IMPORT){String s;try(InputStream in=getContentResolver().openInputStream(uri)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>10*1024*1024)throw new Exception("備份超過10MB");}s=out.toString("UTF-8");}String before=state();try{load(s);}catch(Exception e){load(before);throw e;}undo.clear();undo.add(before);save();draw();toast("匯入成功");}
    }catch(Exception e){toast("備份操作失敗："+e.getMessage());}}
}
