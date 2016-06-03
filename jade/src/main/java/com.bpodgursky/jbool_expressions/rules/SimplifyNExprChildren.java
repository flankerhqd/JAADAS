package com.bpodgursky.jbool_expressions.rules;

import com.bpodgursky.jbool_expressions.And;
import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.NExpression;
import com.bpodgursky.jbool_expressions.Or;
import com.google.common.collect.Lists;

import java.util.List;

//  a | (a & b) = a, and the like
public class SimplifyNExprChildren<K> extends Rule<NExpression<K>, K> {

  @Override
  public Expression<K> applyInternal(NExpression<K> input) {

    //  for each child of the or

    for (int i = 0; i < input.expressions.length; i++) {
      Expression<K> child1 = input.expressions[i];

      for (int j = 0; j < input.expressions.length; j++) {
        Expression<K> child2 = input.expressions[j];

        // if child2 is true whenever child1 is true, return without child1
        if(i != j){
          if(checkExprSubset(child1, child2, input)){
            return removeChild(input, i);
          }
        }
      }
    }

    return input;
  }

  private boolean checkContains(NExpression expr, Expression toCheck){
    for(int i = 0; i < expr.expressions.length; i++){
      Expression child = expr.expressions[i];

      if(child.equals(toCheck)){
        return true;
      }
    }

    return false;
  }

  private boolean checkContainsAllChildren(NExpression expr1, NExpression toCheck){

    int i = 0;
    int j = 0;

    while(i < expr1.expressions.length && j < toCheck.expressions.length){
      if(expr1.expressions[i].equals(toCheck.expressions[j])){
        j++;
      }
      i++;
    }

    return j == toCheck.expressions.length;
  }

  //  return true if we know that expr is always true when exprCheckSubset is true
  //  TODO this is really naive so far... probably a smarter way to check here
  private boolean checkExprSubset(Expression<K> expr, Expression<K> exprCheckSubset, Expression<K> parent){

    if(expr.equals(exprCheckSubset)){
      return true;
    }

    //  (a | b) & (a | b | c)
    if(expr instanceof Or && exprCheckSubset instanceof Or && parent instanceof And){
      return checkContainsAllChildren((Or) expr, (Or) exprCheckSubset);
    }

    //  (a & b) | (a & b & c)
    else if(expr instanceof And && exprCheckSubset instanceof And && parent instanceof Or){
      return checkContainsAllChildren((And) expr, (And) exprCheckSubset);
    }

    //  a | (a & b & c)
    //  a & (a | b | c)
    //  !a & (!a | b | c)
    else if(expr instanceof And && parent instanceof Or || expr instanceof Or && parent instanceof And){
      return checkContains((NExpression) expr, exprCheckSubset);
    }

    return false;
  }

  private NExpression<K> removeChild(NExpression<K> node, int index) {

    List<Expression<K>> copy = Lists.newArrayList();
    for (int i = 0; i < node.expressions.length; i++) {
      if (i != index) {
        copy.add(node.expressions[i]);
      }
    }

    //  TODO factory probably
    if(node instanceof And){
      return And.of(copy);
    }

    if(node instanceof Or){
      return Or.of(copy);
    }

    throw new RuntimeException("Unknown child of NExpression");
  }

  @Override
  protected boolean isApply(Expression<K> input) {
    return input instanceof NExpression;
  }
}
