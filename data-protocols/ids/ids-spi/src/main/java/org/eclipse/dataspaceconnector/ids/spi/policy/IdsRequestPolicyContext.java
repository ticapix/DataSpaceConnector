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

package org.eclipse.dataspaceconnector.ids.spi.policy;

import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;

/**
 * IDS policy context for provider-side request evaluation functions.
 */
public class IdsRequestPolicyContext {
    private final String correlationId;
    private final String clientConnectorId;
    private final ClaimToken claimToken;

    public IdsRequestPolicyContext(String clientConnectorId, String correlationId, ClaimToken claimToken) {
        this.clientConnectorId = clientConnectorId;
        this.correlationId = correlationId;
        this.claimToken = claimToken;
    }

    public String getClientConnectorId() {
        return clientConnectorId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public ClaimToken getClaimToken() {
        return claimToken;
    }
}
