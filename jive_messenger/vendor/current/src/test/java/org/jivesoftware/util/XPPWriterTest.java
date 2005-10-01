/**
 * $RCSfile$
 * $Revision: 247 $
 * $Date: 2004-11-09 14:50:00 -0500 (Tue, 09 Nov 2004) $
 *
 * Copyright (C) 1999-2003 CoolServlets, Inc. All rights reserved.
 *
 * This software is the proprietary information of CoolServlets, Inc.
 * Use is subject to license terms.
 */
package org.jivesoftware.util;

import junit.framework.TestCase;
import org.dom4j.Document;

import java.io.FileReader;
import java.io.FileWriter;

/**
 * <p>Test the writing of dom4j documents using the XPP serializer.</p>
 *
 * @author Iain Shigeoka
 */
public class XPPWriterTest extends TestCase {
    /**
     * <p>Create a new test with the given name.</p>
     *
     * @param name The name of the test
     */
    public XPPWriterTest(String name){
        super(name);
    }

    /**
     * <p>Run a standard config document through a round trip and compare.</p>
     */
    public void testRoundtrip(){
        // NOTE: disabling this test case until we get resources working again.
        /*
        try {
            Document doc = XPPReader.parseDocument(new FileReader("../conf/jive-messenger.xml"),this.getClass());
            XPPWriter.write(doc, new FileWriter("../conf/xmpp_writer_test_copy.xml"));
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        */
    }
}
