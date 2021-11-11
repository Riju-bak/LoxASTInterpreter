package com.craftinginterpreters.lox;

import java.util.List;

interface LoxCallable {
//    We pass in the interpreter in case the class implementing call() needs it.
//    We also give it the list of evaluated argument values.
//    The implementer’s job is then to return the value that the call expression produces.
    Object call(Interpreter interpreter, List<Object> arguments);

    int arity();

    String toString();

}
