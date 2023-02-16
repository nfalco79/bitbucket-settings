package com.github.nfalco79.bitbucket.reposettings.filter;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

import com.github.nfalco79.bitbucket.client.ClientException;
import com.github.nfalco79.bitbucket.client.model.BranchRestriction;
import com.github.nfalco79.bitbucket.client.model.BranchRestriction.Builder;

public class UpdatePermissionIfNotExists implements UpdatePermission {
    private final Collection<BranchRestriction> currentBranchPermissions;
    private final String branchPattern;
    private final Collection<BranchRestriction> toApply;

    public UpdatePermissionIfNotExists(Collection<BranchRestriction> current, String branchPattern, Collection<BranchRestriction> bucket) {
        this.currentBranchPermissions = current;
        this.branchPattern = branchPattern;
        this.toApply = bucket;
    }

    @Override
    public void apply(BranchRestriction newPermission) throws IOException ,ClientException {
        // existing bitbucket branch permission of same type and pattern of newPermission
        BranchRestriction currentMatchingPermission = currentBranchPermissions.stream() //
                .filter(perm -> perm.getPattern().equals(branchPattern)) //
                .filter(perm -> perm.getKind().equals(newPermission.getKind())) //
                .findFirst() //
                .orElse(null);

        // looks permissions collected previously (they do not yet exists) and matches newPermission
        // the case is configuration override
        BranchRestriction otherPermission = toApply.stream() //
                .filter(apply -> Objects.nonNull(apply.getId())) //
                .filter(apply -> Objects.equals(newPermission.getId(), apply.getId())) //
                .findFirst() //
                .orElseGet(() -> toApply.stream() //
                        .filter(apply -> Objects.equals(newPermission.getPattern(), apply.getPattern())) //
                        .filter(apply -> Objects.equals(newPermission.getKind(), apply.getKind())) //
                        .findFirst() //
                        .orElse(null));

        if (currentMatchingPermission != null) {
            boolean requireMerge = !Objects.equals(currentMatchingPermission, newPermission);
            // there is already a defined branch restriction but different configuration, have to be update
            if (requireMerge) {
                newPermission.setId(currentMatchingPermission.getId());
                // if update was already planned to add than merge also these changes
                if (otherPermission == null) {
                    toApply.add(newPermission);
                } else {
                    Builder.merge(newPermission, otherPermission);
                }
            } else if (otherPermission != null) {
                // there is a change request by previous rule different
                // than current in bitbucket.
                // newPermission must merge otherPermission, this could mean
                // otherPermission matches current bitbucket permission, so we
                // remove from addition
                Builder.merge(newPermission, otherPermission);
                if (Objects.equals(currentMatchingPermission, otherPermission)) {
                    toApply.remove(otherPermission);
                }
            }
        } else {
            if (otherPermission == null) {
                toApply.add(newPermission);
            } else {
                Builder.merge(newPermission, otherPermission);
            }
        }
    }
}