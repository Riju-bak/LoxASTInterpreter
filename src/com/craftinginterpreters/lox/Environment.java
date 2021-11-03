package com.craftinginterpreters.lox;

import java.util.Map;
import java.util.HashMap;

public class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment(){
        // The no-arg ctor is for global scope's environment, which ends the chain.
        enclosing = null;
    }

    Environment(Environment enclosing){
        this.enclosing = enclosing;
    }

    void define(String name, Object value){
        values.put(name, value);
    }

    void assign(Token name, Object value){
        // assignment is not allowed to create a new variable.
        if(values.containsKey(name.lexeme)){
            values.put(name.lexeme, value);
            return;
        }

        if(enclosing != null){
            //If the variable isn’t in this environment, it checks the outer one, recursively.
            enclosing.assign(name, value);
            return;
        }
        throw new RuntimeError(name,
                "Undefined Variable '"+name.lexeme + "'.");
    }

    Object get(Token name){
        if(values.containsKey(name.lexeme)){
            return values.get(name.lexeme);
        }

        // If the variable isn't found in this environment, we try the enclosing one.
        if(enclosing != null){
            return enclosing.get(name);
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
