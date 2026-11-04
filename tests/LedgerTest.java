import hk.mahjong.ledger.Ledger;
import java.math.BigDecimal;
public class LedgerTest {
 static void eq(String expected,BigDecimal actual){if(new BigDecimal(expected).compareTo(actual)!=0)throw new AssertionError(expected+" != "+actual);}
 public static void main(String[] args){
  Ledger l=new Ledger();l.dealer=3;l.win(0,-1,13,true);eq("36",l.debt[1]);eq("38",l.debt[3]);
  // Published monetary example omits dealer bonuses. Suppress dealer's win bonus to isolate it.
  l=new Ledger();l.diceVisible=false;l.win(0,-1,13,true);eq("36",l.debt[1]);
  l.win(0,1,5,false);eq("74",l.debt[1]);eq("36",l.debt[2]);
  l.win(1,0,0,false);eq("37",l.balance[1].negate());eq("109",l.balance[0]);
  l=new Ledger();l.dealer=0;l.streak=3;l.win(2,-1,10,true);eq("44",l.debt[0]);eq("30",l.debt[1]);
  l=new Ledger();l.diceVisible=false;l.win(0,-1,13,true);l.win(1,-1,0,true);eq("18",l.balance[1].negate());eq("90",l.balance[0]);
  l=new Ledger();l.penalty(1);eq("10",l.pot);l.win(0,2,0,false);eq("10",l.balance[0]);eq("0",l.pot);
  l=new Ledger();l.diceVisible=false;l.win(0,1,0,false);l.win(0,1,0,false);l.win(0,1,0,false);eq("47.5",l.debt[1]);l.settle(1);eq("23.75",l.balance[0]);
  System.out.println("All engine scenarios passed");
 }
}
