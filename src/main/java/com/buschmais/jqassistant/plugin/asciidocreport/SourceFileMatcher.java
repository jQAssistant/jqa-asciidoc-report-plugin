package com.buschmais.jqassistant.plugin.asciidocreport;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.rule.api.source.RuleSource;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FilePatternMatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Arrays.asList;

public class SourceFileMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceFileMatcher.class);

    private static final String DEFAULT_INDEX_FILE = "index.adoc";

    private final File ruleDirectory;

    private final String fileInclude;

    private final String fileExclude;

    SourceFileMatcher(File ruleDirectory, String fileInclude, String fileExclude) {
        this.ruleDirectory = ruleDirectory;
        this.fileInclude = fileInclude;
        this.fileExclude = fileExclude;
    }

    /**
     * Determine the files to be rendered grouped by their base directory.
     *
     * @param ruleSources
     *            The {@link RuleSource}s.
     * @return A {@link Map} containing {@link File}s to be rendered as values
     *         grouped by their base directories.
     * @throws ReportException
     *             If execution fails.
     */
    public Map<File, List<File>> match(Set<RuleSource> ruleSources) throws ReportException {
        Map<File, List<File>> files = new HashMap<>();
        if (ruleDirectory != null) {
            // Use explicitly configured rule directory and inclusion filter
            if (ruleDirectory.exists()) {
                files.put(ruleDirectory, matchFilesFromRuleDirectory());
            } else {
                LOGGER.warn("Specified rule directory does not exist: '{}'.", ruleDirectory.getAbsolutePath());
            }
        } else {
            // Auto-detect index documents
            for (RuleSource ruleSource : ruleSources) {
                URL url;
                try {
                    url = ruleSource.getURL();
                } catch (IOException e) {
                    throw new ReportException("Cannot get URL from file " + ruleSource, e);
                }
                // Only use file://
                if ("file".equals(url.getProtocol())) {
                    String path = url.getPath();
                    if (path.endsWith("/" + DEFAULT_INDEX_FILE)) {
                        File file;
                        try {
                            file = new File(URLDecoder.decode(url.getFile(), "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            throw new ReportException("Cannot get URL from file " + ruleSource, e);
                        }
                        LOGGER.info("Found index document '{}'.", file);
                        File directory = file.getParentFile();
                        List<File> filesByDirectory = files.get(directory);
                        if (filesByDirectory == null) {
                            filesByDirectory = new ArrayList<>();
                            files.put(directory, filesByDirectory);
                        }
                        filesByDirectory.add(file);
                    }
                }
            }
        }
        return files;
    }

    private List<File> matchFilesFromRuleDirectory() {
        FilePatternMatcher filePatternMatcher = FilePatternMatcher.builder().include(this.fileInclude).exclude(this.fileExclude).build();
        return asList(ruleDirectory.listFiles(file -> file.isFile() && filePatternMatcher.accepts(file.getName())));
    }
}
