package org.gradle.samples.fixtures

import org.commonmark.node.*
import org.commonmark.parser.Parser

import java.util.regex.Pattern

/**
 * Locates the documentation for a sample in the README.md
 *
 * The documentation for a sample should be formatted as:
 *
 * - A level 2 heading with '(<sample-name>)' at the end of the heading text
 * - Includes all content up to the next level 2 heading.
 * - Must contain a level 3 `C++` and/or `Swift` and/or `C` heading for each language.
 * - Must contain at least one fenced code block containing the instructions for the sample for each language.
 * - All lines in the instructions that start with '>' are considered user input
 * - Instructions should include a `> cd <sample-dir>` command, this directory is assumed to be the working directory for subsequent commands.
 * - All `./gradlew` invocations prior to `./gradlew assemble` or `./gradlew build` are assumed to be setup commands.
 *
 */
class Documentation {
    private final Map<String, SampleDocumentation> samples = new HashMap<>()

    Documentation(File documentationFile) {
//        samples = loadSamples(documentationFile)
    }

    Documentation() {
        this(new File(Samples.rootSampleDir, "README.md"))
    }

    SampleDocumentation getSample(String name) {
        println('wat ' + name)
        return samples.computeIfAbsent(name, path -> {
            println(new File(Samples.rootSampleDir, path))
            return loadSample(new File(new File(Samples.rootSampleDir, path), "README.md"));
        })
    }

//    private static Map<String, SampleDocumentation> loadSamples(File readme) {
//        def parser = Parser.builder().build()
//        def root = parser.parse(readme.text)
//        def visitor = new HeadingVisitor()
//        root.accept(visitor)
//        return visitor.samples
//    }

    private static SampleDocumentation loadSample(File readme) {
        def parser = Parser.builder().build()
        def root = parser.parse(readme.text)
        def visitor = new HeadingVisitor()
        root.accept(visitor)
        return visitor.sample
    }

    private static class HeadingVisitor extends AbstractVisitor {
        private SampleDocumentation sample = null;

        @Override
        void visit(Heading heading) {
            if (heading.level != 1) {
                return
            }
            String text = getText(heading)
            def matcher = Pattern.compile(".+\\s+\\((.+)\\)\\s*").matcher(text)
            if (!matcher.matches()) {
                return
            }
            def name = matcher.group(1)
            sample = new SampleDocumentation(name, heading)
        }

        private String getText(Heading heading) {
            def result = new StringBuilder()
            heading.accept(new AbstractVisitor() {
                @Override
                void visit(Text text) {
                    result.append(text.literal)
                }
            })
            return result.toString()
        }
    }

    static class SampleDocumentation {
        final Heading heading
        final String name
        private List<String> instructions

        SampleDocumentation(String name, Heading heading) {
            this.name = name
            this.heading = heading
        }

        /**
         * Returns the instructions for this sample.*/
        List<String> getInstructions() {
            if (instructions == null) {
                def instructions = []
                Node node = heading.next
                while (node != null) {
                    if (node instanceof FencedCodeBlock) {
                        node.literal.readLines().each { str ->
                            if (str.startsWith(">")) {
                                instructions.add(str.substring(1).trim())
                            }
                        }
                    }
                    if (node instanceof Heading && node.level <= 3) {
                        break
                    }
                    node = node.next
                }
                this.instructions = instructions
            }
            return instructions
        }

        String getWorkingDir() {
            def instructions = getInstructions()
            def cdInstruction = instructions.find { it.startsWith("cd ") }
            if (cdInstruction == null) {
                throw new AssertionError("Could not find 'cd' instruction for sample $name:\n${instructions.join("\n")}")
            }
            return cdInstruction.substring(3).trim()
        }

        List<String> getSetupSteps() {
            def instructions = getInstructions()
            // An approximation
            int assemble = instructions.findIndexOf { it == "./gradlew assemble" || it.startsWith("./gradlew build ") }
            if (assemble < 0) {
                throw new AssertionError("Could not find assemble step in instructions for sample $name:\n${instructions.join("\n")}")
            }
            return instructions.subList(0, assemble).findAll { it.startsWith("./gradlew") }
        }
    }
}
