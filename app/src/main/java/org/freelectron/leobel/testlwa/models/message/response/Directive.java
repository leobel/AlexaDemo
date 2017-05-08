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
package org.freelectron.leobel.testlwa.models.message.response;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.freelectron.leobel.testlwa.models.message.DialogRequestIdHeader;
import org.freelectron.leobel.testlwa.models.message.Header;

import java.io.IOException;

public class Directive extends org.freelectron.leobel.testlwa.models.message.Directive {

    @JsonIgnore
    private final String dialogRequestId;

    public Directive(Header header, JsonNode payload, String rawMessage)
            throws JsonParseException, JsonMappingException, IOException {
        super(header, payload, rawMessage);
        dialogRequestId = extractDialogRequestId();
    }

    public String getDialogRequestId() {
        return dialogRequestId;
    }

    private String extractDialogRequestId() {
        if (header instanceof DialogRequestIdHeader) {
            DialogRequestIdHeader dialogRequestIdHeader = (DialogRequestIdHeader) header;
            return dialogRequestIdHeader.getDialogRequestId();
        } else {
            return null;
        }
    }
}
