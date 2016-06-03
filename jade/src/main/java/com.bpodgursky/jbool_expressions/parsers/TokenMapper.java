package com.bpodgursky.jbool_expressions.parsers;

public interface TokenMapper<T> {
  public T getVariable(String name);
}
