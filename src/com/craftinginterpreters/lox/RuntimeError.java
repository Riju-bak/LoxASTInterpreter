package com.craftinginterpreters.lox;

public class RuntimeError extends RuntimeException{
    final Token token;

    RuntimeError(Token token, String message){
        super(message);
        this.token = token; //storing the token, but how does this info reach the user?
    }
}
