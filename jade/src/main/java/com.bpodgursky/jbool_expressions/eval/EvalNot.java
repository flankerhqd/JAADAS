package com.bpodgursky.jbool_expressions.eval;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Not;

import java.util.Map;

public class EvalNot<K> extends EvalRule<K> {
  @Override
  public boolean evaluate(Expression<K> expression, Map<String, EvalRule<K>> rules) {
    Not<K> not = (Not<K>) expression;
    return !evaluateInternal(not.getE(), rules);
  }
}
