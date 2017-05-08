package org.freelectron.leobel.testlwa.models.message.response;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.freelectron.leobel.testlwa.models.exception.AlexaSystemException;
import org.freelectron.leobel.testlwa.models.message.Header;
import org.freelectron.leobel.testlwa.models.message.Directive;

import java.io.IOException;

/**
 * Created by leobel on 2/22/17.
 */

public class AlexaExceptionResponse extends Directive {

    public AlexaExceptionResponse(Header header, JsonNode payload, String rawMessage)
            throws JsonParseException, JsonMappingException, IOException {
        super(header, payload, rawMessage);
    }

    /**
     * @throws AlexaSystemException
     */
    public void throwException() throws AlexaSystemException {
        org.freelectron.leobel.testlwa.models.message.response.system.Exception payload = (org.freelectron.leobel.testlwa.models.message.response.system.Exception) this.payload;
        throw new AlexaSystemException(payload.getCode(), payload.getDescription());
    }
}
