/*
 * Copyright (c) 2016 VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with separate copyright notices
 * and license terms. Your use of these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package com.vmware.admiral.host;

import java.util.ArrayList;

import com.vmware.admiral.common.ManagementUriParts;
import com.vmware.admiral.common.util.ConfigurationUtil;
import com.vmware.admiral.compute.EnvironmentMappingService;
import com.vmware.admiral.compute.container.ContainerHostDataCollectionService;
import com.vmware.admiral.compute.container.GroupResourcePlacementService;
import com.vmware.admiral.compute.container.HostContainerListDataCollection.HostContainerListDataCollectionFactoryService;
import com.vmware.admiral.compute.container.HostNetworkListDataCollection.HostNetworkListDataCollectionFactoryService;
import com.vmware.admiral.compute.container.SystemContainerDescriptions;
import com.vmware.admiral.service.common.AbstractInitialBootService;
import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.ServiceDocument;

/**
 * Initial boot service for creating system default documents for the common module.
 */
public class ComputeInitialBootService extends AbstractInitialBootService {
    public static final String SELF_LINK = ManagementUriParts.CONFIG + "/compute-initial-boot";

    @Override
    public void handlePost(Operation post) {
        ArrayList<ServiceDocument> states = new ArrayList<>();
        states.add(SystemContainerDescriptions.buildCoreAgentContainerDescription());
        states.addAll(EnvironmentMappingService.getDefaultMappings());
        initInstances(Operation.createGet(null), false, false,
                states.toArray(new ServiceDocument[states.size()]));

        states = new ArrayList<>();
        states.add(ContainerHostDataCollectionService.buildDefaultStateInstance());
        states.add(HostContainerListDataCollectionFactoryService.buildDefaultStateInstance());
        states.add(HostNetworkListDataCollectionFactoryService.buildDefaultStateInstance());
        states.add(GroupResourcePlacementService.buildDefaultResourcePool());

        if (!ConfigurationUtil.isEmbedded()) {
            states.add(GroupResourcePlacementService.buildDefaultStateInstance());
        }

        initInstances(post, states.toArray(new ServiceDocument[states.size()]));
    }
}
