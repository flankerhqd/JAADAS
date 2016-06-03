package com.bpodgursky.jbool_expressions;

import com.bpodgursky.jbool_expressions.rules.Rule;

import java.util.List;

public class Literal<K> extends Expression<K>{
  public static final String EXPR_TYPE = "literal";

  private final boolean value;

  private static final Literal TRUE = new Literal(true);
  private static final Literal FALSE = new Literal(false);

  @SuppressWarnings("unchecked")
  public static <V> Literal<V> getTrue(){
    return TRUE;
  }

  @SuppressWarnings("unchecked")
  public static <V> Literal<V> getFalse(){
    return FALSE;
  }

  public static <K> Literal<K> of(boolean value){
    if(value){
      return getTrue();
    }else{
      return getFalse();
    }
  }

  private Literal(boolean value){
    this.value = value;
  }

  public String toString(){
    return Boolean.valueOf(value).toString();
  }

  public boolean getValue(){
    return value;
  }

  @Override
  public Expression<K> apply(List<Rule<?, K>> rules) {
    return this;
  }

  @Override
  public boolean equals(Expression expr) {
    return expr instanceof Literal && ((Literal)expr).getValue() == getValue();
  }

  @Override
  public String getExprType() {
    return EXPR_TYPE;
  }

@Override
public Expression<K> makeCopy() {
	return of(this.value);
}
}
