package com.bpodgursky.jbool_expressions.eval;

import com.bpodgursky.jbool_expressions.Expression;

import java.util.Map;

public abstract class EvalRule<K> {
  public abstract boolean evaluate(Expression<K> expression, Map<String, EvalRule<K>> rules);

  protected static <K> boolean evaluateInternal(Expression<K> expression, Map<String, EvalRule<K>> rules){
    EvalRule<K> rule = rules.get(expression.getExprType());
    if(rule == null){
      throw new RuntimeException("no evaluation rule found for expression type: "+expression.getExprType()+"!");
    }

    return rule.evaluate(expression, rules);
  }
}
