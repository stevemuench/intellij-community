package com.intellij.debugger.ui.tree.render;

import com.intellij.debugger.ClassFilter;
import com.intellij.debugger.DebuggerContext;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.StackFrameContext;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.ui.tree.DebuggerTreeNode;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.ValueDescriptor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.PsiExpression;
import com.sun.jdi.*;
import org.jdom.Element;

import java.util.Iterator;

/*
 * Copyright (c) 2000-2004 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */

public class ToStringRenderer extends NodeRendererImpl {
  public static final String UNIQUE_ID = "ToStringRenderer";

  private boolean USE_CLASS_FILTERS = false;
  private ClassFilter[] myClassFilters = ClassFilter.EMPTY_ARRAY;

  public ToStringRenderer() {
    setEnabled(true);
  }

  public String getUniqueId() {
    return UNIQUE_ID;
  }

  public String getName() {
    return "toString";
  }

  public void setName(String name) {
    // prohibit change
  }

  public ToStringRenderer clone() {
    final ToStringRenderer cloned = (ToStringRenderer)super.clone();
    final ClassFilter[] classFilters = (myClassFilters.length > 0)? new ClassFilter[myClassFilters.length] : ClassFilter.EMPTY_ARRAY;
    for (int idx = 0; idx < classFilters.length; idx++) {
      classFilters[idx] = myClassFilters[idx].clone();
    }
    cloned.myClassFilters = classFilters;
    return cloned;
  }

  public String calcLabel(final ValueDescriptor valueDescriptor, EvaluationContext evaluationContext, final DescriptorLabelListener labelListener)
    throws EvaluateException {
    Value value = valueDescriptor.getValue();
    BatchEvaluator.getBatchEvaluator(evaluationContext.getDebugProcess()).invoke(new ToStringCommand(evaluationContext, value) {
      public void evaluationResult(String message) {
        valueDescriptor.setValueLabel(message != null ? "\"" + message + "\"" : "");
        labelListener.labelChanged();
      }

      public void evaluationError(String message) {
        valueDescriptor.setValueLabelFailed(new EvaluateException(message + " Failed to evaluate toString() for this object", null));
        labelListener.labelChanged();
      }
    });
    return NodeDescriptor.EVALUATING_MESSAGE;
  }

  public boolean isUseClassFilters() {
    return USE_CLASS_FILTERS;
  }

  public void setUseClassFilters(boolean value) {
    USE_CLASS_FILTERS = value;
  }

  public boolean isApplicable(Type type) {
    if(!(type instanceof ReferenceType)) {
      return false;
    }

    if(type.name().equals("java.lang.String")) {
      return false; // do not render 'String' objects for performance reasons
    }

    if(!overridesToString(type)) {
      return false;
    }

    if (USE_CLASS_FILTERS) {
      if (!isFiltered(type)) {
        return false;
      }
    }

    return true;
  }

  private static boolean overridesToString(Type type) {
    if(type instanceof ClassType) {
      final ClassType classType = (ClassType)type;
      final java.util.List methods = classType.methodsByName("toString", "()Ljava/lang/String;");
      if (methods.size() > 0) {
        for (Iterator iterator = methods.iterator(); iterator.hasNext();) {
          final Method method = (Method)iterator.next();
          if(!(method.declaringType().name()).equals("java.lang.Object")){
            return true;
          }
        }
      }
    }
    return false;
  }

  public void buildChildren(Value value, ChildrenBuilder builder, EvaluationContext evaluationContext) {
    getDefaultRenderer(value, evaluationContext).buildChildren(value, builder, evaluationContext);
  }

  private static NodeRenderer getDefaultRenderer(Value value, StackFrameContext context) {
    Type type = value != null ? value.type() : null;
    return ((DebugProcessImpl)context.getDebugProcess()).getDefaultRenderer(type);
  }

  public PsiExpression getChildValueExpression(DebuggerTreeNode node, DebuggerContext context) throws EvaluateException {
    return getDefaultRenderer(((ValueDescriptor) node.getDescriptor()).getValue(), context).getChildValueExpression(node, context);
  }

  public boolean isExpandable(Value value, EvaluationContext evaluationContext, NodeDescriptor parentDescriptor) {
    return getDefaultRenderer(value, evaluationContext).isExpandable(value, evaluationContext, parentDescriptor);
  }

  public void readExternal(Element element) throws InvalidDataException {
    super.readExternal(element);
    final String value = JDOMExternalizerUtil.readField(element, "USE_CLASS_FILTERS");
    USE_CLASS_FILTERS = "true".equalsIgnoreCase(value);
    myClassFilters = DebuggerUtilsEx.readFilters(element.getChildren("filter"));
  }

  public void writeExternal(Element element) throws WriteExternalException {
    super.writeExternal(element);
    JDOMExternalizerUtil.writeField(element, "USE_CLASS_FILTERS", USE_CLASS_FILTERS? "true" : "false");
    DebuggerUtilsEx.writeFilters(element, "filter", myClassFilters);
  }

  public ClassFilter[] getClassFilters() {
    return myClassFilters;
  }

  public void setClassFilters(ClassFilter[] classFilters) {
    myClassFilters = classFilters != null? classFilters : ClassFilter.EMPTY_ARRAY;
  }

  private boolean isFiltered(Type t) {
    if (t instanceof ReferenceType) {
      for (int i = 0; i < myClassFilters.length; i++) {
        ClassFilter classFilter = myClassFilters[i];
        if(classFilter.isEnabled() && DebuggerUtilsEx.getSuperType(t, classFilter.getPattern()) != null) {
          return true;
        }
      }
    }
    return DebuggerUtilsEx.isFiltered(t.name(), myClassFilters);
  }
}
