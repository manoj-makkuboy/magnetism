package com.dumbhippo.server;

import com.dumbhippo.server.Dummy;

import junit.framework.TestCase;

/* useless comment */
public class DummyTest extends TestCase {

	public void testMultiplyByThree() {
		Dummy dummy = new Dummy();
		int v = dummy.multiplyByThree(3);
		assertEquals(v, 9);
		
		System.err.println("asserting bogus stuff");
		assert (2 == 5);
		System.err.println("done asserting bogus stuff");
	}

}
