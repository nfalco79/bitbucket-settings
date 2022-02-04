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
package com.github.nfalco79.bitbucket.reposettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Include all the repository's details in order to update its settings.
 */
public class RepoSettingsInfo {

    private List<String> filter = Arrays.asList("*");
    private List<String> projects = Collections.emptyList();
    private String workspace;
    private boolean onlyBranches;
    private boolean debug;
    private boolean oauth2;
    private String username;
    private String password;
    private String webHookHostname;
    private boolean dryRun;
    private String accessRules = "/repository-permissions.json";
    private String branchRules = "/branch-permissions.json";
    private int successBuilds = 1;
    private int approvals = 2;

    public List<String> getFilter() {
        return filter;
    }

    public void setFilter(List<String> repos) {
        this.filter = new ArrayList<String>(repos);
    }

    public boolean isOnlyBranches() {
        return onlyBranches;
    }

    public void setOnlyBranches(boolean onlyBranches) {
        this.onlyBranches = onlyBranches;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccessRules() {
        return accessRules;
    }

    public void setAccessRules(String accessRules) {
        this.accessRules = accessRules;
    }

    public String getBranchRules() {
        return branchRules;
    }

    public void setBranchRules(String branchRules) {
        this.branchRules = branchRules;
    }

    public List<String> getProjects() {
        return projects;
    }

    public void setProjects(List<String> projects) {
        this.projects = projects;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getWebHookHostname() {
        return webHookHostname;
    }

    public void setWebHookHostname(String webHookHostname) {
        this.webHookHostname = webHookHostname;
    }

    public void setOAuth2(boolean enabled) {
        oauth2 = enabled;
    }

    public boolean isOAuth2() {
        return oauth2;
    }

    public int getSuccessBuilds() {
        return successBuilds ;
    }

    public int getApprovals() {
        return approvals ;
    }

    public void setSuccessBuilds(int successBuilds) {
        this.successBuilds = successBuilds;
    }

    public void setApprovals(int approvals) {
        this.approvals = approvals;
    }
}