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

/**
 * Include the user details for "Branch permissions" section.
 */
public class BranchPermissionUserRule {

    private Boolean writeAccess;
    private Boolean mergePR;
    private String uuid;
    private String username;

    /**
     * If this user has write access.
     *
     * @return true if user has write access, else false
     */
    public Boolean isWriteAccess() {
        return writeAccess;
    }

    public void setWriteAccess(Boolean writeAccess) {
        this.writeAccess = writeAccess;
    }

    /**
     * If this user can merge PRs.
     *
     * @return true if user can merge PRs, else false
     */
    public Boolean isMergePR() {
        return mergePR;
    }

    public void setMergePR(Boolean mergePR) {
        this.mergePR = mergePR;
    }

    /**
     * Get the uuid which identifies a BB user.
     *
     * @return the uuid, else null
     */
    public String getUUID() {
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public boolean accept(String userId) {
        return (uuid != null ? uuid : username).equals(userId);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return (username == null ? uuid : username) + " write:" + writeAccess + " merge:" + mergePR;
    }
}