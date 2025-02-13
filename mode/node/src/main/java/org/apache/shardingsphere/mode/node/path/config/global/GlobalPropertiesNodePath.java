/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.mode.node.path.config.global;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.mode.node.path.version.VersionNodePathGenerator;
import org.apache.shardingsphere.mode.node.path.version.VersionNodePathParser;

/**
 * Global properties node path.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GlobalPropertiesNodePath {
    
    private static final String ROOT_NODE = "/props";
    
    private static final VersionNodePathParser PARSER = new VersionNodePathParser(getRootPath());
    
    /**
     * Get properties path.
     *
     * @return properties path
     */
    public static String getRootPath() {
        return ROOT_NODE;
    }
    
    /**
     * Get properties version node path generator.
     *
     * @return properties version node path generator
     */
    public static VersionNodePathGenerator getVersionNodePathGenerator() {
        return new VersionNodePathGenerator(getRootPath());
    }
    
    /**
     * Get properties version node path parser.
     *
     * @return properties version node path parser
     */
    public static VersionNodePathParser getVersionNodePathParser() {
        return PARSER;
    }
}
