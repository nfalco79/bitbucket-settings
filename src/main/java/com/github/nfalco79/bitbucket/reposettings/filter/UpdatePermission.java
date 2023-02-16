package com.github.nfalco79.bitbucket.reposettings.filter;

import java.io.IOException;

import com.github.nfalco79.bitbucket.client.ClientException;
import com.github.nfalco79.bitbucket.client.model.BranchRestriction;

@FunctionalInterface
public interface UpdatePermission {
    void apply(BranchRestriction p) throws IOException, ClientException;
}