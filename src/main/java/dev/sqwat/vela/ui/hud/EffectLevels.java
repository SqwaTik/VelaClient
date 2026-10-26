package dev.sqwat.vela.ui.hud;
public final class EffectLevels {
    private EffectLevels(){}
    public static String suffix(int amplifier){
        if(amplifier<=0)return "";int level=amplifier+1;StringBuilder result=new StringBuilder(" ");
        int[] values={1000,900,500,400,100,90,50,40,10,9,5,4,1};String[] digits={"M","CM","D","CD","C","XC","L","XL","X","IX","V","IV","I"};
        for(int i=0;i<values.length;i++)while(level>=values[i]){result.append(digits[i]);level-=values[i];}return result.toString();
    }
}
