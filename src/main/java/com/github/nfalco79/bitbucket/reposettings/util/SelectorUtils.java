package com.github.nfalco79.bitbucket.reposettings.util;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
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

/**
 * <p>
 * This is a utility class used by selectors and DirectoryScanner. The
 * functionality more properly belongs just to selectors, but unfortunately
 * DirectoryScanner exposed these as protected methods. Thus we have to support
 * any subclasses of DirectoryScanner that may access these methods. (Revised
 * only for ANT patterns)
 * </p>
 * <p>
 * This is a Singleton.
 * </p>
 *
 * @author Arnout J. Kuiper <a href="mailto:ajkuiper@wxs.nl">ajkuiper@wxs.nl</a>
 * @author Magesh Umasankar
 * @author <a href="mailto:bruce@callenish.com">Bruce Atherton</a>
 *
 *         use {@code java.nio.file.Files.walkFileTree()} and related classes
 */
public final class SelectorUtils {

    /**
     * Private Constructor
     */
    private SelectorUtils() {
    }

    /**
     * Tests whether or not a string matches against a pattern. The pattern may
     * contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param pattern The pattern to match against. Must not be
     *        <code>null</code>.
     * @param str The string which must be matched against the pattern. Must not
     *        be <code>null</code>.
     * @return <code>true</code> if the string matches against the pattern, or
     *         <code>false</code> otherwise.
     */
    public static boolean match(String pattern, String str) {
        return match(pattern, str, true);
    }

    /**
     * Tests whether or not a string matches against a list of pattern. The pattern may
     * contain two special characters:<br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @param patterns a list of patterns to match against. Must not be
     *        <code>null</code>.
     * @param str The string which must be matched against the pattern. Must not
     *        be <code>null</code>.
     * @return <code>true</code> if the string matches against the pattern, or
     *         <code>false</code> otherwise.
     */
    public static boolean match(String[] patterns, String str) {
        boolean isAccepted = false;
        for (String pattern : patterns) {
            if (!pattern.startsWith("!")) {
                isAccepted |= match(pattern, str);
            } else {
                isAccepted &= match(pattern, str);
            }
        }
        return isAccepted;
    }

    /**
     * Tests whether or not a string matches against a pattern. The pattern may
     * contain three special characters:
     * <ul>
     * <li>'*' means zero or more characters</li>
     * <li>'?' means one and only one character</li>
     * <li>'!' means not matches</li>
     * </ul>
     *
     * @param pattern The pattern to match against. Must not be
     *        <code>null</code>.
     * @param str The string which must be matched against the pattern. Must not
     *        be <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed case
     *        sensitively.
     * @return <code>true</code> if the string matches against the pattern, or
     *         <code>false</code> otherwise.
     */
    public static boolean match(String pattern, String str, boolean isCaseSensitive) {
        boolean negate = false;
        if (pattern.startsWith("!")) {
            pattern = pattern.substring(1);
            negate = true;
        }
        char[] patArr = pattern.toCharArray();
        char[] strArr = str.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;
        char ch;

        boolean containsStar = false;
        for (char aPatArr : patArr) {
            if (aPatArr == '*') {
                containsStar = true;
                break;
            }
        }

        if (!containsStar) {
            // No '*'s, so we make a shortcut
            if (patIdxEnd != strIdxEnd) {
                return negate ? true : false; // Pattern and string do not have the same size
            }
            for (int i = 0; i <= patIdxEnd; i++) {
                ch = patArr[i];
                if (ch != '?' && !equals(ch, strArr[i], isCaseSensitive)) {
                    return negate ? true : false; // Character mismatch
                }
            }
            return negate ? false : true; // String matches against pattern
        }

        if (patIdxEnd == 0) {
            return negate ? false : true; // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        // CHECKSTYLE_OFF: InnerAssignment
        while ((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd)
        // CHECKSTYLE_ON: InnerAssignment
        {
            if (ch != '?' && !equals(ch, strArr[strIdxStart], isCaseSensitive)) {
                return negate ? true : false; // Character mismatch
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return negate ? true : false;
                }
            }
            return negate ? false : true;
        }

        // Process characters after last star
        // CHECKSTYLE_OFF: InnerAssignment
        while ((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd)
        // CHECKSTYLE_ON: InnerAssignment
        {
            if (ch != '?' && !equals(ch, strArr[strIdxEnd], isCaseSensitive)) {
                return negate ? true : false; // Character mismatch
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return negate ? true : false;
                }
            }
            return negate ? false : true;
        }

        // process pattern between stars. padIdxStart and patIdxEnd point
        // always to a '*'.
        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (patArr[i] == '*') {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // Two stars next to each other, skip the first one.
                patIdxStart++;
                continue;
            }

            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - patIdxStart - 1);
            int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop: for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    ch = patArr[patIdxStart + j + 1];
                    if (ch != '?' && !equals(ch, strArr[strIdxStart + i + j], isCaseSensitive)) {
                        continue strLoop;
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return negate ? true : false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        // All characters in the string are used. Check if only '*'s are left
        // in the pattern. If so, we succeeded. Otherwise failure.
        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (patArr[i] != '*') {
                return negate ? true : false;
            }
        }

        return negate ? false : true;
    }

    /**
     * Tests whether two characters are equal.
     */
    private static boolean equals(char c1, char c2, boolean isCaseSensitive) {
        if (c1 == c2) {
            return true;
        }
        if (!isCaseSensitive) {
            // NOTE: Try both upper case and lower case as done by
            // String.equalsIgnoreCase()
            if (Character.toUpperCase(c1) == Character.toUpperCase(c2)
                    || Character.toLowerCase(c1) == Character.toLowerCase(c2)) {
                return true;
            }
        }
        return false;
    }

}