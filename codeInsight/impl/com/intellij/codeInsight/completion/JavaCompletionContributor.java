/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.MatchingContext;
import static com.intellij.patterns.PlatformPatterns.psiElement;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.PlatformPatterns;
import static com.intellij.patterns.StandardPatterns.or;
import com.intellij.psi.*;
import com.intellij.psi.filters.FilterUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author peter
 */
public class JavaCompletionContributor extends CompletionContributor{
  @NonNls public static final String VARIABLE_NAME = "VARIABLE_NAME";
  @NonNls public static final String METHOD_NAME = "METHOD_NAME";
  @NonNls public static final String JAVA_LEGACY = "JAVA_LEGACY";
  private static final JavaSmartCompletionData SMART_DATA = new JavaSmartCompletionData();
  private static final CompletionData CLASS_NAME_DATA = new ClassNameCompletionData();
  private static final ElementPattern INSIDE_TYPE_PARAMS_PATTERN = psiElement().afterLeaf(psiElement().withText("?").afterLeaf(psiElement().withText("<")));


  public void registerCompletionProviders(final CompletionRegistrar registrar) {
    registrar.extendBasicCompletion(
      psiElement(PsiIdentifier.class).andNot(INSIDE_TYPE_PARAMS_PATTERN).withParent(
        or(
          psiElement(PsiLocalVariable.class),
          psiElement(PsiParameter.class)
        ))).withId(VARIABLE_NAME).dependingOn(LegacyCompletionContributor.LEGACY).withProvider(new CompletionProvider<LookupElement, CompletionParameters>() {
      public void addCompletions(@NotNull final CompletionParameters parameters, final MatchingContext matchingContext, @NotNull final CompletionResultSet<LookupElement> result) {
        CompletionContext context = parameters.getPosition().getUserData(CompletionContext.COMPLETION_CONTEXT_KEY);
        final PsiFile file = context.file;
        final PsiElement lastElement = file.findElementAt(context.startOffset - 1);
        PsiElement insertedElement = parameters.getPosition();
        CompletionData completionData = CompletionUtil.getCompletionDataByElement(lastElement, context.file, context.startOffset);
        context.setPrefix(insertedElement, context.startOffset, completionData);

        Set<LookupItem> lookupSet = new THashSet<LookupItem>();
        final PsiVariable variable = (PsiVariable)parameters.getPosition().getParent();
        JavaCompletionUtil.completeLocalVariableName(lookupSet, context, variable);
        result.addAllElements(lookupSet);
      }
    });
    registrar.extendBasicCompletion(psiElement(PsiIdentifier.class).withParent(PsiField.class)).withId(VARIABLE_NAME).
      withProvider(new CompletionProvider<LookupElement, CompletionParameters>() {
      public void addCompletions(@NotNull final CompletionParameters parameters, final MatchingContext matchingContext, @NotNull final CompletionResultSet<LookupElement> result) {
        CompletionContext context = parameters.getPosition().getUserData(CompletionContext.COMPLETION_CONTEXT_KEY);
        final PsiFile file = context.file;
        final PsiElement lastElement = file.findElementAt(context.startOffset - 1);
        PsiElement insertedElement = parameters.getPosition();
        CompletionData completionData = CompletionUtil.getCompletionDataByElement(lastElement, context.file, context.startOffset);
        context.setPrefix(insertedElement, context.startOffset, completionData);

        Set<LookupItem> lookupSet = new THashSet<LookupItem>();
        final PsiVariable variable = (PsiVariable)parameters.getPosition().getParent();
        JavaCompletionUtil.completeFieldName(lookupSet, context, variable);
        JavaCompletionUtil.completeMethodName(lookupSet, context, variable);
        result.addAllElements(lookupSet);
      }
    });
    registrar.extendBasicCompletion(PsiJavaPatterns.psiElement().nameIdentifierOf(PsiJavaPatterns.psiMethod().withParent(PsiClass.class))).
      withId(METHOD_NAME).dependingOn(LegacyCompletionContributor.LEGACY).
      withProvider(new CompletionProvider<LookupElement, CompletionParameters>() {
      public void addCompletions(@NotNull final CompletionParameters parameters, final MatchingContext matchingContext, @NotNull final CompletionResultSet<LookupElement> result) {
        CompletionContext context = parameters.getPosition().getUserData(CompletionContext.COMPLETION_CONTEXT_KEY);
        final PsiFile file = context.file;
        final PsiElement lastElement = file.findElementAt(context.startOffset - 1);
        PsiElement insertedElement = parameters.getPosition();
        CompletionData completionData = CompletionUtil.getCompletionDataByElement(lastElement, context.file, context.startOffset);
        context.setPrefix(insertedElement, context.startOffset, completionData);

        Set<LookupItem> lookupSet = new THashSet<LookupItem>();
        JavaCompletionUtil.completeMethodName(lookupSet, context, parameters.getPosition().getParent());
        result.addAllElements(lookupSet);
      }
    });

    registrar.extendBasicCompletion(psiElement()).dependingOn(LegacyCompletionContributor.LEGACY, VARIABLE_NAME, METHOD_NAME).withProvider(new CompletionProvider<LookupElement, CompletionParameters>() {
      public void addCompletions(@NotNull final CompletionParameters parameters, final MatchingContext context, @NotNull final CompletionResultSet<LookupElement> result) {
        result.processResults(new Processor<LookupElement>() {
          public boolean process(final LookupElement lookupElement) {
            LookupItem item = (LookupItem) lookupElement;
            CompletionUtil.highlightMemberOfContainer(item);

            if (item.getInsertHandler() != null) return true;

            item.setAttribute(LookupItem.INSERT_HANDLER_ATTR, new InsertHandler() {
              public void handleInsert(final CompletionContext context, final int startOffset, final LookupData data, final LookupItem item,
                                       final boolean signatureSelected,
                                       final char completionChar) {
                analyzeItem(context, item, item.getObject(), parameters.getPosition());
                new DefaultInsertHandler().handleInsert(context, startOffset, data, item, signatureSelected, completionChar);
              }
            });
            return true;
          }
        });
      }
    });

    registrar.extendSmartCompletion(psiElement()).withId(JAVA_LEGACY).withProvider(new CompletionProvider<LookupElement, CompletionParameters>() {
      public void addCompletions(@NotNull final CompletionParameters parameters, final MatchingContext matchingContext, @NotNull final CompletionResultSet<LookupElement> result) {
        final Set<LookupItem> set = new LinkedHashSet<LookupItem>();
        final PsiElement identifierCopy = parameters.getPosition();
        final Set<CompletionVariant> keywordVariants = new HashSet<CompletionVariant>();
        final CompletionContext context = parameters.getPosition().getUserData(CompletionContext.COMPLETION_CONTEXT_KEY);

        final PsiReference ref = identifierCopy.getContainingFile().findReferenceAt(identifierCopy.getTextRange().getStartOffset());
        if (ref != null) {
          SMART_DATA.completeReference(ref, set, context, identifierCopy, result.getPrefixMatcher());
        }
        SMART_DATA.addKeywordVariants(keywordVariants, context, identifierCopy);
        CompletionData.completeKeywordsBySet(set, keywordVariants, context, identifierCopy, result.getPrefixMatcher());
        CompletionUtil.highlightMembersOfContainer(set);
        result.addAllElements(set);
      }
    });

    registrar.extendClassNameCompletion(PlatformPatterns.psiElement()).withId(JAVA_LEGACY).withProvider(new CompletionProvider<LookupElement, CompletionParameters>() {
      public void addCompletions(@NotNull final CompletionParameters parameters, final MatchingContext matchingContext, @NotNull final CompletionResultSet<LookupElement> result) {
        CompletionContext context = parameters.getPosition().getUserData(CompletionContext.COMPLETION_CONTEXT_KEY);
        final PsiFile file = context.file;
        PsiElement insertedElement = parameters.getPosition();
        CompletionData completionData = CLASS_NAME_DATA;
        context.setPrefix(insertedElement, context.startOffset, completionData);

        final Set<LookupItem> lookupSet = new LinkedHashSet<LookupItem>();
        final PsiReference ref = insertedElement.getContainingFile().findReferenceAt(context.offset);
        if (ref != null) {
          completionData.completeReference(ref, lookupSet, context, insertedElement, result.getPrefixMatcher());
        }
        if (lookupSet.isEmpty() || !CodeInsightUtil.isAntFile(file)) {
          final Set<CompletionVariant> keywordVariants = new HashSet<CompletionVariant>();
          completionData.addKeywordVariants(keywordVariants, context, insertedElement);
          CompletionData.completeKeywordsBySet(lookupSet, keywordVariants, context, insertedElement, result.getPrefixMatcher());
        }
        result.addAllElements(lookupSet);
      }
    });


  }

  private static void analyzeItem(final CompletionContext context, final LookupItem item, final Object completion, final PsiElement position) {
    if(completion instanceof PsiKeyword){
      if(PsiKeyword.BREAK.equals(((PsiKeyword)completion).getText())
         || PsiKeyword.CONTINUE.equals(((PsiKeyword)completion).getText())){
        PsiElement scope = position;
        while(true){
          if (scope instanceof PsiFile
              || scope instanceof PsiMethod
              || scope instanceof PsiClassInitializer){
            item.setTailType(TailType.SEMICOLON);
            break;
          }
          else if (scope instanceof PsiLabeledStatement){
            item.setTailType(TailType.NONE);
            break;
          }
          scope = scope.getParent();
        }
      }
      if(PsiKeyword.RETURN.equals(((PsiKeyword)completion).getText())){
        PsiElement scope = position;
        while(true){
          if (scope instanceof PsiFile
              || scope instanceof PsiClassInitializer){
            item.setTailType(TailType.NONE);
            break;
          }
          else if (scope instanceof PsiMethod){
            final PsiMethod method = (PsiMethod)scope;
            if(method.isConstructor() || PsiType.VOID == method.getReturnType()) {
              item.setTailType(TailType.SEMICOLON);
            }
            else item.setTailType(TailType.SPACE);

            break;
          }
          scope = scope.getParent();
        }
      }
      if(PsiKeyword.DEFAULT.equals(((PsiKeyword)completion).getText())){
        if (!(position.getParent() instanceof PsiAnnotationMethod)) {
          item.setTailType(TailType.CASE_COLON);
        }
      }
      if(PsiKeyword.SYNCHRONIZED.equals(((PsiKeyword)completion).getText())){
        if (PsiTreeUtil.getParentOfType(position, PsiMember.class, PsiCodeBlock.class) instanceof PsiCodeBlock){
          item.setTailType(TailTypes.SYNCHRONIZED_LPARENTH);
        }
      }
    }
    if (completion instanceof PsiClass) {
      final PsiElement prevElement = FilterUtil.searchNonSpaceNonCommentBack(position);
      if (prevElement != null && prevElement.getParent() instanceof PsiNewExpression) {
        ExpectedTypeInfo[] infos = ExpectedTypesProvider.getInstance(context.project).getExpectedTypes((PsiExpression) prevElement.getParent(), true);
        boolean flag = true;
        PsiTypeParameter[] typeParameters = ((PsiClass)completion).getTypeParameters();
        for (ExpectedTypeInfo info : infos) {
          final PsiType type = info.getType();

          if (info.isArrayTypeInfo()) {
            flag = false;
            break;
          }
          if (typeParameters.length > 0 && type instanceof PsiClassType) {
            if (!((PsiClassType)type).isRaw()) {
              flag = false;
            }
          }
        }
        if (flag) {
          item.setAttribute(LookupItem.NEW_OBJECT_ATTR, "");
        }
      }
    }

    if (completion instanceof PsiElement &&
        CompletionUtil.isCompletionOfAnnotationMethod((PsiElement)completion, position)) {
      item.setTailType(TailType.EQ);
    }
  }


}