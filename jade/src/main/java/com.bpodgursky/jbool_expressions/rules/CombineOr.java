package com.bpodgursky.jbool_expressions.rules;

import com.google.common.collect.Lists;
import com.bpodgursky.jbool_expressions.ExprUtil;
import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Or;

import java.util.List;

public class CombineOr<K> extends Rule<Or<K>, K> {

  @Override
  public Expression<K> applyInternal(Or<K> or) {
    for (Expression<K> expr : or.expressions) {
      if (expr instanceof Or) {
        Or<K> childAnd = (Or<K>) expr;

        List<Expression<K>> newChildren = Lists.newArrayList();
        ExprUtil.addAll(newChildren, ExprUtil.allExceptMatch(or.expressions, childAnd));
        ExprUtil.addAll(newChildren, childAnd.expressions);

        return Or.of(newChildren);
      }
    }
    return or;
  }

  @Override
  protected boolean isApply(Expression input) {
    return input instanceof Or;
  }

}
