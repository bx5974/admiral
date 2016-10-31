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

package com.vmware.admiral.compute;

import java.util.List;

import com.vmware.admiral.compute.container.CompositeComponentRegistry;
import com.vmware.admiral.compute.content.Binding;
import com.vmware.xenon.common.ServiceDocument;
import com.vmware.xenon.common.Utils;

public class ComponentDescription {

    private transient ServiceDocument component;
    public String componentJson;
    public String type;

    public String name;
    public List<Binding> bindings;

    public ComponentDescription(ServiceDocument component, String type, String name,
            List<Binding> bindings) {
        this.component = component;
        this.componentJson = Utils.toJson(component);
        this.type = type;
        this.name = name;
        this.bindings = bindings;
    }

    public ComponentDescription() {
    }

    public ServiceDocument getServiceDocument() {
        if (this.componentJson == null) {
            return null;
        }

        if (this.component == null) {
            this.component = Utils.fromJson(componentJson,
                    CompositeComponentRegistry.metaByType(this.type).descriptionClass);
        }
        return component;
    }

    public void updateServiceDocument(ServiceDocument serviceDocument) {
        this.componentJson = Utils.toJson(serviceDocument);
        this.component = serviceDocument;
    }
}
