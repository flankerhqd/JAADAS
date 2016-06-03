package com.bpodgursky.jbool_expressions.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class QuotedMapper<T> implements TokenMapper<T> {

  private static final Pattern QUOTED = Pattern.compile("'([^\"])+'");

  @Override
  public T getVariable(String name) {
    Matcher m = QUOTED.matcher(name);
    if(m.matches()){
      return getValue(m.group(1));
    }else{
      throw new RuntimeException("Invalid variable name: "+name);
    }
  }

  public abstract T getValue(String name);
}
