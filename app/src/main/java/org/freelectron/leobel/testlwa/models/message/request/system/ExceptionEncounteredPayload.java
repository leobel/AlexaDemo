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
package org.freelectron.leobel.testlwa.models.message.request.system;


import org.freelectron.leobel.testlwa.models.exception.DirectiveHandlingException;
import org.freelectron.leobel.testlwa.models.message.Payload;

public class ExceptionEncounteredPayload extends Payload {

    private String unparsedDirective;
    private ErrorStructure error;

    public ExceptionEncounteredPayload(String unparsedDirective, DirectiveHandlingException.ExceptionType type, String message) {
        this.unparsedDirective = unparsedDirective;
        error = new ErrorStructure(type, message);
    }

    public String getUnparsedDirective() {
        return unparsedDirective;
    }

    public ErrorStructure getError() {
        return error;
    }

    private static class ErrorStructure {
        private DirectiveHandlingException.ExceptionType type;
        private String message;

        public ErrorStructure(DirectiveHandlingException.ExceptionType type, String message) {
            this.type = type;
            this.message = message;
        }

        public DirectiveHandlingException.ExceptionType getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }
}
