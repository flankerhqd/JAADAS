package com.bpodgursky.jbool_expressions.rules;


import com.google.common.collect.Lists;
import com.bpodgursky.jbool_expressions.And;
import com.bpodgursky.jbool_expressions.ExprUtil;
import com.bpodgursky.jbool_expressions.Expression;

import java.util.List;

public class CombineAnd<K> extends Rule<And<K>, K> {

  @Override
  public Expression<K> applyInternal(And<K> and) {
    for (Expression<K> expr : and.expressions) {
      if (expr instanceof And) {
        And<K> childAnd = (And<K>) expr;

        List<Expression<K>> newChildren = Lists.newArrayList();
        ExprUtil.addAll(newChildren, ExprUtil.allExceptMatch(and.expressions, childAnd));
        ExprUtil.addAll(newChildren, childAnd.expressions);

        return And.of(newChildren);
      }
    }
    return and;
  }

  @Override
  protected boolean isApply(Expression<K> input) {
    return input instanceof And;
  }
}

