package org.jetbrains.bsp.bazel;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetCapabilities;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.DependencySourcesItem;
import ch.epfl.scala.bsp4j.DependencySourcesParams;
import ch.epfl.scala.bsp4j.DependencySourcesResult;
import ch.epfl.scala.bsp4j.InverseSourcesParams;
import ch.epfl.scala.bsp4j.InverseSourcesResult;
import ch.epfl.scala.bsp4j.JvmBuildTarget;
import ch.epfl.scala.bsp4j.JvmEnvironmentItem;
import ch.epfl.scala.bsp4j.JvmRunEnvironmentParams;
import ch.epfl.scala.bsp4j.JvmRunEnvironmentResult;
import ch.epfl.scala.bsp4j.JvmTestEnvironmentParams;
import ch.epfl.scala.bsp4j.JvmTestEnvironmentResult;
import ch.epfl.scala.bsp4j.ResourcesItem;
import ch.epfl.scala.bsp4j.ResourcesParams;
import ch.epfl.scala.bsp4j.ResourcesResult;
import ch.epfl.scala.bsp4j.ScalaBuildTarget;
import ch.epfl.scala.bsp4j.ScalaMainClass;
import ch.epfl.scala.bsp4j.ScalaMainClassesItem;
import ch.epfl.scala.bsp4j.ScalaMainClassesParams;
import ch.epfl.scala.bsp4j.ScalaMainClassesResult;
import ch.epfl.scala.bsp4j.ScalaPlatform;
import ch.epfl.scala.bsp4j.ScalaTestClassesItem;
import ch.epfl.scala.bsp4j.ScalaTestClassesParams;
import ch.epfl.scala.bsp4j.ScalaTestClassesResult;
import ch.epfl.scala.bsp4j.SourceItem;
import ch.epfl.scala.bsp4j.SourceItemKind;
import ch.epfl.scala.bsp4j.SourcesItem;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.TextDocumentIdentifier;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario;
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep;

public class BazelBspSampleRepoTest extends BazelBspTestBaseScenario {

  private static final String REPO_NAME = "sample-repo";

  public BazelBspSampleRepoTest() {
    super(REPO_NAME);
  }

  // we cannot use `bazel test ...` because test runner blocks bazel daemon,
  // but testing server needs it for queries and etc
  public static void main(String[] args) {
    var test = new BazelBspSampleRepoTest();
    test.executeScenario();
  }

  @Override
  protected List<BazelBspTestScenarioStep> getScenarioSteps() {
    return List.of(
        resolveProject(),
        compareWorkspaceTargetsResults(),
        sourcesResults(),
        resourcesResults(),
        inverseSourcesResults(),
        dependencySourcesResults(),
        scalaMainClasses(),
        scalaTestClasses(),
        jvmRunEnvironment(),
        jvmTestEnvironment()
        // TODO
        //  new BazelBspServerSingleTest(
        //      "targets run unsuccessfully",
        //      client::testTargetsRunUnsuccessfully),
        //  new BazelBspServerSingleTest(
        //      "targets test unsuccessfully",
        //      client::testTargetsTestUnsuccessfully),
        //      new BazelBspServerSingleTest(
        //          "target capabilities",
        //          client::testTargetCapabilities)
        );
  }

  private BazelBspTestScenarioStep resolveProject() {
    return new BazelBspTestScenarioStep(
        "resolve project", () -> testClient.testResolveProject(Duration.ofMinutes(2)));
  }

  private BazelBspTestScenarioStep compareWorkspaceTargetsResults() {
    WorkspaceBuildTargetsResult expectedWorkspaceBuildTargetsResult =
        getExpectedWorkspaceBuildTargetsResult();

    return new BazelBspTestScenarioStep(
        "compare workspace targets results",
        () ->
            testClient.testWorkspaceTargets(
                Duration.ofSeconds(30), expectedWorkspaceBuildTargetsResult));
  }

  private BazelBspTestScenarioStep sourcesResults() {
    String exampleScalaSourceUri = String.format("%s/example/Example.scala", REPO_NAME);
    SourceItem exampleScalaSource =
        new SourceItem(exampleScalaSourceUri, SourceItemKind.FILE, false);

    SourcesItem exampleExampleSources =
        new SourcesItem(
            new BuildTargetIdentifier("//example:example"), List.of(exampleScalaSource));

    String testScalaUri = String.format("%s/dep/Test.scala", REPO_NAME);
    SourceItem testScalaSource = new SourceItem(testScalaUri, SourceItemKind.FILE, false);

    String javaTestJavaUri = String.format("%s/dep/JavaTest.java", REPO_NAME);
    SourceItem javaTestJavaSource = new SourceItem(javaTestJavaUri, SourceItemKind.FILE, false);

    String depScalaUri = String.format("%s/dep/Dep.scala", REPO_NAME);
    SourceItem depScalaSource = new SourceItem(depScalaUri, SourceItemKind.FILE, false);

    SourcesItem depDepSources =
        new SourcesItem(
            new BuildTargetIdentifier("//dep:dep"),
            List.of(testScalaSource, javaTestJavaSource, depScalaSource));

    SourcesResult expectedSourcesResult =
        new SourcesResult(List.of(exampleExampleSources, depDepSources));

    SourcesParams sourcesParams = new SourcesParams(getExpectedTargetIdentifiers());

    return new BazelBspTestScenarioStep(
        "sources results",
        () -> testClient.testSources(Duration.ofSeconds(30), sourcesParams, expectedSourcesResult));
  }

  private BazelBspTestScenarioStep resourcesResults() {
    String fileTxtUri = String.format("%s/example/file.txt", REPO_NAME);
    String file2TxtUri = String.format("%s/example/file2.txt", REPO_NAME);

    ResourcesItem exampleExampleResource =
        new ResourcesItem(
            new BuildTargetIdentifier("//example:example"), List.of(fileTxtUri, file2TxtUri));

    ResourcesResult expectedResourcesResult = new ResourcesResult(List.of(exampleExampleResource));

    ResourcesParams resourcesParams = new ResourcesParams(getExpectedTargetIdentifiers());

    return new BazelBspTestScenarioStep(
        "resources results",
        () ->
            testClient.testResources(
                Duration.ofSeconds(30), resourcesParams, expectedResourcesResult));
  }

  private BazelBspTestScenarioStep inverseSourcesResults() {
    String depScalaUri = String.format("file://%s/dep/Dep.scala", getTestingRepoPath(REPO_NAME));
    TextDocumentIdentifier inverseSourcesDocument = new TextDocumentIdentifier(depScalaUri);

    InverseSourcesResult expectedInverseSourcesResult =
        new InverseSourcesResult(List.of(new BuildTargetIdentifier("//dep:dep")));

    InverseSourcesParams inverseSourcesParams = new InverseSourcesParams(inverseSourcesDocument);

    return new BazelBspTestScenarioStep(
        "inverse sources results",
        () ->
            testClient.testInverseSources(
                Duration.ofSeconds(30), inverseSourcesParams, expectedInverseSourcesResult));
  }

  private BazelBspTestScenarioStep scalaMainClasses() {
    ScalaMainClassesParams scalaMainClassesParams =
        new ScalaMainClassesParams(
            List.of(
                new BuildTargetIdentifier("//example:example"),
                new BuildTargetIdentifier("//target_without_main_class:library"),
                new BuildTargetIdentifier("//target_without_args:binary"),
                new BuildTargetIdentifier("//target_without_jvm_flags:binary")));

    ScalaMainClass exampleExampleMainClass =
        new ScalaMainClass("example.Example", List.of("arg1", "arg2"), List.of("-Xms2G -Xmx5G"));
    ScalaMainClassesItem exampleExampleMainClasses =
        new ScalaMainClassesItem(
            new BuildTargetIdentifier("//example:example"), List.of(exampleExampleMainClass));

    ScalaMainClass withoutArgsBinaryMainClass =
        new ScalaMainClass("example.Example", List.of(), List.of("-Xms2G -Xmx5G"));
    ScalaMainClassesItem withoutArgsBinaryMainClasses =
        new ScalaMainClassesItem(
            new BuildTargetIdentifier("//target_without_args:binary"),
            List.of(withoutArgsBinaryMainClass));

    ScalaMainClass withoutJvmFlagsBinaryMainClass =
        new ScalaMainClass("example.Example", List.of("arg1", "arg2"), List.of());
    ScalaMainClassesItem withoutJvmFlagsBinaryMainClasses =
        new ScalaMainClassesItem(
            new BuildTargetIdentifier("//target_without_jvm_flags:binary"),
            List.of(withoutJvmFlagsBinaryMainClass));

    ScalaMainClassesResult expectedScalaMainClassesResult =
        new ScalaMainClassesResult(
            List.of(
                exampleExampleMainClasses,
                withoutArgsBinaryMainClasses,
                withoutJvmFlagsBinaryMainClasses));

    return new BazelBspTestScenarioStep(
        "Scala main classes",
        () ->
            testClient.testScalaMainClasses(
                Duration.ofSeconds(30), scalaMainClassesParams, expectedScalaMainClassesResult));
  }

  private BazelBspTestScenarioStep scalaTestClasses() {
    ScalaTestClassesParams scalaTestClassesParams =
        new ScalaTestClassesParams(
            List.of(
                new BuildTargetIdentifier("//example:example"),
                new BuildTargetIdentifier("//example:example-test"),
                new BuildTargetIdentifier("//example:example-spec2-test")));

    ScalaTestClassesItem exampleExampleTestTestClasses =
        new ScalaTestClassesItem(
            new BuildTargetIdentifier("//example:example-test"), List.of("example.ExampleTest"));

    // TODO (https://github.com/JetBrains/bazel-bsp/issues/96)
    ScalaTestClassesItem exampleExampleSpec2TestTestClasses =
        new ScalaTestClassesItem(
            new BuildTargetIdentifier("//example:example-spec2-test"), List.of());

    ScalaTestClassesResult expectedScalaTestClassesResult =
        new ScalaTestClassesResult(
            List.of(exampleExampleTestTestClasses, exampleExampleSpec2TestTestClasses));

    return new BazelBspTestScenarioStep(
        "Scala test classes",
        () ->
            testClient.testScalaTestClasses(
                Duration.ofSeconds(30), scalaTestClassesParams, expectedScalaTestClassesResult));
  }

  private BazelBspTestScenarioStep dependencySourcesResults() {
    var dependencies =
        List.of(
            "https/repo1.maven.org/maven2/com/google/j2objc/j2objc-annotations/1.3/j2objc-annotations-1.3-sources.jar",
            "https/repo1.maven.org/maven2/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2-sources.jar",
            "https/repo1.maven.org/maven2/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1-sources.jar",
            "https/repo1.maven.org/maven2/com/google/errorprone/error_prone_annotations/2.3.2/error_prone_annotations-2.3.2-sources.jar",
            "https/repo1.maven.org/maven2/org/codehaus/mojo/animal-sniffer-annotations/1.17/animal-sniffer-annotations-1.17-sources.jar",
            "https/repo1.maven.org/maven2/org/checkerframework/checker-qual/2.8.1/checker-qual-2.8.1-sources.jar",
            "https/repo1.maven.org/maven2/com/google/guava/guava/28.0-jre/guava-28.0-jre-sources.jar");

    DependencySourcesItem exampleExampleDependencies =
        new DependencySourcesItem(new BuildTargetIdentifier("//example:example"), List.of());

    DependencySourcesItem depDepDependencies =
        new DependencySourcesItem(new BuildTargetIdentifier("//dep:dep"), List.of());

    DependencySourcesItem depDeeperDeeperDependencies =
        new DependencySourcesItem(new BuildTargetIdentifier("//dep/deeper:deeper"), dependencies);

    DependencySourcesResult expectedDependencies =
        new DependencySourcesResult(
            List.of(exampleExampleDependencies, depDepDependencies, depDeeperDeeperDependencies));

    DependencySourcesParams dependencySourcesParams =
        new DependencySourcesParams(getExpectedTargetIdentifiers());

    return new BazelBspTestScenarioStep(
        "dependency sources results",
        () ->
            testClient.testDependencySources(
                Duration.ofSeconds(30), dependencySourcesParams, expectedDependencies));
  }

  private BazelBspTestScenarioStep jvmRunEnvironment() {
    JvmRunEnvironmentParams params =
        new JvmRunEnvironmentParams(List.of(new BuildTargetIdentifier("//example:example")));

    JvmRunEnvironmentResult expectedResult =
        new JvmRunEnvironmentResult(
            List.of(
                new JvmEnvironmentItem(
                    new BuildTargetIdentifier("//example:example"),
                    List.of(
                        "/dep/dep.jar",
                        "/dep/dep_java.jar",
                        "/dep/deeper/deeper.jar",
                        "/example/example.jar",
                        "/scala-library-2.12.8.jar",
                        "/https/repo1.maven.org/maven2/com/google/guava/guava/28.0-jre/guava-28.0-jre.jar"),
                    List.of("-Xms2G -Xmx5G"),
                    "/e2e/test-resources/sample-repo",
                    System.getenv())));

    return new BazelBspTestScenarioStep(
        "jvm run environment results",
        () -> testClient.testJvmRunEnvironment(Duration.ofSeconds(30), params, expectedResult));
  }

  private BazelBspTestScenarioStep jvmTestEnvironment() {
    JvmTestEnvironmentParams params =
        new JvmTestEnvironmentParams(List.of(new BuildTargetIdentifier("//example:example-test")));

    JvmTestEnvironmentResult expectedResult =
        new JvmTestEnvironmentResult(
            List.of(
                new JvmEnvironmentItem(
                    new BuildTargetIdentifier("//example:example-test"),
                    List.of(
                        "/scala-library-2.12.8.jar",
                        "/scalatest_2.12-3.0.5.jar",
                        "/example/example-test.jar",
                        "/dep/dep.jar",
                        "/dep/dep_java.jar",
                        "/dep/deeper/deeper.jar",
                        "/https/repo1.maven.org/maven2/com/google/guava/guava/28.0-jre/guava-28.0-jre.jar"),
                    List.of(),
                    "/e2e/test-resources/sample-repo",
                    System.getenv())));

    return new BazelBspTestScenarioStep(
        "jvm test environment results",
        () -> testClient.testJvmTestEnvironment(Duration.ofSeconds(30), params, expectedResult));
  }

  private List<BuildTargetIdentifier> getExpectedTargetIdentifiers() {
    return getExpectedWorkspaceBuildTargetsResult().getTargets().stream()
        .map(BuildTarget::getId)
        .collect(Collectors.toList());
  }

  private WorkspaceBuildTargetsResult getExpectedWorkspaceBuildTargetsResult() {

    JvmBuildTarget jvmBuildTarget =
        new JvmBuildTarget("file://$BAZEL_CACHE/external/local_jdk/", "8");

    BuildTarget javaTargetsJavaBinary =
        new BuildTarget(
            new BuildTargetIdentifier("//java_targets:java_binary"),
            List.of("application"),
            List.of("java"),
            List.of(),
            new BuildTargetCapabilities(true, false, true, false));

    javaTargetsJavaBinary.setDisplayName("//java_targets:java_binary");
    javaTargetsJavaBinary.setBaseDirectory("file://$WORKSPACE/java_targets/");
    javaTargetsJavaBinary.setDataKind("jvm");
    javaTargetsJavaBinary.setData(jvmBuildTarget);

    ScalaBuildTarget scalaBuildTarget =
        new ScalaBuildTarget(
            "org.scala-lang",
            "2.12.14",
            "2.12",
            ScalaPlatform.JVM,
            List.of(
                "file://$BAZEL_CACHE/external/io_bazel_rules_scala_scala_compiler/scala-compiler-2.12.14.jar",
                "file://$BAZEL_CACHE/external/io_bazel_rules_scala_scala_compiler/scala-library-2.12.14.jar",
                "file://$BAZEL_CACHE/external/io_bazel_rules_scala_scala_compiler/scala-reflect-2.12.14.jar"));

    scalaBuildTarget.setJvmBuildTarget(jvmBuildTarget);

    BuildTarget scalaTargetsScalaBinary =
        new BuildTarget(
            new BuildTargetIdentifier("//scala_targets:scala_binary"),
            List.of("application"),
            List.of("scala"),
            List.of(),
            new BuildTargetCapabilities(true, false, true, false));

    scalaTargetsScalaBinary.setDisplayName("//scala_targets:scala_binary");
    scalaTargetsScalaBinary.setBaseDirectory("file://$WORKSPACE/scala_targets/");
    scalaTargetsScalaBinary.setDataKind("scala");
    scalaTargetsScalaBinary.setData(scalaBuildTarget);

    BuildTarget javaTargetsSubpackageSubpackage =
        new BuildTarget(
            new BuildTargetIdentifier("//java_targets/subpackage:subpackage"),
            List.of("library"),
            List.of("java"),
            List.of(),
            new BuildTargetCapabilities(true, false, false, false));
    javaTargetsSubpackageSubpackage.setDisplayName("//java_targets/subpackage:subpackage");
    javaTargetsSubpackageSubpackage.setBaseDirectory("file://$WORKSPACE/java_targets/subpackage/");
    javaTargetsSubpackageSubpackage.setDataKind("jvm");
    javaTargetsSubpackageSubpackage.setData(jvmBuildTarget);

    BuildTarget javaTargetsJavaLibrary =
        new BuildTarget(
            new BuildTargetIdentifier("//java_targets:java_library"),
            List.of("library"),
            List.of("java"),
            List.of(),
            new BuildTargetCapabilities(true, false, false, false));
    javaTargetsJavaLibrary.setDisplayName("//java_targets:java_library");
    javaTargetsJavaLibrary.setBaseDirectory("file://$WORKSPACE/java_targets/");
    javaTargetsJavaLibrary.setDataKind("jvm");
    javaTargetsJavaLibrary.setData(jvmBuildTarget);

    BuildTarget targetWithoutJvmFlagsBinary =
        new BuildTarget(
            new BuildTargetIdentifier("//target_without_jvm_flags:binary"),
            List.of("application"),
            List.of("scala"),
            List.of(),
            new BuildTargetCapabilities(true, false, true, false));
    targetWithoutJvmFlagsBinary.setDisplayName("//target_without_jvm_flags:binary");
    targetWithoutJvmFlagsBinary.setBaseDirectory("file://$WORKSPACE/target_without_jvm_flags/");
    targetWithoutJvmFlagsBinary.setDataKind("scala");
    targetWithoutJvmFlagsBinary.setData(scalaBuildTarget);

    BuildTarget targetWithoutMainClassLibrary =
        new BuildTarget(
            new BuildTargetIdentifier("//target_without_main_class:library"),
            List.of("library"),
            List.of("scala"),
            List.of(),
            new BuildTargetCapabilities(true, false, false, false));
    targetWithoutMainClassLibrary.setDisplayName("//target_without_main_class:library");
    targetWithoutMainClassLibrary.setBaseDirectory("file://$WORKSPACE/target_without_main_class/");
    targetWithoutMainClassLibrary.setDataKind("scala");
    targetWithoutMainClassLibrary.setData(scalaBuildTarget);

    BuildTarget targetWithoutArgsBinary =
        new BuildTarget(
            new BuildTargetIdentifier("//target_without_args:binary"),
            List.of("application"),
            List.of("scala"),
            List.of(),
            new BuildTargetCapabilities(true, false, true, false));
    targetWithoutArgsBinary.setDisplayName("//target_without_args:binary");
    targetWithoutArgsBinary.setBaseDirectory("file://$WORKSPACE/target_without_args/");
    targetWithoutArgsBinary.setDataKind("scala");
    targetWithoutArgsBinary.setData(scalaBuildTarget);

    BuildTarget targetWithDependencyJavaBinary =
        new BuildTarget(
            new BuildTargetIdentifier("//target_with_dependency:java_binary"),
            List.of("application"),
            List.of("java"),
            List.of(
                new BuildTargetIdentifier("//java_targets:java_library_exported"),
                new BuildTargetIdentifier("@guava//:guava"),
                new BuildTargetIdentifier("//java_targets/subpackage:subpackage")),
            new BuildTargetCapabilities(true, false, true, false));
    targetWithDependencyJavaBinary.setDisplayName("//target_with_dependency:java_binary");
    targetWithDependencyJavaBinary.setBaseDirectory("file://$WORKSPACE/target_with_dependency/");
    targetWithDependencyJavaBinary.setDataKind("jvm");
    targetWithDependencyJavaBinary.setData(jvmBuildTarget);

    BuildTarget scalaTargetsScalaTest =
        new BuildTarget(
            new BuildTargetIdentifier("//scala_targets:scala_test"),
            List.of("test"),
            List.of("scala"),
            List.of(),
            new BuildTargetCapabilities(true, true, false, false));
    scalaTargetsScalaTest.setDisplayName("//scala_targets:scala_test");
    scalaTargetsScalaTest.setBaseDirectory("file://$WORKSPACE/scala_targets/");
    scalaTargetsScalaTest.setDataKind("scala");
    scalaTargetsScalaTest.setData(scalaBuildTarget);

    BuildTarget targetWithResourcesJavaBinary =
        new BuildTarget(
            new BuildTargetIdentifier("//target_with_resources:java_binary"),
            List.of("application"),
            List.of("java"),
            List.of(),
            new BuildTargetCapabilities(true, false, true, false));
    targetWithResourcesJavaBinary.setDisplayName("//target_with_resources:java_binary");
    targetWithResourcesJavaBinary.setBaseDirectory("file://$WORKSPACE/target_with_resources/");
    targetWithResourcesJavaBinary.setDataKind("jvm");
    targetWithResourcesJavaBinary.setData(jvmBuildTarget);

    BuildTarget javaTargetsJavaLibraryExported =
        new BuildTarget(
            new BuildTargetIdentifier("//java_targets:java_library_exported"),
            List.of("library"),
            List.of(),
            List.of(new BuildTargetIdentifier("//java_targets/subpackage:subpackage")),
            new BuildTargetCapabilities(true, false, false, false));
    javaTargetsJavaLibraryExported.setDisplayName("//java_targets:java_library_exported");
    javaTargetsJavaLibraryExported.setBaseDirectory("file://$WORKSPACE/java_targets/");

    BuildTarget bspWorkspaceRoot =
        new BuildTarget(
            new BuildTargetIdentifier("bsp-workspace-root"),
            List.of(),
            List.of(),
            List.of(),
            new BuildTargetCapabilities(false, false, false, false));

    bspWorkspaceRoot.setDisplayName("bsp-workspace-root");
    bspWorkspaceRoot.setBaseDirectory("file://$WORKSPACE/");

    return new WorkspaceBuildTargetsResult(
        List.of(
            javaTargetsJavaBinary,
            scalaTargetsScalaBinary,
            javaTargetsSubpackageSubpackage,
            javaTargetsJavaLibrary,
            targetWithoutJvmFlagsBinary,
            targetWithoutMainClassLibrary,
            targetWithoutArgsBinary,
            targetWithDependencyJavaBinary,
            scalaTargetsScalaTest,
            targetWithResourcesJavaBinary,
            javaTargetsJavaLibraryExported,
            bspWorkspaceRoot));
  }
}
