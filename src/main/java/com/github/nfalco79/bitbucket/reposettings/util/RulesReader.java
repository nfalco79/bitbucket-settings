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
package com.github.nfalco79.bitbucket.reposettings.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.nfalco79.bitbucket.reposettings.rule.BranchPermissionRule;
import com.github.nfalco79.bitbucket.reposettings.rule.RepositoryAccessRule;

/**
 * Utility class to read JSON files.
 */
public class RulesReader {

    private String repositoryAccess;
    private String branchPermission;
    private ObjectMapper objectMapper;

    public RulesReader(String repositoryAccess, String branchPermission) {
        this.repositoryAccess = repositoryAccess;
        this.branchPermission = branchPermission;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Read the JSON file containing the specifications to update any BB
     * repository's "User and group access" section.
     *
     * @return the access rules for every repository
     *
     * @throws IOException if couldn't read file
     */
    public List<RepositoryAccessRule> getRepositoryRules() throws IOException {
        try (InputStream inputStream = getStream(repositoryAccess)) {
            if (inputStream == null) {
                throw new FileNotFoundException("File " + repositoryAccess + " not found");
            }

            return objectMapper.readValue(inputStream, new TypeReference<List<RepositoryAccessRule>>() {});
        }
    }

    private InputStream getStream(String resource) throws IOException {
        try {
            return new URL(resource).openStream();
        } catch (MalformedURLException e) {
            if (new File(resource).isFile()) {
                return new FileInputStream(resource);
            }
        }
        return getClass().getResourceAsStream(resource);
    }

    /**
     * Read the JSON file containing the specifications to update any BB
     * repository's "Branch permissions" section.
     *
     * @return the branch rules for any repository
     *
     * @throws IOException if couldn't read file
     */
    public List<BranchPermissionRule> getBranchPermissions() throws IOException {
        try (InputStream inputStream = getStream(branchPermission)) {
            if (inputStream == null) {
                throw new FileNotFoundException("File " + branchPermission + " not found");
            }

            return objectMapper.readValue(inputStream, new TypeReference<List<BranchPermissionRule>>() {});
        }
    }
}
