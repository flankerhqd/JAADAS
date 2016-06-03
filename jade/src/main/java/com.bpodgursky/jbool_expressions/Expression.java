package com.bpodgursky.jbool_expressions;

import com.bpodgursky.jbool_expressions.rules.Rule;

import java.util.List;

public abstract class Expression<K> implements Comparable<Expression> {

  @Override
  public int compareTo(Expression o) {
    return toString().compareTo(o.toString());
  }

  @Override
  public boolean equals(Object o){
    return o instanceof Expression && equals((Expression) o);
  }

  @Override
  public int hashCode(){
    return toString().hashCode();
  }

  public abstract Expression<K> apply(List<Rule<?, K>> rules);

  public abstract boolean equals(Expression expr);

  public abstract String getExprType();
  
  public abstract Expression<K> makeCopy();
}
