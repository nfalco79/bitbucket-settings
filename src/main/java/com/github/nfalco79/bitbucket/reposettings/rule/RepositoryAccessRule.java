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

import java.util.ArrayList;
import java.util.List;

import com.github.nfalco79.bitbucket.reposettings.util.SelectorUtils;

/**
 * Include the details for "User and group access" section.
 */
public class RepositoryAccessRule {

    private String repositoryPatterns;
    private List<AccessRule> users = new ArrayList<>();
    private List<AccessRule> groups = new ArrayList<>();
    private boolean inherited = true;
    private String comment;

    public List<AccessRule> getGroups() {
        return groups;
    }

    public void setGroups(List<AccessRule> groups) {
        this.groups = groups;
    }

    /**
     * Get the repoPattern.
     *
     * @return the repoPattern
     */
    public String getRepositoryPatterns() {
        return repositoryPatterns;
    }

    public void setRepositoryPatterns(String repositoryPatterns) {
        this.repositoryPatterns = repositoryPatterns;
    }

    public boolean accept(String repositoryName) {
        return SelectorUtils.match(repositoryPatterns.split(","), repositoryName);
    }

    public List<AccessRule> getUsers() {
        return users;
    }

    public void setUsers(List<AccessRule> users) {
        this.users = users;
    }

    public boolean isInherited() {
        return inherited;
    }

    public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return repositoryPatterns;
    }
}