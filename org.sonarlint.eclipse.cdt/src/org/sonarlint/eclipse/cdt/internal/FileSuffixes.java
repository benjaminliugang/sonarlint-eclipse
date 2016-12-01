/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.cdt.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.content.IContentType;

public class FileSuffixes {
  static final Set<String> C_TYPES = new HashSet<>();
  static final Set<String> CPP_TYPES = new HashSet<>();
  private static final String C_SUFFIXES_KEY = "sonar.c.file.suffixes";
  private static final String CPP_SUFFIXES_KEY = "sonar.cpp.file.suffixes";

  static {
    C_TYPES.add(CCorePlugin.CONTENT_TYPE_CHEADER);
    C_TYPES.add(CCorePlugin.CONTENT_TYPE_CSOURCE);
    CPP_TYPES.add(CCorePlugin.CONTENT_TYPE_CXXHEADER);
    CPP_TYPES.add(CCorePlugin.CONTENT_TYPE_CXXSOURCE);
  }

  public Map<String, String> getSuffixes(IProject project, Collection<IFile> files) {
    Map<String, String> suffixes = new HashMap<>();

    for (IFile file : files) {
      IContentType contentType = CCorePlugin.getContentType(project, file.getName());
      if (contentType == null || file.getFileExtension() == null) {
        continue;
      }

      if (C_TYPES.contains(contentType.getId())) {
        addSuffix(suffixes, C_SUFFIXES_KEY, file.getFileExtension());
      } else if (CPP_TYPES.contains(contentType.getId())) {
        addSuffix(suffixes, CPP_SUFFIXES_KEY, file.getFileExtension());
      }
    }

    return suffixes;
  }

  private static void addSuffix(Map<String, String> map, String key, String suffix) {
    String suffixWithDot = "." + suffix;
    if (map.containsKey(key)) {
      map.put(key, map.get(key) + "," + suffixWithDot);
    } else {
      map.put(key, suffixWithDot);
    }
  }
}
