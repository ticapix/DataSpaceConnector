/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.policy.engine.model;

import org.eclipse.dataspaceconnector.policy.engine.PolicyEvaluator;
import org.eclipse.dataspaceconnector.policy.model.*;
import org.junit.jupiter.api.Test;

import static org.eclipse.dataspaceconnector.policy.engine.model.PolicyTestFunctions.createLiteralAtomicConstraint;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class PolicyEvaluatorScenarioTest {

    /**
     * Atomic constraint function models the case where an asset can only be used in the EU.
     */
    @Test
    void geospatialPermissionConstraint() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("spatial", "EU");

        Permission permission = Permission.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().permission(permission).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().permissionFunction("spatial", (operator, value, p) -> "EU".equals(value)).build();
        assertTrue(evaluator.evaluate(policy).valid());
    }

    /**
     * Atomic constraint function models the case where an asset is used by a client only authorized in the EU, and there is a permission that requires the
     * client to be authorized in both the EU and US.
     */
    @Test
    void geospatialPermissionInTwoRegionsConstraint() {
        AtomicConstraint constraintEu = createLiteralAtomicConstraint("spatial", "EU");
        AtomicConstraint constraintUs = createLiteralAtomicConstraint("spatial", "US");

        AndConstraint andConstraint = AndConstraint.Builder.newInstance().constraint(constraintEu).constraint(constraintUs).build();
        Permission permission = Permission.Builder.newInstance().constraint(andConstraint).build();

        Policy policy = Policy.Builder.newInstance().permission(permission).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().permissionFunction("spatial", (operator, value, p) -> "EU".equals(value)).build();
        assertFalse(evaluator.evaluate(policy).valid());
    }

    /**
     * Atomic constraint function models the case where an asset is intended to be used in the EU, but there is a prohibition to do so.
     */
    @Test
    void geospatialProhibitionConstraint() {
        AtomicConstraint constraint = createLiteralAtomicConstraint("spatial", "EU");

        Prohibition prohibition = Prohibition.Builder.newInstance().constraint(constraint).build();
        Policy policy = Policy.Builder.newInstance().prohibition(prohibition).build();

        PolicyEvaluator evaluator = PolicyEvaluator.Builder.newInstance().prohibitionFunction("spatial", (operator, value, pr) -> "EU".equals(value)).build();
        assertFalse(evaluator.evaluate(policy).valid());
    }


}
