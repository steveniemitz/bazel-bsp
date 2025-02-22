package org.jetbrains.bsp.bazel.server.diagnostics

object CompilerDiagnosticParser : Parser {

  override fun tryParse(output: Output): List<Diagnostic> =
      listOfNotNull(tryParseOne(output, targetLabel = null))

  // Example:
  // server/DiagnosticsServiceTest.kt:12:18: error: type mismatch: inferred type is String but Int was expected
  private val DiagnosticHeader = """
      ^                # start of line
      ([^:]+)          # file path (1)
      :(\d+)           # line number (2)
      (?::(\d+))?      # optional column number (3)
      :\               # ": " separator
      ([a-zA-Z\ ]+):\  # level (4)
      .*               # actual error message
      $                # end of line
      """.toRegex(RegexOption.COMMENTS)

  fun tryParseOne(output: Output, targetLabel: String?): Diagnostic? =
      output.tryTake(DiagnosticHeader)
          ?.let { match ->
            val path = match.groupValues[1]
            val line = match.groupValues[2].toInt()
            val column = match.groupValues[3].toIntOrNull() ?: 1
            val level = if (match.groupValues[4] == "warning") Level.Warning else Level.Error
            val message = constructMessage(match.value, output)
            Diagnostic(Position(line, column), message, level, path, targetLabel)
          }

  private fun constructMessage(header: String, output: Output): String {
    val lines = mutableListOf<String>()
    lines.addAll(tryCollectLinesMatchingIssueDetails(output))
    if (lines.isEmpty()) {
      lines.addAll(tryCollectLinesTillErrorMarker(output))
    }
    lines.add(0, header)
    return lines.joinToString("\n")
  }

  private val IssuePositionMarker = """^\s*\^\s*$""".toRegex() // ^ surrounded by whitespace only

  private fun tryCollectLinesTillErrorMarker(output: Output): List<String> {
    val peeked = output.peek(limit = 20)
    val index = peeked.indexOfFirst { IssuePositionMarker.matches(it) }
    return if (index != -1) output.take(count = index + 1) else emptyList()
  }

  private val IssueDetails = """^\s+.*|${IssuePositionMarker.pattern}""".toRegex() // indented line or ^

  private fun tryCollectLinesMatchingIssueDetails(output: Output) =
      generateSequence { output.tryTake(IssueDetails)?.value }.toList()
}
