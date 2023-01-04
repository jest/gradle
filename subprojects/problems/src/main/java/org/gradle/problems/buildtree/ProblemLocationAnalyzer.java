/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.problems.buildtree;

import org.gradle.internal.service.scopes.Scopes;
import org.gradle.internal.service.scopes.ServiceScope;
import org.gradle.problems.Location;

import javax.annotation.Nullable;
import java.util.List;

@ServiceScope(Scopes.BuildTree.class)
public interface ProblemLocationAnalyzer {
    /**
     * Calculates the location for a problem with the given stack.
     * @return A display name for the location or null for an unknown location.
     */
    @Nullable
    Location locationForUsage(List<StackTraceElement> stack);
}
