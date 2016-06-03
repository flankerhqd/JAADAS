package com.bpodgursky.jbool_expressions.rules;

import java.util.List;
import java.util.Map;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.Not;
import com.google.common.collect.Lists;

public class RuleSet {

  public static <K> List<Rule<?, K>> simplifyRules(){
    List<Rule<?, K>> rules = Lists.newArrayList();
    rules.add(new SimplifyAnd<K>());
    rules.add(new SimplifyOr<K>());
    rules.add(new SimplifyNot<K>());
    rules.add(new CombineAnd<K>());
    rules.add(new CombineOr<K>());
    rules.add(new SimplifyNExpression<K>());
    rules.add(new SimplifyNExprChildren<K>());

    return rules;
  }

  public static <K> List<Rule<?, K>> toSopRules(){
    List<Rule<?, K>> rules = Lists.newArrayList(RuleSet.<K>simplifyRules());
    rules.add(new ToSOP<K>());
    rules.add(new DeMorgan<K>());

    return rules;
  }

  public static <K> List<Rule<?, K>> demorganRules(){
    List<Rule<?, K>> rules = Lists.newArrayList(RuleSet.<K>simplifyRules());
    rules.add(new DeMorgan<K>());

    return rules;
  }

  public static <K> Expression<K> applyAll(Expression<K> e, List<Rule<?, K>> rules){
    Expression<K> orig = e;
    Expression<K> simplified = applyAllSingle(orig, rules);

    while(!orig.equals(simplified)){
      orig = simplified;
      simplified = applyAllSingle(orig, rules);
    }

    return simplified;
  }

  private static <K> Expression<K> applyAllSingle(Expression<K> e, List<Rule<?, K>> rules){
    Expression<K> tmp = e.apply(rules);
    for(Rule<?, K> r: rules){
      tmp = r.apply(tmp);
    }
    return tmp;
  }

  public static <K> Expression<K> simplify(Expression<K> root){
    return applySet(root, RuleSet.<K>simplifyRules());
  }

  /**
   * More formal name for sum-of-products
   */
  public static <K> Expression<K> toDNF(Expression<K> root){
    return toSop(root);
  }

  /**
   * More formal name for product-of-sums
   */
  public static <K> Expression<K> toCNF(Expression<K> root){
    return toPos(root);
  }

  public static <K> Expression<K> toSop(Expression<K> root){
    return applySet(root, RuleSet.<K>toSopRules());
  }

  public static <K> Expression<K> toPos(Expression<K> root){

    //   not + simplify
    Not<K> inverse = Not.of(root);
    Expression<K> sopInv = toSop(inverse);

    //  not + demorgan
    Not<K> inverse2 = Not.of(sopInv);

    return applySet(inverse2, RuleSet.<K>demorganRules());
  }


  public static <K> Expression<K> assign(Expression<K> root, Map<K, Boolean> values){
    List<Rule<?, K>> rules = Lists.newArrayList(RuleSet.<K>simplifyRules());
    rules.add(new Assign<K>(values));
    return applySet(root, rules);
  }

  public static <K> Expression<K> applySet(Expression<K> root, List<Rule<?, K>> allRules){
    return applyAll(root, allRules);
  }
}
