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

import com.github.nfalco79.bitbucket.client.model.Permission;
import com.github.nfalco79.bitbucket.reposettings.util.SelectorUtils;

/**
 * Include the details relative to group or user for "User and group access"
 * section.
 */
public class AccessRule {

    private Permission privilege;
    private String pattern;

    public Permission getPrivilege() {
        return privilege;
    }

    public void setPrivilege(Permission privilege) {
        this.privilege = privilege;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean accept(String group) {
        boolean isAccepted = true;
        // group pattern could contain multiple patterns with negation
        for (String pattern : getPattern().split(",")) {
            isAccepted &= SelectorUtils.match(pattern, group);
        }
        return isAccepted;
    }

    @Override
    public String toString() {
        return pattern + ' ' + privilege;
    }
}