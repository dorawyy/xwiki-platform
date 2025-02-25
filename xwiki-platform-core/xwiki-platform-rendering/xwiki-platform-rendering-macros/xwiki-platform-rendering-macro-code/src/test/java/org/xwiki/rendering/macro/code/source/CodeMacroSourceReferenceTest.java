/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.rendering.macro.code.source;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link CodeMacroSourceReference}.
 * 
 * @version $Id$
 */
class CodeMacroSourceReferenceTest
{
    @Test
    void tostring()
    {
        assertEquals("type:reference", new CodeMacroSourceReference("type", "reference").toString());
    }

    @Test
    void equals()
    {
        assertTrue(new CodeMacroSourceReference("type", "reference")
            .equals(new CodeMacroSourceReference("type", "reference")));

        assertFalse(new CodeMacroSourceReference("type", "reference")
            .equals(new CodeMacroSourceReference("type", "reference2")));
        assertFalse(new CodeMacroSourceReference("type", "reference")
            .equals(new CodeMacroSourceReference("type2", "reference")));
        assertFalse(new CodeMacroSourceReference("type", "reference").equals(null));
        assertFalse(new CodeMacroSourceReference("type", "reference").equals("other"));
    }

    @Test
    void hasCode()
    {
        CodeMacroSourceReference reference1 = new CodeMacroSourceReference("type", "reference");
        CodeMacroSourceReference reference2 = new CodeMacroSourceReference("type", "reference");

        assertEquals(reference1.hashCode(), reference2.hashCode());
    }
}
