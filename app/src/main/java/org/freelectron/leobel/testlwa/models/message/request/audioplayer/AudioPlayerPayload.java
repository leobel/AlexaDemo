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
package org.freelectron.leobel.testlwa.models.message.request.audioplayer;


import org.freelectron.leobel.testlwa.models.message.Payload;

public class AudioPlayerPayload extends Payload {

    private final String token;
    private final long offsetInMilliseconds;

    public AudioPlayerPayload(String token, long offsetInMilliseconds) {
        this.token = token;
        this.offsetInMilliseconds = offsetInMilliseconds;
    }

    public String getToken() {
        return token;
    }

    public long getOffsetInMilliseconds() {
        return offsetInMilliseconds;
    }
}