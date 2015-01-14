/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.structuralsearch.impl.matcher.filters;

import com.intellij.dupLocator.util.NodeFilter;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;

/**
 * @author Bas
 */
public class AnnotationFilter implements NodeFilter {

  private static class NodeFilterHolder {
    private static final NodeFilter instance = new AnnotationFilter();
  }

  public static NodeFilter getInstance() {
    return NodeFilterHolder.instance;
  }

  private AnnotationFilter() {}

  public boolean accepts(PsiElement element) {
    return element instanceof PsiAnnotation;
  }
}
