package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.patterns.MatchingContext;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlExtension;
import com.intellij.xml.impl.schema.AnyXmlElementDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Dmitry Avdeev
 */
public class XmlCompletionContributor extends CompletionContributor{
  public void registerCompletionProviders(final CompletionRegistrar registrar) {
    registrar.extendClassNameCompletion(PlatformPatterns.psiElement(XmlTokenType.XML_NAME)).
      dependingOn(JavaCompletionContributor.JAVA_LEGACY).
      withProvider(new CompletionProvider<LookupElement, CompletionParameters>() {
      public void addCompletions(@NotNull final CompletionParameters parameters, final MatchingContext matchingContext, @NotNull final CompletionResultSet<LookupElement> result) {
        result.clearResults();
        CompletionContext context = parameters.getPosition().getUserData(CompletionContext.COMPLETION_CONTEXT_KEY);
        final PsiElement element = parameters.getPosition();
        final XmlTag parent = (XmlTag)element.getParent();
        final PsiReference reference = parent.getReference();
        final String namespace = parent.getNamespace();
        final XmlElementDescriptor parentDescriptor = parent.getDescriptor();
        final String prefix = context.getPrefix();
        final int pos = prefix.indexOf(':');
        final String namespacePrefix = pos > 0 ? prefix.substring(0, pos) : null;

        if (reference != null && namespace.length() > 0 && parentDescriptor != null && !(parentDescriptor instanceof AnyXmlElementDescriptor)) {
          final Set<LookupItem> set = new HashSet<LookupItem>();
          new XmlCompletionData().completeReference(reference, set, context, element, result.getPrefixMatcher());
          result.addAllElements(set);
        } else {

          result.setPrefixMatcher(pos >= 0 ? prefix.substring(pos + 1) : prefix);

          final XmlFile file = (XmlFile)parameters.getOriginalFile();
          final XmlExtension extension = XmlExtension.getExtension(file);
          final Set<String> names = extension.getAvailableTagNames(file, parent);
          if (names.isEmpty()) {
            return;
          }
          for (String name : names) {
            if (result.getPrefixMatcher().prefixMatches(name)) {
              final LookupItem item = new LookupItem<String>(name, name);
              final XmlTagInsertHandler insertHandler = new ExtendedTagInsertHandler(name, namespacePrefix);
              item.setAttribute(LookupItem.INSERT_HANDLER_ATTR, insertHandler);
              final Set<String> namespaces = extension.getNamespacesByTagName(name, file);
              if (namespaces.size() > 0) {
                item.setAttribute(LookupItem.TAIL_TEXT_ATTR, " (" + namespaces.iterator().next() + ")");
                item.setAttribute(LookupItem.TAIL_TEXT_SMALL_ATTR, "");
              }
              result.addElement(item);
            }
          }
        }
      }
    });
  }

}
