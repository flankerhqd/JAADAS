package com.bpodgursky.jbool_expressions;

import java.util.*;

public class ExprUtil {

  public static <K> Expression<K>[] allExceptMatch(Expression<K>[] exprs, Expression<K> omit){
    Set<Expression<K>> andTerms = new HashSet<Expression<K>>();
    for(Expression<K> eachExpr: exprs){
      if(!eachExpr.equals(omit)){
        andTerms.add(eachExpr);
      }
    }

    int i = 0;
    Expression<K>[] result = expr(andTerms.size());
    for(Expression<K> expr: andTerms){
      result[i++] = expr;
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public static <K> Expression<K>[] expr(int length){
    return new Expression[length];
  }

  public static <K> void addAll(Collection<Expression<K>> set, Expression<K>[] values){
    Collections.addAll(set, values);
  }

  @SuppressWarnings("unchecked")
  public static <K> List<Expression<K>> list(Expression... exprs){
    return Arrays.<Expression<K>>asList(exprs);
  }

  public static <K> Set<K> getVariables(Expression<K> expr){
    if(expr instanceof Variable){
      return Collections.singleton(((Variable<K>) expr).getValue());
    }else if(expr instanceof Not){
      return getVariables(((Not<K>) expr).getE());
    }else if(expr instanceof NExpression){
      Set<K> vars = new HashSet<K>();
      for(Expression<K> child: ((NExpression<K>)expr).expressions){
        vars.addAll(getVariables(child));
      }
      return vars;
    }
    return Collections.emptySet();
  }
}
