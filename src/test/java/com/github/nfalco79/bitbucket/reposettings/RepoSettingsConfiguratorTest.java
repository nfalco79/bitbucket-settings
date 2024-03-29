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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.github.nfalco79.bitbucket.client.BitbucketCloudClient;
import com.github.nfalco79.bitbucket.client.model.BitbucketObject;
import com.github.nfalco79.bitbucket.client.model.BranchRestriction;
import com.github.nfalco79.bitbucket.client.model.BranchRestriction.Builder;
import com.github.nfalco79.bitbucket.client.model.GroupInfo;
import com.github.nfalco79.bitbucket.client.model.Permission;
import com.github.nfalco79.bitbucket.client.model.Repository;
import com.github.nfalco79.bitbucket.client.model.UserInfo;

public class RepoSettingsConfiguratorTest {

    private String workspace = "user1";

    @Test
    public void test_multiple_rule_matches() throws Exception {
        String repository = "prj1.prod.repo2";
        String group = "group2";

        RepoSettingsInfo configuration = new RepoSettingsInfo();
        configuration.setWorkspace(workspace);
        configuration.setAccessRules("/test-repository-permissions.json");
        configuration.setBranchRules("/test-branch-permissions.json");

        BitbucketCloudClient client = Mockito.mock(BitbucketCloudClient.class);
        Mockito.when(client.getRepositories(workspace)).thenReturn(Arrays.asList(new Repository(repository)));
        Map<GroupInfo, Permission> permissions = new HashMap<>();
        permissions.put(new GroupInfo(group), Permission.READ);
        Mockito.when(client.getGroupsPermissions(workspace, repository)).thenReturn(permissions);
        Mockito.when(client.getGroups(workspace)).thenReturn(Arrays.asList(new GroupInfo("group1"), new GroupInfo(group)));
        Mockito.when(client.getPermission(repository)).thenReturn(Permission.ADMIN);
        Mockito.when(client.getUser()).thenReturn(Mockito.mock(UserInfo.class));

        RepoSettingsConfigurator configurator = new RepoSettingsConfigurator(configuration, client);
        configurator.exec();

        Mockito.verify(client).updateGroupPermission(workspace, repository, group, Permission.WRITE);
    }

    @Test
    public void test_rule_override_for_min_approvals() throws Exception {
        String repository = "prj1.prod.repo2";
        String group = "group2";

        RepoSettingsInfo configuration = new RepoSettingsInfo();
        configuration.setWorkspace(workspace);
        configuration.setAccessRules("/test-repository-permissions.json");
        configuration.setBranchRules("/branch-min-approvals.json");
        configuration.setMinApprovals(3);

        BitbucketCloudClient client = Mockito.mock(BitbucketCloudClient.class);
        Mockito.when(client.getRepositories(workspace)).thenReturn(Arrays.asList(new Repository(repository)));
        Map<GroupInfo, Permission> permissions = new HashMap<>();
        permissions.put(new GroupInfo(group), Permission.READ);
        Mockito.when(client.getGroupsPermissions(workspace, repository)).thenReturn(permissions);
        Mockito.when(client.getGroups(workspace)).thenReturn(Arrays.asList(new GroupInfo("group1"), new GroupInfo(group)));
        Mockito.when(client.getPermission(repository)).thenReturn(Permission.ADMIN);
        Mockito.when(client.getUser()).thenReturn(Mockito.mock(UserInfo.class));
        Mockito.when(client.getBranchRestrictions(workspace, repository)) //
                .thenReturn(Arrays.asList(Builder.newMinApprovalsPermission("support/*", 3)));

        RepoSettingsConfigurator configurator = new RepoSettingsConfigurator(configuration, client);
        configurator.exec();
        Mockito.verify(client).updateBranchRestriction(eq(workspace), eq(repository), eq(Builder.newMinApprovalsPermission("support/*", 1)));
    }

    @Test
    public void verify_rule_override_does_not_trigger_changes_when_min_approvals_is_already_configured_as_expected() throws Exception {
        String repository = "prj1.prod.repo2";
        String group = "group2";

        RepoSettingsInfo configuration = new RepoSettingsInfo();
        configuration.setWorkspace(workspace);
        configuration.setAccessRules("/test-repository-permissions.json");
        configuration.setBranchRules("/branch-min-approvals.json");
        configuration.setMinApprovals(3);

        BitbucketCloudClient client = Mockito.mock(BitbucketCloudClient.class);
        Mockito.when(client.getRepositories(workspace)).thenReturn(Arrays.asList(new Repository(repository)));
        Map<GroupInfo, Permission> permissions = new HashMap<>();
        permissions.put(new GroupInfo(group), Permission.READ);
        Mockito.when(client.getGroupsPermissions(workspace, repository)).thenReturn(permissions);
        Mockito.when(client.getGroups(workspace)).thenReturn(Arrays.asList(new GroupInfo("group1"), new GroupInfo(group)));
        Mockito.when(client.getPermission(repository)).thenReturn(Permission.ADMIN);
        Mockito.when(client.getUser()).thenReturn(Mockito.mock(UserInfo.class));
        Mockito.when(client.getBranchRestrictions(workspace, repository)) //
                .thenReturn(Arrays.asList(Builder.newMinApprovalsPermission("support/*", 1)));

        RepoSettingsConfigurator configurator = new RepoSettingsConfigurator(configuration, client);
        configurator.exec();
        ArgumentCaptor<BranchRestriction> captor = ArgumentCaptor.forClass(BranchRestriction.class);
        Mockito.verify(client, atLeastOnce()).updateBranchRestriction(eq(workspace), eq(repository), captor.capture());
        String unexpectedKind = Builder.newMinApprovalsPermission("support/*", 0).getKind();
        Assertions.assertThat(captor.getAllValues()).noneMatch(perm -> perm.getKind().equalsIgnoreCase(unexpectedKind));
    }

    @Test
    public void test_exclude_rule() throws Exception {
        String repository = "repo3-deploy";
        String groupName = "group2";
        GroupInfo group2 = new GroupInfo(groupName);

        RepoSettingsInfo configuration = new RepoSettingsInfo();
        configuration.setWorkspace(workspace);
        configuration.setAccessRules("/test-repository-permissions-exclude.json");
        configuration.setBranchRules("/test-branch-permissions.json");

        BitbucketCloudClient client = Mockito.mock(BitbucketCloudClient.class);
        Mockito.when(client.getRepositories(workspace)).thenReturn(Arrays.asList(new Repository(repository)));
        Map<GroupInfo, Permission> permissions = new HashMap<>();
        permissions.put(group2, Permission.READ);
        Mockito.when(client.getGroupsPermissions(workspace, repository)).thenReturn(permissions);
        Mockito.when(client.getGroups(workspace)).thenReturn(Arrays.asList(new GroupInfo("group1"), group2));
        Mockito.when(client.getPermission(repository)).thenReturn(Permission.ADMIN);
        Mockito.when(client.getUser()).thenReturn(Mockito.mock(UserInfo.class));

        RepoSettingsConfigurator configurator = new RepoSettingsConfigurator(configuration, client);
        configurator.exec();

        Mockito.verify(client).deleteGroupPermission(workspace, repository, groupName);
        Mockito.verify(client, Mockito.never()).updateGroupPermission(workspace, repository, groupName, Permission.WRITE);
    }

    @Test
    public void test_user_unauthorized() throws Exception {
        String repository = "repo3-deploy";

        RepoSettingsInfo configuration = new RepoSettingsInfo();
        configuration.setWorkspace(workspace);

        BitbucketCloudClient client = Mockito.mock(BitbucketCloudClient.class);
        Mockito.when(client.getRepositories(workspace)).thenReturn(Arrays.asList(new Repository(repository)));
        Mockito.when(client.getUser()).thenReturn(Mockito.mock(UserInfo.class));
        Mockito.when(client.getPermission(repository)).thenReturn(Permission.NONE);

        RepoSettingsConfigurator configurator = new RepoSettingsConfigurator(configuration, client) {
            @Override
            protected List<BitbucketObject> processRepositoryPermission(String repoName) throws IOException {
                Assert.fail("Current user should not call authorized!");
                return Collections.emptyList();
            };
        };
        configurator.exec();
    }
}