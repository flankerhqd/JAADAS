package com.bpodgursky.jbool_expressions.rules;

import com.google.common.collect.Lists;
import com.bpodgursky.jbool_expressions.*;

import java.util.*;

public class SimplifyOr<K> extends Rule<Or<K>, K> {

  @Override
  public Expression<K> applyInternal(Or<K> input) {

    for (Expression<K> expr : input.expressions) {
      if (expr instanceof Literal) {
        Literal l = (Literal) expr;

        //  ignore anything that is "false"
        if (!l.getValue()) {
          return copyWithoutFalse(input);
        } else {
          return Literal.of(true);
        }
      }

      //  succeed immediately if require something or its opposite
      if( expr instanceof Not){
        Expression<K> notChild = ((Not<K>)expr).getE();
        for(Expression<K> child: input.expressions){
          if(child.equals(notChild)){
            return Literal.getTrue();
          }
        }
      }
    }

    return input;
  }

  private Expression<K> copyWithoutFalse(Or<K> input){
    List<Expression<K>> copy = Lists.newArrayList();
    for (Expression<K> expr : input.expressions) {
      if (expr instanceof Literal) {
        Literal l = (Literal) expr;

        //  ignore anything that is "false"
        if (!l.getValue()) {
          continue;
        }
      }
      copy.add(expr);
    }

    if (copy.isEmpty()) {
      return Literal.of(false);
    }

    return Or.of(copy);
  }

  @Override
  protected boolean isApply(Expression<K> input) {
    return input instanceof Or;
  }
}
