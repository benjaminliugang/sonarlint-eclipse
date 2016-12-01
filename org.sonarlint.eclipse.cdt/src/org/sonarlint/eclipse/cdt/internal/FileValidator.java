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

import java.util.function.Predicate;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.content.IContentType;

public class FileValidator implements Predicate<IFile> {
  @Override
  public boolean test(IFile file) {
    if (file.getFileExtension() == null) {
      return false;
    }
    IContentType contentType = CCorePlugin.getContentType(file.getProject(), file.getName());
    if (contentType == null) {
      return false;
    }

    return FileSuffixes.C_TYPES.contains(contentType.getId()) || FileSuffixes.CPP_TYPES.contains(contentType.getId());
  }
}
