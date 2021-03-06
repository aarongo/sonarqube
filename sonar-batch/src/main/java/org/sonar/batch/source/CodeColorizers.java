/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.source;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.web.CodeColorizerFormat;
import org.sonar.batch.highlighting.SyntaxHighlightingData;
import org.sonar.colorizer.CodeColorizer;
import org.sonar.colorizer.Tokenizer;

import javax.annotation.CheckForNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central point for sonar-colorizer extensions
 */
public class CodeColorizers implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(CodeColorizers.class);

  private final Map<String, CodeColorizerFormat> byLang;

  public CodeColorizers(List<CodeColorizerFormat> formats) {
    byLang = new HashMap<String, CodeColorizerFormat>();
    for (CodeColorizerFormat format : formats) {
      byLang.put(format.getLanguageKey(), format);
    }

    LOG.debug("Code colorizer, supported languages: " + StringUtils.join(byLang.keySet(), ","));
  }

  /**
   * Used when no plugin is defining some CodeColorizerFormat
   */
  public CodeColorizers() {
    this(Lists.<CodeColorizerFormat>newArrayList());
  }

  @CheckForNull
  public SyntaxHighlightingData toSyntaxHighlighting(File file, String encoding, String language) {
    CodeColorizerFormat format = byLang.get(language);
    List<Tokenizer> tokenizers;
    if (format == null) {
      // Workaround for Java test code since Java plugin only provides highlighting for main source and no colorizer
      // TODO can be dropped when Java plugin embed its own CodeColorizerFormat of (better) provides highlighting for tests
      if ("java".equals(language)) {
        tokenizers = CodeColorizer.Format.JAVA.getTokenizers();
      } else {
        return null;
      }
    } else {
      tokenizers = format.getTokenizers();
    }
    Reader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(new BOMInputStream(new FileInputStream(file)), encoding));
      return new HighlightingRenderer().render(reader, tokenizers);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to read source file for colorization", e);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }
}
