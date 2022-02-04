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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.nfalco79.bitbucket.client.BitbucketCloudClient;
import com.github.nfalco79.bitbucket.client.ClientException;
import com.github.nfalco79.bitbucket.client.Credentials;
import com.github.nfalco79.bitbucket.client.Credentials.CredentialsBuilder;
import com.github.nfalco79.bitbucket.client.model.BitbucketObject;
import com.github.nfalco79.bitbucket.client.model.BranchRestriction;
import com.github.nfalco79.bitbucket.client.model.BranchRestriction.Builder;
import com.github.nfalco79.bitbucket.reposettings.rule.AccessRule;
import com.github.nfalco79.bitbucket.reposettings.rule.BranchPermissionRule;
import com.github.nfalco79.bitbucket.reposettings.rule.RepositoryAccessRule;
import com.github.nfalco79.bitbucket.reposettings.util.RulesReader;
import com.github.nfalco79.bitbucket.reposettings.util.SelectorUtils;
import com.github.nfalco79.bitbucket.reposettings.util.WebhookUtil;
import com.github.nfalco79.bitbucket.client.model.GroupInfo;
import com.github.nfalco79.bitbucket.client.model.Permission;
import com.github.nfalco79.bitbucket.client.model.UserInfo;
import com.github.nfalco79.bitbucket.client.model.UserPermission;
import com.github.nfalco79.bitbucket.client.model.Webhook;

/**
 * Configurator for every supported settings of any BB repository.
 */
public class RepoSettingsConfigurator {

    @FunctionalInterface
    public interface UpdatePermission {
        void apply(BranchRestriction p) throws IOException, ClientException;
    }

    private RepoSettingsInfo configuration;
    private BitbucketCloudClient client;
    private RulesReader rulesReader;

    private Logger log;
    private String workspace;

    /**
     * RepoSettingsConfigurator constructor which requires repo settings info.
     *
     * @param repoInfo the object containing the settings
     */
    public RepoSettingsConfigurator(RepoSettingsInfo repoInfo) {
        this.configuration = repoInfo;
        this.workspace = repoInfo.getWorkspace();
        this.rulesReader = new RulesReader(repoInfo.getAccessRules(), repoInfo.getBranchRules());
    }

    /**
     * For test purpose only
     * @param repoInfo the object containing the settings
     * @param client a bitbucket client instance
     */
    /*package*/ RepoSettingsConfigurator(RepoSettingsInfo repoInfo, BitbucketCloudClient client) {
        this(repoInfo);
        this.client = client;
    }

    /**
     * RepoSettingsConfigurator main execution method.
     *
     * @throws IOException error occurs when resource read issue
     */
    public void exec() throws IOException, IllegalArgumentException {
        // Get all the FX BB repositories
        if (client == null) {
            client = new BitbucketCloudClient(buildCredentials());
            client.setDryRun(configuration.isDryRun());
        }

        log = Logger.getLogger("app");

        if (client.getUser() == null) {
            throw new IllegalArgumentException("Bad credentials for user " + configuration.getUsername());
        }

        Collection<String> repositories = client.getRepositories(workspace).stream() //
                // keep repository that at least one matches one filter
                .filter(repo -> configuration.getFilter().stream() //
                        .anyMatch(f -> SelectorUtils.match(f, repo.getSlug()))) //
                // keep repository that at least matches one project filter
                .filter(repo -> configuration.getProjects().isEmpty() || configuration.getProjects().contains(repo.getProject().getKey())) //
                .map(repo -> repo.getSlug()) //
                .collect(Collectors.toSet());

        if (repositories.isEmpty()) {
            log.severe("No repository matches filter & project");
        }

        for (String repo : repositories) {
            log = Logger.getLogger(repo);
            log.log(Level.INFO, "Processing repository {0}", repo);

            boolean canSetup = checkSettingsPermission(repo);
            if (canSetup) {
                // Setup user and group access
                Collection<BitbucketObject> usersAndGroups = processRepositoryPermission(repo);

                // Set Branch permissions section (error if any already exist)
                processBranchPermissions(repo, usersAndGroups);

                // Set Jenkins webhook
                processWebhook(repo);
            } else {
                log.log(Level.SEVERE, "Cannot setup repository {0}. Missing admin permission", repo);
            }
        }
    }

    private boolean checkSettingsPermission(String repo) throws ClientException {
        Permission privilege = client.getPermission(repo);
        return privilege.equals(Permission.ADMIN);
    }

    /**
     * Creates a ServerInfo object containing the user credentials declared.
     *
     * @return the created ServerInfo
     */
    private Credentials buildCredentials() {
        return configuration.isOAuth2() ?
                CredentialsBuilder.oauth2(configuration.getUsername(), configuration.getPassword())
                :
                CredentialsBuilder.appPassword(configuration.getUsername(), configuration.getPassword());
    }

    /**
     * Update the User and Group Access settings of a Bitbucket repository and
     * get the groups with access right.
     *
     * @param repoName the repository to update
     *
     * @return the list of all the groups within the repository that have now
     *         access
     * @throws IOException error occurs when resource read issue
     * @throws ClientException if any error communicating to bitbucket occurs
     */
    protected Collection<BitbucketObject> processRepositoryPermission(String repoName) throws IOException {
        // Do not update
        if (configuration.isOnlyBranches()) {
            if (configuration.isDebug()) {
                log.info("Skip repository user/group access");
            }

            // Groups that already have access (slug and actual privilege)
            Map<GroupInfo, Permission> groupsPermission = client.getGroupsPermissions(workspace, repoName);
            return new ArrayList<>(groupsPermission.keySet());
        }

        // Read repository permission rules
        List<RepositoryAccessRule> repoRules = rulesReader.getRepositoryRules();

        // Groups and Users that will be the present with access right
        List<BitbucketObject> allowed = new ArrayList<>();

        // Check if there is at least an access rule that is satisfied for this repository
        List<RepositoryAccessRule> accessRules = repoRules.stream() //
                .filter(rule -> rule.accept(repoName)) //
                .collect(Collectors.toList());
        if (!accessRules.isEmpty()) {
            List<RepositoryAccessRule> notInheritedRules = accessRules.stream() //+
                    .filter(rule -> !rule.isInherited()) //
                    .collect(Collectors.toList());
            // Cannot have more than one matching rules without inheritance
            if (notInheritedRules.size() > 1) {
                throw new IllegalArgumentException("Repository pattern matches multiple independent access rules: " + notInheritedRules);
            } else if (notInheritedRules.size() == 1) { // Between all, consider only the most specific rule
                accessRules.retainAll(notInheritedRules);
            }
            allowed.addAll(usersPermission(repoName, accessRules));
            allowed.addAll(groupsPermission(repoName, accessRules));
        }

        return allowed;
    }

    // Update groups permissions for this repository based on its matching rules
    private Collection<GroupInfo> groupsPermission(String repoName, List<RepositoryAccessRule> accessRules) throws IOException {
        List<GroupInfo> allowed = new LinkedList<>();

        // Groups that already have access (slug and actual privilege)
        Map<GroupInfo, Permission> groupsPermission = client.getGroupsPermissions(workspace, repoName);
        Collection<GroupInfo> allGroups = client.getGroups(workspace);

        Map<GroupInfo, Permission> newPermissions = new HashMap<>();
        for (GroupInfo group : allGroups) {
            // Compare every group rule with every pattern from matching rules
            for (RepositoryAccessRule accessRule : accessRules) {
                for (AccessRule groupRule : accessRule.getGroups()) {
                    if (groupRule.accept(group.getName())) { // grant access
                        Permission permission = groupRule.getPrivilege();
                        Permission currentPermission = newPermissions.get(group);
                        if (currentPermission == null || currentPermission.compareTo(permission) < 0) {
                            newPermissions.put(group, permission);
                        }
                    }
                }
            }

            // Check correctness of group that already has permission
            if (groupsPermission.containsKey(group) && !newPermissions.containsKey(group)) {
                // group does not match any rule
                newPermissions.put(group, Permission.NONE); // group to remove
            }
        }

        for (Entry<GroupInfo, Permission> newPermission : newPermissions.entrySet()) {
            GroupInfo group = newPermission.getKey();
            Permission permission = newPermission.getValue();
            if (permission != Permission.NONE) {
                if (groupsPermission.get(group) != permission) {
                    log.log(Level.INFO, //
                            "Granting access right for group: {0} with privilege: {1}", //
                            new String[] { //
                                           group.getName(),
                                           permission.toString()
                            });
                    client.updateGroupPermission(workspace, repoName, group.getSlug(), permission);
                }
                allowed.add(group);
            } else {
                log.log(Level.INFO, "Deleting access right for group: {0}", group.getName());
                client.deleteGroupPermission(workspace, repoName, group.getSlug());
            }
        }

        return allowed;
    }

    private List<UserInfo> usersPermission(String repoName, List<RepositoryAccessRule> accessRules) throws IOException {
        List<UserInfo> allowed = new LinkedList<>();

        // permission map that contains user name to change with
        // greater right that comes from all matching accessRules
        Map<String, Permission> userPermissions = new HashMap<>();
        for (RepositoryAccessRule accessRule : accessRules) {
            for (AccessRule r : accessRule.getUsers()) {
                Permission rulePrivilege = r.getPrivilege();
                String username = r.getPattern();

                UserInfo user = client.getUser(username);
                if (user == null) {
                    log.log(Level.WARNING, "User {0} not found", username);
                    continue;
                }

                UserPermission userPermission = client.getUserPermission(workspace, repoName, username);

                Permission permission = userPermission.getPermission();
                if (permission != rulePrivilege) {
                    if (rulePrivilege.ordinal() < permission.ordinal()) {
                        log.log(Level.WARNING, "User {0} has higher permission than configured.", username);
                    }
                    Permission currentPermission = userPermissions.get(user.getUUID());
                    if (currentPermission == null || currentPermission.compareTo(permission) < 0) {
                        // Access should be granted to this user but is not allowed now
                        // or access for this user is updated but not revoked, still allowed
                        userPermissions.put(user.getUUID(), rulePrivilege);
                        allowed.add(user);
                    }
                } else {
                    allowed.add(user);
                }
            }
        }
        for (Entry<String, Permission> userPermission : userPermissions.entrySet()) {
            client.updateUserPermission(workspace, repoName, userPermission.getKey(), userPermission.getValue());
        }

        return allowed;
    }

    /**
     * Add the Branch Permissions settings of a Bitbucket repository.
     *
     * @param repo
     *      the repository to update
     *
     * @param granted
     *      the BB groups and users list that have access to the repository
     *
     * @throws IOException error occurs when resource read issue
     * @throws ClientException if any error communicating to bitbucket occurs
     */
    protected void processBranchPermissions(String repo, Collection<BitbucketObject> granted) throws ClientException, IOException {
        List<BranchRestriction> toApply = new LinkedList<>();

        // Read branch-permissions.json
        List<BranchPermissionRule> branchRules = rulesReader.getBranchPermissions().stream() //
                .filter(r -> r.accept(repo)) //
                .collect(Collectors.toList());

        List<BranchRestriction> branchPermissions = client.getBranchRestrictions(workspace, repo);

        for (BranchPermissionRule branchRule : branchRules) {
            String branchPattern = branchRule.getBranchPattern();

            UpdatePermission ifNotExists = newPermission -> {
                List<BranchRestriction> currentMatchingPermissions = branchPermissions.stream() //
                        .filter(r -> r.getPattern().equals(branchPattern)) //
                        .filter(r -> r.getKind().equals(newPermission.getKind())) //
                        .collect(Collectors.toList());

                BranchRestriction otherPermission = toApply.stream() //
                        .filter(apply -> Objects.nonNull(apply.getId())) //
                        .filter(apply -> Objects.equals(newPermission.getId(), apply.getId())) //
                        .findFirst() //
                        .orElseGet(() -> toApply.stream() //
                                .filter(apply -> Objects.equals(newPermission.getPattern(), apply.getPattern())) //
                                .filter(apply -> Objects.equals(newPermission.getKind(), apply.getKind())) //
                                .findFirst() //
                                .orElse(null));

                if (!currentMatchingPermissions.isEmpty()) {
                    // if a branch restriction already exists than update it
                    BranchRestriction match = currentMatchingPermissions.stream() //
                            .filter(r -> !r.getUsers().containsAll(newPermission.getUsers()) || !r.getGroups().containsAll(newPermission.getGroups())) //
                            .findFirst() //
                            .orElse(null);
                    // if there is already a defined branch restriction but with different configuration, have to be update
                    if (match != null) {
                        newPermission.setId(match.getId());
                        // if update was already planned to add than merge also these changes
                        if (otherPermission == null) {
                            toApply.add(newPermission);
                        } else {
                            Builder.merge(newPermission, otherPermission);
                        }
                    }
                } else {
                    if (otherPermission == null) {
                        toApply.add(newPermission);
                    } else {
                        Builder.merge(newPermission, otherPermission);
                    }
                }
            };

            // Write access
            Set<UserInfo> users = filterUsers.apply(granted) //
                    .filter(grantedUser -> branchRule.getUsers().stream() //
                                .filter(rule -> rule.isWriteAccess()) //
                                .anyMatch(rule -> rule.accept(grantedUser.getUUID()))) //
                    .collect(Collectors.toSet());
            Set<GroupInfo> groups = filterGroups.apply(granted) //
                    .filter(grantedGroup -> branchRule.getGroups().stream() //
                            .filter(rule -> rule.isWriteAccess()) //
                            .anyMatch(rule -> rule.accept(grantedGroup.getName()))) //
                    .collect(Collectors.toSet());
            ifNotExists.apply(Builder.newPushPermission(branchPattern, users, groups));

            // Merge via pull request
            users = filterUsers.apply(granted) //
                    .filter(grantedUser -> branchRule.getUsers().stream() //
                            .anyMatch(rule -> rule.accept(grantedUser.getUUID()))) //
                    .collect(Collectors.toSet());
            groups = filterGroups.apply(granted) //
                    .filter(grantedGroup -> branchRule.getGroups().stream() //
                            .anyMatch(rule -> rule.accept(grantedGroup.getName()))) //
                    .collect(Collectors.toSet());
            ifNotExists.apply(Builder.newMergePermission(branchPattern, users, groups));

            // Deleting this branch is not allowed
            ifNotExists.apply(Builder.newDeletePermission(branchPattern));

            // Rewriting branch history is not allowed
            ifNotExists.apply(Builder.newForcePushPermission(branchPattern));

            // Check the last commit for at least N successful build and no failed builds
            ifNotExists.apply(Builder.newSucessBuildsPermission(branchPattern, configuration.getSuccessBuilds()));

            // Check for at least N approvals
            ifNotExists.apply(Builder.newMinApprovalsPermission(branchPattern, configuration.getApprovals()));

            //  Check that no changes are requested
            ifNotExists.apply(Builder.newRequireNoChanges(branchPattern));

            // Reset requested changes when the source branch is modified
            ifNotExists.apply(Builder.newResetPROnChange(branchPattern));

            // Check for unresolved pull request tasks
            ifNotExists.apply(Builder.newRequireTasksCompletion(branchPattern));
        }

        toApply.forEach(p -> {
            if (configuration.isDebug()) {
                log.log(Level.INFO, "Adding permission {0} for branch: {1}", new String[] {p.getKind(), p.getPattern()});
            }
            try {
                client.updateBranchRestriction(workspace, repo, p);
            } catch (ClientException e) {
            }
        });
    }

    private Function<Collection<BitbucketObject>, Stream<GroupInfo>> filterGroups = (credentials) ->
        credentials.stream() //
            .filter(c -> "group".equals(c.getType())) //
            .map(GroupInfo.class::cast);

    private Function<Collection<BitbucketObject>, Stream<UserInfo>> filterUsers = (credentials) ->
        credentials.stream() //
            .filter(c -> "user".equals(c.getType()))//
            .map(UserInfo.class::cast);

    protected void processWebhook(String repo) throws ClientException {
        Webhook webhook = WebhookUtil.DEFAULT;
        List<Webhook> webhooks = client.getWebhooks(workspace, repo, WebhookUtil.JENKINS_WEBHOOKS_NAMES);
        if (webhooks.stream().noneMatch(webhook::equals)) {
            webhook.setUrl(String.format(webhook.getUrl(), configuration.getWebHookHostname()));
            if (webhooks.size() > 0) {
                webhook.setUUID(webhooks.get(0).getUUID());
                client.updateWebhook(workspace, repo, webhook);
                if (configuration.isDebug()) {
                    log.log(Level.INFO, "Update webhook {0} id {1}", new String[] {webhook.getDescription(), webhook.getUUID()});
                }
            } else {
                client.addWebHook(workspace, repo, webhook);
                if (configuration.isDebug()) {
                    log.log(Level.INFO, "Set webhook {0}", webhook.getDescription());
                }
            }
        }

        // remove duplicates
        webhooks.stream().skip(1).forEach(hook -> {
            try {
                client.deleteWebhook(workspace, repo, hook.getUUID());
                log.log(Level.INFO, "Deleted webhook {0}", webhook.getDescription());
            } catch (ClientException e) {
            }
        });
    }

}