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

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.nfalco79.bitbucket.reposettings.CLI;
import com.github.nfalco79.bitbucket.reposettings.RepoSettingsInfo;

public class CLITest {

    @Test
    public void verify_options() throws Exception {
        String file = "file";
        RepoSettingsInfo settings = CLI.parseOptions(new String[] {"-branchRules", file, "-u", "name", "-p", "pwd", "-accessRules", file, "-f", "*", "-workspace", "TRK"});
        Assertions.assertThat(settings.getBranchRules()).contains(file);
    }

    @Test(expected = IllegalArgumentException.class)
    public void verify_required_options() throws Exception {
        String file = "file";
        CLI.parseOptions(new String[] {"-u", "username", "-p", "pwd", "-accessRules", file, "-f", "*"});
    }

}