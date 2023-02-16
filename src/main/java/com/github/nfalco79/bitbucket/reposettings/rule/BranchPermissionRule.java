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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.github.nfalco79.bitbucket.reposettings.util.SelectorUtils;

/**
 * Include the details to update the "Branch permissions" section of any BB
 * repository.
 */
public class BranchPermissionRule {

    private String repositoryPatterns;
    @JsonAlias("branchPattern")
    private String branchPatterns;
    private Integer minApprovals;
    private Integer successBuilds;
    private List<BranchPermissionUserRule> users = new LinkedList<>();
    private List<BranchPermissionGroupRule> groups = new LinkedList<>();

    public List<BranchPermissionGroupRule> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public void setGroups(List<BranchPermissionGroupRule> groups) {
        this.groups = groups;
    }

    public List<BranchPermissionUserRule> getUsers() {
        return Collections.unmodifiableList(users);
    }

    public void setUsers(List<BranchPermissionUserRule> users) {
        this.users = users;
    }

    public String getBranchPatterns() {
        return branchPatterns;
    }

    public void setBranchPatterns(String branchPatterns) {
        this.branchPatterns = branchPatterns;
    }

    public String getRepositoryPatterns() {
        return repositoryPatterns;
    }

    public void setRepositoryPatterns(String repositoryPatterns) {
        this.repositoryPatterns = repositoryPatterns;
    }

    public boolean accept(String repository) {
        return SelectorUtils.match(getRepositoryPatterns(), repository);
    }

    @Override
    public String toString() {
        return repositoryPatterns + " -> " + branchPatterns;
    }

    public Integer getMinApprovals() {
        return minApprovals;
    }

    public void setMinApprovals(Integer minApprovals) {
        this.minApprovals = minApprovals;
    }

    public Integer getSuccessBuilds() {
        return successBuilds;
    }

    public void setSuccessBuilds(Integer successBuilds) {
        this.successBuilds = successBuilds;
    }
}