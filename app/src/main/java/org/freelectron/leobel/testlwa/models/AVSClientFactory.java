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
package org.freelectron.leobel.testlwa.models;

import org.freelectron.leobel.testlwa.services.PreferenceService;

public class AVSClientFactory {

//    public AVSClientFactory(DeviceConfig config) {
//        this.config = config;
//    }
//
//    public AVSClient getAVSClient(DirectiveEnqueuer directiveEnqueuer,
//            ParsingFailedHandler parsingFailedHandler) throws Exception {
//        return new AVSClient(config.getAvsHost(), directiveEnqueuer, new SslContextFactory(),
//                parsingFailedHandler);
//    }

    public AVSClientFactory(){

    }

    public AVSClient getAVSClient(AVSClient.ConnectionListener listener, DirectiveEnqueuer directiveEnqueuer, PreferenceService preferenceService){
        return new AVSClient(listener, directiveEnqueuer, preferenceService);
    }
}
