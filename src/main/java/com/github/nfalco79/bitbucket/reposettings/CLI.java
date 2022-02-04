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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.logging.LogManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Command line parser for reposettings arguments.
 */
public class CLI {

    private static final String ONLY_BRANCHES_OPT = "onlyBranches";
    private static final String REPOSITORY_WEBHOOK_URL_OPT = "webHook";
    private static final String REPOSITORY_FILTER_OPT = "f";
    private static final String REPOSITORY_FILTER_LONG_OPT = "filter";
    private static final String REPOSITORY_WORKSPACE_OPT = "workspace";
    private static final String REPOSITORY_PROJECT_OPT = "prj";
    private static final String REPOSITORY_PROJECT_LONG_OPT = "project";
    private static final String DEBUG_OPT = "debug";
    private static final String OAUTH2_CLIENTID_LONG_OPT = "clientId";
    private static final String OAUTH2_SECRET_LONG_OPT = "clientSecret";
    private static final String PR_APPROVALS_COUNT_LONG_OPT = "approvals";
    private static final String PR_SUCCESS_BUILDS_LONG_OPT = "successBuilds";
    private static final String USER_OPT = "u";
    private static final String USER_LONG_OPT = "username";
    private static final String PWD_OPT = "p";
    private static final String PWD_LONG_OPT = "password";
    private static final String ACCESS_RULE_OPT = "accessRules";
    private static final String BRANCH_RULE_OPT = "branchRules";
    private static final String DRY_RUN_OPT = "dryRun";

    /**
     * Main method.
     *
     * @param args The command line arguments
     * @throws Exception if any error occur
     **/
    public static void main(String[] args) throws Exception {
        if (args != null && args.length != 0) {
            LogManager lm = LogManager.getLogManager();
            try (InputStream is = CLI.class.getResourceAsStream("/log.properties")) {
                lm.readConfiguration(is);
            }

            RepoSettingsInfo repoInfo = parseOptions(args);
            new RepoSettingsConfigurator(repoInfo).exec();
        } else { // if no arguments, print help
            printHelp();
        }
    }

    public static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("reposettings", createOptions());
    }

    public static RepoSettingsInfo parseOptions(String[] args) throws IOException {
        Options options = createOptions();
        RepoSettingsInfo repoInfo = new RepoSettingsInfo();

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption(REPOSITORY_FILTER_OPT)) {
                repoInfo.setFilter(Arrays.asList(line.getOptionValues(REPOSITORY_FILTER_OPT)));
            }
            if (line.hasOption(REPOSITORY_PROJECT_OPT)) {
                repoInfo.setProjects(Arrays.asList(line.getOptionValues(REPOSITORY_PROJECT_OPT)));
            }
            if (!line.hasOption(REPOSITORY_FILTER_OPT) && !line.hasOption(REPOSITORY_PROJECT_OPT)) {
                throw new ParseException("At least one between " + REPOSITORY_FILTER_OPT + " and " + REPOSITORY_PROJECT_OPT + " is required");
            }
            if (line.hasOption(ONLY_BRANCHES_OPT)) {
                repoInfo.setOnlyBranches(true);
            }
            if (line.hasOption(DEBUG_OPT)) {
                repoInfo.setDebug(true);
            }
            if (line.hasOption(OAUTH2_CLIENTID_LONG_OPT) && line.hasOption(OAUTH2_SECRET_LONG_OPT)) {
                repoInfo.setOAuth2(true);
                repoInfo.setUsername(line.getOptionValue(OAUTH2_CLIENTID_LONG_OPT));
                repoInfo.setPassword(line.getOptionValue(OAUTH2_SECRET_LONG_OPT));
            } else if (line.hasOption(USER_OPT) && line.hasOption(PWD_OPT)) {
                repoInfo.setOAuth2(false);
                repoInfo.setUsername(line.getOptionValue(USER_OPT));
                repoInfo.setPassword(line.getOptionValue(PWD_OPT));
            } else {
                throw new IllegalArgumentException("Reason: Missing required option: username/username or clientId/clientSecret");
            }
            repoInfo.setWebHookHostname(line.getOptionValue(REPOSITORY_WEBHOOK_URL_OPT));
            repoInfo.setAccessRules(line.getOptionValue(ACCESS_RULE_OPT));
            repoInfo.setBranchRules(line.getOptionValue(BRANCH_RULE_OPT));
            repoInfo.setWorkspace(line.getOptionValue(REPOSITORY_WORKSPACE_OPT));
            repoInfo.setDryRun(line.hasOption(DRY_RUN_OPT));
        } catch (ParseException exp) {
            throw new IllegalArgumentException("Parsing failed.  Reason: " + exp.getMessage());
        }

        // if username has been specified than password is required
        if (repoInfo.getUsername() != null && repoInfo.getPassword() == null) {
            while (repoInfo.getPassword() == null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

                String pwd;
                // Reading data using readLine
                if (System.console() != null) {
                    pwd = new String(System.console().readPassword("Password: "));
                } else {
                    // eclipse console case
                    pwd = reader.readLine();
                }
                repoInfo.setPassword(pwd);
            }
        }
        return repoInfo;
    }

    private static Options createOptions() {
        Options options = new Options();

        options.addOption(ONLY_BRANCHES_OPT, "update only branches permissions");
        options.addOption(DEBUG_OPT, "print debugging information");

        Builder optBuilder = Option.builder(REPOSITORY_FILTER_OPT);
        optBuilder.longOpt(REPOSITORY_FILTER_LONG_OPT);
        optBuilder.valueSeparator(',');
        optBuilder.argName("filter repositories");
        optBuilder.desc("repositories list to update settings seperated by ','");
        optBuilder.hasArgs();
        options.addOption(optBuilder.build());

        optBuilder = Option.builder(REPOSITORY_WORKSPACE_OPT);
        optBuilder.argName("bitbucket workspace");
        optBuilder.desc("the bitbucket workspace (called also owner)");
        optBuilder.hasArgs();
        optBuilder.required(true);
        options.addOption(optBuilder.build());

        optBuilder = Option.builder(REPOSITORY_PROJECT_OPT);
        optBuilder.longOpt(REPOSITORY_PROJECT_LONG_OPT);
        optBuilder.valueSeparator(',');
        optBuilder.argName("filter repository projects");
        optBuilder.desc("project list in which the repositories are, seperated by ','");
        optBuilder.hasArgs();
        options.addOption(optBuilder.build());

        optBuilder = Option.builder(USER_OPT);
        optBuilder.longOpt(USER_LONG_OPT);
        optBuilder.argName(USER_LONG_OPT);
        optBuilder.desc("username to access Bitbucket");
        optBuilder.hasArg();
        options.addOption(optBuilder.build());

        optBuilder = Option.builder(REPOSITORY_WEBHOOK_URL_OPT);
        optBuilder.argName("hostname");
        optBuilder.desc("hostname used to register the Bitbucket cloud web-hook");
        optBuilder.hasArg();
        options.addOption(optBuilder.build());

        optBuilder = Option.builder(PWD_OPT);
        optBuilder.longOpt(PWD_LONG_OPT);
        optBuilder.argName(PWD_LONG_OPT);
        optBuilder.desc("password to access Bitbucket");
        optBuilder.hasArg();
        options.addOption(optBuilder.build());

        optBuilder = Option.builder();
        optBuilder.longOpt(OAUTH2_CLIENTID_LONG_OPT);
        optBuilder.argName("key");
        optBuilder.desc("The client ID issued to the client during the Application registration process");
        optBuilder.hasArg();
        options.addOption(optBuilder.build());

        optBuilder = Option.builder();
        optBuilder.longOpt(OAUTH2_SECRET_LONG_OPT);
        optBuilder.argName("secret");
        optBuilder.desc("The client secret issued to the client during the Application registration process");
        optBuilder.hasArg();
        options.addOption(optBuilder.build());

        optBuilder = Option.builder(ACCESS_RULE_OPT);
        optBuilder.desc("JSON file or URL with repository access rules");
        optBuilder.hasArg();
        optBuilder.type(File.class);
        optBuilder.required(true);
        options.addOption(optBuilder.build());

        optBuilder = Option.builder(BRANCH_RULE_OPT);
        optBuilder.desc("JSON file or URL with branch permission rules");
        optBuilder.hasArg();
        optBuilder.type(File.class);
        optBuilder.required(true);
        options.addOption(optBuilder.build());

        optBuilder = Option.builder();
        optBuilder.desc("The minimal success builds to allow a pull request to be merged");
        optBuilder.longOpt(PR_SUCCESS_BUILDS_LONG_OPT);
        optBuilder.hasArg();
        optBuilder.argName("success builds");
        optBuilder.type(Integer.class);
        options.addOption(optBuilder.build());

        optBuilder = Option.builder();
        optBuilder.desc("The number of review approvals to allow a pull request to be merged");
        optBuilder.longOpt(PR_APPROVALS_COUNT_LONG_OPT);
        optBuilder.hasArg();
        optBuilder.argName("minimal approvals");
        optBuilder.type(Integer.class);
        options.addOption(optBuilder.build());

        optBuilder = Option.builder(DRY_RUN_OPT);
        optBuilder.desc("Will log all REST call instead to perform the real operation");
        optBuilder.hasArg(false);
        options.addOption(optBuilder.build());

        return options;
    }

}
