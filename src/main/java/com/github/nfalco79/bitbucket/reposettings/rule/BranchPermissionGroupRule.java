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
package com.github.nfalco79.bitbucket.reposettings.rule;

import com.github.nfalco79.bitbucket.reposettings.util.SelectorUtils;

/**
 * Include the details of a group relative to a branch for "Branch permissions" section.
 */
public class BranchPermissionGroupRule {

    private Boolean writeAccess;
    private Boolean mergePR;
    private String pattern;

    public Boolean isWriteAccess() {
        return writeAccess;
    }

    public void setWriteAccess(Boolean writeAccess) {
        this.writeAccess = writeAccess;
    }

    public Boolean isMergePR() {
        return mergePR;
    }

    public void setMergePR(Boolean mergePR) {
        this.mergePR = mergePR;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean accept(String groupName) {
        return SelectorUtils.match(getPattern(), groupName);
    }

    @Override
    public String toString() {
        return pattern + " write: " + writeAccess + " merge: " + mergePR;
    }
}