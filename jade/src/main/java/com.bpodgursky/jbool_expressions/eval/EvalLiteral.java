package com.bpodgursky.jbool_expressions.eval;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Literal;

import java.util.Map;

public class EvalLiteral<K> extends EvalRule<K> {
  @Override
  public boolean evaluate(Expression<K> expression, Map<String, EvalRule<K>> rules) {
    Literal<K> literal = (Literal<K>) expression;
    return literal.getValue();
  }
}
