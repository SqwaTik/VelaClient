package dev.sqwat.vela.module.setting;

import dev.sqwat.vela.ui.Lang;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NumberUnitsTest {
    @Test void unitsComeFromNameAndDescription(){
        assertEquals(NumberUnits.Unit.SECONDS,NumberUnits.infer("Окно повторов","Объединять повторы не старше, секунд"));
        assertEquals(NumberUnits.Unit.MILLIS,NumberUnits.infer("Длительность","Время жизни волны в миллисекундах"));
        assertEquals(NumberUnits.Unit.PERCENT,NumberUnits.infer("Порог брони, %","Остаток прочности"));
        assertEquals(NumberUnits.Unit.BLOCKS,NumberUnits.infer("Радиус","Радиус появления в блоках"));
        assertEquals(NumberUnits.Unit.DEGREES_PER_SECOND,NumberUnits.infer("Скорость облёта","Градусов в секунду"));
        assertEquals(NumberUnits.Unit.MESSAGES,NumberUnits.infer("История чата","Сколько сообщений хранит чат"));
        assertEquals(NumberUnits.Unit.NONE,NumberUnits.infer("Размер","Масштаб рук и предметов"));
    }
    @Test void minutesFromSixtySeconds(){
        String s=Lang.pick(" s"," с"),min=Lang.pick(" min"," мин");
        assertEquals("45"+s,NumberUnits.format(45,NumberUnits.Unit.SECONDS));
        assertEquals("1"+min,NumberUnits.format(60,NumberUnits.Unit.SECONDS));
        assertEquals("1"+min+" 30"+s,NumberUnits.format(90,NumberUnits.Unit.SECONDS));
        assertEquals("10"+min,NumberUnits.format(600,NumberUnits.Unit.SECONDS));
        assertEquals("0.8"+s,NumberUnits.format(.8,NumberUnits.Unit.SECONDS));
        assertEquals("12:30",NumberUnits.format(12.5,NumberUnits.Unit.HOURS));
    }
}
