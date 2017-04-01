
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.blau.android.util.Util;

public class UtilTest {
    
    @Test
	public void notZero(){
		double d = 0D;
		assertFalse(Util.notZero(d));
		d = 0.0000001;
		assertTrue(Util.notZero(d));
	}
}