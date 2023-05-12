import org.junit.Before;
import org.junit.Test;
import junit.framework.TestCase;
import org.secuso.privacyfriendlyactivitytracker.ModuleTesting.Pedometer;

import static org.junit.Assert.assertEquals;

public class PedometerTest extends TestCase {
    private Pedometer pedometer;

    @Before
    public void setUp() {
        pedometer = new Pedometer();
    }

    @Test
    public void testAddStep_incrementsStepCount() {
        pedometer.addStep();
        assertEquals(1, pedometer.getSteps());
    }

    @Test
    public void testReset_setsStepCountToZero() {
        pedometer.addStep();
        pedometer.reset();
        assertEquals(0, pedometer.getSteps());
    }
}