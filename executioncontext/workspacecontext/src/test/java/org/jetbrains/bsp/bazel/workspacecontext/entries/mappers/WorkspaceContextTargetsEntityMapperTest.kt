package org.jetbrains.bsp.bazel.workspacecontext.entries.mappers

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import io.kotest.matchers.shouldBe
import io.vavr.collection.List
import org.jetbrains.bsp.bazel.executioncontext.api.entries.mappers.ProjectViewToExecutionContextEntityMapperException
import org.jetbrains.bsp.bazel.projectview.model.ProjectView
import org.jetbrains.bsp.bazel.projectview.model.sections.ProjectViewTargetsSection
import org.jetbrains.bsp.bazel.workspacecontext.entries.ExecutionContextTargetsEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WorkspaceContextTargetsEntityMapperTest {

    private lateinit var mapper: WorkspaceContextTargetsEntityMapper

    @BeforeEach
    fun beforeEach() {
        // given
        this.mapper = WorkspaceContextTargetsEntityMapper()
    }

    @Test
    fun `should return fail if targets are null`() {
        // given
        val projectView = ProjectView.Builder(targets = null).build().get()

        // when
        val targetsTry = mapper.map(projectView)

        // then
        targetsTry.isFailure shouldBe true
        targetsTry.cause::class shouldBe ProjectViewToExecutionContextEntityMapperException::class
        targetsTry.cause.message shouldBe "Mapping project view into 'targets' failed! 'targets' section in project view is empty."
    }

    @Test
    fun `should return fail if targets have no included values`() {
        // given
        val projectView = ProjectView.Builder(
            targets = ProjectViewTargetsSection(
                List.of(),
                List.of(
                    BuildTargetIdentifier("//excluded_target1"),
                    BuildTargetIdentifier("//excluded_target2"),
                )
            )
        ).build().get()

        // when
        val targetsTry = mapper.map(projectView)

        // then
        targetsTry.isFailure shouldBe true
        targetsTry.cause::class shouldBe ProjectViewToExecutionContextEntityMapperException::class
        targetsTry.cause.message shouldBe "Mapping project view into 'targets' failed! 'targets' section has no included targets."
    }

    @Test
    fun `should return success for successful mapping`() {
        // given
        val projectView =
            ProjectView.Builder(
                targets = ProjectViewTargetsSection(
                    List.of(
                        BuildTargetIdentifier("//included_target1"),
                        BuildTargetIdentifier("//included_target2"),
                        BuildTargetIdentifier("//included_target3")
                    ),
                    List.of(
                        BuildTargetIdentifier("//excluded_target1"),
                        BuildTargetIdentifier("//excluded_target2"),
                    ),
                )
            ).build().get()

        // when
        val targetsTry = mapper.map(projectView)

        // then
        targetsTry.isSuccess shouldBe true
        val targets = targetsTry.get()

        val expectedTargets =
            ExecutionContextTargetsEntity(
                List.of(
                    BuildTargetIdentifier("//included_target1"),
                    BuildTargetIdentifier("//included_target2"),
                    BuildTargetIdentifier("//included_target3"),
                ),
                List.of(
                    BuildTargetIdentifier("//excluded_target1"),
                    BuildTargetIdentifier("//excluded_target2"),
                ),
            )
        targets shouldBe expectedTargets
    }
}
