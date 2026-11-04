package hk.mahjong.ledger;
import java.math.BigDecimal;
import java.util.Arrays;

/** Pure calculation engine. All amounts are exact decimals; no implicit rounding. */
public class Ledger {
    public BigDecimal base=new BigDecimal("10"), rate=new BigDecimal("2");
    public BigDecimal[] balance=zeros(), debt=zeros();
    public BigDecimal pot=BigDecimal.ZERO;
    public int chain=-1, dealer=0, streak=0;
    public boolean diceVisible=true;
    public static BigDecimal[] zeros(){ BigDecimal[] a=new BigDecimal[4]; Arrays.fill(a,BigDecimal.ZERO); return a; }
    public static String money(BigDecimal x){return x.stripTrailingZeros().toPlainString();}
    public int bonus(){return 2*streak+1;}
    public BigDecimal amount(int fan){return base.add(rate.multiply(BigDecimal.valueOf(fan)));}
    public void transfer(int from,int to,BigDecimal n){balance[from]=balance[from].subtract(n); balance[to]=balance[to].add(n);}
    public void settle(int halfPlayer){
        if(chain<0)return;
        for(int p=0;p<4;p++) if(p!=chain){
            BigDecimal due=debt[p];
            if(p==halfPlayer)due=due.divide(new BigDecimal("2"));
            transfer(p,chain,due);
        }
        debt=zeros(); chain=-1;
    }
    public void win(int winner,int loser,int fan,boolean self){
        if(winner<0||winner>3||fan<0||(!self&&(loser<0||loser>3||loser==winner)))throw new IllegalArgumentException("食胡資料不正確");
        int oldDealer=dealer;
        int add=bonus();
        if(chain>=0&&chain!=winner)settle(self||loser==chain ? winner : -1);
        if(chain<0)chain=winner;
        for(int p=0;p<4;p++) if(p!=winner&&(self||p==loser)){
            int extra=0;
            if(winner==oldDealer)extra=diceVisible?add:0;
            else if(p==oldDealer)extra=add;
            BigDecimal n=amount(fan+extra);
            debt[p]=debt[p].multiply(new BigDecimal("1.5")).add(n);
        }
        if(pot.signum()>0){balance[winner]=balance[winner].add(pot);pot=BigDecimal.ZERO;}
        if(winner==oldDealer)streak++;else{dealer=(dealer+1)%4;streak=0;}
    }
    public void penalty(int p){balance[p]=balance[p].subtract(base);pot=pot.add(base);}
    public BigDecimal[] projected(){BigDecimal[] b=balance.clone();if(chain>=0)for(int p=0;p<4;p++)if(p!=chain){b[p]=b[p].subtract(debt[p]);b[chain]=b[chain].add(debt[p]);}return b;}
}
