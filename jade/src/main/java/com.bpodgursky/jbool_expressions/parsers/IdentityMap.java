package com.bpodgursky.jbool_expressions.parsers;

public class IdentityMap implements TokenMapper<String> {
  @Override
  public String getVariable(String name) {
    return name;
  }
}
