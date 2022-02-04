/*
 * Copyright 2021 DevOps Team
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.nfalco79.bitbucket.reposettings.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import com.github.nfalco79.bitbucket.reposettings.util.SelectorUtils;

public class SelectorUtilsTest {

    @Test
    public void match_pattern_test() throws Exception {
        assertTrue(SelectorUtils.match("ab*", "abcd"));
        assertFalse(SelectorUtils.match("ab*", "axcd"));
        assertTrue(SelectorUtils.match("aB*", "Abcd", false));
        assertTrue("Equal strings with the same lenght without star should match", SelectorUtils.match("ab", "ab"));
        assertFalse("Different strings with the same lenght without star shouldn't match", SelectorUtils.match("ab", "ac"));
        assertFalse("Different strings with different lenght without star shouldn't match", SelectorUtils.match("ab", "abg"));
    }

    @Test
    public void match_negate_pattern_test() throws Exception {
        assertFalse("String matches but is negated", SelectorUtils.match("!abz*", "abzxop"));
        assertTrue("String doesn't match but is negated", SelectorUtils.match("!abz*", "abxx"));
        assertTrue("Different character but is negated", SelectorUtils.match("!a", "b"));
        assertFalse("Same string but negated", SelectorUtils.match("!cy", "cy"));
    }
}