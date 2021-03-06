/** 
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License"). You may not use this file 
 * except in compliance with the License. A copy of the License is located at
 *
 *   http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 */

package org.freelectron.leobel.testlwa.models.message.request;


import org.freelectron.leobel.testlwa.models.message.request.context.ComponentState;

import java.util.List;

public class ContextEventRequestBody extends RequestBody {

    private final List<ComponentState> context;

    public ContextEventRequestBody(List<ComponentState> context, Event event) {
        super(event);
        this.context = context;
    }

    public final List<ComponentState> getContext() {
        return context;
    }

}
