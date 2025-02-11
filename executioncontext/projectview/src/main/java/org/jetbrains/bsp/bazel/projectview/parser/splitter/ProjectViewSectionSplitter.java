package org.jetbrains.bsp.bazel.projectview.parser.splitter;

import io.vavr.collection.List;
import java.util.regex.Pattern;

/**
 * Splitter is responsible for splitting file content into "raw" sections - section name and entire
 * section body. Also, it removes comments (starting with '#') from section content
 *
 * <p>e.g.: <br>
 * file content:
 *
 * <pre><code>
 * ---
 *    import path/to/another/file.bazelproject
 *
 *    section1: value1 # comment
 *
 *    section2:
 *      included_value1
 *      included_value2
 *      -excluded_value3
 * ---
 * </code></pre>
 *
 * <p>will be split into raw sections:<br>
 * 1) <code>'import'</code> -- <code>'path/to/another/file.bazelproject\n\n'</code><br>
 * 2) <code>'section1'</code> -- <code>'value1 \n\n'</code><br>
 * 3) <code>'section2'</code> -- <code>
 * '\n  included_value1\n  included_value2\n  -excluded_value3\n'</code><br>
 */
public final class ProjectViewSectionSplitter {

  private static final Pattern SECTION_HEADER_REGEX =
      Pattern.compile("((^[^:\\-/*\\s]+)([: ]))", Pattern.MULTILINE);
  private static final int SECTION_HEADER_NAME_GROUP_ID = 2;

  private static final String COMMENT_LINE_REGEX = "#(.)*(\\n|\\z)";
  private static final String COMMENT_LINE_REPLACEMENT = "\n";

  public static ProjectViewRawSections getRawSectionsForFileContent(String fileContent) {
    var fileContentWithoutComments = removeLinesWithComments(fileContent);
    var rawSections = findRawSections(fileContentWithoutComments);

    return new ProjectViewRawSections(rawSections);
  }

  private static String removeLinesWithComments(String fileContent) {
    return fileContent.replaceAll(COMMENT_LINE_REGEX, COMMENT_LINE_REPLACEMENT);
  }

  private static List<ProjectViewRawSection> findRawSections(String fileContent) {
    var sectionHeadersNames = findSectionsHeadersNames(fileContent);
    var sectionBodies = findSectionsBodiesAndSkipFirstEmptyEntry(fileContent);

    return sectionHeadersNames.zipWith(sectionBodies, ProjectViewRawSection::new);
  }

  private static List<String> findSectionsHeadersNames(String fileContent) {
    var results = List.ofAll(SECTION_HEADER_REGEX.matcher(fileContent).results());

    return results.map(match -> match.group(SECTION_HEADER_NAME_GROUP_ID));
  }

  private static List<String> findSectionsBodiesAndSkipFirstEmptyEntry(String fileContent) {
    var sectionsBodies = SECTION_HEADER_REGEX.split(fileContent);

    return List.of(sectionsBodies).drop(1);
  }
}
