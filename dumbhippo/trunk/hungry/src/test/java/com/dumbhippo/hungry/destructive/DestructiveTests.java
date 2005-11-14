package com.dumbhippo.hungry.destructive;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.dumbhippo.hungry.util.CheatSheet;
import com.dumbhippo.hungry.util.PackageSuite;
import com.dumbhippo.hungry.util.WebServices;

public class DestructiveTests {
    public static Test suite() {
    	CheatSheet cs = CheatSheet.getReadWrite();
    	System.out.println("Erasing database contents...");
    	cs.nukeDatabase();
    	System.out.println("Done.");
    	
    	WebServices ws = new WebServices();
    	String text = ws.getTextPOST("/dologin",
    			"email", "foo@localhost");
    	
    	System.out.println("got login: " + text);
        
        TestSuite suite = new PackageSuite("Destructive tests", DestructiveTests.class);
        return suite;
    }
}
