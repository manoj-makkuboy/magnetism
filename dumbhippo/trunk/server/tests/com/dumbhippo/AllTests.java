package com.dumbhippo;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
    public static Test suite() {
        TestSuite suite = new TestSuite("All tests");
        suite.addTest(new TestSuite(FilesystemTests.class));
        return suite;
    }
}
