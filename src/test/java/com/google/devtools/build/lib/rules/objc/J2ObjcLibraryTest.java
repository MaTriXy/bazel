// Copyright 2017 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.rules.objc;

import com.google.devtools.build.lib.actions.ActionInputHelper;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.Artifact.ArtifactExpander;
import com.google.devtools.build.lib.actions.Artifact.SpecialArtifact;
import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.packages.util.MockJ2ObjcSupport;
import com.google.devtools.build.lib.packages.util.MockObjcSupport;
import com.google.devtools.build.lib.packages.util.MockProtoSupport;
import java.util.Collection;
import org.junit.Before;

/**
 * Setup for unit tests for j2objc transpilation.
 */
public class J2ObjcLibraryTest extends ObjcRuleTestCase {
  protected static final ArtifactExpander DUMMY_ARTIFACT_EXPANDER =
      new ArtifactExpander() {
        @Override
        public void expand(Artifact artifact, Collection<? super Artifact> output) {
          output.add(ActionInputHelper.treeFileArtifact((SpecialArtifact) artifact, "children1"));
          output.add(ActionInputHelper.treeFileArtifact((SpecialArtifact) artifact, "children2"));
        }
      };

  /**
   * The configuration to be used for genfiles artifacts.
   */
  protected BuildConfiguration getGenfilesConfig() throws InterruptedException {
    return getAppleCrosstoolConfiguration();
  }

  /**
   * Creates and injects a j2objc_library target that depends upon the given label, then returns the
   * ConfiguredTarget for the label with the aspects added.
   */
  protected ConfiguredTarget getJ2ObjCAspectConfiguredTarget(String label) throws Exception {
    // Blaze exposes no interface to summon aspects ex nihilo.
    // To get an aspect, you must create a dependent target that requires the aspect.
    scratch.file("java/com/google/dummy/aspect/BUILD",
        "j2objc_library(",
        "    name = 'transpile',",
        "    deps = ['" + label + "'],",
        ")");

    return view.getPrerequisiteConfiguredTargetForTesting(
        reporter,
        getConfiguredTarget("//java/com/google/dummy/aspect:transpile"),
        Label.parseAbsoluteUnchecked(label),
        masterConfig);
  }

  @Before
  public final void setup() throws Exception  {
    scratch.file("java/com/google/dummy/test/test.java");
    scratch.file("java/com/google/dummy/test/BUILD",
        "package(default_visibility=['//visibility:public'])",
        "java_library(",
        "    name = 'test',",
        "    srcs = ['test.java'])",
        "",
        "j2objc_library(",
        "    name = 'transpile',",
        "    deps = ['test'])");
    MockObjcSupport.setup(mockToolsConfig);
    MockObjcSupport.setupIosSimDevice(mockToolsConfig);
    MockJ2ObjcSupport.setup(mockToolsConfig);
    MockProtoSupport.setup(mockToolsConfig);

    useConfiguration("--proto_toolchain_for_java=//tools/proto/toolchains:java");

    mockToolsConfig.create(
        "tools/proto/toolchains/BUILD",
        "package(default_visibility=['//visibility:public'])",
        "proto_lang_toolchain(name = 'java', command_line = 'dont_care')",
        "proto_lang_toolchain(name='java_stubby1_immutable', command_line = 'dont_care')",
        "proto_lang_toolchain(name='java_stubby3_immutable', command_line = 'dont_care')",
        "proto_lang_toolchain(name='java_stubby_compatible13_immutable', "
            + "command_line = 'dont_care')");

    invalidatePackages();
  }
}
