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


import java.util.Collections;
import java.util.Set;

public abstract class StateTransition<E> {

    protected Set<E> validStartStates;

    public StateTransition(Set<E> validStartStates) {
        this.validStartStates = Collections.unmodifiableSet(validStartStates);
    }

    public final void transition(State<E> currentState) {
        if (validStartStates.contains(currentState.get())) {
            onTransition(currentState);
        } else {
            onInvalidStartState(currentState);
        }
    }

    protected abstract void onTransition(State<E> state);

    protected abstract void onInvalidStartState(State<E> currentState);
}