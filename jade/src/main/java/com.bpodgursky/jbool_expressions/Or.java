package com.bpodgursky.jbool_expressions;

import org.apache.commons.lang.StringUtils;

import java.util.List;

public class Or<K> extends NExpression<K> {
  public static final String EXPR_TYPE = "or";

  private Or(List<Expression<K>> children) {
    super(children);
  }
  private Or(Expression<K>[] children) {
	    super(children);
	  }
  @Override
  protected Expression<K> createInternal(List<Expression<K>> children) {
    return new Or<K>(children);
  }

  public String toString(){
    return "("+ StringUtils.join(expressions, " | ")+")";
  }

  @Override
  public boolean equals(Expression expr) {
    if(!(expr instanceof Or)){
      return false;
    }
    Or other = (Or) expr;

    if(other.expressions.length != expressions.length){
      return false;
    }

    for(int i = 0; i < expressions.length; i++){
      if(!expressions[i].equals(other.expressions[i])){
        return false;
      }
    }

    return true;
  }

  public static <K> Or<K> of(Expression<K> child1, Expression<K> child2, Expression<K> child3, Expression<K> child4){
    return of(ExprUtil.<K>list(child1, child2, child3, child4));
  }

  public static <K> Or<K> of(Expression<K> child1, Expression<K> child2, Expression<K> child3){
    return of(ExprUtil.<K>list(child1, child2, child3));
  }

  public static <K> Or<K> of(Expression<K> child1, Expression<K> child2){
    return of(ExprUtil.<K>list(child1, child2));
  }

  public static <K> Or<K> of(List<Expression<K>> children){
    return new Or<K>(children);
  }

  @Override
  public String getExprType() {
    return EXPR_TYPE;
  }

@Override
public Expression<K> makeCopy() {
	Expression<K>[] children = new Expression[this.expressions.length];
	for (int i = 0; i < children.length; i++) {
		children[i] = this.expressions[i].makeCopy();
	}
	return new Or<K>(children);
}
}
