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
package org.freelectron.leobel.testlwa.models.message;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.freelectron.leobel.testlwa.config.ObjectMapperFactory;
import org.freelectron.leobel.testlwa.models.AVSAPIConstants;
import org.freelectron.leobel.testlwa.models.message.response.AlexaExceptionResponse;
import org.freelectron.leobel.testlwa.models.message.request.Event;
import org.freelectron.leobel.testlwa.models.message.response.system.Exception;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * A message from the server. Can be an
 * {@link  Exception},
 * {@link  Event} , or {@link org.freelectron.leobel.testlwa.models.message.response.Directive}
 */
@JsonDeserialize(using = Directive.MessageDeserializer.class)
public abstract class Directive {
    protected Header header;
    protected Payload payload;

    @JsonIgnore
    private String rawMessage;


    protected Directive(Header header, JsonNode payload, String rawMessage)
            throws JsonParseException, JsonMappingException, IOException {

        this.header = header;
        try {
            ObjectReader reader = ObjectMapperFactory.getObjectReader();
            Class<?> type = Class.forName(getClass().getPackage().getName() + "."
                    + header.getNamespace().toLowerCase() + "." + header.getName());
            this.payload = (Payload) reader.forType(type).readValue(payload);
        } catch (ClassNotFoundException e) {
            // Default to empty payload
            this.payload = new Payload();
        }

        this.rawMessage = rawMessage;
    }

    protected Directive(Header header, Payload payload, String rawMessage) {
        this.header = header;
        this.payload = payload;
        this.rawMessage = rawMessage;
    }

    @JsonIgnore
    public String getName() {
        return header.getName();
    }

    @JsonIgnore
    public String getNamespace() {
        return header.getNamespace();
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Header getHeader() {
        return header;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public Payload getPayload() {
        return payload;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    @Override
    public String toString() {
        return header.toString();
    }

    public static class MessageDeserializer extends JsonDeserializer<Directive> {

        public MessageDeserializer() {
            // For Jackson
        }

        @Override
        public Directive deserialize(JsonParser jp, DeserializationContext ctx)
                throws IOException, JsonProcessingException {
            ObjectReader reader = ObjectMapperFactory.getObjectReader();
            ObjectNode obj = (ObjectNode) reader.readTree(jp);
            Iterator<Map.Entry<String, JsonNode>> elementsIterator = obj.fields();

            String rawMessage = obj.toString();

            DialogRequestIdHeader header = null;
            JsonNode payloadNode = null;
            ObjectReader headerReader =
                    ObjectMapperFactory.getObjectReader(DialogRequestIdHeader.class);
            while (elementsIterator.hasNext()) {
                Map.Entry<String, JsonNode> element = elementsIterator.next();
                if (element.getKey().equals("header")) {
                    header = headerReader.readValue(element.getValue());
                }
                if (element.getKey().equals("payload")) {
                    payloadNode = element.getValue();
                }
            }
            if (header == null) {
                throw ctx.mappingException("Missing header");
            }
            if (payloadNode == null) {
                throw ctx.mappingException("Missing payload");
            }

            return createMessage(header, payloadNode, rawMessage);
        }

        private Directive createMessage(Header header, JsonNode payload, String rawMessage)
                throws JsonParseException, JsonMappingException, IOException {
            if (AVSAPIConstants.System.NAMESPACE.equals(header.getNamespace())
                    && AVSAPIConstants.System.Exception.NAME.equals(header.getName())) {
                return new AlexaExceptionResponse(header, payload, rawMessage);
            } else {
                return new org.freelectron.leobel.testlwa.models.message.response.Directive(header, payload, rawMessage);
            }
        }

    }
}
