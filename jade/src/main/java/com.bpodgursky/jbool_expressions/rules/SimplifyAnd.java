package com.bpodgursky.jbool_expressions.rules;

import com.google.common.collect.Lists;
import com.bpodgursky.jbool_expressions.*;

import java.util.*;

public class SimplifyAnd<K> extends Rule<And<K>, K> {

  @Override
  public Expression<K> applyInternal(And<K> input) {

    for (Expression<K> expr : input.expressions) {
      if (expr instanceof Literal) {
        Literal l = (Literal) expr;

        if (l.getValue()) {
          return copyWithoutTrue(input);
        } else {
          return Literal.getFalse();
        }
      }

      //  succeed immediately if require something or its opposite
      if( expr instanceof Not){
        Expression<K> notChild = ((Not<K>)expr).getE();
        for(Expression<K> child: input.expressions){
          if(child.equals(notChild)){
            return Literal.getFalse();
          }
        }
      }
    }

    return input;
  }

  private Expression<K> copyWithoutTrue(And<K> input){
    List<Expression<K>> copy = Lists.newArrayList();
    for (Expression<K> expr : input.expressions) {
      if (expr instanceof Literal) {
        Literal l = (Literal) expr;

        if (l.getValue()) {
          continue;
        }
      }
      copy.add(expr);
    }

    if (copy.isEmpty()) {
      return Literal.getTrue();
    }

    return And.of(copy);
  }

  @Override
  protected boolean isApply(Expression<K> input) {
    return input instanceof And;
  }
}
