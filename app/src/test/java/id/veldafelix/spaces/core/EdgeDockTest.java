package id.veldafelix.spaces.core;
import org.junit.Test;
import static org.junit.Assert.*;
public class EdgeDockTest {
    @Test public void leftIsFlush() { EdgeDock.Placement p=EdgeDock.place("left",.4f,1600,720,18,52);assertEquals(0,p.x);assertEquals(18,p.width);assertEquals(52,p.height); }
    @Test public void rightIsFlush() { EdgeDock.Placement p=EdgeDock.place("right",.4f,1600,720,18,52);assertEquals(1600,p.x+p.width); }
    @Test public void topIsHorizontalAndFlush() { EdgeDock.Placement p=EdgeDock.place("top",.5f,1600,720,18,52);assertEquals(0,p.y);assertEquals(52,p.width);assertEquals(18,p.height); }
    @Test public void nearestLeftSnaps() { assertEquals("left",EdgeDock.snap(10,350,1600,720,18,52).edge); }
    @Test public void nearestRightSnaps() { assertEquals("right",EdgeDock.snap(1590,350,1600,720,18,52).edge); }
    @Test public void nearestTopSnaps() { assertEquals("top",EdgeDock.snap(800,10,1600,720,18,52).edge); }
    @Test public void bottomDropChoosesSideNotFloating() { EdgeDock.Placement p=EdgeDock.snap(600,710,1600,720,18,52);assertEquals("left",p.edge);assertEquals(0,p.x);assertTrue(p.y+p.height<=720); }
    @Test public void outOfRangeOffsetsClamped() { assertEquals(0,EdgeDock.place("left",-9,1600,720,18,52).y);assertEquals(668,EdgeDock.place("left",9,1600,720,18,52).y); }
    @Test public void invalidPreferenceSafe() { assertEquals("right",EdgeDock.place("invalid",Float.NaN,1600,720,18,52).edge);assertEquals(.2f,EdgeDock.fraction(Float.POSITIVE_INFINITY),0); }
    @Test public void rotationRecomputesWithinBounds() { EdgeDock.Placement p=EdgeDock.place("top",.95f,720,1600,18,52);assertTrue(p.x+p.width<=720);assertEquals(0,p.y); }
    @Test public void tinyDisplayStillFits() { EdgeDock.Placement p=EdgeDock.place("top",1,10,8,18,52);assertEquals(10,p.width);assertEquals(8,p.height);assertEquals(0,p.x);assertEquals(0,p.y); }
    @Test public void eachEdgeRecognizesOnlyInwardSwipes() {
        assertTrue(EdgeDock.inwardSwipe("left",30,2,12));assertTrue(EdgeDock.inwardSwipe("right",-30,2,12));assertTrue(EdgeDock.inwardSwipe("top",2,30,12));
        assertFalse(EdgeDock.inwardSwipe("right",30,0,12));assertFalse(EdgeDock.inwardSwipe("top",30,2,12));
    }
    @Test public void topRespectsReservedSystemBand() {
        EdgeDock.Placement p=EdgeDock.place("top",.5f,1600,720,18,52,25);
        assertEquals(25,p.y);assertEquals(18,p.height);assertEquals(52,p.width);
    }
    @Test public void topInsetClampsOnTinyDisplays() {
        assertEquals(0,EdgeDock.place("top",.5f,10,8,18,52,999).y);
        assertEquals(0,EdgeDock.place("top",.5f,1600,720,18,52,-9).y);
    }
    @Test public void sideTabsIgnoreTopInsetAndStayFlush() {
        EdgeDock.Placement p=EdgeDock.place("right",.4f,1600,720,18,52,25);
        assertEquals(1600,p.x+p.width);assertEquals(Math.round(.4f*(720-52)),p.y);
    }

}
