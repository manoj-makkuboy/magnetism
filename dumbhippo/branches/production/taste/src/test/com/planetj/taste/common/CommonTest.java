/*
 * Copyright 2005 Sean Owen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.planetj.taste.common;

import com.planetj.taste.impl.TasteTestCase;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * <p>Tests common classes.</p>
 *
 * @author Sean Owen
 */
public final class CommonTest extends TasteTestCase {

	public void testException() {
		// Just make sure this all doesn't, ah, throw an exception
		final TasteException te1 = new TasteException();
		final TasteException te2 = new TasteException(te1);
		final TasteException te3 = new TasteException(te2.toString(), te2);
		final TasteException te4 = new TasteException(te3.toString());
		te4.printStackTrace(new PrintStream(new ByteArrayOutputStream()));
		te4.printStackTrace(new PrintWriter(new OutputStreamWriter(new ByteArrayOutputStream())));
	}

}
