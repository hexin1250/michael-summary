package michael.slf4j.demo.controller;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class GreetingControllerTest {
	GreetingController controller = null;
	
	@Before
	public void setup() {
		controller = new GreetingController();
	}

	@Test
	public void testIndex() {
		assertEquals("Hello World!", controller.index());
	}

}
