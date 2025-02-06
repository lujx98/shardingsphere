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

package org.apache.shardingsphere.mode.node.path.config;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalRuleNodePathTest {
    
    @Test
    void assertGetRootPath() {
        assertThat(GlobalRuleNodePath.getRootPath(), is("/rules"));
    }
    
    @Test
    void assertGetRulePath() {
        assertThat(GlobalRuleNodePath.getRulePath("foo_rule"), is("/rules/foo_rule"));
    }
    
    @Test
    void assertGetVersionNodePath() {
        assertThat(GlobalRuleNodePath.getVersionNodePath("foo_rule").getActiveVersionPath(), is("/rules/foo_rule/active_version"));
        assertThat(GlobalRuleNodePath.getVersionNodePath("foo_rule").getVersionsPath(), is("/rules/foo_rule/versions"));
        assertThat(GlobalRuleNodePath.getVersionNodePath("foo_rule").getVersionPath(0), is("/rules/foo_rule/versions/0"));
    }
    
    @Test
    void assertFindRuleTypeNameFromActiveVersion() {
        Optional<String> actual = GlobalRuleNodePath.findRuleTypeNameFromActiveVersion("/rules/foo_rule/active_version");
        assertTrue(actual.isPresent());
        assertThat(actual.get(), is("foo_rule"));
    }
    
    @Test
    void assertNotFindRuleTypeNameFromActiveVersion() {
        Optional<String> actual = GlobalRuleNodePath.findRuleTypeNameFromActiveVersion("/rules/foo_rule/active_version/xxx");
        assertFalse(actual.isPresent());
    }
    
    @Test
    void assertFindVersion() {
        Optional<String> actual = GlobalRuleNodePath.findVersion("foo_rule", "/rules/foo_rule/versions/0");
        assertTrue(actual.isPresent());
        assertThat(actual.get(), is("0"));
    }
    
    @Test
    void assertNotFindVersion() {
        Optional<String> actual = GlobalRuleNodePath.findVersion("foo_rule", "/rules/foo_rule/versions/0/xxx");
        assertFalse(actual.isPresent());
    }
}
