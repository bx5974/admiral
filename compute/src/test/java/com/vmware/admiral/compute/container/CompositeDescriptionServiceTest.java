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

package com.vmware.admiral.compute.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.vmware.admiral.common.ManagementUriParts;
import com.vmware.admiral.compute.ComponentDescription;
import com.vmware.admiral.compute.ResourceType;
import com.vmware.admiral.compute.container.CompositeDescriptionService.CompositeDescription;
import com.vmware.admiral.compute.container.CompositeDescriptionService.CompositeDescriptionExpanded;
import com.vmware.admiral.compute.container.ContainerDescriptionService.ContainerDescription;
import com.vmware.xenon.common.Operation;
import com.vmware.xenon.common.ServiceDocument;
import com.vmware.xenon.common.UriUtils;
import com.vmware.xenon.common.Utils;

public class CompositeDescriptionServiceTest extends ComputeBaseTest {

    private ContainerDescription createdFirstContainer;
    private ContainerDescription createdSecondContainer;
    private CompositeDescription createdComposite;

    @Before
    public void setUp() throws Throwable {
        waitForServiceAvailability(CompositeDescriptionFactoryService.SELF_LINK);
    }

    @Before
    public void initObjects() throws Throwable {
        ContainerDescription firstContainer = new ContainerDescription();
        firstContainer.name = "testContainer";
        firstContainer.image = "registry.hub.docker.com/nginx";
        firstContainer._cluster = 1;
        firstContainer.maximumRetryCount = 1;
        firstContainer.privileged = true;
        firstContainer.affinity = new String[] { "cond1", "cond2" };
        firstContainer.customProperties = new HashMap<String, String>();
        firstContainer.customProperties.put("key1", "value1");
        firstContainer.customProperties.put("key2", "value2");

        createdFirstContainer = doPost(firstContainer, ContainerDescriptionService.FACTORY_LINK);

        ContainerDescription secondContainer = new ContainerDescription();
        secondContainer.name = "testContainer2";
        secondContainer.image = "registry.hub.docker.com/kitematic/hello-world-nginx";

        createdSecondContainer = doPost(secondContainer, ContainerDescriptionService.FACTORY_LINK);

        CompositeDescription composite = new CompositeDescription();
        composite.name = "testComposite";
        composite.customProperties = new HashMap<String, String>();
        composite.customProperties.put("key1", "value1");
        composite.customProperties.put("key2", "value2");
        composite.descriptionLinks = new ArrayList<String>();
        composite.descriptionLinks.add(createdFirstContainer.documentSelfLink);
        composite.descriptionLinks.add(createdSecondContainer.documentSelfLink);

        createdComposite = doPost(composite, CompositeDescriptionService.SELF_LINK);
    }

    @Test
    public void testContainerDescriptionServices() throws Throwable {
        verifyService(
                CompositeDescriptionFactoryService.class,
                CompositeDescription.class,
                (prefix, index) -> {
                    CompositeDescription containerDesc = new CompositeDescription();
                    containerDesc.name = prefix + "name" + index;
                    containerDesc.customProperties = new HashMap<>();

                    return containerDesc;
                },
                (prefix, serviceDocument) -> {
                    CompositeDescription contDesc = (CompositeDescription) serviceDocument;
                    assertTrue(contDesc.name.startsWith(prefix + "name"));
                });
    }

    @Test
    public void testGetCompositeDescription() throws Throwable {
        CompositeDescription[] result = new CompositeDescription[] { null };

        Operation getCompositeDesc = Operation.createGet(
                UriUtils.buildUri(host, createdComposite.documentSelfLink))
                .setCompletion(
                        (o, e) -> {
                            if (e != null) {
                                host.log("Can't get composite description.", Utils.toString(e));
                                host.failIteration(e);
                                return;
                            } else {
                                CompositeDescription cd = o.getBody(CompositeDescription.class);
                                result[0] = cd;
                                host.completeIteration();
                            }
                        });
        host.testStart(1);
        host.send(getCompositeDesc);
        host.testWait();

        CompositeDescription retrievedComposite = result[0];

        checkCompositesForEquality(createdComposite, retrievedComposite);
    }

    @Test
    public void testGetCompositeDescriptionExpanded() throws Throwable {
        CompositeDescriptionExpanded[] result = new CompositeDescriptionExpanded[] { null };

        Operation getCompositeDescExpanded = Operation.createGet(
                UriUtils.buildUri(host, createdComposite.documentSelfLink + ManagementUriParts.EXPAND_SUFFIX))
                .setCompletion(
                        (o, e) -> {
                            if (e != null) {
                                host.log("Can't get expanded composite description.", Utils.toString(e));
                                host.failIteration(e);
                                return;
                            } else {
                                CompositeDescriptionExpanded cdExpanded = o.getBody(CompositeDescriptionExpanded.class);
                                result[0] = cdExpanded;
                                host.completeIteration();
                            }
                        });
        host.testStart(1);
        host.send(getCompositeDescExpanded);
        host.testWait();

        CompositeDescriptionExpanded cdExpanded = result[0];

        checkCompositesForEquality(createdComposite, cdExpanded);
        assertNotNull(cdExpanded.componentDescriptions);
        assertEquals(createdComposite.descriptionLinks.size(),
                cdExpanded.componentDescriptions.size());

        checkRetrievedContainers(cdExpanded.componentDescriptions, createdFirstContainer,
                createdSecondContainer);
    }

    @Test
    public void testPutExpanded() throws Throwable {
        ContainerDescription container = new ContainerDescription();
        container.name = "container";
        container.image = "registry.hub.docker.com/kitematic/hello-world-nginx";
        container = doPost(container, ContainerDescriptionService.FACTORY_LINK);

        ComponentDescription containerComponent = new ComponentDescription();
        containerComponent.name = "container";
        container.name = "updated";
        containerComponent.updateServiceDocument(container);
        containerComponent.type = ResourceType.CONTAINER_TYPE.getContentType();

        CompositeDescription cd = new CompositeDescription();
        cd.name = "testComposite";
        cd = doPost(cd, CompositeDescriptionFactoryService.SELF_LINK);

       // Make PUT but as expanded state, so that components are also updated
        CompositeDescriptionExpanded cdUpdate = new CompositeDescriptionExpanded();
        cdUpdate.documentSelfLink = cd.documentSelfLink;
        cdUpdate.name = cd.name;
        cdUpdate.componentDescriptions = new ArrayList<>();
        cdUpdate.componentDescriptions.add(containerComponent);
        cdUpdate = doPut(cdUpdate);

        // Explicitly search for document to validate that the list returns the right document kind
        CompositeDescription foundCd = searchForDocument(CompositeDescription.class,
                cd.documentSelfLink);
        assertEquals(Utils.buildKind(CompositeDescription.class), foundCd.documentKind);

        container = getDocument(ContainerDescription.class, container.documentSelfLink);
        assertEquals("updated", container.name);
    }

    private void checkRetrievedContainers(List<ComponentDescription> retrievedContainers,
            ContainerDescription... createdContainers) {
        for (ContainerDescription createdContainer : createdContainers) {
            for (int i = 0; i < retrievedContainers.size(); i++) {
                ContainerDescription retrievedContainer = (ContainerDescription) retrievedContainers
                        .get(i).getServiceDocument();
                if (retrievedContainer.documentSelfLink.equals(createdContainer.documentSelfLink)) {
                    checkContainersForЕquality(createdContainer, retrievedContainer);
                    retrievedContainers.remove(i);
                    break;
                }
            }
        }

        assertEquals(0, retrievedContainers.size());
    }

    private void checkContainersForЕquality(ContainerDescription createdContainer,
            ServiceDocument retrievedContainerInput) {
        ContainerDescription retrievedContainer = (ContainerDescription) retrievedContainerInput;
        assertNotNull(createdContainer);
        assertNotNull(retrievedContainer);
        assertEquals(createdContainer.documentSelfLink, retrievedContainer.documentSelfLink);
        assertEquals(createdContainer.name, retrievedContainer.name);
        assertEquals(createdContainer.image, retrievedContainer.image);
        assertEquals(createdContainer._cluster, retrievedContainer._cluster);
        assertEquals(createdContainer.maximumRetryCount, retrievedContainer.maximumRetryCount);
        assertEquals(createdContainer.privileged, retrievedContainer.privileged);
        assertTrue(Arrays.equals(createdContainer.affinity, retrievedContainer.affinity));
        assertTrue(Arrays.equals(createdContainer.env, retrievedContainer.env));
        assertEquals(createdContainer.customProperties, retrievedContainer.customProperties);
        assertEquals(createdContainer.tenantLinks, retrievedContainer.tenantLinks);
    }

    private void checkCompositesForEquality(CompositeDescription createdComposite, CompositeDescription retrievedComposite) {
        assertNotNull(createdComposite);
        assertNotNull(retrievedComposite);
        assertEquals(createdComposite.documentSelfLink, retrievedComposite.documentSelfLink);
        assertEquals(createdComposite.name, retrievedComposite.name);
        assertEquals(createdComposite.customProperties, retrievedComposite.customProperties);
        assertEquals(createdComposite.descriptionLinks, retrievedComposite.descriptionLinks);
    }
}
