package com.craftinginterpreters.lox;

import java.util.List;
import static com.craftinginterpreters.lox.TokenType.*;

/*
A parser really has two jobs:

1) Given a valid sequence of tokens, produce a corresponding syntax tree.

2) Given an invalid sequence of tokens, detect any errors and tell the user about their mistakes.
*/

//Below is a syntax tree for -123*(45.67)
//            *
//           /  \
//          -   ()
//          /    \
//         123   45.67

//The parser below is a recursive descent parser (RDP), meaning top to bottom in below table
// i.e. lowest precedence(expression) to highest(primary)


public class Parser {
// Lowest Precedence: //    expression
                      //    equality
                      //    comparison
                      //    term
                      //    factor
                      //    unary
// Highest Precedence //    primary

/******* Complete expression grammar ********************************/
//    expression     → equality ;
//    equality       → comparison ( ( "!=" | "==" ) comparison )* ;
//    comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
//    term           → factor ( ( "-" | "+" ) factor )* ;
//    factor         → unary ( ( "/" | "*" ) unary )* ;
//    unary          → ( "!" | "-" ) unary
//                     | primary ;
//    primary        → NUMBER | STRING | "true" | "false" | "nil"
//            | "(" expression ")" ;
/*  ******************************************************************/

/*    unary can match to unary and primary
    factor can match to factor and unary(covers primary as well)
            .
            .
            .
    expression can match to expression, equality, comparison, term all the way down to primary
            i.e effectively expression matches to equality and equality covers the rest*/


    private static class ParseError extends RuntimeException{}
    private final List<Token> tokens;
    private static int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse(){
        try{
            return expression();
        }
        catch(ParseError error){
            return null;
        }
    }

    private Expr expression(){
        return equality();
    }

    private Expr equality(){
        // equality -> comparison (("!=" | "==") comparison)*
        // eg. a==b!=c==d==f!=g ...
        Expr expr = comparison();

        while(match(BANG_EQUAL, EQUAL_EQUAL)){
            Token operator = previous(); //during match() you'll advance once so that's why we use previous()
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr comparison(){
//        comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
        Expr expr = term();
        while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)){
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term(){
//        term  → factor ( ( "-" | "+" ) factor )* ;
        Expr expr = factor();
        while(match(MINUS, PLUS)){
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor(){
//        factor  → unary ( ( "/" | "*" ) unary )*
        Expr expr = unary();
        while(match(SLASH, STAR)){
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary(){
//        unary → ( "!" | "-" ) unary | primary ;
        if(match(BANG, MINUS)){
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    private Expr primary(){
//        primary        → NUMBER | STRING | "true" | "false" | "nil"
//            | "(" expression ")" ;
        if(match(FALSE)) return new Expr.Literal(false);
        if(match(TRUE)) return new Expr.Literal(true);
        if(match(NIL)) return new Expr.Literal(null);

        if(match(NUMBER, STRING)){
            return new Expr.Literal(previous().literal);
        }

        if(match(LEFT_PAREN)){
            Expr expr = expression();
            consume(RIGHT_PAREN, "Except ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");
    }

    private Token consume(TokenType type, String message){
        if(check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message){
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize(){
        advance();
        while(!isAtEnd()){
            if(previous().type == SEMICOLON) return;

            //TODO(Rijubak): Figure out what this switch statement is for? What does it do?
            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }

    private boolean match(TokenType... types){
        // check if the current token has any of the given types
        // if yes, consume the token and return true,
        // if not, leave the current token alone and return false
        for(TokenType type : types){
            if(check(type)){
                advance();
                return true;
            }
        }
        return false;
    }

    private Token advance(){
        // essentially return the current token and advance onto the next one
        if(!isAtEnd()) current++;
        return previous();
    }

    private boolean check(TokenType type){
        // check if current tokentype matches the type passed to check()
        if(isAtEnd()) return false;
        return peek().type == type;
    }

    private boolean isAtEnd(){
        // Check if you are at the last element of List<Tokens>
        // the last token will always be EOF
        return peek().type == EOF;
    }
    private Token peek(){
        // get the current token
        return tokens.get(current);
    }

    private Token previous(){
        // get the token one position before the current
        return tokens.get(current-1);
    }

}
