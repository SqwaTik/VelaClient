package dev.sqwat.vela;

import dev.sqwat.vela.ui.render.AccountChrome;
import dev.sqwat.vela.ui.render.Rect;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Account layout is the single source for rendered bounds and pointer targets. */
class AccountLayoutTest {
    @Test void authTabsAndFieldsRemainInsideModal(){
        for(int[] size:new int[][]{{320,240},{854,480},{1280,800}}){
            var l=AccountChrome.layout(size[0],size[1]);var d=AccountChrome.authDialog(l);
            for(var r:new Rect[]{d.content(),d.input(),d.message(),d.primary(),d.cancel()})assertInside(d.panel(),r);
            if(d.panel().height()>=340){
                assertInside(d.panel(),AccountChrome.authTab(d,true));assertInside(d.panel(),AccountChrome.authTab(d,false));
                assertInside(d.panel(),AccountChrome.authClose(d));
                assertTrue(bottom(d.message())<d.primary().y());assertTrue(bottom(d.primary())<d.cancel().y());
            }
        }
    }
    @Test void profilePaneHasSeparateBoundsAndCollapsesOnSmallScreens() {
        var wide = AccountChrome.layout(1280, 800);
        assertTrue(wide.side() > 0);
        assertInside(wide.panel(), wide.details());
        assertInside(wide.details(), wide.profileAvatar());
        assertInside(wide.details(), wide.profileLogin());
        assertTrue(right(wide.list()) < wide.details().x());
        assertEquals(-1, wide.rowAt(wide.profileLogin().x(), wide.profileLogin().y(), 20, 0));
        for (int[] size : new int[][] {{320,240}, {480,320}, {610,424}})
            assertEquals(0, AccountChrome.layout(size[0], size[1]).side());
    }
    @Test void expandsBeyondOldPopoverWithoutGrowingUnbounded() {
        var large = AccountChrome.layout(1280, 800);
        assertTrue(large.width() > 380);
        assertTrue(large.height() > 350);
        assertTrue(large.width() <= 694 && large.height() <= 440);
        assertEquals(227,large.listWidth());
        assertEquals(640, large.x() + large.width() / 2);
        assertEquals(400, large.y() + large.height() / 2);
        assertEquals(large, AccountChrome.layout(1280, 800, 10000));
    }

    @Test void regionsStayInsideAndDoNotOverlapAtSupportedViewports() {
        for (int[] size : new int[][] {{320,240}, {480,320}, {610,424}, {854,480}, {1280,720}, {3840,2160}}) {
            var l = AccountChrome.layout(size[0], size[1]);
            assertInside(new Rect(0, 0, size[0], size[1]), l.panel());
            for (Rect r : new Rect[] {l.back(), l.search(), l.searchText(), l.list(), l.microsoft(), l.offline(), l.status()})
                assertInside(l.panel(), r);
            assertTrue(bottom(l.search()) < l.list().y());
            assertTrue(bottom(l.list()) <= l.status().y());
            if(l.side()==0)assertTrue(bottom(l.status()) <= l.microsoft().y());
            else assertTrue(l.status().x()>l.split());
            assertTrue(right(l.microsoft()) < l.offline().x());
            assertTrue(l.list().height() >= l.rowHeight());
            for (int index = 0; index < 20; index++) {
                var row = l.row(index, 0);
                assertInside(row.card(), row.avatar());
                assertInside(row.card(), row.name());
                assertInside(row.card(), row.login());
                assertInside(row.card(), row.delete());
                assertTrue(right(row.name()) < row.login().x());
                assertTrue(right(row.login()) < row.delete().x());
                assertFalse(row.login().contains(row.name().x(), row.name().y()));
            }
            var dialog = AccountChrome.dialog(l, 242);
            assertInside(l.panel(), dialog.panel());
            assertInside(dialog.panel(), dialog.primary());
            assertInside(dialog.panel(), dialog.cancel());
            assertTrue(right(dialog.cancel()) < dialog.primary().x());
        }
    }

    @Test void emptyAndManyRowsHaveBoundedScrollingAndExactHitTargets() {
        var l = AccountChrome.layout(800, 600);
        assertEquals(0, l.maxScroll(0));
        assertEquals(0, l.maxScroll(1));
        assertEquals(-1, l.rowAt(l.list().x(), l.list().y(), 0, 0));
        int count = 1000, max = l.maxScroll(count);
        assertTrue(max > 0);
        assertEquals(count - 1, l.rowAt(l.list().x() + 5, bottom(l.list()) - 1, count, max));
        assertEquals(-1, l.rowAt(l.list().x() + 5, bottom(l.list()), count, max));
        assertEquals(-1, l.rowAt(l.list().x() - 1, l.list().y() + 1, count, 0));
        assertEquals(-1, l.rowAt(l.list().x() + 5, l.list().y() + l.rowHeight() + 1, count, 0));
        for (int index : new int[] {0, 3, 99, 999}) {
            int scroll = l.reveal(index, 0, count);
            var row = l.row(index, scroll);
            assertTrue(row.card().y() >= l.list().y());
            assertTrue(bottom(row.card()) <= bottom(l.list()));
            assertEquals(index, l.rowAt(row.name().x(), row.name().y(), count, scroll));
            assertEquals(index, l.rowAt(row.login().x(), row.login().y(), count, scroll));
        }
        Rect start = l.scrollThumb(count, 0), end = l.scrollThumb(count, max);
        assertInside(l.scrollTrack(), start);
        assertInside(l.scrollTrack(), end);
        assertEquals(bottom(l.scrollTrack()), bottom(end));
        assertEquals(0, l.scrollFromPointer(l.scrollTrack().y(), 0, count));
        assertEquals(max, l.scrollFromPointer(bottom(l.scrollTrack()), 0, count));
    }

    @Test void filteredSelectionClampsInsteadOfReusingStaleRowIndex() {
        var l = AccountChrome.layout(610, 424);
        assertEquals(0, l.reveal(0, l.maxScroll(500), 1));
        assertEquals(0, l.reveal(-1, 123, 0));
        assertEquals(-1, l.rowAt(l.microsoft().x(), l.microsoft().y(), 500, 0));
    }

    @Test void rowSelectionNeverImpliesAuthenticationOrDeletion() {
        for (int[] size : new int[][] {{320,240}, {610,424}, {1280,800}}) {
            var l = AccountChrome.layout(size[0], size[1]);
            var r = l.row(0, 0);
            assertEquals(AccountChrome.RowAction.SELECT, AccountChrome.action(l, r.name().x(), r.name().y(), 10, 0, false));
            assertEquals(AccountChrome.RowAction.SELECT, AccountChrome.action(l, r.avatar().x(), r.avatar().y(), 10, 0, false));
            assertEquals(l.side()>0?AccountChrome.RowAction.SELECT:AccountChrome.RowAction.LOGIN, AccountChrome.action(l, r.login().x(), r.login().y(), 10, 0, false));
            assertEquals(AccountChrome.RowAction.DELETE, AccountChrome.action(l, r.delete().x(), r.delete().y(), 10, 0, false));
            assertEquals(AccountChrome.RowAction.SELECT, AccountChrome.action(l, r.login().x(), r.login().y(), 10, 0, true));
            assertEquals(AccountChrome.RowAction.SELECT, AccountChrome.action(l, r.delete().x(), r.delete().y(), 10, 0, true));
            assertEquals(AccountChrome.RowAction.NONE, AccountChrome.action(l, r.name().x(), l.microsoft().y(), 10, 0, false));
        }
    }

    @Test void everyDialogFitsAndKeepsInputErrorsAndActionsSeparate() {
        for (int[] size : new int[][] {{320,240}, {480,320}, {610,424}, {1280,800}}) {
            var l = AccountChrome.layout(size[0], size[1]);
            for (int height : new int[] {196, 222, 230, 236, 252}) {
                var d = AccountChrome.dialog(l, height);
                for (var r : new Rect[] {d.content(), d.input(), d.message(), d.primary(), d.cancel()}) assertInside(d.panel(), r);
                assertTrue(bottom(d.input()) < d.message().y());
                assertTrue(bottom(d.message()) < d.primary().y());
                assertTrue(bottom(l.searchText()) <= bottom(l.search()));
                assertTrue(right(l.searchText()) < l.clearSearch().x());
            }
        }
    }

    @Test void scrollingHitTestsUseTheDisplayedPositionIncludingPartialRows() {
        var l = AccountChrome.layout(480, 320);
        int count = 90;
        for (int scroll = 0; scroll <= l.maxScroll(count); scroll += 7) {
            for (int y = l.list().y(); y < bottom(l.list()); y++) {
                int index = l.rowAt(l.list().x() + 4, y, count, scroll);
                if (index >= 0) assertTrue(l.row(index, scroll).card().contains(l.list().x() + 4, y));
            }
            assertInside(l.scrollTrack(), l.scrollThumb(count, scroll));
        }
    }

    private static int right(Rect r) { return r.x() + r.width(); }
    private static int bottom(Rect r) { return r.y() + r.height(); }
    private static void assertInside(Rect outer, Rect inner) {
        assertTrue(inner.width() > 0 && inner.height() > 0, inner.toString());
        assertTrue(inner.x() >= outer.x() && inner.y() >= outer.y()
            && right(inner) <= right(outer) && bottom(inner) <= bottom(outer), outer + " contains " + inner);
    }
}
